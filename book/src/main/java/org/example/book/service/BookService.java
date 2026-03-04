package org.example.book.service;

import org.example.book.entity.Book;
import org.example.book.mapper.BookMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BookService{

    @Autowired
    private BookMapper bookMapper;

    public int addBook(Book book) {
        return bookMapper.addBook(book);
    }

    public int updateBook(Book book) {
        return bookMapper.updateBook(book);
    }

    public int deleteBook(Integer bookId, Integer userId) {
        return bookMapper.deleteBook(bookId, userId);
    }

    public int adminDeleteBook(Integer bookId) {
        return bookMapper.adminDeleteBook(bookId);
    }

    public List<Book> getBooksByUserId(Integer userId) {
        return bookMapper.getBooksByUserId(userId);
    }

    public List<Book> getAllBooks() {
        try {
            return bookMapper.getAllBooks(); // 用户版
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Book> getAllBooksForAdmin() {
        return bookMapper.getAllBooksForAdmin(); // 管理员版
    }

    public List<Book> getBookInfoById(Integer bookId) {
        return bookMapper.getBookInfoById(bookId); // 用户版
    }

    public List<Book> getBookInfoByIdForAdmin(Integer bookId) {
        return bookMapper.getBookInfoByIdForAdmin(bookId); // 管理员版
    }

    public List<Book> getBooksByName(String name) {
        return bookMapper.getBooksByName(name); // 用户版
    }

    public List<Book> getBooksByNameForAdmin(String name) {
        return bookMapper.getBooksByNameForAdmin(name); // 管理员版
    }

    public List<Book> getBooksByType(String type) {
        return bookMapper.getBooksByType(type); // 用户版
    }

    public List<Book> getBooksByTypeForAdmin(String type) {
        return bookMapper.getBooksByTypeForAdmin(type); // 管理员版
    }

    // 更新实现方法，增加classify和subClassify参数
    public List<Book> getTextbooksByInfo(String bookName, String version, Integer note, String author,
                                         String publisher, String classify, String subClassify, Integer size) {
        return bookMapper.getTextbooksByInfo(bookName, version, note, author, publisher, classify, subClassify, size);
    }

    public List<Book> getTextbooksByInfoForAdmin(String bookName, String version, Integer note, String author,
                                                 String publisher, String classify, String subClassify, Integer size) {
        return bookMapper.getTextbooksByInfoForAdmin(bookName, version, note, author, publisher, classify, subClassify, size);
    }

    public List<Book> getOtherBooksByInfo(String bookName, String version, Integer note, String author,
                                          String publisher, String classify, String subClassify, Integer size) {
        return bookMapper.getOtherBooksByInfo(bookName, version, note, author, publisher, classify, subClassify, size);
    }

    public List<Book> getOtherBooksByInfoForAdmin(String bookName, String version, Integer note, String author,
                                                  String publisher, String classify, String subClassify, Integer size) {
        return bookMapper.getOtherBooksByInfoForAdmin(bookName, version, note, author, publisher, classify, subClassify, size);
    }

    public List<Book> getTextbooksSortByPrice(String bookName, String version, Integer note, String author,
                                              String publisher, String classify, String subClassify, Integer size) {
        return bookMapper.getTextbooksSortByPrice(bookName, version, note, author, publisher, classify, subClassify, size);
    }

    public List<Book> getTextbooksSortByPriceForAdmin(String bookName, String version, Integer note, String author,
                                                      String publisher, String classify, String subClassify, Integer size) {
        return bookMapper.getTextbooksSortByPriceForAdmin(bookName, version, note, author, publisher, classify, subClassify, size);
    }

    public List<Book> getOtherBooksSortByPrice(String bookName, String version, Integer note, String author,
                                               String publisher, String classify, String subClassify, Integer size) {
        return bookMapper.getOtherBooksSortByPrice(bookName, version, note, author, publisher, classify, subClassify, size);
    }

    public List<Book> getOtherBooksSortByPriceForAdmin(String bookName, String version, Integer note, String author,
                                                       String publisher, String classify, String subClassify, Integer size) {
        return bookMapper.getOtherBooksSortByPriceForAdmin(bookName, version, note, author, publisher, classify, subClassify, size);
    }

    public int updateBookStatus(Integer bookId, Integer status) {
        return bookMapper.updateBookStatus(bookId, status);
    }

    public Book getBookById(Integer bookId) {
        return bookMapper.getBookById(bookId);
    }

    public int updateBookAvatar(Integer bookId, String avatarUrl) {
        if (bookId == null || avatarUrl == null || avatarUrl.trim().isEmpty()) {
            return 0; // 参数无效，返回0表示失败
        }
        return bookMapper.updateBookAvatar(bookId, avatarUrl);
    }

    public List<Book> getBookByIdForOrder(Integer bookId) {
        return bookMapper.getBookByIdForOrder(bookId);
    }
}