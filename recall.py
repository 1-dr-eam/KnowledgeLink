import math
import string
from collections import defaultdict,deque
from heapq import nlargest
import random
from faker import Faker
from sklearn.cluster import KMeans
from sklearn.metrics.pairwise import cosine_similarity
import numpy as np

class ItemCF:
    def __init__(self):
        self.item_sim_matrix = {}      # {item_i: {item_j: sim}}
        self.item_norm_sq = {}         # N_i = sum_u (r_ui)^2
        self.user_items_rating = {}    # 关键索引1：用户历史行为{user: {item: rating}}
        self.item_sim={}               # 关键索引2：每个物品最相似的n个物品{item:items}

    def fit(self, user_item_rating_list,top_k=10):
        """
        :param user_item_rating_list: List of (user_id, item_id, rating)
        :param top_k:每个物品索引topk个最相似的物品
        """
        # Step 1: 构建用户-物品-评分字典，并计算每个物品的 ||r_i||^2
        self.user_items_rating = defaultdict(dict)
        self.item_norm_sq = defaultdict(float)

        for user, item, rating in user_item_rating_list:
            self.user_items_rating[user][item] = rating # 构造关键索引1
            self.item_norm_sq[item] += rating * rating  # 求和

        # Step 2: 计算加权共现 C[i][j] = sum_u (r_ui * r_uj)，这部分是余弦相似度的分子
        cooccur = defaultdict(lambda: defaultdict(float)) # {item_i:{item_j:sum_score}}
        for user, item_rating_dict in self.user_items_rating.items():
            items = list(item_rating_dict.keys())
            n = len(items)
            for i in range(n):
                for j in range(i + 1, n):
                    a, b = items[i], items[j]
                    r_ua = item_rating_dict[a]
                    r_ub = item_rating_dict[b]
                    weight = r_ua * r_ub
                    cooccur[a][b] += weight
                    cooccur[b][a] += weight  # 对称

        # Step 3: 计算余弦相似度
        self.item_sim_matrix = defaultdict(dict)
        self.item_sim = defaultdict(dict)
        for item_i, neighbors in cooccur.items():
            for item_j, c_ij in neighbors.items():
                norm_i = self.item_norm_sq[item_i]
                norm_j = self.item_norm_sq[item_j]
                if norm_i > 0 and norm_j > 0:
                    sim = c_ij / math.sqrt(norm_i * norm_j) # norm_i和norm_i已经是求和后的值了
                    self.item_sim_matrix[item_i][item_j] = sim

        # 构造关键索引2
        for item,neighbors in self.item_sim_matrix.items():
            top_neighbors = nlargest(top_k, neighbors.items(), key=lambda x: x[1])
            self.item_sim[item]=dict(top_neighbors)

        print("ItemCF training completed.")

    def itemCF_recommend(self, user_id, n_rec=50):
        """
        推荐时考虑用户对历史物品的评分强度
        score(j) = sum_{i in hist} r_ui * sim(i, j)
        """
        if user_id not in self.user_items_rating:
            return []

        user_hist = self.user_items_rating[user_id]  # {item: rating}
        item_scores = defaultdict(float) # # {item: score}

        for item_i, r_ui in user_hist.items():
            if item_i not in self.item_sim:
                continue
            top_neighbors = self.item_sim[item_i]

            for item_j, sim_score in top_neighbors.items():
                if item_j in user_hist:
                    continue  # 不推荐已交互过的
                item_scores[item_j] += r_ui * sim_score  # 加权累加，用户对某物品的交互等级*物品与物品的相似度

        recs = nlargest(n_rec, item_scores.items(), key=lambda x: x[1]) # [(item:final_score)]
        res = [x[0] for x in recs]
        return set(res) # 只返回物品ID集合

