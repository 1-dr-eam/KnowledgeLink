import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader
from feature_processor import FeatureProcessor
from dataset import ThreeTowerDataset
from utils import collate_fn_three_towers
from DCN import DCN

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
                 hidden_dims=[128, 64],
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
        self.backbone = DCN(2,total_input_dim,hidden_dims).to(device) # 输出维度是hidden_dims[-1]

        # 多任务头
        self.task_heads = []
        prev_dim=hidden_dims[-1]
        for task_idx in range(n_tasks):
            task_head = nn.Sequential(
                nn.Linear(prev_dim, 32),
                nn.ReLU(),
                nn.Linear(32, 1),
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

class FineRankingRecommender:
    def __init__(self):
        self.model = None
        self.processor = None
        # 特征列定义
        self.user_discrete_cols = ['gender', 'user_categories', 'user_keywords']
        self.user_cont_cols = ['age']
        self.item_discrete_cols = ['name','author', 'city', 'item_categories', 'item_keywords']
        self.item_cont_cols = ['price']
        self.scene_discrete_cols = ['hour', 'is_weekend', 'is_holiday']
        self.stat_cont_cols = ['user_click_last3m', 'user_cart_last3m', 'user_buy_last3m', 'user_forward_last3m',
                          'item_click_last3m', 'item_cart_last3m', 'item_buy_last3m', 'item_forward_last3m']
        self.target_cols = ['click', 'cart', 'forward', 'buy']

    def train_multi_task_model(self,df_train):
        """训练多目标模型"""
        print("multi-task model training...")
        # 使用特征处理器
        processor = FeatureProcessor()
        processor.build_vocab_and_scale(df_train,
                                        self.user_discrete_cols, self.item_discrete_cols,
                                        self.user_cont_cols, self.item_cont_cols,
                                        self.scene_discrete_cols, self.stat_cont_cols)

        # Dataset中包含数据的transform操作
        train_dataset = ThreeTowerDataset(df_train, processor,
                                          self.user_discrete_cols, self.item_discrete_cols,
                                          self.scene_discrete_cols, self.user_cont_cols, self.item_cont_cols, self.stat_cont_cols,
                                          self.target_cols)
        # 创建数据加载器
        train_loader = DataLoader(train_dataset, batch_size=256, shuffle=True, collate_fn=collate_fn_three_towers)

        # ========== 定义模型 ==========
        model = MultiTaskNet(
            n_users=len(processor.user_id_vocab),
            n_items=len(processor.item_id_vocab),
            user_discrete_sizes=[len(processor.user_discrete_vocab[col]) for col in self.user_discrete_cols],
            user_cont_dim=len(self.user_cont_cols),
            scene_discrete_sizes=[len(processor.scene_discrete_vocab[col]) for col in self.scene_discrete_cols],
            item_discrete_sizes=[len(processor.item_discrete_vocab[col]) for col in self.item_discrete_cols],
            item_cont_dim=len(self.item_cont_cols),
            stat_cont_dim=len(self.stat_cont_cols),
            hidden_dims=[128, 64, 32],
            n_tasks=len(self.target_cols)
        ).to(device)

        # ========== 定义损失函数和优化器 ==========
        # 使用交叉熵损失
        criterion = nn.CrossEntropyLoss()
        optimizer = optim.Adam(model.parameters(), lr=0.001)
        # =========== 训练循环 ============
        num_epochs = 1
        # 训练阶段
        model.train()
        for epoch in range(num_epochs):
            for batch in train_loader:
                # 获取数据
                user_ids = batch['user_ids'].to(device)
                user_discrete = batch['user_discrete'].to(device)
                user_continuous = batch['user_continuous'].unsqueeze(1).to(device)
                scene_discrete = batch['scene_discrete'].to(device)
                item_ids = batch['item_ids'].to(device)
                item_discrete = batch['item_discrete'].to(device)
                item_continuous = batch['item_continuous'].unsqueeze(1).to(device)
                stat_continuous = batch['stat_continuous'].to(device)
                targets = batch['targets'].to(device)

                # 前向传播
                task_outputs = model(
                    user_ids, user_discrete, user_continuous,
                    item_ids, item_discrete, item_continuous,
                    scene_discrete, stat_continuous
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

        self.model=model
        self.processor=processor

        print("multi-task model training finished")

    def fine_ranking(self, df)->dict:
        """
        直接利用 df 构造输入数据，原始特征->预处理->转tensor输入多目标模型
        """
        if self.model is None or self.processor is None:
            raise ValueError("请先调用train_multi_task_model训练多目标模型")

        print("fine ranking...")
        self.model.eval()
        item_index_score = dict()  # 物品精排分数字典，{item_index:score}
        # 这里因为要处理的物品较少，最多只有几百个，因此不分batch直接一次性计算
        # 转换特征
        user_ids, user_discrete, user_continuous = self.processor.transform_user_features(
            df, self.user_discrete_cols, self.user_cont_cols)  # 离散特征ID转索引，连续特征归一化

        scene_discrete = self.processor.transform_scene_features(df, self.scene_discrete_cols)

        item_ids, item_discrete, item_continuous = self.processor.transform_item_features(
            df, self.item_discrete_cols, self.item_cont_cols)

        stat_continuous = self.processor.transform_stat_features(df, self.stat_cont_cols)
        click_pred, like_pred, collect_pred, forward_pred = self.model(
            user_ids, user_discrete, user_continuous,
            item_ids, item_discrete, item_continuous,
            scene_discrete,stat_continuous
        )
        # 融分公式融合排序
        for i in range(len(item_ids)):
            # 这里的item_id其实是索引
            item_index_score[int(item_ids[i])] = (0.4 * float(click_pred[i].detach()) + 0.1 * float(like_pred[i].detach())
                                                 + 0.3 * float(collect_pred[i].detach()) + 0.2 * float(forward_pred[i].detach()))
        # 带着精排分数返回，不做截断
        index_id_dict = {index: id for id, index in self.processor.item_id_vocab.items()}
        item_id_score={index_id_dict[index]:score for index,score in item_index_score.items()} # item_id->fine ranking score

        print("精排分数字典：",item_id_score)
        print("fine ranking finished\n")

        return item_id_score