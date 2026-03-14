import torch
import torch.nn as nn
import pandas as pd
import numpy as np
from sklearn.preprocessing import StandardScaler
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader
from feature_processor import FeatureProcessor
from dataset import ThreeTowerDataset
from utils import collate_fn_three_towers

device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')

class MultiTaskNet(nn.Module):
    """多目标精排模型"""
    def __init__(self,
                 # 用户特征
                 n_users, user_discrete_sizes, user_cont_dim,
                 # 物品特征
                 n_items, item_discrete_sizes, item_cont_dim,
                 # 场景特征
                 scene_discrete_sizes,
                 # 统计特征
                 stat_cont_dim,
                 # 主干神经网络部分
                 hidden_dims=[128, 64, 32],
                 n_tasks=4):
        super().__init__()

        # 用户特征（embedding层，ID映射为32维，其他离散特征映射为16维）
        self.user_id_embed = nn.Embedding(n_users, 32)
        self.user_discrete_embeds = nn.ModuleList([nn.Embedding(size, 16) for size in user_discrete_sizes])
        user_input_dim = 32 + len(user_discrete_sizes) * 16 + user_cont_dim

        # 物品塔
        self.item_id_embed = nn.Embedding(n_items, 32)
        self.item_discrete_embeds = nn.ModuleList([nn.Embedding(size, 16) for size in item_discrete_sizes])
        item_input_dim = 32 + len(item_discrete_sizes) * 16 + item_cont_dim

        # 场景特征
        self.scene_discrete_embeds = nn.ModuleList([nn.Embedding(size, 16) for size in scene_discrete_sizes])
        scene_input_dim = len(scene_discrete_sizes) * 16

        # 统计特征
        cross_input_dim = stat_cont_dim

        total_input_dim = user_input_dim + item_input_dim + scene_input_dim + cross_input_dim

        # 主干网络
        layers = []
        prev_dim = total_input_dim
        for hidden_dim in hidden_dims:
            layers.extend([nn.Linear(prev_dim, hidden_dim), nn.ReLU()])
            prev_dim = hidden_dim

        self.backbone = nn.Sequential(*layers)

        # 多任务头
        self.task_heads = []
        for task_idx in range(n_tasks):
            task_head = nn.Sequential(
                nn.Linear(prev_dim, 16),
                nn.ReLU(),
                nn.Linear(16, 1),
                nn.Sigmoid()  # 使用Sigmoid激活函数适用于二分类
            ).to(device)
            self.task_heads.append(task_head)

    def forward(self,
                user_ids, user_discrete, user_continuous,
                item_ids, item_discrete, item_continuous,
                scene_discrete,
                stat_continuous):
        """前向传播，返回的是n_tasks个浮点数"""
        # 处理用户特征，离散特征做embedding，连续特征直接拼接（因为已经在transform中做过归一化）
        user_emb_list=[]
        user_emb_list.append(self.user_id_embed(user_ids))
        for i in range(user_discrete.size(1)):
            user_emb_list.append(self.user_discrete_embeds[i](user_discrete[:,i]))
        user_emb_list.append(user_continuous)
        # 物品特征
        item_emb_list = []
        item_emb_list.append(self.item_id_embed(item_ids))
        for i in range(item_discrete.size(1)):
            item_emb_list.append(self.item_discrete_embeds[i](item_discrete[:, i]))
        item_emb_list.append(item_continuous)
        # 场景特征
        scene_emb_list = []
        for i in range(scene_discrete.size(1)):
            scene_emb_list.append(self.scene_discrete_embeds[i](scene_discrete[:,i]))

        # 拼接所有特征
        combined_features = torch.cat([*user_emb_list,*item_emb_list,*scene_emb_list,stat_continuous], dim=1)
        # 主干网络
        output = self.backbone(combined_features)
        # 多任务头
        task_outputs = []
        for head in self.task_heads:
            score = head(output)
            task_outputs.append(score)
        return task_outputs


def create_sample_data_with_features():
    """创建带特征的示例数据"""
    n_samples = 500

    df = pd.DataFrame({
        'user_id': np.random.randint(0, 100, n_samples),
        'item_id': np.random.randint(0, 500, n_samples),

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

        # 目标变量
        'click': np.random.choice([0, 1], n_samples, p=[0.8, 0.2]),
        'like': np.random.choice([0, 1], n_samples, p=[0.7, 0.3]),
        'collect': np.random.choice([0, 1], n_samples, p=[0.9, 0.1]),
        'forward': np.random.choice([0, 1], n_samples, p=[0.95, 0.05])
    })

    return df


