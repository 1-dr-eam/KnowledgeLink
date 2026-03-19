import math
from collections import defaultdict,deque
from heapq import nlargest
from PIL import Image
from io import BytesIO
import requests
from sklearn.cluster import KMeans
from sklearn.metrics.pairwise import cosine_similarity
import numpy as np
from twin_towers_model import *
import clip
from entities import *
from recall import recall

def main():
    """主流程"""
    # =================================================
    # =================== 数据准备 ======================
    # =================================================
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
    # 准备双塔模型训练数据(根据交互记录)
    df_train = pd.merge(df_interactions, df_users, how='left', on='user_id')
    df_train = pd.merge(df_train, df_items, how='left', on='item_id')
    # =================================================
    # ==================== 召回 ========================
    # =================================================
    # 对第一个用户做推荐
    recall_items=recall(items,interactions,df_train,id_item_dict,users[0])
    print("召回物品：",recall_items)

if __name__ == '__main__':
    main()