class UserCF:
    def __init__(self):
        self.user_sim_matrix = {}       # {u: {v: sim}}
        self.user_top_sim={}                # 关键索引2：{u: {v: sim}}
        self.user_items_rating = {}     # 关键索引1：{user: {item: rating}}
        self.item_users = {}            # 反向索引：{item: {user: rating}}，用于快速找谁评过分

    def fit(self, user_item_rating_list,top_k=10):
        """
        训练 UserCF 模型（用户之间的相似度只用数量计算）
        :param user_item_rating_list: List of (user_id, item_id, rating)
        """
        # Step 1: 构建正向和反向索引，并计算用户向量的 L2 范数平方
        self.user_items_rating = defaultdict(dict)
        self.item_users = defaultdict(dict)

        for user, item, rating in user_item_rating_list:
            self.user_items_rating[user][item] = rating
            self.item_users[item][user] = rating

        # Step 2: 计算用户共现（通过共同物品）
        # 优化：遍历每个物品，对该物品的所有用户两两组合
        cooccur = defaultdict(lambda: defaultdict(float))
        for item, user_rating_dict in self.item_users.items():
            users = list(user_rating_dict.keys())
            n = len(users)
            for i in range(n):
                for j in range(i + 1, n):
                    u, v = users[i], users[j]
                    cooccur[u][v] += 1
                    cooccur[v][u] += 1  # 对称

        # Step 3: 计算相似度
        self.user_sim_matrix = defaultdict(dict)
        self.user_top_sim = defaultdict(dict)
        for u, neighbors in cooccur.items():
            for v, inner_prod in neighbors.items():
                sim = cooccur[u][v] / math.sqrt(len(self.user_items_rating[u].keys())*len(self.user_items_rating[v].keys()))
                self.user_sim_matrix[u][v] = sim

        for user,neighbors in self.user_sim_matrix.items():
            top_neighbors = nlargest(top_k, neighbors.items(), key=lambda x: x[1])
            self.user_top_sim[user]=dict(top_neighbors)

        print("UserCF training completed.")

    def userCF_recommend(self, user_id, n_rec=50):
        """
        为用户推荐物品
        :param user: 目标用户ID
        :param n_rec: 推荐 top-N 物品
        :return: set of item_id
        """
        if user_id not in self.user_items_rating:
            return []

        user_hist = set(self.user_items_rating[user_id].keys())
        item_scores = defaultdict(float) # {item:score}

        # 获取最相似的 top_k_sim_users 个用户
        if user_id not in self.user_top_sim:
            return []
        sim_users = self.user_top_sim[user_id] # {users:scores}
        # 遍历每个相似用户 v
        for v, sim_uv in sim_users.items():
            # 遍历 v 交互过但 user 未交互的物品
            for item, rating_v in self.user_items_rating[v].items():
                if item in user_hist:
                    continue
                # 加权累加：相似度 × v 对 item 的兴趣度分数
                item_scores[item] += sim_uv * rating_v

        # 返回 top-n_rec
        recs = nlargest(n_rec, item_scores.items(), key=lambda x: x[1])
        res=[x[0] for x in recs]
        return set(res)

class CFRecommender:
    """
    封装类，封装两种协同过滤召回方法
    """
    def __init__(self):
        self.user_cf=UserCF()
        self.item_cf=ItemCF()

    def fit(self, user_item_rating_list,top_k=10):
        self.user_cf.fit(user_item_rating_list,top_k)
        self.item_cf.fit(user_item_rating_list,top_k)

    def cf_recommend(self, user_id, n_rec=50):
        user_cf_recalls=self.user_cf.userCF_recommend(user_id, n_rec)
        item_cf_recalls=self.item_cf.itemCF_recommend(user_id, n_rec)
        return user_cf_recalls.union(item_cf_recalls)

class Note:
    """笔记类，表示系统中的内容项"""
    def __init__(self, note_id, title, categories, keywords, created_time,content_feature):
        """
        初始化笔记对象
        Args:
            note_id: 笔记ID
            title: 笔记标题
            categories: 笔记所属类目列表
            keywords: 笔记包含的关键词列表
            created_time: 笔记创建时间
            content_feature: 基于内容的特征向量
        """
        self.note_id = note_id
        self.title = title
        self.categories = categories
        self.keywords = keywords
        self.created_time = created_time
        self.content_feature = content_feature

