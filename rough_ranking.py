import heapq
import torch
import torch.nn as nn
import pandas as pd
import numpy as np
from torch.utils.data import Dataset, DataLoader
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

def train_three_tower_model():
    """训练三塔模型"""
    # ========= 1. 模拟数据生成 =========
    n_samples = 10000
    n_users = 1000
    n_items = 5000

    df = pd.DataFrame({
        'user_id': np.random.randint(0, n_users, n_samples),
        'item_id': np.random.randint(0, n_items, n_samples),

        # 用户离散特征
        'city': np.random.choice(['Beijing', 'Shanghai', 'Guangzhou', 'Shenzhen'], n_samples),
        'topic': np.random.choice(['Tech', 'Sports', 'Entertainment', 'Finance'], n_samples),

        # 用户连续特征
        'age': np.random.randint(18, 60, n_samples),
        'activity_level': np.random.uniform(0, 1, n_samples),
        'spending_amount': np.random.uniform(0, 1000, n_samples),

        # 场景离散特征
        'hour': np.random.randint(0, 24, n_samples),
        'is_weekend': np.random.choice([0, 1], n_samples),
        'is_holiday': np.random.choice([0, 1], n_samples),

        # 物品离散特征
        'category': np.random.choice(['News', 'Video', 'Article', 'Image'], n_samples),
        'tag': np.random.choice(['Hot', 'Recommended', 'New', 'Popular'], n_samples),

        # 物品连续特征
        'publish_date': np.random.uniform(0, 365, n_samples),
        'view_count': np.random.randint(0, 10000, n_samples),

        # 统计特征
        'user_click_last30d': np.random.randint(0, 100, n_samples),
        'user_like_last30d': np.random.randint(0, 50, n_samples),
        'item_click_last30d': np.random.randint(0, 1000, n_samples),
        'item_like_last30d': np.random.randint(0, 500, n_samples),

        # 目标值（模拟）
        'click': np.random.choice([0, 1], n_samples, p=[0.8, 0.2]),
        'like': np.random.choice([0, 1], n_samples, p=[0.9, 0.1]),
        'collect': np.random.choice([0, 1], n_samples, p=[0.95, 0.05]),
        'forward': np.random.choice([0, 1], n_samples, p=[0.98, 0.02])
    })

    # 划分训练和验证集
    train_size = int(0.8 * len(df))
    df_train = df.iloc[:train_size].reset_index(drop=True)
    df_val = df.iloc[train_size:].reset_index(drop=True)

    # ========= 2. 特征列定义 =========
    user_discrete_cols = ['city', 'topic']
    item_discrete_cols = ['category', 'tag']
    scene_discrete_cols = ['hour', 'is_weekend', 'is_holiday']
    user_cont_cols = ['age', 'activity_level', 'spending_amount']
    item_cont_cols = ['publish_date', 'view_count']
    stat_cont_cols = ['user_click_last30d', 'user_like_last30d', 'item_click_last30d', 'item_like_last30d']
    target_cols = ['click', 'like', 'collect', 'forward']

    # ========= 3. 特征预处理 =========
    processor = FeatureProcessor()
    processor.build_vocab_and_scale(df_train,
                                    user_discrete_cols, item_discrete_cols,
                                    user_cont_cols, item_cont_cols,
                                    scene_discrete_cols,stat_cont_cols)

    # ========= 4. 数据加载器 =========
    train_dataset = ThreeTowerDataset(df_train, processor,
                                      user_discrete_cols, item_discrete_cols,
                                      scene_discrete_cols, user_cont_cols, item_cont_cols, stat_cont_cols,
                                      target_cols)
    val_dataset = ThreeTowerDataset(df_val, processor,
                                    user_discrete_cols, item_discrete_cols,
                                    scene_discrete_cols, user_cont_cols, item_cont_cols, stat_cont_cols,
                                    target_cols)

    train_loader = DataLoader(train_dataset, batch_size=256, shuffle=True, collate_fn=collate_fn_three_towers)
    val_loader = DataLoader(val_dataset, batch_size=256, shuffle=False, collate_fn=collate_fn_three_towers)

    # ========= 5. 模型初始化 =========
    model = ThreeTowerModel(
        n_users=len(processor.user_id_vocab),
        n_items=len(processor.item_id_vocab),
        user_discrete_sizes=[len(processor.user_discrete_vocab[col]) for col in user_discrete_cols],
        user_cont_dim=len(user_cont_cols),
        scene_discrete_sizes=[len(processor.scene_discrete_vocab[col]) for col in scene_discrete_cols],
        item_discrete_sizes=[len(processor.item_discrete_vocab[col]) for col in item_discrete_cols],
        item_cont_dim=len(item_cont_cols),
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
    num_epochs = 1
    for epoch in range(num_epochs):
        # 训练
        model.train()
        total_train_loss = 0
        for batch in train_loader:
            # 获取数据
            user_ids = batch['user_ids'].to(device)
            user_discrete = batch['user_discrete'].to(device)
            user_continuous = batch['user_continuous'].to(device)
            scene_discrete = batch['scene_discrete'].to(device)
            item_ids = batch['item_ids'].to(device)
            item_discrete = batch['item_discrete'].to(device)
            item_continuous = batch['item_continuous'].to(device)
            stat_continuous = batch['stat_continuous'].to(device)
            targets = batch['targets'].to(device)

            # 前向传播
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
        print(f"Epoch {epoch + 1}/{num_epochs}:")
        print(f"  Train Loss: {total_train_loss / len(train_loader):.4f}")

    # 验证
    model.eval()
    total_val_loss = 0
    item_score=dict() # 物品粗排分数字典，{item_id:score}
    with torch.no_grad():
        for batch in val_loader:
            user_ids = batch['user_ids'].to(device)
            user_discrete = batch['user_discrete'].to(device)
            user_continuous = batch['user_continuous'].to(device)
            scene_discrete = batch['scene_discrete'].to(device)
            item_ids = batch['item_ids'].to(device)
            item_discrete = batch['item_discrete'].to(device)
            item_continuous = batch['item_continuous'].to(device)
            stat_continuous = batch['stat_continuous'].to(device)
            targets = batch['targets'].to(device)

            click_pred, like_pred, collect_pred, forward_pred = model(
                user_ids, user_discrete, user_continuous, scene_discrete,
                item_ids, item_discrete, item_continuous,
                stat_continuous
            )# 带batch的
            # 融分公式融合排序
            for i in range(len(item_ids)):
                item_score[item_ids[i]] = 0.4*click_pred[i]+0.1*like_pred[i]+0.3*collect_pred[i]+0.2*forward_pred[i]

            loss_click = criterion(click_pred, targets[:, 0])
            loss_like = criterion(like_pred, targets[:, 1])
            loss_collect = criterion(collect_pred, targets[:, 2])
            loss_forward = criterion(forward_pred, targets[:, 3])

            val_total_loss = loss_click + loss_like + loss_collect + loss_forward
            total_val_loss += val_total_loss.item()

    # 根据粗排分数截断返回
    topn=heapq.nlargest(100,item_score.items(),key=lambda x:x[1])
    ids=set([int(x[0]) for x in topn])
    print(f"  Val Loss: {total_val_loss / len(val_loader):.4f}")

    return model, processor,ids


# 运行训练
if __name__ == "__main__":
    model, processor,item_ids = train_three_tower_model()
    print("三塔模型训练完成！")
    print("选中的item_ids：",item_ids)