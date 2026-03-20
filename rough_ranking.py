import heapq
import torch
import torch.nn as nn
from torch.utils.data import DataLoader
import torch.optim as optim
from feature_processor import FeatureProcessor
from utils import collate_fn_three_towers
from dataset import ThreeTowerDataset

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

# ---------------------
# 三塔模型
# ---------------------
class ThreeTowerModel(nn.Module):
    def __init__(self,
                 # 用户塔参数
                 n_users, user_discrete_sizes, user_cont_dim, scene_discrete_sizes,
                 # 物品塔参数
                 n_items, item_discrete_sizes, item_cont_dim,
                 # 交叉塔参数
                 stat_cont_dim,
                 # 塔内部结构
                 user_tower_hidden=[64, 32],
                 item_tower_hidden=[32, 16],
                 cross_tower_hidden=[16],
                 # MLP部分
                 mlp_hidden=[16, 16],
                 n_tasks=4):
        super().__init__()

        # 用户塔
        self.user_id_embed = nn.Embedding(n_users, 32)
        self.user_discrete_embeds = nn.ModuleList([nn.Embedding(size, 16) for size in user_discrete_sizes])
        self.scene_discrete_embeds = nn.ModuleList([nn.Embedding(size, 16) for size in scene_discrete_sizes])
        user_input_dim = 32 + len(user_discrete_sizes) * 16 + user_cont_dim + len(scene_discrete_sizes)*16
        user_layers = []
        prev_dim = user_input_dim
        for h in user_tower_hidden:
            user_layers.extend([nn.Linear(prev_dim, h), nn.ReLU()])
            prev_dim = h
        self.user_tower = nn.Sequential(*user_layers)

        # 物品塔
        self.item_id_embed = nn.Embedding(n_items, 32)
        self.item_discrete_embeds = nn.ModuleList([nn.Embedding(size, 16) for size in item_discrete_sizes])
        item_input_dim = 32 + len(item_discrete_sizes) * 16 + item_cont_dim
        item_layers = []
        prev_dim = item_input_dim
        for h in item_tower_hidden:
            item_layers.extend([nn.Linear(prev_dim, h), nn.ReLU()])
            prev_dim = h
        self.item_tower = nn.Sequential(*item_layers)

        # 交叉塔
        cross_input_dim = stat_cont_dim
        cross_layers = []
        prev_dim = cross_input_dim
        for h in cross_tower_hidden:
            cross_layers.extend([nn.Linear(prev_dim, h), nn.ReLU()])
            prev_dim = h
        self.cross_tower = nn.Sequential(*cross_layers)

        # 合并后的MLP部分
        combined_dim = user_tower_hidden[-1] + item_tower_hidden[-1] + cross_tower_hidden[-1]
        self.mlps = nn.ModuleList()
        for _ in range(n_tasks):
            mlp_layers = []
            prev_dim = combined_dim
            for h in mlp_hidden:
                mlp_layers.extend([nn.Linear(prev_dim, h), nn.ReLU()])
                prev_dim = h
            mlp_layers.append(nn.Linear(prev_dim, 1)) # 最后加一层输出层，输出维度为1，后接sigmoid
            mlp_layers.append(nn.Sigmoid())
            self.mlps.append(nn.Sequential(*mlp_layers))

    def forward(self,
                user_ids, user_discrete, user_continuous, scene_discrete,
                item_ids, item_discrete, item_continuous,
                stat_continuous):
        # 用户塔
        user_emb_list = [self.user_id_embed(user_ids)]
        for i in range(user_discrete.size(1)):
            emb = self.user_discrete_embeds[i](user_discrete[:, i]) # 取一整列，即某个离散特征的所有值，一次性embedding
            user_emb_list.append(emb)
        user_emb_list.append(user_continuous) # 连续特征已在FeatureProcessor中进行过归一化

        # 离散场景特征也需进入用户塔
        for i in range(scene_discrete.size(1)):
            emb = self.scene_discrete_embeds[i](scene_discrete[:, i])
            user_emb_list.append(emb)
        user_concat = torch.cat(user_emb_list, dim=1)
        user_vector = self.user_tower(user_concat)

        # 物品塔
        item_emb_list = [self.item_id_embed(item_ids)]
        for i in range(item_discrete.size(1)):
            emb = self.item_discrete_embeds[i](item_discrete[:, i])
            item_emb_list.append(emb)
        item_emb_list.append(item_continuous)
        item_concat = torch.cat(item_emb_list, dim=1)
        item_vector = self.item_tower(item_concat)

        # 交叉塔
        cross_vector = self.cross_tower(stat_continuous)

        # 拼接三个塔的输出
        combined_vector = torch.cat([user_vector, item_vector, cross_vector], dim=1)

        # 通过四个独立MLP
        outputs = []
        for mlp in self.mlps:
            output = mlp(combined_vector)
            outputs.append(output.squeeze(-1))

        click_pred, like_pred, collect_pred, forward_pred = outputs
        return click_pred, like_pred, collect_pred, forward_pred