class UserProfile:
    """用户画像类"""
    def __init__(self, user_id, categories=None, keywords=None,max_history=50):
        """
        初始化用户画像
        Args:
            user_id: 用户ID
            categories: 用户感兴趣的类目列表
            keywords: 用户感兴趣的关键词列表
            max_history: 用户历史交互队列的最大长度
        """
        self.user_id = user_id
        self.categories = categories  # 存储用户感兴趣的类目
        self.keywords = keywords  # 存储用户感兴趣的关键词
        self.interaction_history = deque(maxlen=max_history)  # 使用双端队列限制历史长度

    def add_interaction(self, note_id):
        """添加用户交互记录"""
        self.interaction_history.append(note_id)

    def get_last_n_interactions(self, n):
        """获取用户最近的n次交互记录"""
        return list(self.interaction_history)[-n:]

class ClassificationRecommender:
    """基于类目和关键词的召回"""
    def __init__(self):
        # 类目到笔记列表的索引（按创建时间倒序排列）
        self.category_index = defaultdict(list)
        # 关键词到笔记列表的索引（按创建时间倒序排列）
        self.keyword_index = defaultdict(list)
        # 所有笔记的映射（用于快速查找）{note_id:note}
        self.notes_map = {}

    def add_note(self, note):
        """添加笔记到系统"""
        self.notes_map[note.note_id] = note

        # 添加到类目索引
        for category in note.categories:
            self.category_index[category].append(note)

        # 添加到关键词索引
        for keyword in note.keywords:
            self.keyword_index[keyword].append(note)

    def build_indices(self):
        """构建索引（按创建时间倒序排列）"""
        for category, notes in self.category_index.items():
            # 按创建时间倒序排列
            self.category_index[category] = sorted(notes, key=lambda x: x.created_time, reverse=True)

        for keyword, notes in self.keyword_index.items():
            # 按创建时间倒序排列
            self.keyword_index[keyword] = sorted(notes, key=lambda x: x.created_time, reverse=True)

    def category_recall(self, user_categories, topk=10)->set[Note]:
        """
        类目召回通道
        Args:
            user_categories: 用户感兴趣的类目列表
            topk: 每个类目召回的笔记数量
        Returns:
            召回的笔记id列表
        """
        recalled_notes = []

        for category in user_categories:
            if category in self.category_index:
                # 从对应类目中取出topk个笔记
                category_notes = self.category_index[category][:topk]
                recalled_notes.extend(category_notes)

        # 去重并返回
        return set(recalled_notes)

    def keyword_recall(self, user_keywords, topk=10):
        """
        关键词召回通道
        Args:
            user_keywords: 用户感兴趣的关键词列表
            topk: 每个关键词召回的笔记数量
        Returns:
            召回的笔记列表
        """
        recalled_notes = []

        for keyword in user_keywords:
            if keyword in self.keyword_index:
                # 从对应关键词中取出topk个笔记
                keyword_notes = self.keyword_index[keyword][:topk]
                recalled_notes.extend(keyword_notes)

        # 去重并返回
        return set(recalled_notes)

    def classification_recommend(self, user_profile, topk_per_channel=10):
        """
        基于类别的推荐主函数
        Args:
            user_profile: 用户画像对象
            topk_per_channel: 每个召回通道返回的笔记数量
        Returns:
            召回池中的笔记列表
        """
        # 从用户画像中获取兴趣类目和关键词
        user_categories = user_profile.categories
        user_keywords = user_profile.keywords

        # 类目召回
        category_recalled = self.category_recall(user_categories, topk=topk_per_channel)

        # 关键词召回
        keyword_recalled = self.keyword_recall(user_keywords, topk=topk_per_channel)

        # 合并召回结果（去重）
        return category_recalled.union(keyword_recalled)

