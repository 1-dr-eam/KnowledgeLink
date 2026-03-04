package org.example.book.mapper;

import org.apache.ibatis.annotations.*;
import org.example.book.entity.Book;
import org.example.book.entity.ShoppingCar;
import java.util.List;

@Mapper
public interface ShoppingCarMapper {

    // 添加到购物车
    @Insert("INSERT INTO shoppingcar (userId, sellerId, bookId, bookName, price) " +
            "VALUES (#{userId}, #{sellerId}, #{bookId}, #{bookName}, #{price})")
    int addToShoppingCar(ShoppingCar shoppingCar);

    // 从购物车删除
    @Delete("DELETE FROM shoppingcar WHERE userId = #{userId} AND bookId = #{bookId}")
    int removeFromShoppingCar(@Param("userId") Integer userId, @Param("bookId") Integer bookId);

    // 根据ID删除购物车商品
    @Delete("DELETE FROM shoppingcar WHERE id = #{id}")
    int removeFromShoppingCarById(@Param("id") Integer id);

    // 获取用户购物车（使用实际存在的字段）
    @Select("SELECT sc.*, " +
            "b.id as book_id, " +
            "b.name as book_name, " +
            "b.author as book_author, " +
            "b.publisher as book_publisher, " +
            "b.avatar as book_avatar, " +
            "b.sellerId as book_sellerId, " +
            "b.status as book_status, " +
            "b.price as book_price, " +
            "b.version as book_version, " +
            "b.type as book_type, " +
            "b.classify as book_classify, " +
            "b.subClassify as book_subClassify, " +
            "b.isNote as book_isNote, " +
            "b.description as book_description " +
            "FROM shoppingcar sc " +
            "LEFT JOIN book b ON sc.bookId = b.id " +
            "WHERE sc.userId = #{userId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "userId", column = "userId"),
            @Result(property = "sellerId", column = "sellerId"),
            @Result(property = "bookId", column = "bookId"),
            @Result(property = "bookName", column = "bookName"),
            @Result(property = "price", column = "price"),
            @Result(property = "book.id", column = "book_id"),
            @Result(property = "book.name", column = "book_name"),
            @Result(property = "book.author", column = "book_author"),
            @Result(property = "book.publisher", column = "book_publisher"),
            @Result(property = "book.avatar", column = "book_avatar"),
            @Result(property = "book.sellerId", column = "book_sellerId"),
            @Result(property = "book.status", column = "book_status"),
            @Result(property = "book.price", column = "book_price"),
            @Result(property = "book.version", column = "book_version"),
            @Result(property = "book.type", column = "book_type"),
            @Result(property = "book.classify", column = "book_classify"),
            @Result(property = "book.subClassify", column = "book_subClassify"),
            @Result(property = "book.isNote", column = "book_isNote"),
            @Result(property = "book.description", column = "book_description")
    })
    List<ShoppingCar> getShoppingCarByUserId(Integer userId);

    // 检查是否已存在购物车
    @Select("SELECT COUNT(*) FROM shoppingcar WHERE userId = #{userId} AND bookId = #{bookId}")
    int existsInShoppingCar(@Param("userId") Integer userId, @Param("bookId") Integer bookId);

    // 清空购物车
    @Delete("DELETE FROM shoppingcar WHERE userId = #{userId}")
    int clearShoppingCar(Integer userId);

    // 获取图书信息（供购物车使用）
    @Select("SELECT * FROM book WHERE id = #{bookId}")
    Book getBookById(Integer bookId);
}