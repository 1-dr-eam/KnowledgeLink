import pandas as pd
from entities import *
from recall import recall
from rough_ranking import train_three_towers_model,rough_ranking

def main():
    """主流程"""
    # =================================================
    # =================== 数据准备 ======================
    # =================================================
    print("data preparing...")
    # clip用于提取图片和文本特征，拼接成为物品内容特征向量
    clip_model, preprocess = clip.load("ViT-B/32", device=device)
    # 准备物品数据
    items = []
    id_item_dict = {}
    df_items = pd.read_csv("data/items.csv", encoding="utf-8")
    for index, row in df_items.iterrows():
        # 这里使用冗余策略，如item_categories既直接保存在categories属性中，又保存在discrete_features中
        # 保存在discrete_features中是双塔模型训练需要，而直接保存在categories属性中是为了冷启动召回提速
        item = Item(row["item_id"], row["name"], row["item_categories"].split('/'), row['item_keywords'].split(';')
                    , {'city': row['city'], 'name': row['name'], 'item_categories': row['item_categories'],
                       'item_keywords': row['item_keywords']},
                    {'price': row['price']}
                    , row["created_time"], row['image'], row['description'])
        id_item_dict[item.item_id] = item
        item.calculate_content_feature(clip_model, preprocess)
        items.append(item)
    # 准备用户数据
    users = []
    df_users = pd.read_csv("data/users.csv", encoding="utf-8")
    for index, row in df_users.iterrows():
        user = UserProfile(row["user_id"], row["user_categories"].split(';'), row["user_keywords"].split(';')
                           , {'gender': row['gender'], 'user_categories': row['user_categories'],
                              'user_keywords': row['user_keywords']},
                           {'age': row['age']}, 50)
        users.append(user)
    # 准备交互数据
    df_interactions = pd.read_csv("data/interactions.csv", encoding="utf-8")
    interactions = []
    for index, row in df_interactions.iterrows():
        interactions.append((row['user_id'], row['item_id'], row['rating']))
    # 准备双塔模型，三塔模型和精排多目标模型训练数据(根据交互记录)
    df_train = pd.merge(df_interactions, df_users, how='left', on='user_id')
    df_train = pd.merge(df_train, df_items, how='left', on='item_id')
    print("data finished\n")
    # =================================================
    # ==================== 召回 ========================
    # =================================================
    # 对第一个用户做推荐
    # 其中包含了双塔模型训练与推荐
    recall_items_ids=recall(items.copy(),interactions.copy(),df_train.copy(),id_item_dict,users[0])
    # =================================================
    # ==================== 粗排 ========================
    # =================================================
    # 训练三塔模型
    three_towers_model,three_processor=train_three_towers_model(df_train.copy())
    # 提取召回的id列表对应的行
    df_recall_items = df_items[df_items['item_id'].isin(recall_items_ids)].reset_index(drop=True)
    df_user = pd.DataFrame([df_users.iloc[0]]*len(df_recall_items)).reset_index(drop=True)
    df_three_towers=pd.concat([df_recall_items,df_user],axis=1)
    # 添加上当前的场景特征，统计特征已经包含在df_three_towers中了
    df_three_towers['hour']=[15]*len(df_three_towers)
    df_three_towers['is_weekend'] = [1] * len(df_three_towers)
    df_three_towers['is_holiday'] = [0] * len(df_three_towers)
    # df_three_towers中的每一行代表一个要进行打分的物品，除了物品特征以外，用户特征，用户统计特征和场景特征均相同
    rough_ranking_ids=rough_ranking(three_towers_model,three_processor,df_three_towers)

if __name__ == '__main__':
    main()