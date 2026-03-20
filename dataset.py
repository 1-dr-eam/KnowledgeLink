import pandas as pd
import torch
from torch.utils.data import Dataset
import random

# ----------------------------
# 数据集类（支持负采样 1:2）
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

class ThreeTowerDataset(Dataset):
    """三塔模型数据集(精排中的多目标模型复用)"""
    def __init__(self, df, processor,
                 user_discrete_cols, item_discrete_cols,
                 scene_discrete_cols, user_cont_cols, item_cont_cols, stat_cont_cols,
                 target_cols=None):
        self.df = df.reset_index(drop=True)
        self.processor = processor
        self.user_discrete_cols = user_discrete_cols
        self.item_discrete_cols = item_discrete_cols
        self.scene_discrete_cols = scene_discrete_cols
        self.user_cont_cols = user_cont_cols
        self.item_cont_cols = item_cont_cols
        self.stat_cont_cols = stat_cont_cols
        self.target_cols = target_cols

    def __len__(self):
        return len(self.df)

    def __getitem__(self, idx):
        row = self.df.iloc[idx]

        # 转换特征
        user_ids, user_discrete, user_continuous = \
            self.processor.transform_user_features(
                pd.DataFrame([row]), self.user_discrete_cols, self.user_cont_cols) # 离散特征ID转索引，连续特征归一化

        scene_discrete=self.processor.transform_scene_features(pd.DataFrame([row]),self.scene_discrete_cols)

        item_ids, item_discrete, item_continuous = \
            self.processor.transform_item_features(
                pd.DataFrame([row]), self.item_discrete_cols, self.item_cont_cols)

        stat_continuous = self.processor.transform_stat_features(
            pd.DataFrame([row]), self.stat_cont_cols)

        # 获取目标值
        targets = torch.tensor([row[col] for col in self.target_cols], dtype=torch.float32)

        return {
            'user_ids': user_ids.squeeze(),
            'user_discrete': user_discrete.squeeze(),
            'user_continuous': user_continuous.squeeze(),
            'scene_discrete': scene_discrete.squeeze(),
            'item_ids': item_ids.squeeze(),
            'item_discrete': item_discrete.squeeze(),
            'item_continuous': item_continuous.squeeze(),
            'stat_continuous': stat_continuous.squeeze(),
            'targets': targets
        }