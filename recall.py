import math
from collections import defaultdict
from heapq import nlargest

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


# ===== 示例使用 =====
if __name__ == "__main__":
    # 评分数据：1=点击，2=点赞
    data = [
        ('u1', 'i1', 1), ('u1', 'i2', 2), ('u1', 'i3', 1),
        ('u2', 'i2', 1), ('u2', 'i3', 2), ('u2', 'i4', 1),
        ('u3', 'i3', 2), ('u3', 'i4', 2), ('u3', 'i5', 1),
        ('u4', 'i1', 2), ('u4', 'i2', 1), ('u4', 'i5', 2),
    ]

    model = UserCF()
    model.fit(data,10)

    res = model.recommend('u1', n_rec=5)
    print("IDs:",res)