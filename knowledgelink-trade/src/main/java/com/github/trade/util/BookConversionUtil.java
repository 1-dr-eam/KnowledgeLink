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
 * Book类和BookDTO类相互转换工具类（主要为avatarList和avatarJSON之间相互转换）
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
        List<String> avatarList = bookDTO.getAvatar();
        String avatarJson = JSONUtil.toJsonStr(avatarList);
        Book book = BeanUtil.copyProperties(bookDTO, Book.class, "avatar");
        book.setAvatar(avatarJson);
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
        JSONArray array = JSONUtil.parseArray(book.getAvatar());
        List<String> avatarList = new ArrayList<>();
        for (Object o : array) {
            avatarList.add(StrUtil.trim(o.toString()).replace("`", ""));
        }
        BookDTO bookDTO = BeanUtil.copyProperties(book, BookDTO.class, "avatar");
        bookDTO.setAvatar(avatarList);
        return bookDTO;
    }
}
