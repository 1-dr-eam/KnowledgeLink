from collections import deque
from PIL import Image
from io import BytesIO
import requests
from twin_towers_model import *
import clip

device=torch.device("cuda:0" if torch.cuda.is_available() else "cpu")

class Item:
    """物品类，表示系统中的内容项"""
    def __init__(self, item_id, name, categories, keywords,discrete_features,
                 continuous_features,created_time,image,description,content_feature=None,relevance_score=None):
        """
        初始化物品类
        Args:
            item_id: 物品ID
            name: 物品标题
            categories: 物品所属类目列表
            keywords: 物品包含的关键词列表
            discrete_features: 离散特征字典，如 {'category': 'A'}
            continuous_features: 连续特征字典，如 {'price': 50.0}
            created_time: 物品创建时间
            image: 物品图片url
            description: 物品文字描述
            content_feature: 基于内容的特征向量
        """
        self.item_id = item_id
        self.name = name
        self.categories = categories
        self.keywords = keywords
        self.created_time = created_time

        self.discrete_features = discrete_features or {}
        self.continuous_features = continuous_features or {}

        self.image = image
        self.description = description
        # 基于内容的向量表征
        self.content_feature = content_feature
        self.relevance_score = relevance_score

    def calculate_content_feature(self,model,preprocess):
        response = requests.get(self.image, stream=True)
        # 2. 将字节数据包装成文件对象
        img_bytes = BytesIO(response.content)
        # 3. 使用Pillow打开图像
        img = Image.open(img_bytes)
        image_tensor = preprocess(img).unsqueeze(0).to(device) # torch.Size([1, 3, 224, 224])
        text_tensor = clip.tokenize(self.description).to(device) # torch.Size([1, 77])

        image_feature=model.encode_image(image_tensor) # torch.Size([1, 512])
        text_feature=model.encode_text(text_tensor) # torch.Size([1, 512])
        content_feature=torch.cat((image_feature,text_feature),dim=1) # torch.Size([1,1024])
        self.content_feature=content_feature

    def __repr__(self):
        return str(self.__dict__)

class UserProfile:
    """用户画像类"""
    def __init__(self, user_id, categories=None, keywords=None, discrete_features=None,
                 continuous_features=None,max_history=50):
        """
        初始化用户画像
        Args:
            user_id: 用户ID
            categories: 用户感兴趣的类目列表
            keywords: 用户感兴趣的关键词列表
            max_history: 用户历史交互队列的最大长度
            discrete_features: 离散特征字典，如 {'gender': 'M'}
            continuous_features: 连续特征字典，如 {'age': 25}
        """
        self.user_id = user_id
        self.categories = categories  # 存储用户感兴趣的类目
        self.keywords = keywords  # 存储用户感兴趣的关键词
        self.interaction_history = deque(maxlen=max_history)  # 使用双端队列限制历史长度
        self.discrete_features = discrete_features or {}
        self.continuous_features = continuous_features or {}

    def __repr__(self):
        return str(self.__dict__)

    def add_interaction(self, item_id):
        """添加用户交互记录"""
        self.interaction_history.append(item_id)

    def get_last_n_interactions(self, n):
        """获取用户最近的n次交互记录"""
        return list(self.interaction_history)[-n:]