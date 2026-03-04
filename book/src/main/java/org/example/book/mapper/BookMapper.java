package org.example.book.mapper;

import org.apache.ibatis.annotations.*;
import org.example.book.entity.Book;
import java.util.List;

@Mapper
public interface BookMapper {

    // 新增图书
    @Insert("INSERT INTO book (sellerId, name, author, publisher, version, price, type, classify, subClassify, isNote, description, status, avatar) " +
            "VALUES (#{sellerId}, #{name}, #{author}, #{publisher}, #{version}, #{price}, #{type}, #{classify}, #{subClassify}, #{isNote}, #{description}, 1, #{avatar})")
    int addBook(Book book);

    // 修改图书
    @Update("UPDATE book SET name=#{name}, author=#{author}, publisher=#{publisher}, " +
            "version=#{version}, price=#{price}, type=#{type}, classify=#{classify}, subClassify=#{subClassify}, isNote=#{isNote}, description=#{description}, avatar=#{avatar} " +
            "WHERE id=#{id} AND sellerId=#{sellerId}")
    int updateBook(Book book);

    // 删除图书（用户版）
    @Delete("DELETE FROM book WHERE id=#{bookId} AND sellerId=#{userId} AND status=1")
    int deleteBook(@Param("bookId") Integer bookId, @Param("userId") Integer userId);

    // 删除图书（管理员版）
    @Delete("DELETE FROM book WHERE id=#{bookId}")
    int adminDeleteBook(@Param("bookId") Integer bookId);

    // 根据用户ID获取图书（用户版，只能看到自己的书）
    @Select("SELECT * FROM book WHERE sellerId=#{userId}")
    List<Book> getBooksByUserId(Integer userId);

    // 获取所有图书（用户版，只能看到未售出的书）
    @Select("SELECT * FROM book WHERE status=1")
    List<Book> getAllBooks();

    // 获取所有图书（管理员版，看到所有状态的书）
    @Select("SELECT * FROM book")
    List<Book> getAllBooksForAdmin();

    // 根据ID获取图书（用户版）
    @Select("SELECT * FROM book WHERE id=#{bookId} AND status=1")
    List<Book> getBookInfoById(Integer bookId);

    // 根据ID获取图书（订单专用，不限制状态）
    @Select("SELECT * FROM book WHERE id = #{bookId}")
    List<Book> getBookByIdForOrder(Integer bookId);



    // 根据ID获取图书（管理员版）
    @Select("SELECT * FROM book WHERE id=#{bookId}")
    List<Book> getBookInfoByIdForAdmin(Integer bookId);

    // 根据名称搜索图书（用户版）
    @Select("SELECT * FROM book WHERE name LIKE CONCAT('%', #{name}, '%') AND status=1")
    List<Book> getBooksByName(String name);

    // 根据名称搜索图书（管理员版）
    @Select("SELECT * FROM book WHERE name LIKE CONCAT('%', #{name}, '%')")
    List<Book> getBooksByNameForAdmin(String name);

    // 根据类型搜索图书（用户版）
    @Select("SELECT * FROM book WHERE type=#{type} AND status=1")
    List<Book> getBooksByType(String type);

    // 根据类型搜索图书（管理员版）
    @Select("SELECT * FROM book WHERE type=#{type}")
    List<Book> getBooksByTypeForAdmin(String type);

    // 根据ID获取图书（单个）
    @Select("SELECT * FROM book WHERE id = #{bookId}")
    Book getBookById(Integer bookId);

    // 更新图书状态
    @Update("UPDATE book SET status = #{status} WHERE id = #{bookId}")
    int updateBookStatus(@Param("bookId") Integer bookId, @Param("status") Integer status);

    // 搜索教材（用户版）
    @Select("SELECT * FROM book WHERE type = '教材' AND status = 1 AND " +
            "(#{bookName} IS NULL OR #{bookName} = 'null' OR name LIKE CONCAT('%', #{bookName}, '%')) AND " +
            "(#{version} IS NULL OR #{version} = 'null' OR version LIKE CONCAT('%', #{version}, '%')) AND " +
            "(#{note} IS NULL OR isNote = #{note}) AND " +
            "(#{author} IS NULL OR #{author} = 'null' OR author LIKE CONCAT('%', #{author}, '%')) AND " +
            "(#{publisher} IS NULL OR #{publisher} = 'null' OR publisher LIKE CONCAT('%', #{publisher}, '%')) AND " +
            "(#{classify} IS NULL OR #{classify} = 'null' OR classify = #{classify}) AND " +
            "(#{subClassify} IS NULL OR #{subClassify} = 'null' OR subClassify = #{subClassify}) " +
            "LIMIT #{size}")
    List<Book> getTextbooksByInfo(@Param("bookName") String bookName, @Param("version") String version,
                                  @Param("note") Integer note, @Param("author") String author,
                                  @Param("publisher") String publisher, @Param("classify") String classify,
                                  @Param("subClassify") String subClassify, @Param("size") Integer size);

