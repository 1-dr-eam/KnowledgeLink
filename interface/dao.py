from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
import database_models as models

async def get_all_user_profiles(user_db_session: AsyncSession) -> list[models.UserProfileBehavior]:
    """获取所有用户信息"""
    stmt = select(models.UserProfileBehavior) # 类定义中关联的有具体数据表
    result = await user_db_session.execute(stmt)
    return result.scalars().all()

async def get_all_book_infos(book_and_interaction_db_session: AsyncSession) -> list[models.BookInfo]:
    """获取所有书籍信息"""
    stmt = select(models.BookInfo)
    result = await book_and_interaction_db_session.execute(stmt)
    return result.scalars().all()

async def get_all_item_interactions(book_and_interaction_db_session: AsyncSession) -> list[models.ItemInteraction]:
    """获取所有书籍统计信息"""
    stmt = select(models.ItemInteraction)
    result = await book_and_interaction_db_session.execute(stmt)
    return result.scalars().all()

async def get_all_user_item_interactions(book_and_interaction_db_session: AsyncSession) -> list[models.UserItemInteraction]:
    """获取所有用户-书籍交互记录"""
    stmt = select(models.UserItemInteraction)
    result = await book_and_interaction_db_session.execute(stmt)
    return result.scalars().all()