import heapq
import pandas as pd
import torch
import torch.nn as nn
from torch.utils.data import DataLoader
import torch.optim as optim
from feature_processor import FeatureProcessor
from utils import collate_fn_three_towers
from dataset import ThreeTowerDataset

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

# =========================================
# ================= 三塔模型 ================
# =========================================
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
        """
        训练时使用
        N个用户和 N 个物品为输入，输出 N 组（点击率、加购物车率、转发率、购买率）
        真正做推荐时还可优化，对于一个用户的 N 个召回物品，用户塔只需计算一次，物品特征向量在向量数据库中存储不需在线计算，交叉塔需计算 N 次
        """
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

        click_pred, cart_pred, forward_pred, buy_pred = outputs
        return click_pred, cart_pred, forward_pred, buy_pred

    def forward_user(self,user_ids, user_discrete, user_continuous, scene_discrete):
        """用户塔前向传播"""
        # 用户塔
        user_emb_list = [self.user_id_embed(user_ids)]
        for i in range(user_discrete.size(1)):
            emb = self.user_discrete_embeds[i](user_discrete[:, i])  # 取一整列，即某个离散特征的所有值，一次性embedding
            user_emb_list.append(emb)
        user_emb_list.append(user_continuous)  # 连续特征已在FeatureProcessor中进行过归一化

        # 离散场景特征也需进入用户塔
        for i in range(scene_discrete.size(1)):
            emb = self.scene_discrete_embeds[i](scene_discrete[:, i])
            user_emb_list.append(emb)
        user_concat = torch.cat(user_emb_list, dim=1)
        return self.user_tower(user_concat)

    def forward_item(self,item_ids, item_discrete, item_continuous):
        # 物品塔
        item_emb_list = [self.item_id_embed(item_ids)]
        for i in range(item_discrete.size(1)):
            emb = self.item_discrete_embeds[i](item_discrete[:, i])
            item_emb_list.append(emb)
        item_emb_list.append(item_continuous)
        item_concat = torch.cat(item_emb_list, dim=1)
        return self.item_tower(item_concat)

    def forward_cross(self,stat_continuous):
        """交叉塔前向传播"""
        return self.cross_tower(stat_continuous)

    def forward_mlps(self,feature_vectors):
        # 通过四个独立MLP
        outputs = []
        for mlp in self.mlps:
            output = mlp(feature_vectors)
            outputs.append(output.squeeze(-1))

        click_pred, cart_pred, forward_pred, buy_pred = outputs
        return click_pred, cart_pred, forward_pred, buy_pred