def train_three_towers_model(df_train):
    """训练三塔模型"""
    # ========== 特征列定义 ==========
    user_discrete_cols = ['gender', 'user_categories', 'user_keywords']
    user_continuous_cols = ['age']
    item_discrete_cols = ['name', 'city', 'item_categories', 'item_keywords']
    item_continuous_cols = ['price']
    scene_discrete_cols = ['hour', 'is_weekend', 'is_holiday']
    stat_cont_cols = ['user_click_last3m','user_cart_last3m','user_buy_last3m','user_forward_last3m',
                      'item_click_last3m','item_cart_last3m','item_buy_last3m','item_forward_last3m']
    target_cols = ['click', 'cart', 'forward', 'buy']

    # ========= 3. 特征预处理 =========
    processor = FeatureProcessor()
    processor.build_vocab_and_scale(df_train,
                                    user_discrete_cols, item_discrete_cols,
                                    user_continuous_cols, item_continuous_cols,
                                    scene_discrete_cols,stat_cont_cols)

    # ========= 4. 数据加载器 =========
    train_dataset = ThreeTowerDataset(df_train, processor,
                                      user_discrete_cols, item_discrete_cols,
                                      scene_discrete_cols, user_continuous_cols, item_continuous_cols, stat_cont_cols,
                                      target_cols)

    train_loader = DataLoader(train_dataset, batch_size=256, shuffle=True, collate_fn=collate_fn_three_towers)

    # ========= 5. 模型初始化 =========
    model = ThreeTowerModel(
        n_users=len(processor.user_id_vocab),
        n_items=len(processor.item_id_vocab),
        user_discrete_sizes=[len(processor.user_discrete_vocab[col]) for col in user_discrete_cols],
        user_cont_dim=len(user_continuous_cols),
        scene_discrete_sizes=[len(processor.scene_discrete_vocab[col]) for col in scene_discrete_cols],
        item_discrete_sizes=[len(processor.item_discrete_vocab[col]) for col in item_discrete_cols],
        item_cont_dim=len(item_continuous_cols),
        stat_cont_dim=len(stat_cont_cols),
        user_tower_hidden=[64, 32],
        item_tower_hidden=[32, 16],
        cross_tower_hidden=[16],
        mlp_hidden=[16, 16],
        n_tasks=4
    )

    # ========= 6. 训练配置 =========
    optimizer = optim.Adam(model.parameters(), lr=1e-3)
    criterion = nn.CrossEntropyLoss()  # 交叉熵损失
    model.to(device)

    # ========= 7. 训练循环 =========
    num_epochs = 10
    for epoch in range(num_epochs):
        # 训练
        model.train()
        total_train_loss = 0
        for batch in train_loader:
            # 获取数据
            user_ids = batch['user_ids'].to(device)
            user_discrete = batch['user_discrete'].to(device)
            user_continuous = batch['user_continuous'].unsqueeze(1).to(device) # (B)->(B,1)，使得下面cat可以正常进行
            scene_discrete = batch['scene_discrete'].to(device)
            item_ids = batch['item_ids'].to(device)
            item_discrete = batch['item_discrete'].to(device)
            item_continuous = batch['item_continuous'].unsqueeze(1).to(device)
            stat_continuous = batch['stat_continuous'].to(device)
            targets = batch['targets'].to(device)

            # 前向传播(传入的数据是带batch的)
            click_pred, like_pred, collect_pred, forward_pred = model(
                user_ids, user_discrete, user_continuous, scene_discrete,
                item_ids, item_discrete, item_continuous,
                stat_continuous
            )

            # 计算损失
            loss_click = criterion(click_pred, targets[:, 0])
            loss_like = criterion(like_pred, targets[:, 1])
            loss_collect = criterion(collect_pred, targets[:, 2])
            loss_forward = criterion(forward_pred, targets[:, 3])

            # 总损失
            total_loss = loss_click + loss_like + loss_collect + loss_forward

            # 反向传播
            optimizer.zero_grad()
            total_loss.backward()
            optimizer.step()

            total_train_loss += total_loss.item()

    print("three towers model training finished")

    return model, processor

