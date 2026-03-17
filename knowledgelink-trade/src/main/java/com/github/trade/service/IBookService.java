package com.github.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.trade.dto.BookDTO;
import com.github.common.dto.Result;
import com.github.trade.dto.BookSearchDTO;
import com.github.trade.entity.Book;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author ning
 * @date 2026/03/10
 */
public interface IBookService extends IService<Book> {
    Result getRecommendedBooks();

    Result getBooksByConditions(BookSearchDTO bookSearchDTO);

    Result uploadBookInfo(BookDTO bookDTO);

    Result uploadBookAvatar(MultipartFile file);

    Result removeBookInfo(Integer id);

    Result updateBookInfo(BookDTO bookDTO);
}
