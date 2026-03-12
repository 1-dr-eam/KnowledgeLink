import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader
import numpy as np
import pandas as pd
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import roc_auc_score, recall_score
from collections import defaultdict
import random

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

# ----------------------------
# 1. 数据预处理类
# ----------------------------
class FeatureProcessor:
    """
    将原始的、异构的用户和物品特征（包括 ID、离散类别、连续数值）统一转换为模型可接受的张量格式。
    """
    def __init__(self):
        self.user_id_vocab = {}  # user_id → index
        self.item_id_vocab = {}  # item_id → index
        self.user_discrete_vocab = defaultdict(dict)  # {某个离散特征col_name: {离散特征值value: 对应索引index}}
        self.item_discrete_vocab = defaultdict(dict)  # 存储离散特征的映射关系
        self.user_scaler = StandardScaler()  # 用于用户连续特征
        self.item_scaler = StandardScaler()  # 用于物品连续特征

    def build_vocab_and_scale(self, df_train, user_discrete_cols, item_discrete_cols,
                              user_continuous_cols, item_continuous_cols):
        """
        构建词典与拟合归一化器
        :param df_train:训练集 DataFrame
        :param user_discrete_cols:用户离散特征列
        :param item_discrete_cols:物品离散特征列
        :param user_continuous_cols:用户连线特征列
        :param item_continuous_cols:物品连续特征列
        """
        # 构建用户ID映射
        all_user_ids = set(df_train['user_id'])
        self.user_id_vocab = {uid: i for i, uid in enumerate(all_user_ids)} # {id:index}比如把749464映射为0(索引从0开始)

        # 构建物品ID映射
        all_item_ids = set(df_train['item_id'])
        self.item_id_vocab = {iid: i for i, iid in enumerate(all_item_ids)}

        # 构建离散特征映射
        for col in user_discrete_cols:
            unique_vals = set(df_train[col].values) # set去重
            self.user_discrete_vocab[col] = {val: i for i, val in enumerate(unique_vals)}

        for col in item_discrete_cols:
            unique_vals = set(df_train[col].values)
            self.item_discrete_vocab[col] = {val: i for i, val in enumerate(unique_vals)}

        # 拟合连续特征标准化器
        self.user_scaler.fit(df_train[user_continuous_cols].values) # fit只记录均值和标准差
        self.item_scaler.fit(df_train[item_continuous_cols].values)

    def transform_user_features(self, df_batch, user_discrete_cols, user_continuous_cols):
        # ID 特征
        user_ids = torch.tensor([self.user_id_vocab.get(x, 0) for x in df_batch['user_id']]) # id映射为索引
        # 离散特征
        user_discrete = torch.stack([
            torch.tensor([self.user_discrete_vocab[col].get(x, 0) for x in df_batch[col]]) # 离散特征值映射为索引
            for col in user_discrete_cols
        ], dim=1)
        # 连续特征归一化(均值为0，方差为1)
        user_continuous = torch.tensor(self.user_scaler.transform(df_batch[user_continuous_cols].values),
                                       dtype=torch.float32)
        return user_ids, user_discrete, user_continuous

    def transform_item_features(self, df_batch, item_discrete_cols, item_continuous_cols):
        item_ids = torch.tensor([self.item_id_vocab.get(x, 0) for x in df_batch['item_id']])
        item_discrete = torch.stack([
            torch.tensor([self.item_discrete_vocab[col].get(x, 0) for x in df_batch[col]])
            for col in item_discrete_cols
        ], dim=1)
        item_continuous = torch.tensor(self.item_scaler.transform(df_batch[item_continuous_cols].values),
                                       dtype=torch.float32)
        return item_ids, item_discrete, item_continuous


