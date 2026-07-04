package com.github.trade.controller;


import com.github.trade.dto.BookDTO;
import com.github.common.dto.Result;
import com.github.trade.dto.BatchIdRequest;
import com.github.trade.dto.BookSearchDTO;
import com.github.trade.service.IBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author ning
 * @date 2026/03/10
 */
@RestController
public class BookController {
    @Autowired
    private IBookService bookService;

    @GetMapping("/getRecommendedBooks")
    public Result getRecommendedBooks() {
        return bookService.getRecommendedBooks();
    }

    @PostMapping("/getBooksByConditions")
    public Result getBooksByConditions(@RequestBody BookSearchDTO bookSearchDTO) {
        return bookService.getBooksByConditions(bookSearchDTO);
    }

    @GetMapping("/getBookById")
    public Result getBookById(@RequestParam("itemId") Long itemId) {
        return bookService.getBookById(itemId);
    }

    @GetMapping("/getMyBooks")
    public Result getMyBooks() {
        return bookService.getMyBooks();
    }

    @PostMapping("/uploadBookInfo")
    public Result uploadBookInfo(@RequestBody BookDTO bookDTO) {
        return bookService.uploadBookInfo(bookDTO);
    }

    @PostMapping("/uploadBookAvatar")
    public Result uploadBookAvatar(@RequestParam("file") MultipartFile file) {
        return bookService.uploadBookAvatar(file);
    }

    @DeleteMapping("/removeBookInfo")
    public Result removeBookInfo(@RequestParam("itemId") Long itemId) {
        return bookService.removeBookInfo(itemId);
    }

    @DeleteMapping("/removeBookInfoBatch")
    public Result removeBookInfoBatch(@RequestBody BatchIdRequest batchIdRequest) {
        return bookService.removeBookInfoBatch(batchIdRequest);
    }

    @PutMapping("/updateBookInfo")
    public Result updateBookInfo(@RequestBody BookDTO bookDTO) {
        return bookService.updateBookInfo(bookDTO);
    }
}
