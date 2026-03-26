import torch
import torch.nn as nn
from torch import optim
from utils import bpr_loss
from dataset import GraphDataset

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
        self.adj_matrix=adj_matrix
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

    def train_light_gcn(self,n_layers,user_ids,item_ids,embed_dim,df_interactions):
        self.dataset=GraphDataset(user_ids,item_ids,df_interactions)
        self.model = LightGCN(n_layers, len(user_ids), len(item_ids), embed_dim, self.dataset.norm_adj_matrix)
        # train
        epochs = 10
        batch_size = 5
        batch_num = 10  # 注意这里不是所有batch加起来是对所有数据过了一遍，因为generate是随机采样
        self.model.train()
        optimizer = optim.Adam(self.model.parameters(), lr=0.001, weight_decay=1e-4)  # weight_decay内置了L2正则化
        for i in range(epochs):
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

    def compute_embeddings(self):
        self.users_embedding, self.items_embedding = self.model.forward()

    def recommend_for_user(self,user_idx):
        user_embedding = self.users_embedding[user_idx] # [1,emb_dim]
        item_scores=torch.matmul(user_embedding,self.items_embedding.T)

        topk=min(self.topk,len(item_scores))
        _,top_idxs=torch.topk(item_scores,topk)
        item_ids=[self.item_idx2id[idx] for idx in top_idxs.cpu().numpy()]

        return set(item_ids)

