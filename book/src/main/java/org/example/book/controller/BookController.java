package org.example.book.controller;

import learning_exchange_platform.model.Result;
import learning_exchange_platform.utils.SessionUtil;
import org.example.book.entity.Book;
import org.example.book.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BookController {
    @Autowired
    private BookService bookService;

    @RequestMapping("/books/getUserBooks")
    public Result getUserBooks() {
        try {
            int user_id= (int)SessionUtil.getSession().getAttribute("user_id");
            List<Book> books= bookService.getBooksByUserId(user_id);
            return Result.success(books);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @RequestMapping("/user/updateBook")
    public Result updateBook(Book book) {
        try {
            bookService.updateBook(book);
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