class ClusteringRecommender:
    """基于聚类的推荐系统"""
    def __init__(self, n_clusters=100):
        """
        初始化聚类推荐器
        Args:
            n_clusters: 聚类数量
        """
        self.n_clusters = n_clusters
        self.kmeans = KMeans(n_clusters=n_clusters, random_state=42, n_init=10)
        self.cluster_centers = None  # 聚类中心向量
        self.notes_by_cluster = defaultdict(list)  # 每个聚类包含的笔记{cluster:notes}

        self.notes_map = {}  # 笔记ID到笔记对象的映射
        self.all_note_features = []  # 所有笔记的特征向量
        self.note_ids = []  # 笔记ID列表，与特征向量顺序一致

    def add_note(self, note):
        """添加单条笔记"""
        self.notes_map[note.note_id] = note
        self.all_note_features.append(note.content_feature)
        self.note_ids.append(note.note_id)

    def fit_clustering(self):
        """训练聚类模型"""
        if not self.all_note_features:
            raise ValueError("没有笔记数据可供聚类")

        # 将特征转换为numpy数组
        X = np.array(self.all_note_features)

        # 训练K-means模型
        cluster_labels = self.kmeans.fit_predict(X)

        # 保存聚类中心
        self.cluster_centers = self.kmeans.cluster_centers_

        # 根据聚类标签组织笔记
        for i, label in enumerate(cluster_labels):
            note_id = self.note_ids[i] # 取出id
            note = self.notes_map[note_id] # 取出笔记对象
            self.notes_by_cluster[label].append(note)

    def get_nearest_cluster(self, seed_note):
        """
        找到种子笔记最接近的聚类
        Args:
            seed_note: 种子笔记对象
        Returns:
            最近聚类的索引
        """
        if self.cluster_centers is None:
            raise ValueError("聚类模型尚未训练")

        # 计算种子笔记特征向量与各聚类中心的余弦相似度
        similarities = cosine_similarity([seed_note.content_feature], self.cluster_centers)[0]

        # 返回相似度最高的聚类索引
        nearest_cluster_idx = np.argmax(similarities)
        return nearest_cluster_idx

    def find_similar_notes_in_cluster(self, seed_note, cluster_idx, m):
        """
        在指定聚类中找到与种子笔记最相似的m篇笔记
        Args:
            seed_note: 种子笔记对象
            cluster_idx: 聚类索引
            m: 要返回的笔记数量
        Returns:
            与种子笔记最相似的笔记列表
        """
        if cluster_idx not in self.notes_by_cluster:
            return []

        cluster_notes = self.notes_by_cluster[cluster_idx] # 所有待选的同类笔记

        # 计算种子笔记与聚类中所有笔记的余弦相似度
        similarities = []
        for note in cluster_notes:
            # 避免返回种子笔记本身
            if note.note_id == seed_note.note_id:
                continue

            sim = cosine_similarity([seed_note.content_feature], [note.content_feature])[0][0]
            similarities.append((note, sim))

        # 按相似度降序排序
        similarities.sort(key=lambda x: x[1], reverse=True)

        # 返回前m个最相似的笔记
        similar_notes = [item[0] for item in similarities[:m]]
        return similar_notes

    def clustering_recommend(self, user_profile, last_n=20, m=10)->set[Note]:
        """
        基于聚类的召回方法
        Args:
            user_profile: 用户画像对象
            last_n: 使用用户最近交互的n个笔记作为种子
            m: 每个种子笔记在对应聚类中召回的笔记数量
        Returns:
            召回的笔记列表
        """
        if self.cluster_centers is None:
            raise ValueError("聚类模型尚未训练")

        # 获取用户最近交互的笔记ID
        recent_note_ids = user_profile.get_last_n_interactions(last_n)

        if not recent_note_ids:
            return set()

        # 存储召回的笔记（去重）
        recalled_notes = set()

        # 对每个种子笔记进行处理
        for note_id in recent_note_ids:
            if note_id not in self.notes_map:
                continue

            seed_note = self.notes_map[note_id]

            # 找到种子笔记所属的最近聚类
            nearest_cluster_idx = self.get_nearest_cluster(seed_note)

            # 在该聚类中找到最相似的m篇笔记
            similar_notes = self.find_similar_notes_in_cluster(seed_note, nearest_cluster_idx, m)

            # 添加到召回结果中（去重）
            for note in similar_notes:
                if note not in recalled_notes:
                    recalled_notes.add(note)

        return recalled_notes