def train_multi_task_model():
    """训练多目标模型"""
    print("开始训练多目标精排模型...")

    # ========= 1. 准备数据 =========
    df = create_sample_data_with_features()

    # 定义特征列
    user_discrete_cols = ['city', 'topic']
    user_cont_cols = ['age', 'activity_level', 'spending_amount']
    item_discrete_cols = ['category', 'tag']
    item_cont_cols = ['publish_date', 'view_count']
    scene_discrete_cols = ['hour', 'is_weekend', 'is_holiday']
    stat_cont_cols = ['user_click_last30d', 'user_like_last30d', 'item_click_last30d', 'item_like_last30d']
    target_cols = ['click', 'like', 'collect', 'forward']

    # 分割数据
    train_size = int(0.8 * len(df))
    df_train = df[:train_size]
    df_val = df[train_size:]

    # 使用特征处理器
    processor = FeatureProcessor()
    processor.build_vocab_and_scale(df_train,
                                    user_discrete_cols, item_discrete_cols,
                                    user_cont_cols, item_cont_cols,
                                    scene_discrete_cols, stat_cont_cols)
    # 划分训练和验证集
    train_size = int(0.8 * len(df))
    df_train = df.iloc[:train_size].reset_index(drop=True)
    df_val = df.iloc[train_size:].reset_index(drop=True)

    # 转换训练和验证数据
    # Dataset中包含数据的transform操作
    train_dataset = ThreeTowerDataset(df_train, processor,
                                      user_discrete_cols, item_discrete_cols,
                                      scene_discrete_cols, user_cont_cols, item_cont_cols, stat_cont_cols,
                                      target_cols)
    val_dataset = ThreeTowerDataset(df_val, processor,
                                    user_discrete_cols, item_discrete_cols,
                                    scene_discrete_cols, user_cont_cols, item_cont_cols, stat_cont_cols,
                                    target_cols)

    # 创建数据加载器
    train_loader = DataLoader(train_dataset, batch_size=256, shuffle=True, collate_fn=collate_fn_three_towers)
    val_loader = DataLoader(val_dataset, batch_size=256, shuffle=False, collate_fn=collate_fn_three_towers)

    # ========= 2. 定义模型 =========
    model = MultiTaskNet(
        n_users=len(processor.user_id_vocab),
        n_items=len(processor.item_id_vocab),
        user_discrete_sizes=[len(processor.user_discrete_vocab[col]) for col in user_discrete_cols],
        user_cont_dim=len(user_cont_cols),
        scene_discrete_sizes=[len(processor.scene_discrete_vocab[col]) for col in scene_discrete_cols],
        item_discrete_sizes=[len(processor.item_discrete_vocab[col]) for col in item_discrete_cols],
        item_cont_dim=len(item_cont_cols),
        stat_cont_dim=len(stat_cont_cols),
        hidden_dims=[128, 64, 32],
        n_tasks=len(target_cols)
    )

    # ========= 3. 定义损失函数和优化器 =========
    # 使用交叉熵损失
    criterion = nn.CrossEntropyLoss()
    optimizer = optim.Adam(model.parameters(), lr=0.001)
    model.to(device)
    # ========= 4. 训练循环 =========
    num_epochs = 50
    best_val_loss = float('inf')

    for epoch in range(num_epochs):
        # 训练阶段
        model.train()
        train_loss = 0.0
        train_correct = 0
        train_total = 0

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
            task_outputs = model(
                user_ids, user_discrete, user_continuous,
                item_ids, item_discrete, item_continuous,
                scene_discrete,stat_continuous
            )

            # 计算多任务损失
            total_loss = 0.0
            for i, output in enumerate(task_outputs):
                task_loss = criterion(output.squeeze(), targets[:, i])
                total_loss += task_loss

            # 反向传播
            optimizer.zero_grad()
            total_loss.backward()
            optimizer.step()

            train_loss += total_loss.item()

            # 计算准确率（阈值为0.5）
            predictions = [torch.round(output.squeeze()) for output in task_outputs]
            correct = sum([(pred == targets[:, i]).sum().item()
                           for i, pred in enumerate(predictions)])
            train_total += targets.size(0) * len(target_cols)
            train_correct += correct

        # 验证阶段
        model.eval()
        val_loss = 0.0
        val_correct = 0
        val_total = 0

        with torch.no_grad():
            for batch in val_loader:
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

                task_outputs = model(
                    user_ids, user_discrete, user_continuous,
                    item_ids, item_discrete, item_continuous,
                    scene_discrete, stat_continuous
                )

                # 计算验证损失
                total_loss = 0.0
                for i, output in enumerate(task_outputs):
                    task_loss = criterion(output.squeeze(), targets[:, i])
                    total_loss += task_loss
                val_loss += total_loss.item()

                # 计算验证准确率
                predictions = [torch.round(output.squeeze()) for output in task_outputs]
                correct = sum([(pred == targets[:, i]).sum().item()
                               for i, pred in enumerate(predictions)])
                val_total += targets.size(0) * len(target_cols)
                val_correct += correct

        # 计算平均损失和准确率
        avg_train_loss = train_loss / len(train_loader)
        avg_val_loss = val_loss / len(val_loader)
        train_acc = train_correct / train_total
        val_acc = val_correct / val_total

        print(f"Epoch [{epoch + 1}/{num_epochs}]")
        print(f"  Train Loss: {avg_train_loss:.4f}, Train Acc: {train_acc:.4f}")
        print(f"  Val Loss: {avg_val_loss:.4f}, Val Acc: {val_acc:.4f}")

        # 更新最佳验证损失
        if avg_val_loss < best_val_loss:
            best_val_loss = avg_val_loss
            print(f"  * 最佳验证损失更新: {best_val_loss:.4f}")

    print(f"\n训练完成！最终验证损失: {best_val_loss:.4f}")

    return model


if __name__ == "__main__":
    model = train_multi_task_model()