# ----------------------------
# 2. 模型定义
# ----------------------------
class TwoTowerModel(nn.Module):
    def __init__(self, n_users, n_items, user_discrete_sizes, item_discrete_sizes,
                 user_cont_dim, item_cont_dim, embed_dim=32, tower_hidden=[128, 64]):
        """
        双塔模型
        :param n_users:用户 ID 的总数（用于 Embedding 层）
        :param n_items:物品 ID 的总数（用于 Embedding 层）
        :param user_discrete_sizes:列表，每个元素是某个用户离散特征的类别数(如2表示用户性别有2类)
        :param item_discrete_sizes:同上，针对物品离散特征
        :param user_cont_dim:用户连续特征维度（如 age, income → 维度=2）
        :param item_cont_dim:同上
        :param embed_dim:所有 Embedding 层的输出维度
        :param tower_hidden:每个塔内部 MLP 的隐藏层结构
        """
        super().__init__()
        self.embed_dim = embed_dim

        # 用户塔嵌入层
        self.user_id_embed = nn.Embedding(n_users, embed_dim) # (词典大小，输出向量维度)
        self.user_discrete_embeds = nn.ModuleList([
            nn.Embedding(size, embed_dim) for size in user_discrete_sizes # 一个离散特征一个Embedding层
        ])
        user_input_dim = embed_dim + len(user_discrete_sizes) * embed_dim + user_cont_dim # concatenate之后的输入向量维度

        # 物品塔嵌入层
        self.item_id_embed = nn.Embedding(n_items, embed_dim)
        self.item_discrete_embeds = nn.ModuleList([
            nn.Embedding(size, embed_dim) for size in item_discrete_sizes
        ])
        item_input_dim = embed_dim + len(item_discrete_sizes) * embed_dim + item_cont_dim

        # 用户塔
        layers = []
        prev_dim = user_input_dim
        for h in tower_hidden:
            layers.extend([nn.Linear(prev_dim, h), nn.ReLU()])
            prev_dim = h
        layers.append(nn.Linear(prev_dim, embed_dim))
        self.user_tower = nn.Sequential(*layers)

        # 物品塔
        layers = []
        prev_dim = item_input_dim
        for h in tower_hidden:
            layers.extend([nn.Linear(prev_dim, h), nn.ReLU()])
            prev_dim = h
        layers.append(nn.Linear(prev_dim, embed_dim))
        self.item_tower = nn.Sequential(*layers)

    def forward_user(self, user_ids, user_discrete, user_continuous):
        """
        用户塔前向传播
        :param user_ids:[B]
        :param user_discrete:[B, num_user_discrete_cols]
        :param user_continuous:[B, user_cont_dim]
        :return:用户塔输出用户特征向量
        """
        emb_list = [self.user_id_embed(user_ids)]
        # 遍历每个离散特征列
        for i in range(user_discrete.size(1)):
            emb_list.append(self.user_discrete_embeds[i](user_discrete[:, i]))
        emb_list.append(user_continuous)
        concat_feat = torch.cat(emb_list, dim=1) # 针对batch中的每个用户，cat其id，离散特征和连续特征
        u_out = self.user_tower(concat_feat)
        return nn.functional.normalize(u_out, p=2, dim=1)  # L2 归一化

    def forward_item(self, item_ids, item_discrete, item_continuous):
        """
        物品塔前向传播
        :param item_ids:[B]
        :param item_discrete:[B, num_item_discrete_cols]
        :param item_continuous:[B, item_cont_dim]
        :return:物品塔输出物品特征向量
        """
        emb_list = [self.item_id_embed(item_ids)]
        for i in range(item_discrete.size(1)):
            emb_list.append(self.item_discrete_embeds[i](item_discrete[:, i]))
        emb_list.append(item_continuous)
        concat_feat = torch.cat(emb_list, dim=1)
        i_out = self.item_tower(concat_feat)
        return nn.functional.normalize(i_out, p=2, dim=1)

    def forward(self, user_ids, user_discrete, user_continuous, item_ids, item_discrete, item_continuous):
        """
        整体前向传播
        :params 与 forward_user和 forward_item 相同
        :return:用户与物品的余弦相似度
        """
        u_vec = self.forward_user(user_ids, user_discrete, user_continuous)
        i_vec = self.forward_item(item_ids, item_discrete, item_continuous)
        # 余弦相似度
        cos_sim = torch.sum(u_vec * i_vec, dim=1)
        return cos_sim