class ColdStartRecommender:
    """
    封装类，封装用于冷启动的类目召回，关键词召回和内容向量聚类召回
    """
    def __init__(self,user_profile, notes,n_clusters=100):
        self.notes = notes
        self.user_profile = user_profile
        self.clustering_recommender = ClusteringRecommender(n_clusters=n_clusters)
        self.classification_recommender = ClassificationRecommender()

    def fit(self):
        # 数据准备
        for note in self.notes:
            self.clustering_recommender.add_note(note)
            self.classification_recommender.add_note(note)
        # 聚类
        self.clustering_recommender.fit_clustering()
        # 类目和关键词
        self.classification_recommender.build_indices()

    def cold_start_recommend(self)->set[Note]:
        # 聚类
        clustering_recalls=self.clustering_recommender.clustering_recommend(user_profile=self.user_profile, last_n=50, m=10)
        # 类目和关键词
        classification_recalls=self.classification_recommender.classification_recommend(user_profile=self.user_profile,topk_per_channel=50)
        # 合并去重
        return classification_recalls.union(clustering_recalls)

def generate_mock_data(notes_number=100):
    """生成模拟数据"""
    # 创建一些模拟笔记
    notes = []
    fake=Faker()
    # 模拟的类目和关键词
    categories_list = [
        ["科技", "人工智能"],
        ["生活", "美食"],
        ["旅行", "摄影"],
        ["教育", "学习"],
        ["娱乐", "电影"],
        ["健康", "运动"],
        ["财经", "投资"],
        ["时尚", "美妆"]
    ]

    keywords_list = [
        ["AI", "机器学习", "深度学习"],
        ["烹饪", "菜谱", "美食"],
        ["旅游攻略", "景点推荐", "摄影技巧"],
        ["考试", "学习方法", "知识分享"],
        ["电影推荐", "影评", "娱乐八卦"],
        ["健身", "减肥", "营养"],
        ["股票", "基金", "理财"],
        ["护肤", "化妆", "穿搭"]
    ]

    for i in range(notes_number):
        # 随机选择类目和关键词
        categories = random.sample(categories_list[random.randint(0, len(categories_list) - 1)],
                                   min(2, len(categories_list[random.randint(0, len(categories_list) - 1)])))
        keywords = random.sample(keywords_list[random.randint(0, len(keywords_list) - 1)],
                                 min(3, len(keywords_list[random.randint(0, len(keywords_list) - 1)])))

        # 生成随机特征向量（模拟图像+文本特征的拼接）
        feature_vector = np.random.randn(256).astype(np.float32)
        # 归一化特征向量（便于余弦相似度计算）
        feature_vector = feature_vector / np.linalg.norm(feature_vector)
        title = f"笔记{i}: {' '.join(categories[:1])}相关内容"
        note = Note(
            note_id=i,
            title=title,
            categories=categories,
            keywords=keywords,
            created_time=fake.date(),
            content_feature=feature_vector
        )
        notes.append(note)

    return notes


# 定义可能的类目和关键词池
CATEGORIES_POOL = [
    "科技", "人工智能", "机器学习", "深度学习", "数据科学", "编程", "软件开发",
    "互联网", "移动应用", "前端开发", "后端开发", "云计算", "大数据", "区块链",
    "物联网", "网络安全", "游戏开发", "虚拟现实", "增强现实", "机器人技术",
    "电子商务", "社交媒体", "数字营销", "产品管理", "用户体验", "界面设计",
    "创业", "投资", "金融科技", "生物科技", "医疗健康", "教育科技"
]