class RoughRankingRecommender:
    def __init__(self):
        self.model = None
        self.processor = None
        self.item_features_dict = {} # {item_id:feature}
        # 特征列定义
        self.user_discrete_cols = ['gender', 'user_categories', 'user_keywords']
        self.user_continuous_cols = ['age']
        self.item_discrete_cols = ['name', 'author','city', 'item_categories', 'item_keywords']
        self.item_continuous_cols = ['price']
        self.scene_discrete_cols = ['hour', 'is_weekend', 'is_holiday']
        self.stat_cont_cols = ['user_click_last3m', 'user_cart_last3m', 'user_buy_last3m', 'user_forward_last3m',
                          'item_click_last3m', 'item_cart_last3m', 'item_buy_last3m', 'item_forward_last3m']
        self.target_cols = ['click', 'cart', 'forward', 'buy']

    def train_three_towers_model(self,df_train):
        """训练三塔模型"""
        print("three towers model training...")
        # ========== 特征预处理 ==========
        processor = FeatureProcessor()
        processor.build_vocab_and_scale(df_train,
                                        self.user_discrete_cols, self.item_discrete_cols,
                                        self.user_continuous_cols, self.item_continuous_cols,
                                        self.scene_discrete_cols, self.stat_cont_cols)

        # ========= 4. 数据加载器 =========
        train_dataset = ThreeTowerDataset(df_train, processor,
                                          self.user_discrete_cols, self.item_discrete_cols,
                                          self.scene_discrete_cols, self.user_continuous_cols, self.item_continuous_cols,
                                          self.stat_cont_cols,
                                          self.target_cols)

        train_loader = DataLoader(train_dataset, batch_size=256, shuffle=True, collate_fn=collate_fn_three_towers)

        # ========== 模型初始化 ==========
        model = ThreeTowerModel(
            n_users=len(processor.user_id_vocab),
            n_items=len(processor.item_id_vocab),
            user_discrete_sizes=[len(processor.user_discrete_vocab[col]) for col in self.user_discrete_cols],
            user_cont_dim=len(self.user_continuous_cols),
            scene_discrete_sizes=[len(processor.scene_discrete_vocab[col]) for col in self.scene_discrete_cols],
            item_discrete_sizes=[len(processor.item_discrete_vocab[col]) for col in self.item_discrete_cols],
            item_cont_dim=len(self.item_continuous_cols),
            stat_cont_dim=len(self.stat_cont_cols),
            user_tower_hidden=[32, 16],
            item_tower_hidden=[32, 16],
            cross_tower_hidden=[16],
            mlp_hidden=[16, 16],
            n_tasks=4
        ).to(device)

        # ========== 训练配置 ==========
        optimizer = optim.Adam(model.parameters(), lr=1e-3)
        criterion = nn.CrossEntropyLoss()  # 交叉熵损失

        # ========== 训练循环 ==========
        num_epochs = 10
        for epoch in range(num_epochs):
            # 训练
            model.train()
            for batch in train_loader:
                # 获取数据
                user_ids = batch['user_ids'].to(device)
                user_discrete = batch['user_discrete'].to(device)
                user_continuous = batch['user_continuous'].unsqueeze(1).to(device)  # (B)->(B,1)，使得下面cat可以正常进行
                scene_discrete = batch['scene_discrete'].to(device)
                item_ids = batch['item_ids'].to(device)
                item_discrete = batch['item_discrete'].to(device)
                item_continuous = batch['item_continuous'].unsqueeze(1).to(device)
                stat_continuous = batch['stat_continuous'].to(device)
                targets = batch['targets'].to(device)

                # 前向传播(传入的数据是带batch的)
                click_pred, cart_pred, forward_pred, buy_pred = model(
                    user_ids, user_discrete, user_continuous, scene_discrete,
                    item_ids, item_discrete, item_continuous,
                    stat_continuous
                )

                # 计算损失
                loss_click = criterion(click_pred, targets[:, 0])
                loss_like = criterion(cart_pred, targets[:, 1])
                loss_collect = criterion(forward_pred, targets[:, 2])
                loss_forward = criterion(buy_pred, targets[:, 3])

                # 总损失
                total_loss = loss_click + loss_like + loss_collect + loss_forward

                # 反向传播
                optimizer.zero_grad()
                total_loss.backward()
                optimizer.step()

        self.model=model
        self.processor=processor

        print("three towers model training finished")

    def calculate_item_features(self,df_items):
        """
        离线计算物品塔输出特征(针对所有物品)
        """
        if self.model is None:
            raise ValueError("请先调用 train_three_towers_model 训练三塔模型 或载入已训练的三塔模型")

        idx_id_dict={idx:id for idx,id in enumerate(df_items['item_id'])}
        item_ids, item_discrete, item_continuous = self.processor.transform_item_features(
            df_items, self.item_discrete_cols, self.item_continuous_cols)
        item_features = self.model.forward_item(item_ids, item_discrete, item_continuous)
        self.item_features_dict = {idx_id_dict[idx]:feature for idx,feature in enumerate(torch.unbind(item_features, dim=0))}

    def fusion_formula(self,click_pred, cart_pred, forward_pred, buy_pred):
        return 0.4 * click_pred + 0.1 * cart_pred + 0.3 * forward_pred + 0.2 * buy_pred

    def rough_ranking(self,df_user_profile,df_recall_items,df_scene)->set[int]:
        """
        在线推荐的粗排过程
        Args:
            df_user_profile:一个用户的画像、统计特征
            df_recall_items:n个物品的画像、统计特征
        """
        if self.model is None or self.processor is None:
            raise ValueError("请先调用train_three_towers_model训练三塔模型")
        if self.item_features_dict is None:
            raise ValueError("请先调用calculate_item_features计算物品特征向量")

        print("rough ranking...")
        n_items = len(df_recall_items)
        idx_id_dict={idx:id for idx,id in enumerate(df_recall_items['item_id'])}
        self.model.eval()
        # 准备用户特征
        user_ids, user_discrete, user_continuous = self.processor.transform_user_features(
            df_user_profile, self.user_discrete_cols, self.user_continuous_cols)  # 离散特征ID转索引，连续特征归一化
        # 准备场景特征
        scene_discrete = self.processor.transform_scene_features(df_scene, self.scene_discrete_cols)
        # 准备交叉特征
        df_cross_user = df_user_profile[['user_click_last3m', 'user_cart_last3m', 'user_buy_last3m', 'user_forward_last3m']] # 1 行
        df_cross_item = df_recall_items[['item_click_last3m', 'item_cart_last3m', 'item_buy_last3m', 'item_forward_last3m']] # n 行
        df_cross_user_repeated = pd.concat([df_cross_user] * n_items, ignore_index=True)
        df_cross = pd.concat([df_cross_user_repeated, df_cross_item], axis=1)
        stat_continuous = self.processor.transform_stat_features(df_cross,self.stat_cont_cols)
        # 用户塔输出特征向量（1行）
        user_feature = self.model.forward_user(user_ids, user_discrete, user_continuous,scene_discrete)
        # 交叉塔输出特征向量（n行）
        cross_features = self.model.forward_cross(stat_continuous)
        # 物品塔输出特征向量(直接从保存的全体物品特征向量中取出来即可)
        recall_item_ids = list(df_recall_items['item_id'])
        item_features = torch.stack([self.item_features_dict[id] for id in recall_item_ids]) # 每个物品特征向量进行堆叠
        # 拼接为完整特征向量
        user_features = torch.cat([user_feature]*n_items, dim=0)
        feature_vectors = torch.cat([user_features, item_features,cross_features], dim=1)

        item_score = dict()  # 物品粗排分数字典，{item_id:score}
        click_preds, cart_preds, forward_preds, buy_preds = self.model.forward_mlps(feature_vectors)
        for i in range(len(feature_vectors)):
            item_score[idx_id_dict[i]]=self.fusion_formula(click_preds[i], cart_preds[i], forward_preds[i], buy_preds[i])
        # 根据粗排分数截断返回
        topn = heapq.nlargest(5, item_score.items(), key=lambda x: x[1])
        rough_ranking_ids = [x[0] for x in topn]

        print("粗排后剩余物品ID:", rough_ranking_ids)
        print("rough ranking finished\n")

        return set(rough_ranking_ids)