    // 搜索教材（管理员版）
    @Select("SELECT * FROM book WHERE type = '教材' AND " +
            "(#{bookName} IS NULL OR #{bookName} = 'null' OR name LIKE CONCAT('%', #{bookName}, '%')) AND " +
            "(#{version} IS NULL OR #{version} = 'null' OR version LIKE CONCAT('%', #{version}, '%')) AND " +
            "(#{note} IS NULL OR isNote = #{note}) AND " +
            "(#{author} IS NULL OR #{author} = 'null' OR author LIKE CONCAT('%', #{author}, '%')) AND " +
            "(#{publisher} IS NULL OR #{publisher} = 'null' OR publisher LIKE CONCAT('%', #{publisher}, '%')) AND " +
            "(#{classify} IS NULL OR #{classify} = 'null' OR classify = #{classify}) AND " +
            "(#{subClassify} IS NULL OR #{subClassify} = 'null' OR subClassify = #{subClassify}) " +
            "LIMIT #{size}")
    List<Book> getTextbooksByInfoForAdmin(@Param("bookName") String bookName, @Param("version") String version,
                                          @Param("note") Integer note, @Param("author") String author,
                                          @Param("publisher") String publisher, @Param("classify") String classify,
                                          @Param("subClassify") String subClassify, @Param("size") Integer size);

    // 搜索非教材（用户版）
    @Select("SELECT * FROM book WHERE type != '教材' AND status = 1 AND " +
            "(#{bookName} IS NULL OR #{bookName} = 'null' OR name LIKE CONCAT('%', #{bookName}, '%')) AND " +
            "(#{version} IS NULL OR #{version} = 'null' OR version LIKE CONCAT('%', #{version}, '%')) AND " +
            "(#{note} IS NULL OR isNote = #{note}) AND " +
            "(#{author} IS NULL OR #{author} = 'null' OR author LIKE CONCAT('%', #{author}, '%')) AND " +
            "(#{publisher} IS NULL OR #{publisher} = 'null' OR publisher LIKE CONCAT('%', #{publisher}, '%')) AND " +
            "(#{classify} IS NULL OR #{classify} = 'null' OR classify = #{classify}) AND " +
            "(#{subClassify} IS NULL OR #{subClassify} = 'null' OR subClassify = #{subClassify}) " +
            "LIMIT #{size}")
    List<Book> getOtherBooksByInfo(@Param("bookName") String bookName, @Param("version") String version,
                                   @Param("note") Integer note, @Param("author") String author,
                                   @Param("publisher") String publisher, @Param("classify") String classify,
                                   @Param("subClassify") String subClassify, @Param("size") Integer size);

    // 搜索非教材（管理员版）
    @Select("SELECT * FROM book WHERE type != '教材' AND " +
            "(#{bookName} IS NULL OR #{bookName} = 'null' OR name LIKE CONCAT('%', #{bookName}, '%')) AND " +
            "(#{version} IS NULL OR #{version} = 'null' OR version LIKE CONCAT('%', #{version}, '%')) AND " +
            "(#{note} IS NULL OR isNote = #{note}) AND " +
            "(#{author} IS NULL OR #{author} = 'null' OR author LIKE CONCAT('%', #{author}, '%')) AND " +
            "(#{publisher} IS NULL OR #{publisher} = 'null' OR publisher LIKE CONCAT('%', #{publisher}, '%')) AND " +
            "(#{classify} IS NULL OR #{classify} = 'null' OR classify = #{classify}) AND " +
            "(#{subClassify} IS NULL OR #{subClassify} = 'null' OR subClassify = #{subClassify}) " +
            "LIMIT #{size}")
    List<Book> getOtherBooksByInfoForAdmin(@Param("bookName") String bookName, @Param("version") String version,
                                           @Param("note") Integer note, @Param("author") String author,
                                           @Param("publisher") String publisher, @Param("classify") String classify,
                                           @Param("subClassify") String subClassify, @Param("size") Integer size);

    // 教材按价格排序（用户版）
    @Select("SELECT * FROM book WHERE type = '教材' AND status = 1 AND " +
            "(#{bookName} IS NULL OR #{bookName} = 'null' OR name LIKE CONCAT('%', #{bookName}, '%')) AND " +
            "(#{version} IS NULL OR #{version} = 'null' OR version LIKE CONCAT('%', #{version}, '%')) AND " +
            "(#{note} IS NULL OR isNote = #{note}) AND " +
            "(#{author} IS NULL OR #{author} = 'null' OR author LIKE CONCAT('%', #{author}, '%')) AND " +
            "(#{publisher} IS NULL OR #{publisher} = 'null' OR publisher LIKE CONCAT('%', #{publisher}, '%')) AND " +
            "(#{classify} IS NULL OR #{classify} = 'null' OR classify = #{classify}) AND " +
            "(#{subClassify} IS NULL OR #{subClassify} = 'null' OR subClassify = #{subClassify}) " +
            "ORDER BY price LIMIT #{size}")
    List<Book> getTextbooksSortByPrice(@Param("bookName") String bookName, @Param("version") String version,
                                       @Param("note") Integer note, @Param("author") String author,
                                       @Param("publisher") String publisher, @Param("classify") String classify,
                                       @Param("subClassify") String subClassify, @Param("size") Integer size);