# ----------------------------
# 3. 数据集类（支持负采样 1:2）
# ----------------------------
class TwoTowerDataset(Dataset):
    def __init__(self, df_all, processor, user_discrete_cols, item_discrete_cols,
                 user_continuous_cols, item_continuous_cols, n_neg=2):
        """
        用于双塔模型的训练数据加载，核心功能是：
        正负样本构造,按 1:2 比例进行采样
        调用 FeatureProcessor 将原始样本转为模型输入张量
        :param df_all:包含所有正样本的 DataFrame（每行是一个用户-物品交互）
        :param processor:已训练好的 FeatureProcessor 实例
        :param *cols: 各类特征列名列表,与 FeatureProcessor 相同
        :param n_neg:每个正样本对应的负样本数量
        """
        self.df_all = df_all.reset_index(drop=True)
        self.processor = processor
        self.user_discrete_cols = user_discrete_cols
        self.item_discrete_cols = item_discrete_cols
        self.user_continuous_cols = user_continuous_cols
        self.item_continuous_cols = item_continuous_cols
        self.n_neg = n_neg
        self.all_item_ids = list(processor.item_id_vocab.values())

    def __len__(self):
        return len(self.df_all)

    def __getitem__(self, idx):
        row = self.df_all.iloc[idx]

        # 正样本特征
        pos_user_ids, pos_user_disc, pos_user_cont = self.processor.transform_user_features(
            pd.DataFrame([row]), self.user_discrete_cols, self.user_continuous_cols)
        pos_item_ids, pos_item_disc, pos_item_cont = self.processor.transform_item_features(
            pd.DataFrame([row]), self.item_discrete_cols, self.item_continuous_cols)

        # 负采样:从所有物品中随机挑，作为负样本
        neg_samples = []
        for _ in range(self.n_neg):
            neg_item_id = random.choice(self.all_item_ids)
            neg_row = row.copy()
            neg_row['item_id'] = list(self.processor.item_id_vocab.keys())[neg_item_id]
            neg_item_ids, neg_item_disc, neg_item_cont = self.processor.transform_item_features(
                pd.DataFrame([neg_row]), self.item_discrete_cols, self.item_continuous_cols)
            neg_samples.append((neg_item_ids, neg_item_disc, neg_item_cont))

        return {
            'pos': (pos_user_ids, pos_user_disc, pos_user_cont, pos_item_ids, pos_item_disc, pos_item_cont),
            'neg': neg_samples,
        }


# 数据样本转tensor
def collate_fn(batch):
    """
    将一批样本（每个含1正+2负）合并为张量
    """
    # === 用户特征（所有样本共享）===
    user_ids = torch.cat([x['pos'][0] for x in batch])
    user_discrete = torch.cat([x['pos'][1] for x in batch])
    user_continuous = torch.cat([x['pos'][2] for x in batch])
    user_feat = (user_ids, user_discrete, user_continuous)

    # === 正样本物品特征 ===
    pos_item_ids = torch.cat([x['pos'][3] for x in batch])
    pos_item_discrete = torch.cat([x['pos'][4] for x in batch])
    pos_item_continuous = torch.cat([x['pos'][5] for x in batch])
    pos_item_feat = (pos_item_ids, pos_item_discrete, pos_item_continuous)

    # === 负样本物品特征 ===
    n_neg = len(batch[0]['neg'])  # 一个正样本对应的负样本数，'neg'中存的是n个元组，每个元组是(neg_item_ids, neg_item_disc, neg_item_cont)
    neg_item_feats = []
    for i in range(n_neg):
        neg_ids = torch.cat([x['neg'][i][0] for x in batch])
        neg_disc = torch.cat([x['neg'][i][1] for x in batch])
        neg_cont = torch.cat([x['neg'][i][2] for x in batch])
        neg_item_feats.append((neg_ids, neg_disc, neg_cont))

    return {
        'user_feat': user_feat,
        'pos_item_feat': pos_item_feat,
        'neg_item_feats': neg_item_feats  # list of (ids, disc, cont)
    }


