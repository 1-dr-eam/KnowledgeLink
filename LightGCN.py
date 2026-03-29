import math
import torch
import torch.nn as nn
from torch import optim
from utils import bpr_loss
from dataset import GraphDataset
import faiss

device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')

class LightGCN(nn.Module):
    def __init__(self,n_layers,n_users,n_items,embed_dim,adj_matrix):
        """
        Args:
            n_layers:图卷积层数
            n_users:用户数
            n_items:物品数
            embed_dim:嵌入层输出维度
            adj_matrix:归一化后的邻接矩阵（sparse_coo_tensor）
        """
        super().__init__()
        self.n_layers=n_layers
        self.n_users=n_users
        self.n_items=n_items
        self.embed_dim=embed_dim
        self.adj_matrix=adj_matrix.to(device)
        self.user_embedding = nn.Parameter(torch.zeros(n_users,embed_dim)) # 没有显式的嵌入层，直接定义等价的查找表，这个相当于就是第一层embedding之后的结果
        self.item_embedding = nn.Parameter(torch.zeros(n_items,embed_dim)) # ID为基础的推荐系统embedding常见实现方式
        nn.init.xavier_uniform_(self.user_embedding) # Xavier均匀初始化,避免训练初始梯度消失
        nn.init.xavier_uniform_(self.item_embedding)

    def forward(self):
        all_embedding=torch.cat([self.user_embedding,self.item_embedding],dim=0) # 所有用户和所有物品
        all_emb_list=[all_embedding]
        for i in range(self.n_layers):
            all_embedding=torch.sparse.mm(self.adj_matrix,all_embedding)
            all_emb_list.append(all_embedding)
        final_all_embedding=sum(all_emb_list)/(self.n_layers+1) # 总共是层数+1个结果

        users_emb,items_emb=final_all_embedding[:self.n_users],final_all_embedding[self.n_users:]

        return users_emb,items_emb

class LightGCNRecommender(nn.Module):
    def __init__(self,item_idx2id):
        super().__init__()
        self.model = None
        self.dataset = None
        self.users_embedding = None
        self.items_embedding = None

        self.item_idx2id = item_idx2id
        self.topk=5
        self.faiss_index = None

    def train_light_gcn(self,n_layers,user_ids,item_ids,embed_dim,df_interactions):
        print("LightGCN training...")
        self.dataset=GraphDataset(user_ids,item_ids,df_interactions)
        self.model = LightGCN(n_layers, len(user_ids), len(item_ids), embed_dim, self.dataset.norm_adj_matrix).to(device)
        # train
        epochs = 10
        batch_size = 5
        batch_num = 10  # 注意这里不是所有batch加起来是对所有数据过了一遍，因为generate是随机采样
        optimizer = optim.Adam(self.model.parameters(), lr=0.001, weight_decay=1e-4)  # weight_decay内置了L2正则化
        for i in range(epochs):
            self.model.train()
            sum_loss = 0.0
            for j in range(batch_num):
                user_emb, item_emb = self.model.forward()
                user_idxs, pos_item_idxs, neg_item_idxs = self.dataset.generate_batch(batch_size)
                user_embs = user_emb[user_idxs]  # [B,emb_dim]
                pos_item_embs = item_emb[pos_item_idxs]  # [B,emb_dim]
                neg_item_embs = item_emb[neg_item_idxs]  # [B,emb_dim]

                pos_scores = (user_embs * pos_item_embs).sum(dim=1)  # [B,1]
                neg_scores = (user_embs * neg_item_embs).sum(dim=1)  # [B,1]
                loss = bpr_loss(pos_scores, neg_scores)

                optimizer.zero_grad()
                loss.backward()
                optimizer.step()

                sum_loss += loss.item()
            # print(f"epoch{i} bpr_loss: {sum_loss / batch_num}")
        print("LightGCN training finished")

    def fit(self):
        self.users_embedding, self.items_embedding = self.model.forward()
        # 构建faiss索引
        self.initialize_faiss_index()

    def initialize_faiss_index(self):
        """
        初始化Faiss索引，将物品Embedding存入向量数据库
        """
        if self.items_embedding is None:
            raise ValueError("请先调用 compute_embeddings 计算Embedding")
        print("LightGCN faiss index building...")
        # 获取物品向量维度
        vector_dim = self.items_embedding.shape[1]
        n_vectors = self.items_embedding.shape[0]
        # 转换为numpy格式，faiss需要float32
        # LightGCN的Embedding通常是未经归一化的，需要进行L2归一化以实现余弦相似度
        items_embedding_numpy = self.items_embedding.detach().cpu().numpy().astype('float32')

        # L2归一化,下面再用IP内积就实现了余弦相似度
        faiss.normalize_L2(items_embedding_numpy)

        # 根据物品数量选择索引类型
        if n_vectors < 1000:  # 小数据量使用精确搜索
            index = faiss.IndexFlatIP(vector_dim)  # 余弦相似度(向量已归一化)
            index.add(items_embedding_numpy)
        else:  # 大数据量使用近似搜索
            # IVF (Inverted File) 参数
            n_cluster = int(math.sqrt(n_vectors))  # 聚类中心数量
            # PQ (Product Quantization) 参数
            # M 是切分成的段数，因此向量维度要能整除M，物品塔的输出维度是32
            M = 8
            # 创建量化器 (用于聚类)
            quantizer = faiss.IndexFlatIP(vector_dim)  # 使用内积，因为已经归一化
            # 创建索引
            index = faiss.IndexIVFPQ(quantizer, vector_dim, n_cluster, M, 8)
            index.train(items_embedding_numpy)
            index.add(items_embedding_numpy)

        # 保存索引
        self.faiss_index = index

        print("LightGCN faiss index finished")

    def recommend_for_user(self, user_idx, top_k=5):
        """
        使用Faiss向量数据库为用户推荐物品
        Args:
            user_idx: 用户索引
            top_k: 推荐物品数量
        Returns:
            推荐的物品ID集合
        """
        if not hasattr(self, 'faiss_index') or self.faiss_index is None:
            raise ValueError("请先调用 fit 函数构建Faiss索引")

        # 获取用户Embedding
        user_embedding = self.users_embedding[user_idx].detach().cpu().numpy().astype('float32')

        # 对用户Embedding也进行L2归一化，以匹配物品向量的相似度计算方式
        faiss.normalize_L2(user_embedding.reshape(1, -1))

        # 设置搜索参数 (nprobe越大越精确但越慢)
        self.faiss_index.nprobe = 5

        # 在Faiss索引中搜索最相似的物品
        scores, indices = self.faiss_index.search(user_embedding.reshape(1, -1), top_k)

        # 将返回的物品索引转换为原始物品ID
        recommended_item_ids = [self.item_idx2id[idx] for idx in indices[0]]

        return set(recommended_item_ids)