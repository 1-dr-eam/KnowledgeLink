import math
from collections import defaultdict
from heapq import nlargest
from datetime import datetime, timedelta
import random
from faker import Faker

class ItemCF:
    def __init__(self):
        self.item_sim_matrix = {}      # {item_i: {item_j: sim}}
        self.item_norm_sq = {}         # N_i = sum_u (r_ui)^2
        self.user_items_rating = {}    # 关键索引1：用户历史行为{user: {item: rating}}
        self.item_sim={}               # 关键索引2：每个物品最相似的n个物品{item:items}

    def fit(self, user_item_rating_list,top_k=10):
        """
        :param user_item_rating_list: List of (user_id, item_id, rating)
        :param n:每个物品索引topk个最相似的物品
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

    def recommend(self, user, n_rec=50):
        """
        推荐时考虑用户对历史物品的评分强度
        score(j) = sum_{i in hist} r_ui * sim(i, j)
        """
        if user not in self.user_items_rating:
            return []

        user_hist = self.user_items_rating[user]  # {item: rating}
        item_scores = defaultdict(float) # # {item: score}

        for item_i, r_ui in user_hist.items():
            if item_i not in self.item_sim:
                continue
            top_neighbors = self.item_sim[item_i]

            for item_j, sim_score in top_neighbors:
                if item_j in user_hist:
                    continue  # 不推荐已交互过的
                item_scores[item_j] += r_ui * sim_score  # 加权累加，用户对某物品的交互等级*物品与物品的相似度

        recs = nlargest(n_rec, item_scores.items(), key=lambda x: x[1]) # [(item:final_score)]
        res = [x[0] for x in recs]
        return res # 只返回物品ID列表

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

    def recommend(self, user, n_rec=50):
        """
        为用户推荐物品
        :param user: 目标用户ID
        :param n_rec: 推荐 top-N 物品
        :return: List of (item, score)
        """
        if user not in self.user_items_rating:
            return []

        user_hist = set(self.user_items_rating[user].keys())
        item_scores = defaultdict(float) # {item:score}

        # 获取最相似的 top_k_sim_users 个用户
        if user not in self.user_top_sim:
            return []
        sim_users = self.user_top_sim[user] # {users:scores}
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
        return res


class Note:
    """笔记类，表示系统中的内容项"""
    def __init__(self, note_id, title, categories, keywords, created_time):
        """
        初始化笔记对象
        Args:
            note_id: 笔记ID
            title: 笔记标题
            categories: 笔记所属类目列表
            keywords: 笔记包含的关键词列表
            created_time: 笔记创建时间
        """
        self.note_id = note_id
        self.title = title
        self.categories = categories
        self.keywords = keywords
        self.created_time = created_time


class UserProfile:
    """用户画像类"""
    def __init__(self, user_id, categories=None, keywords=None):
        """
        初始化用户画像
        Args:
            user_id: 用户ID
            categories: 用户感兴趣的类目列表
            keywords: 用户感兴趣的关键词列表
        """
        self.user_id = user_id
        self.categories = categories  # 存储用户感兴趣的类目
        self.keywords = keywords  # 存储用户感兴趣的关键词


class ColdStartRecommender:
    """冷启动召回系统"""
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

    def cold_start_recommend(self, user_profile, topk_per_channel=10):
        """
        冷启动推荐主函数
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


def generate_mock_data():
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

    for i in range(100):
        # 随机选择类目和关键词
        categories = random.sample(categories_list[random.randint(0, len(categories_list) - 1)],
                                   min(2, len(categories_list[random.randint(0, len(categories_list) - 1)])))
        keywords = random.sample(keywords_list[random.randint(0, len(keywords_list) - 1)],
                                 min(3, len(keywords_list[random.randint(0, len(keywords_list) - 1)])))

        title = f"笔记{i}: {' '.join(categories[:1])}相关内容"
        note = Note(
            note_id=i,
            title=title,
            categories=categories,
            keywords=keywords,
            created_time=fake.date()
        )
        notes.append(note)

    return notes


def demo_cold_start_system():
    """演示冷启动召回系统"""
    print("开始演示冷启动召回系统...\n")

    # ========= 1. 初始化推荐系统 =========
    recommender = ColdStartRecommender()

    # ========= 2. 添加模拟笔记数据 =========
    print("1. 添加模拟笔记数据...")
    notes = generate_mock_data()
    for note in notes:
        recommender.add_note(note)

    # 构建索引
    recommender.build_indices()
    print(f"添加了 {len(notes)} 个笔记\n")

    # ========= 3. 创建用户画像 =========
    print("2. 创建用户画像...")
    user_profile = UserProfile(
        user_id="user_123",
        categories=["科技", "人工智能", "机器学习"],  # 用户感兴趣的类目
        keywords=["AI", "深度学习", "编程"]  # 用户感兴趣的关键词
    )

    print(f"用户ID: {user_profile.user_id}")
    print(f"感兴趣类目: {user_profile.categories}")
    print(f"感兴趣关键词: {user_profile.keywords}\n")

    # ========= 4. 执行冷启动推荐 =========
    print("3. 执行冷启动推荐...")
    recall_pool = recommender.cold_start_recommend(
        user_profile=user_profile,
        topk_per_channel=5  # 每个通道召回5个
    )

    print(f"召回池中共有 {len(recall_pool)} 个笔记\n")

    # ========= 5. 显示召回结果 =========
    print("4. 召回结果详情:")
    print("-" * 80)
    print(f"{'排名':<4} {'笔记ID':<10} {'标题':<20} {'类目':<15} {'关键词':<20} {'创建时间':<12}")
    print("-" * 80)

    # 按创建时间倒序显示（最近的在前面）
    sorted_recall_pool = sorted(recall_pool, key=lambda x: x.created_time, reverse=True)

    for i, note in enumerate(sorted_recall_pool, 1):
        categories_str = ", ".join(note.categories[:2])  # 只显示前2个类目
        keywords_str = ", ".join(note.keywords[:3])  # 只显示前3个关键词
        time_str = note.created_time

        print(f"{i:<4} {note.note_id:<10} {note.title[:18]:<20} {categories_str:<15} {keywords_str:<20} {time_str:<12}")

    print("-" * 80)

    # ========= 6. 分析召回通道贡献 =========
    print(f"\n5. 召回通道分析:")

    # 单独计算每个通道的召回结果
    category_recalled = recommender.category_recall(user_profile.categories, topk=5)
    keyword_recalled = recommender.keyword_recall(user_profile.keywords, topk=5)

    print(f"类目召回数量: {len(category_recalled)}")
    print(f"关键词召回数量: {len(keyword_recalled)}")
    print(f"合并后召回数量: {len(recall_pool)} (可能存在重复)")

    # 计算交集
    category_ids = {note.note_id for note in category_recalled}
    keyword_ids = {note.note_id for note in keyword_recalled}
    intersection = category_ids.intersection(keyword_ids)
    print(f"两个通道共同召回的数量: {len(intersection)}")

    print(f"\n冷启动召回系统执行完成！")
    print("系统特点:")
    print("- 支持类目召回和关键词召回双通道")
    print("- 索引按创建时间倒序排列")
    print("- 自动去重合并召回结果")
    print("- 适用于新用户冷启动场景")


if __name__ == "__main__":
    demo_cold_start_system()