from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession
from sqlalchemy.orm import sessionmaker

# ===== 数据库连接 URL =====
BOOK_INTERACTION_DATABASE_URL = "mysql+aiomysql://username:password@host/kl_trade"
USER_DATABASE_URL = "mysql+aiomysql://username:password@host/kl_user"
# ===== 创建异步引擎 =====
# 为三个数据库分别创建引擎
book_and_interaction_db_engine = create_async_engine(BOOK_INTERACTION_DATABASE_URL,echo=True)
user_db_engine = create_async_engine(USER_DATABASE_URL,echo=True)

# 创建对应的会话工厂
BookAndInteractionDBSession = sessionmaker(bind=book_and_interaction_db_engine, class_=AsyncSession,autocommit=False, autoflush=False, expire_on_commit=False)
UserDBSession = sessionmaker(bind=user_db_engine, class_=AsyncSession,autocommit=False, autoflush=False, expire_on_commit=False)

# 得到数据库Session对象
async def get_book_and_interaction_db():
    async with BookAndInteractionDBSession() as session:
        yield session

async def get_user_db():
    async with UserDBSession() as session:
        yield session