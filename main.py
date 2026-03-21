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
        self.users=[]
        self.interactions=[]

        self.df_items=pd.DataFrame()
        self.df_users=pd.DataFrame()
        self.df_train=pd.DataFrame()
        # 各阶段的推荐器
        self.recall_recommender=RecallRecommender()
        self.rough_ranking_recommender=RoughRankingRecommender()
        self.fine_ranking_recommender=FineRankingRecommender()
        self.rearrangement_recommender=MmrDiversity()

    def prepare_data(self):
        print("data preparing...")
        # clip用于提取图片和文本特征，拼接成为物品内容特征向量
        clip_model, preprocess = clip.load("ViT-B/32", device=device)
        # 准备物品数据
        self.df_items = pd.read_csv("data/items.csv", encoding="utf-8")
        for index, row in self.df_items.iterrows():
            # 这里使用冗余策略，如item_categories既直接保存在categories属性中，又保存在discrete_features中
            # 保存在discrete_features中是双塔模型训练需要，而直接保存在categories属性中是为了冷启动召回提速
            item = Item(row["item_id"], row["name"], row["item_categories"].split('/'), row['item_keywords'].split(';')
                        , {'city': row['city'], 'name': row['name'], 'item_categories': row['item_categories'],
                           'item_keywords': row['item_keywords']},
                        {'price': row['price']}
                        , row["created_time"], row['image'], row['description'])
            self.id_item_dict[item.item_id] = item
            item.calculate_content_feature(clip_model, preprocess)
            self.items.append(item)
        # 准备用户数据
        self.df_users = pd.read_csv("data/users.csv", encoding="utf-8")
        for index, row in self.df_users.iterrows():
            user = UserProfile(row["user_id"], row["user_categories"].split(';'), row["user_keywords"].split(';')
                               , {'gender': row['gender'], 'user_categories': row['user_categories'],
                                  'user_keywords': row['user_keywords']},
                               {'age': row['age']}, 50)
            self.users.append(user)
        # 准备交互数据
        df_interactions = pd.read_csv("data/interactions.csv", encoding="utf-8")
        for index, row in df_interactions.iterrows():
            self.interactions.append((row['user_id'], row['item_id'], row['rating']))
        # 准备双塔模型，三塔模型和精排多目标模型训练数据(根据交互记录)
        df_merge = pd.merge(df_interactions, self.df_users, how='left', on='user_id')
        self.df_train = pd.merge(df_merge, self.df_items, how='left', on='item_id')
        print("data finished\n")

    def fit(self):
        # 召回
        self.recall_recommender.fit(self.items,self.interactions,self.df_train)
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
        rearrangement_items = []
        for item in self.items:
            if item.item_id in item_id_score:
                item.relevance_score = item_id_score[item.item_id]
                rearrangement_items.append(item)
        final_selected_items=self.rearrangement_recommender.mmr_diversity_selection(rearrangement_items)
        print("最终推荐的物品及顺序：", final_selected_items)

def main():
    """主流程"""
    recommender_system = RecommenderSystem()
    recommender_system.prepare_data()
    recommender_system.fit()
    recommender_system.recommend("U1001",15,1,0)

if __name__ == '__main__':
    main()