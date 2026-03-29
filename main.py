import os
os.environ["OMP_NUM_THREADS"] = "1"
import pandas as pd
from entities import *
from recall import RecallRecommender
from rough_ranking import RoughRankingRecommender
from fine_ranking import FineRankingRecommender
from rearrangement import MmrDiversity

class RecommenderSystem:
    def __init__(self):
        # 需要用到的各种数据
        self.items=[]
        self.id_item_dict={}
        self.item_idx2id={}
        self.users=[]
        self.interactions=[] # [(user_id,item_id,rating)]

        self.df_items=pd.DataFrame()
        self.df_users=pd.DataFrame()
        self.df_interactions=pd.DataFrame() # 只包含 user_id 和 item_id 的 Dataframe
        self.df_train=pd.DataFrame()
        # 各阶段的推荐器
        self.recall_recommender=RecallRecommender(self.item_idx2id)
        self.rough_ranking_recommender=RoughRankingRecommender()
        self.fine_ranking_recommender=FineRankingRecommender()
        self.rearrangement_recommender=MmrDiversity()

    def prepare_data(self):
        print("data preparing...")
        # clip用于提取图片和文本特征，拼接成为物品内容特征向量
        clip_model, preprocess = clip.load("ViT-B/32", device=device)
        # 准备物品数据
        self.df_items = pd.read_csv("data/items.csv", encoding="utf-8")
        self.df_items['item_keywords'] = self.df_items['item_keywords'].apply(lambda x: tuple(x.split(';')))
        print("开始构建物品索引...")
        for row in self.df_items.itertuples(index=True):  # index=True 获取原始的 DataFrame 索引
            item_idx = row.Index
            # 构建 discrete_features 和 continuous_features 字典
            discrete_features = {
                'city': row.city,
                'name': row.name,
                'author': row.author,
                'item_categories': row.item_categories,
                'item_keywords': row.item_keywords
            }
            continuous_features = {'price': row.price}
            # 创建 Item 对象
            item = Item(
                item_idx,
                row.item_id,
                row.name,
                row.author,
                row.item_categories,
                row.item_keywords,
                discrete_features,
                continuous_features,
                row.created_time,
                row.image,
                row.description
            )
            # 填充字典和列表
            self.id_item_dict[item.item_id] = item
            self.item_idx2id[item_idx] = item.item_id
            # 计算特征
            item.calculate_content_feature(clip_model, preprocess)
            self.items.append(item)
        print("物品索引构建完成")
        # 准备用户数据
        self.df_users = pd.read_csv("data/users.csv", encoding="utf-8")
        self.df_users['user_categories'] = self.df_users['user_categories'].fillna('').apply(lambda x: tuple(x.split(';')))
        self.df_users['user_keywords'] = self.df_users['user_keywords'].fillna('').apply(lambda x: tuple(x.split(';')))
        print("开始构建用户画像...")
        # itertuples 返回命名元组，访问速度极快
        for row in self.df_users.itertuples(index=True):
            # 获取原始索引
            user_idx = row.Index
            # 构建离散和连续特征字典
            discrete_features = {
                'gender': row.gender,
                'user_categories': row.user_categories,
                'user_keywords': row.user_keywords
            }
            continuous_features = {'age': row.age}
            # 创建 UserProfile 对象
            user = UserProfile(
                user_idx,
                row.user_id,
                row.user_categories,
                row.user_keywords,
                discrete_features,
                continuous_features,
                50
            )
            self.users.append(user)
        print("用户画像构建完成")
        # 准备交互数据
        self.df_interactions = pd.read_csv("data/interactions.csv", encoding="utf-8")
        self.interactions=list(zip(
            self.df_interactions['user_id'],
            self.df_interactions['item_id'],
            self.df_interactions['rating']
        ))
        # 准备双塔模型，三塔模型和精排多目标模型训练数据(根据交互记录)
        df_merge = pd.merge(self.df_interactions, self.df_users, how='left', on='user_id')
        self.df_train = pd.merge(df_merge, self.df_items, how='left', on='item_id')
        print("data finished\n")

    def fit(self):
        # 召回
        self.recall_recommender.fit(self.df_train,self.items,self.interactions,self.df_interactions
                                    ,list(self.df_users['user_id']),list(self.df_items['item_id']))
        # 粗排
        self.rough_ranking_recommender.train_three_towers_model(self.df_train)
        # 精排
        self.fine_ranking_recommender.train_multi_task_model(self.df_train)
        # 重排
        self.rearrangement_recommender.build_cosine_similarity_matrix(self.items)

    def recommend(self,user_id,hour,is_weekend,is_holiday):
        """
        Args:
            user_id: 用户ID
            hour: 当前时间(小时)
            is_weekend: 当前是否是周末
            is_holiday: 当前是否是节假日
        """
        user_profile = next((user for user in self.users if user.user_id == user_id),None)
        df_user_profile=self.df_users.loc[self.df_users['user_id']==user_id]
        # ============= 召回 ================
        recall_items_ids=self.recall_recommender.recall(user_profile)
        # ============= 粗排 ================
        # 提取召回的id列表对应的行
        df_recall_items = self.df_items[self.df_items['item_id'].isin(recall_items_ids)].reset_index(drop=True)
        df_user = pd.concat([df_user_profile] * len(df_recall_items)).reset_index(drop=True)
        df_rough_ranking = pd.concat([df_recall_items.copy(), df_user.copy()], axis=1)  # 因为下面还要用，用copy防止出现未知bug
        # 添加上当前的场景特征，统计特征已经包含在df_three_towers中了
        # df_three_towers中的每一行代表一个要进行打分的物品，除了物品特征以外，用户特征，用户统计特征和场景特征均相同
        df_rough_ranking['hour'] = [hour] * len(df_rough_ranking)
        df_rough_ranking['is_weekend'] = [is_weekend] * len(df_rough_ranking)
        df_rough_ranking['is_holiday'] = [is_holiday] * len(df_rough_ranking)
        rough_ranking_ids = self.rough_ranking_recommender.rough_ranking(df_rough_ranking)
        # ============= 精排 ================
        df_rough_items = df_recall_items[df_recall_items['item_id'].isin(rough_ranking_ids)].reset_index(drop=True)
        df_user = pd.concat([df_user_profile] * len(df_rough_items)).reset_index(drop=True)
        df_multi_task = pd.concat([df_rough_items, df_user], axis=1)
        # 添加上当前的场景特征
        df_multi_task['hour'] = [hour] * len(df_multi_task)
        df_multi_task['is_weekend'] = [is_weekend] * len(df_multi_task)
        df_multi_task['is_holiday'] = [is_holiday] * len(df_multi_task)
        item_id_score = self.fine_ranking_recommender.fine_ranking(df_multi_task)
        # ============= 重排 ================
        print("rearrangement...")
        rearrangement_items = []
        for item in self.items:
            if item.item_id in item_id_score:
                item.relevance_score = item_id_score[item.item_id]
                rearrangement_items.append(item)
        final_selected_items=self.rearrangement_recommender.mmr_diversity_selection(rearrangement_items)
        print("最终推荐的物品及顺序：", final_selected_items)
        print("recommend finished\n")

def main():
    """主流程"""
    recommender_system = RecommenderSystem()
    recommender_system.prepare_data()
    recommender_system.fit()
    recommender_system.recommend("U1001",15,1,0)

if __name__ == '__main__':
    main()