KEYWORDS_POOL = [
    "AI", "深度学习", "编程", "Python", "Java", "JavaScript", "React", "Vue",
    "Node.js", "Django", "Flask", "TensorFlow", "PyTorch", "机器学习", "数据分析",
    "云计算", "AWS", "Azure", "Docker", "Kubernetes", "区块链", "比特币",
    "以太坊", "智能合约", "物联网", "5G", "边缘计算", "大数据", "Hadoop",
    "Spark", "SQL", "NoSQL", "MongoDB", "Redis", "网络安全", "加密",
    "UI设计", "UX设计", "产品经理", "敏捷开发", "DevOps", "测试", "自动化"
]


def generate_random_user_profile(index):
    """
    生成随机用户画像
    """
    # 生成随机的user_id
    user_id = f"user_{index:03d}_{''.join(random.choices(string.ascii_lowercase + string.digits, k=4))}"

    # 随机选择2-5个类目
    num_categories = random.randint(2, 5)
    categories = random.sample(CATEGORIES_POOL, min(num_categories, len(CATEGORIES_POOL)))

    # 随机选择3-7个关键词
    num_keywords = random.randint(3, 7)
    keywords = random.sample(KEYWORDS_POOL, min(num_keywords, len(KEYWORDS_POOL)))

    return UserProfile(
        user_id=user_id,
        categories=categories,
        keywords=keywords,
        max_history=50
    )

def main():
    """召回系统"""
    print("开始演示冷启动召回系统...\n")
    # =============== 数据准备 ==================
    # 1000条笔记
    notes=generate_mock_data(1000)
    # 生成100个随机用户画像
    user_profiles = [generate_random_user_profile(i) for i in range(1, 101)]
    # 对于每一个用户，生成10条随机交互记录
    user_item_rating_list=[]
    for user_profile in user_profiles:
        for i in range(20):
            note_id = random.randint(0,1000)
            ranking = random.randint(1,4)
            user_profile.add_interaction(note_id)
            user_item_rating_list.append((user_profile.user_id, note_id, ranking))
    # 给第一个用户做推荐
    user_profile=user_profiles[0]
    # =============== 协同过滤推荐 ================
    cf_recommender = CFRecommender()
    cf_recommender.fit(user_item_rating_list,10)
    cf_recall_notes_ids=cf_recommender.cf_recommend(user_profile.user_id,50)
    cf_recall_notes=set([notes[id] for id in cf_recall_notes_ids])
    print("cf_recall:",len(cf_recall_notes))
    # ===============  冷启动推荐  ===============
    cold_start_recommender = ColdStartRecommender(user_profile, notes,50)
    cold_start_recommender.fit()
    cold_recall_notes=cold_start_recommender.cold_start_recommend()
    print("cold_recall:",len(cold_recall_notes))
    # 合并去重
    recall_notes=cf_recall_notes.union(cold_recall_notes)
    # ========= 显示召回结果 =========
    print(f"用户ID: {user_profile.user_id}")
    print(f"感兴趣类目: {user_profile.categories}")
    print(f"感兴趣关键词: {user_profile.keywords}\n")
    print(f"召回池中共有 {len(recall_notes)} 个笔记\n")


    print("-" * 80)
    print(f"{'排名':<4} {'笔记ID':<10} {'标题':<20} {'类目':<15} {'关键词':<20} {'创建时间':<12}")
    print("-" * 80)

    # 按创建时间倒序显示（最近的在前面）
    sorted_recall_pool = sorted(recall_notes, key=lambda x: x.created_time, reverse=True)

    for i, note in enumerate(sorted_recall_pool, 1):
        categories_str = ", ".join(note.categories[:2])  # 只显示前2个类目
        keywords_str = ", ".join(note.keywords[:3])  # 只显示前3个关键词
        time_str = note.created_time

        print(f"{i:<4} {note.note_id:<10} {note.title[:18]:<20} {categories_str:<15} {keywords_str:<20} {time_str:<12}")

    print("-" * 80)

if __name__ == "__main__":
    main()
