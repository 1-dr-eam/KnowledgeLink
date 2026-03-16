from collections import defaultdict
from typing import List
from sklearn.metrics.pairwise import cosine_similarity
import clip
import torch
import random
from PIL import Image
import numpy as np
import string
from utils import print_selection_results

device = "cuda" if torch.cuda.is_available() else "cpu"

class Item:
    """物品类，包含物品的基本信息"""
    def __init__(self, item_id, item_image,item_text,relevance_score):
        """
        初始化物品对象
        Args:
            item_id: 物品ID
            item_image: 物品图片
            item_text: 物品文字描述
            relevance_score: 物品的精排分数（相关性分数）
        """
        self.id = item_id
        self.image = item_image
        self.text = item_text
        self.relevance_score = relevance_score
        # 计算基于内容的向量表征（图像特征和文本特征的拼接）
        self.content_feature = None

    def calculate_content_feature(self,model,preprocess):
        image_tensor = preprocess(self.image).unsqueeze(0).to(device) # torch.Size([1, 3, 224, 224])
        text_tensor = clip.tokenize(self.text).to(device) # torch.Size([1, 77])

        image_feature=model.encode_image(image_tensor) # torch.Size([1, 512])
        text_feature=model.encode_text(text_tensor) # torch.Size([1, 512])
        content_feature=torch.cat((image_feature,text_feature),dim=1) # torch.Size([1,1024])
        self.content_feature=content_feature

class MmrDiversity:
    def __init__(self,items:List[Item],lambda_param=0.7, selection_count=30, window_size=5):
        """
        基于MMR（最大边际相关性）的多样性重排算法
        Args:
            items: 所有物品对象的列表
            lambda_param: MMR平衡参数，控制相关性和多样性的权衡 (0 <= lambda <= 1)
            selection_count: 要选择的物品数量
            window_size: 滑动窗口大小
        """
        self.items = items
        self.lambda_param = lambda_param
        self.selection_count = selection_count
        self.window_size = window_size
        # 物品间的余弦相似度矩阵（基于内容）
        self.cosine_similarity_matrix = self.build_cosine_similarity_matrix()

    def build_cosine_similarity_matrix(self):
        """离线计算所有物品间的余弦相似度（基于内容）"""
        n=len(self.items)
        matrix = [[0]*n for _ in range(n)]
        for i in range(n):
            for j in range(i+1,n):
                sim=cosine_similarity(self.items[i].content_feature.cpu().detach().numpy(),self.items[j].content_feature.cpu().detach().numpy())
                matrix[i][j] = sim
                matrix[j][i] = sim
        return matrix

    def mmr_diversity_selection(self)->List[Item]:
        """执行MMR算法"""
        if not self.items:
            return []

        # 初始化已选择物品列表
        selected_items = []
        remaining_items = self.items.copy()

        # 第一个物品选择相关性最高的
        first_item = max(remaining_items, key=lambda x: x.relevance_score)
        selected_items.append(first_item)
        remaining_items.remove(first_item)

        # 逐步选择剩余物品(考虑剩余物品不够选择的情况)
        for _ in range(min(self.selection_count - 1, len(remaining_items))):
            if not remaining_items:
                break

            # 确定当前窗口范围(考虑窗口超出已挑选物品列表的情况)
            current_window_size = min(self.window_size, len(selected_items))  # 实际的窗口大小，因为刚开始已挑选的物品列表较小
            current_window = selected_items[len(selected_items) - current_window_size:]

            # 计算每个候选物品的MMR得分
            mmr_scores = defaultdict(float)
            for item_r in remaining_items:
                # 计算与窗口中物品的最大相似度
                max_sim = -1
                for item_w in current_window:
                    sim = float(self.cosine_similarity_matrix[item_r.id][item_w.id])
                    if sim > max_sim:
                        max_sim = sim
                mmr_scores[item_r] = self.lambda_param * item_r.relevance_score + (1 - self.lambda_param) * max_sim

            # 选择MMR得分最高的物品
            best_item = max(mmr_scores.items(), key=lambda x: x[1])[0]

            # 将选中的物品加入已选择列表
            selected_items.append(best_item)
            remaining_items.remove(best_item)

        return selected_items

def generate_mock_items(n_items=50):
    """生成模拟物品数据"""
    items = []

    for i in range(n_items):
        # 生成随机的相关性分数（0-1之间）
        relevance_score = random.random()
        # 生成模拟图片数据
        rgb_array = np.random.randint(0, 256, (300, 200, 3), dtype=np.uint8)
        image = Image.fromarray(rgb_array, 'RGB')
        # 生成模拟文字描述
        characters = string.ascii_letters + string.digits
        random_string = ''.join(random.choice(characters) for _ in range(10))
        item = Item(
            item_id=i,
            item_image=image,
            item_text=random_string,
            relevance_score=relevance_score,
        )
        items.append(item)

    return items

def main():
    """主函数"""
    print("开始执行MMR多样性重排算法...\n")

    # ========= 1. 模型和数据准备 =========
    model, preprocess = clip.load("ViT-B/32", device=device)
    print("1. 生成模拟物品数据...")
    items = generate_mock_items(n_items=50)
    print(f"生成了 {len(items)} 个物品\n")
    print("计算基于内容的特征向量...")
    for item in items:
        item.calculate_content_feature(model,preprocess)
    # ========= 2. 设置MMR参数 =========
    lambda_param = 0.7  # 平衡相关性和多样性的参数
    selection_count = 30  # 选择物品数量
    window_size = 5  # 滑动窗口大小
    mmr_diversity = MmrDiversity(items,lambda_param, selection_count, window_size)

    # ========= 3. 执行MMR重排算法 =========
    print("2. 执行MMR多样性重排算法...")
    selected_items = mmr_diversity.mmr_diversity_selection()
    print(f"完成重排，选择了 {len(selected_items)} 个物品\n")

    # ========= 4. 打印结果 =========
    print_selection_results(items, selected_items, lambda_param, selection_count)

    print(f"\nMMR算法执行完成！")
    print("算法特点:")
    print("- 平衡相关性和多样性")
    print("- 使用滑动窗口提高效率")
    print("- 基于图像和文本特征的余弦相似度计算多样性")
    print("- λ参数控制相关性与多样性的权衡")


if __name__ == "__main__":
    main()