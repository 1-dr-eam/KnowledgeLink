import math
from collections import defaultdict
from heapq import nlargest

class ItemCFWithRating:
    def __init__(self):
        self.item_sim_matrix = {}      # {item_i: {item_j: sim}}
        self.item_norm_sq = {}         # N_i = sum_u (r_ui)^2
        self.user_items_rating = {}    # {user: {item: rating}}

    def fit(self, user_item_rating_list):
        """
        :param user_item_rating_list: List of (user_id, item_id, rating)
        """
        # Step 1: 构建用户-物品-评分字典，并计算每个物品的 ||r_i||^2
        self.user_items_rating = defaultdict(dict)
        self.item_norm_sq = defaultdict(float)

        for user, item, rating in user_item_rating_list:
            self.user_items_rating[user][item] = rating
            self.item_norm_sq[item] += rating * rating

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
        for item_i, neighbors in cooccur.items():
            for item_j, c_ij in neighbors.items():
                norm_i = self.item_norm_sq[item_i]
                norm_j = self.item_norm_sq[item_j]
                if norm_i > 0 and norm_j > 0:
                    sim = c_ij / math.sqrt(norm_i * norm_j)
                    self.item_sim_matrix[item_i][item_j] = sim

        print("ItemCF with rating training completed.")

    def recommend(self, user, top_k=10, n_rec=50):
        """
        推荐时考虑用户对历史物品的评分强度
        score(j) = sum_{i in hist} r_ui * sim(i, j)
        """
        if user not in self.user_items_rating:
            return []

        user_hist = self.user_items_rating[user]  # {item: rating}
        item_scores = defaultdict(float) # # {item: score}

        for item_i, r_ui in user_hist.items():
            if item_i not in self.item_sim_matrix:
                continue
            sims = self.item_sim_matrix[item_i] # {item_j:sim}
            top_neighbors = nlargest(top_k, sims.items(), key=lambda x: x[1])

            for item_j, sim_score in top_neighbors:
                if item_j in user_hist:
                    continue  # 不推荐已交互过的
                item_scores[item_j] += r_ui * sim_score  # 加权累加，用户对某物品的交互等级*物品与物品的相似度

        recs = nlargest(n_rec, item_scores.items(), key=lambda x: x[1]) # {item:final_score}
        return recs # 也可只返回物品ID列表


if __name__ == "__main__":
    # 评分数据：1=点击，2=点赞
    data = [
        ('u1', 'i1', 1), ('u1', 'i2', 2), ('u1', 'i3', 1),
        ('u2', 'i2', 1), ('u2', 'i3', 2), ('u2', 'i4', 1),
        ('u3', 'i3', 2), ('u3', 'i4', 2), ('u3', 'i5', 1),
        ('u4', 'i1', 2), ('u4', 'i2', 1), ('u4', 'i5', 2),
    ]

    model = ItemCFWithRating()
    model.fit(data)

    recs = model.recommend('u1', top_k=5, n_rec=5)
    print("Recommendations for u1 (with rating):")
    for item, score in recs:
        print(f"  {item}: {score:.4f}")