    // 教材按价格排序（管理员版）
    @Select("SELECT * FROM book WHERE type = '教材' AND " +
            "(#{bookName} IS NULL OR #{bookName} = 'null' OR name LIKE CONCAT('%', #{bookName}, '%')) AND " +
            "(#{version} IS NULL OR #{version} = 'null' OR version LIKE CONCAT('%', #{version}, '%')) AND " +
            "(#{note} IS NULL OR isNote = #{note}) AND " +
            "(#{author} IS NULL OR #{author} = 'null' OR author LIKE CONCAT('%', #{author}, '%')) AND " +
            "(#{publisher} IS NULL OR #{publisher} = 'null' OR publisher LIKE CONCAT('%', #{publisher}, '%')) AND " +
            "(#{classify} IS NULL OR #{classify} = 'null' OR classify = #{classify}) AND " +
            "(#{subClassify} IS NULL OR #{subClassify} = 'null' OR subClassify = #{subClassify}) " +
            "ORDER BY price LIMIT #{size}")
    List<Book> getTextbooksSortByPriceForAdmin(@Param("bookName") String bookName, @Param("version") String version,
                                               @Param("note") Integer note, @Param("author") String author,
                                               @Param("publisher") String publisher, @Param("classify") String classify,
                                               @Param("subClassify") String subClassify, @Param("size") Integer size);

    // 非教材按价格排序（用户版）
    @Select("SELECT * FROM book WHERE type != '教材' AND status = 1 AND " +
            "(#{bookName} IS NULL OR #{bookName} = 'null' OR name LIKE CONCAT('%', #{bookName}, '%')) AND " +
            "(#{version} IS NULL OR #{version} = 'null' OR version LIKE CONCAT('%', #{version}, '%')) AND " +
            "(#{note} IS NULL OR isNote = #{note}) AND " +
            "(#{author} IS NULL OR #{author} = 'null' OR author LIKE CONCAT('%', #{author}, '%')) AND " +
            "(#{publisher} IS NULL OR #{publisher} = 'null' OR publisher LIKE CONCAT('%', #{publisher}, '%')) AND " +
            "(#{classify} IS NULL OR #{classify} = 'null' OR classify = #{classify}) AND " +
            "(#{subClassify} IS NULL OR #{subClassify} = 'null' OR subClassify = #{subClassify}) " +
            "ORDER BY price LIMIT #{size}")
    List<Book> getOtherBooksSortByPrice(@Param("bookName") String bookName, @Param("version") String version,
                                        @Param("note") Integer note, @Param("author") String author,
                                        @Param("publisher") String publisher, @Param("classify") String classify,
                                        @Param("subClassify") String subClassify, @Param("size") Integer size);

    // 非教材按价格排序（管理员版）
    @Select("SELECT * FROM book WHERE type != '教材' AND " +
            "(#{bookName} IS NULL OR #{bookName} = 'null' OR name LIKE CONCAT('%', #{bookName}, '%')) AND " +
            "(#{version} IS NULL OR #{version} = 'null' OR version LIKE CONCAT('%', #{version}, '%')) AND " +
            "(#{note} IS NULL OR isNote = #{note}) AND " +
            "(#{author} IS NULL OR #{author} = 'null' OR author LIKE CONCAT('%', #{author}, '%')) AND " +
            "(#{publisher} IS NULL OR #{publisher} = 'null' OR publisher LIKE CONCAT('%', #{publisher}, '%')) AND " +
            "(#{classify} IS NULL OR #{classify} = 'null' OR classify = #{classify}) AND " +
            "(#{subClassify} IS NULL OR #{subClassify} = 'null' OR subClassify = #{subClassify}) " +
            "ORDER BY price LIMIT #{size}")
    List<Book> getOtherBooksSortByPriceForAdmin(@Param("bookName") String bookName, @Param("version") String version,
                                                @Param("note") Integer note, @Param("author") String author,
                                                @Param("publisher") String publisher, @Param("classify") String classify,
                                                @Param("subClassify") String subClassify, @Param("size") Integer size);


    @Update("UPDATE book SET avatar = #{avatar} WHERE id = #{bookId}")
    int updateBookAvatar(@Param("bookId") Integer bookId, @Param("avatar") String avatar);


}