def rough_ranking(model,processor,df)->set[int]:
    """
    直接利用 df 构造输入数据，原始特征->预处理->转tensor输入三塔模型
    """
    print("rough ranking...")
    # ============ 特征列定义 ===========
    user_discrete_cols = ['gender', 'user_categories', 'user_keywords']
    user_continuous_cols = ['age']
    item_discrete_cols = ['name', 'city', 'item_categories', 'item_keywords']
    item_continuous_cols = ['price']
    scene_discrete_cols = ['hour', 'is_weekend', 'is_holiday']
    stat_cont_cols = ['user_click_last3m', 'user_cart_last3m', 'user_buy_last3m', 'user_forward_last3m',
                      'item_click_last3m', 'item_cart_last3m', 'item_buy_last3m', 'item_forward_last3m']
    model.eval()
    item_score = dict()  # 物品粗排分数字典，{item_id:score}
    for i in range(0,len(df),100):
        df_batch = df.iloc[i:i+100] if i+100 < len(df) else df.iloc[i:]
        # 转换特征
        user_ids, user_discrete, user_continuous = processor.transform_user_features(
                df_batch, user_discrete_cols, user_continuous_cols)  # 离散特征ID转索引，连续特征归一化

        scene_discrete = processor.transform_scene_features(df_batch, scene_discrete_cols)

        item_ids, item_discrete, item_continuous = processor.transform_item_features(
                df_batch, item_discrete_cols, item_continuous_cols)

        stat_continuous = processor.transform_stat_features(df_batch, stat_cont_cols)
        # 带batch维度，一次性计算
        click_pred, like_pred, collect_pred, forward_pred = model(
            user_ids, user_discrete, user_continuous, scene_discrete,
            item_ids, item_discrete, item_continuous,
            stat_continuous
        )
        # 融分公式融合排序
        for i in range(len(item_ids)):
            # 这里的item_id其实是索引
            item_score[item_ids[i]] = 0.4 * click_pred[i] + 0.1 * like_pred[i] + 0.3 * collect_pred[i] + 0.2 * \
                                      forward_pred[i]
    # 根据粗排分数截断返回
    topn = heapq.nlargest(5, item_score.items(), key=lambda x: x[1])
    indexs = set([int(x[0]) for x in topn])
    index_id_dict={index:id for id,index in processor.item_id_vocab.items()}
    ids=set()
    for index in indexs:
        ids.add(index_id_dict[index])

    print("粗排后剩余物品ID:",ids)
    print("rough ranking finished\n")

    return ids