# ----------------------------
# 4. 训练与评估
# ----------------------------
def evaluate_recall_at_k(model, df_val, processor, user_meta_df, item_meta_df,
                         user_discrete_cols, user_continuous_cols,
                         item_discrete_cols, item_continuous_cols,
                         k=10, device='cuda'):
    model.eval()

    # 构建 user_id → 用户特征的映射
    user_meta_dict = {}
    for _, row in user_meta_df.iterrows():
        user_meta_dict[row['user_id']] = row.to_dict()

    # 构建 item_id → 物品特征的映射
    item_meta_dict = {}
    for _, row in item_meta_df.iterrows():
        item_meta_dict[row['item_id']] = row.to_dict()

    # 用户 → 正样本物品集合
    user_to_items = defaultdict(set)
    for _, row in df_val.iterrows():
        user_to_items[row['user_id']].add(row['item_id'])

    # 获取所有候选物品ID（用 item_meta_df 中的物品）
    all_item_ids_list = list(item_meta_dict.keys())
    all_items_df = pd.DataFrame([item_meta_dict[iid] for iid in all_item_ids_list])
    item_ids, item_disc, item_cont = processor.transform_item_features(
        all_items_df, item_discrete_cols, item_continuous_cols
    )
    item_ids = item_ids.to(device)
    item_disc = item_disc.to(device)
    item_cont = item_cont.to(device)

    with torch.no_grad():
        # 事先计算物品塔，输出全部物品特征向量
        all_item_vecs = model.forward_item(item_ids, item_disc, item_cont)  # [N, D]

    recalls = []
    with torch.no_grad():
        for user_id, true_items in user_to_items.items():# 对于验证集中用户-交互过的若干物品
            if not true_items or user_id not in user_meta_dict:
                continue

            # 使用用户真实特征
            user_feat = user_meta_dict[user_id]
            user_df = pd.DataFrame([user_feat])
            u_ids, u_disc, u_cont = processor.transform_user_features(
                user_df, user_discrete_cols, user_continuous_cols
            )
            u_ids = u_ids.to(device)
            u_disc = u_disc.to(device)
            u_cont = u_cont.to(device)

            user_vec = model.forward_user(u_ids, u_disc, u_cont)  # [1, D]
            scores = torch.matmul(user_vec, all_item_vecs.T).squeeze(0)  # 矩阵乘法，一次性求出当前用户与所有物品的余弦相似度[N]

            # Top-K
            topk = min(k, len(scores))
            _, top_indices = torch.topk(scores, topk)
            top_item_ids = [all_item_ids_list[i] for i in top_indices.cpu().numpy()] # 提取出预测余弦相似度最高的k个物品

            hits = len(set(top_item_ids) & true_items) # 真实交互的物品true_items中有几个是在topk中，即预测准了几个
            recall = hits / len(true_items)
            recalls.append(recall)

    return np.mean(recalls) if recalls else 0.0


