import random
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List
import logging
import uuid
import torch
from interface.database import BookAndInteractionDBSession, UserDBSession
from interface.recommender_system import RecommenderSystem  # 推荐系统类
from interface.db_utils import prepare_user_data,prepare_book_and_interaction_data
from contextlib import asynccontextmanager
from utils import getItems

# ===== 全局推荐器 ======
# 应用启动时创建一个实例即可
recsys_model: RecommenderSystem = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    """APP生命周期函数"""
    """应用启动时自动调用，初始化推荐系统类"""
    global recsys_model
    logging.info("Initializing RecSys model...")
    try:
        # 创建独立的会话（不使用依赖注入）
        async with BookAndInteractionDBSession() as book_and_interaction_session:
            async with UserDBSession() as user_session:
                # 从数据库读取数据
                df_items, df_interactions = await prepare_book_and_interaction_data(book_and_interaction_session)
                df_users = await prepare_user_data(user_session)
                print(f"读取到{len(df_items)}条物品数据，{len(df_users)}条用户数据,{len(df_interactions)}条交互记录")
                logging.info("Data loaded successfully from database.")
        # 初始化推荐系统
        recsys_model = RecommenderSystem(df_items, df_users, df_interactions)
        # 加载预训练好的权重进行推荐
        # 各模型权重路径
        twin_towers_model_weights_path = "../model_weights/twin_towers_model.pth"
        light_gcn_weights_path = "../model_weights/lightgcn.pth"
        three_towers_model_weights_path = "../model_weights/three_towers_model.pth"
        multi_task_model_path = "../model_weights/multi_task_model.pth"
        recsys_model.fit_with_weights(twin_towers_model_weights_path, light_gcn_weights_path,
                                      three_towers_model_weights_path, multi_task_model_path)
        logging.info("RecSys model initialized successfully.")
    except Exception as e:
        logging.error(f"Failed to initialize RecSys model: {e}")
        raise RuntimeError(f"Failed to start service due to model initialization error: {e}")
    # yield 控制权交给应用
    yield
    """应用关闭时自动释放空间"""
    logging.info("Shutting down RecSys service...")
    if recsys_model:
        del recsys_model
        torch.cuda.empty_cache()
    logging.info("Shutdown complete")

app = FastAPI(
    title="Recommendation Service with Database",
    description="A microservice for providing recommendations using the RecSys model.",
    version="3.0",
    lifespan=lifespan
)

# ===== 交互类 =====
class RecommendationRequest(BaseModel):
    user_id: int
    hour: int
    is_weekend: bool
    is_holiday: bool

class RecommendationResponse(BaseModel):
    status: str
    user_id: int
    recommendations: List[int]
    request_id: str

class ErrorResponse(BaseModel):
    status: str
    message: str


# ========= 核心推荐函数 ==========
async def get_recommendations_from_model(user_id: int, hour: int, is_weekend:bool, is_holiday:bool) -> List[int]:
    """
    调用已实例化的推荐器进行推荐
    Args:
        user_id: 用户ID
        hour: 当前时间(小时)
        is_weekend: 当前是否是周末
        is_holiday: 当前是否是节假日
    """
    global recsys_model
    if not recsys_model:
        raise RuntimeError("RecSys model is not initialized")

    logging.info(f"Generating recommendations for user '{user_id}' using the real model...")

    try:
        predicted_item_ids = recsys_model.recommend(user_id,hour,is_weekend,is_holiday)
        logging.info(f"Successfully generated {len(predicted_item_ids)} recommendations for user '{user_id}'.")
        return predicted_item_ids

    except KeyError as e:
        logging.warning(f"User '{user_id}' not found in model data. Returning empty list or handling cold start.")
        raise HTTPException(status_code=404, detail=f"User '{user_id}' not found.")

    except Exception as e:
        logging.error(f"Error during prediction for user '{user_id}': {str(e)}")
        raise HTTPException(status_code=500, detail="Internal server error during recommendation prediction.")

# ========= 每日微调函数 ==========
async def models_fine_tuning(df_items, df_users, df_interactions):
    """
    Args:
            df_items: 当日新物品数据
            df_users: 当日新用户数据
            df_interactions: 当日新交互数据
    """
    logging.info("Model fine-tuning started...")

    global recsys_model
    items= await getItems(df_items) # 当日新增的物品对象列表
    recsys_model.fine_tuning(df_items, df_users, df_interactions,items)

    logging.info("Model fine-tuning finished")

# =========== 接口路由 ===========
# 推荐接口
@app.post("/recommend",
              response_model=RecommendationResponse,
          responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}},
          summary="Get Recommendations for a User")
async def recommend(request: RecommendationRequest):
    user_id = request.user_id
    hour = request.hour
    is_weekend = request.is_weekend
    is_holiday = request.is_holiday

    # recommendations是推荐的物品ID列表，顺序是有意义的
    recommendations = await get_recommendations_from_model(
        user_id=user_id,
        hour=hour,
        is_weekend=is_weekend,
        is_holiday=is_holiday
    )

    return RecommendationResponse(
        status="success",
        user_id=user_id,
        recommendations=recommendations,
        request_id=str(uuid.uuid4())
    )

# 健康检查接口
@app.get("/health", summary="Health Check")
async def health_check():
    global recsys_model
    if not recsys_model:
        return {"status": "unhealthy", "reason": "Model not loaded",
                "timestamp": __import__('datetime').datetime.utcnow().isoformat()}
    return {"status": "healthy", "timestamp": __import__('datetime').datetime.utcnow().isoformat()}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="your_host", port=8000)