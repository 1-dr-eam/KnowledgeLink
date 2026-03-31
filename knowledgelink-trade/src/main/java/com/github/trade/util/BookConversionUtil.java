package com.github.trade.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.github.trade.dto.BookDTO;
import com.github.trade.entity.Book;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Book类和BookDTO类相互转换工具类（主要为imageList和imageJSON之间相互转换）
 *
 * @author ning
 * @date 2026-03-12
 */
@Component
public class BookConversionUtil {

    /**
     * BookDTO转换为Book
     *
     * @param bookDTO 书籍DTO类
     * @return BOOK
     */
    public Book toBook(BookDTO bookDTO) {
        if(bookDTO == null){
            return null;
        }
        List<String> imageList = bookDTO.getImage();
        String imageJson = JSONUtil.toJsonStr(imageList);
        Book book = BeanUtil.copyProperties(bookDTO, Book.class, "image");
        book.setImage(imageJson);
        return book;
    }

    /**
     * Book转换为BookDTO
     *
     * @param book 书籍类
     * @return BookDTO类
     */
    public BookDTO toBookDTO(Book book) {
        if(book == null){
            return null;
        }
        JSONArray array = JSONUtil.parseArray(book.getImage());
        List<String> imageList = new ArrayList<>();
        for (Object o : array) {
            imageList.add(StrUtil.trim(o.toString()).replace("`", ""));
        }
        BookDTO bookDTO = BeanUtil.copyProperties(book, BookDTO.class, "image");
        bookDTO.setImage(imageList);
        return bookDTO;
    }
}