def train_model():
    # ========== 1. 生成完整交互数据 ==========
    n_users, n_items = 1000, 5000
    total_interactions = 10000

    full_df = pd.DataFrame({
        'user_id': np.random.randint(0, n_users, total_interactions),
        'item_id': np.random.randint(0, n_items, total_interactions),
        'age': np.random.randint(18, 60, total_interactions),
        'gender': np.random.choice(['M', 'F'], total_interactions),
        'category': np.random.choice(['A', 'B', 'C'], total_interactions),
        'price': np.random.uniform(10, 100, total_interactions),
    })

    # ========== 2. 划分训练集和验证集 ==========
    # 按用户划分更合理，但这里简化：随机划分交互
    train_frac = 0.8
    train_size = int(total_interactions * train_frac)
    df_train = full_df.iloc[:train_size].reset_index(drop=True)
    df_val = full_df.iloc[train_size:].reset_index(drop=True)

    # ========== 3. 提取元数据（关键！）==========
    # 用户元数据：去重保留每个用户的特征（假设同一用户特征不变）
    user_meta_df = df_train[['user_id', 'age', 'gender']].drop_duplicates('user_id')
    # 物品元数据：去重保留每个物品的特征
    item_meta_df = df_train[['item_id', 'category', 'price']].drop_duplicates('item_id')

    # ========== 4. 特征列定义 ==========
    user_discrete_cols = ['gender']
    item_discrete_cols = ['category']
    user_continuous_cols = ['age']
    item_continuous_cols = ['price']

    # ========== 5. 预处理（只用训练集）==========
    processor = FeatureProcessor()
    processor.build_vocab_and_scale(
        df_train, user_discrete_cols, item_discrete_cols,
        user_continuous_cols, item_continuous_cols
    )

    # ========== 6. 模型与数据加载器==========
    model = TwoTowerModel(
        n_users=len(processor.user_id_vocab),
        n_items=len(processor.item_id_vocab),
        user_discrete_sizes=[len(processor.user_discrete_vocab[col]) for col in user_discrete_cols],
        item_discrete_sizes=[len(processor.item_discrete_vocab[col]) for col in item_discrete_cols],
        user_cont_dim=len(user_continuous_cols),
        item_cont_dim=len(item_continuous_cols),
        embed_dim=32,
        tower_hidden=[128, 64]
    )
    optimizer = optim.Adam(model.parameters(), lr=1e-3)
    criterion = nn.MSELoss()

    model.to(device)

    dataset = TwoTowerDataset(
        df_train, processor,
        user_discrete_cols, item_discrete_cols,
        user_continuous_cols, item_continuous_cols,
        n_neg=2
    )
    dataloader = DataLoader(dataset, batch_size=256, shuffle=True, collate_fn=collate_fn)

    # ========== 7. 训练循环（同修正版）==========
    for epoch in range(10):
        model.train()
        total_loss = 0
        for batch in dataloader:
            user_feat = [x.to(device) for x in batch['user_feat']]
            pos_item_feat = [x.to(device) for x in batch['pos_item_feat']]
            neg_item_feats = [[x.to(device) for x in neg] for neg in batch['neg_item_feats']]

            pos_score = model(*user_feat, *pos_item_feat)
            neg_scores = []
            for neg_feat in neg_item_feats:
                neg_score = model(*user_feat, *neg_feat)
                neg_scores.append(neg_score)
            neg_scores = torch.stack(neg_scores, dim=1)

            batch_size = pos_score.size(0)
            targets = torch.cat([
                torch.ones(batch_size, 1, device=device),
                -torch.ones(batch_size, 2, device=device)
            ], dim=1)
            all_scores = torch.cat([pos_score.unsqueeze(1), neg_scores], dim=1)
            loss = criterion(all_scores, targets)

            optimizer.zero_grad()
            loss.backward()
            optimizer.step()
            total_loss += loss.item()

        # ========== 8. 评估 ==========
        recall_at_10 = evaluate_recall_at_k(
            model, df_val, processor,
            user_meta_df, item_meta_df,
            user_discrete_cols, user_continuous_cols,
            item_discrete_cols, item_continuous_cols,
            k=10, device="cuda"
        )
        print(f"Epoch {epoch + 1}, Avg Loss: {total_loss / len(dataloader):.4f}, Val Recall@10: {recall_at_10:.4f}")

    return model, processor


if __name__ == "__main__":
    model, processor = train_model()