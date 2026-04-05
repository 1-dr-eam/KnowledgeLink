package com.github.trade.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.common.dto.Result;
import com.github.common.utils.CosUtil;
import com.github.common.utils.UserHolder;
import com.github.trade.dto.BookDTO;
import com.github.trade.dto.BookSearchDTO;
import com.github.trade.dto.RecommendHealthResponse;
import com.github.trade.dto.RecommendIdsResponse;
import com.github.trade.entity.Book;
import com.github.trade.feign.RecommendFeignClient;
import com.github.trade.mapper.BookMapper;
import com.github.trade.service.BookEsService;
import com.github.trade.service.IBookService;
import com.github.trade.util.BookConversionUtil;
import com.github.trade.util.UserItemInteractionRecordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.github.common.utils.RedisConstant.BOOK_INFO_KEY;
import static com.github.common.utils.RedisConstant.BOOK_INFO_TTL;

/**
 * 书籍商品操作逻辑实现类
 *
 * @author ning
 * @date 2026/03/10
 */
@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements IBookService{

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private BookConversionUtil bookConversionUtil;

    @Autowired
    private CosUtil cosUtil;

    @Autowired
    private BookEsService bookEsService;

    @Autowired
    private RecommendFeignClient recommendFeignClient;
    @Autowired
    private UserItemInteractionRecordUtil userItemInteractionRecordUtil;

    @Override
    public Result getRecommendedBooks() {
        try {
            String health = recommendFeignClient.health();
            RecommendHealthResponse recommendHealthResponse = JSONUtil.toBean(health, RecommendHealthResponse.class);
            if (recommendHealthResponse == null || !"healthy".equals(recommendHealthResponse.getHealth())) {
                return Result.success(listRandomBooksFallback(50));
            }
            String recommend = recommendFeignClient.recommend();
            RecommendIdsResponse recommendIdsResponse = JSONUtil.toBean(recommend, RecommendIdsResponse.class);
            if (recommendIdsResponse == null || recommendIdsResponse.getRecommendIds() == null || recommendIdsResponse.getRecommendIds().isEmpty()) {
                return Result.success(listRandomBooksFallback(50));
            }
            List<Long> recommendations = Stream.of(recommendIdsResponse.getRecommendIds().toArray(new Object[0]))
                    .map(Object::toString)
                    .map(Long::parseLong)
                    .limit(50)
                    .toList();
            List<BookDTO> bookDTOList = new ArrayList<>();
            for (Long recommendation : recommendations) {
                BookDTO bookDTO = getBookInfoById(recommendation, false);
                if (bookDTO == null) {
                    continue;
                }
                bookDTOList.add(bookDTO);
            }
            if (bookDTOList.isEmpty()) {
                return Result.success(listRandomBooksFallback(50));
            }
            return Result.success(bookDTOList);
        } catch (Exception e) {
            return Result.success(listRandomBooksFallback(50));
        }
    }

    /**
     * 根据传递的信息返回具体书籍信息或者书籍列表
     *
     * @param bookSearchDTO 搜索信息DTO类
     * @return Result(BookDTO) || Result(List<BookDTO>)
     */
    @Override
    public Result getBooksByConditions(BookSearchDTO bookSearchDTO) {
        if(bookSearchDTO == null){
            return Result.error("搜索条件缺失");
        }
        normalizePageLimit(bookSearchDTO);
        // itemId 不为空直接返回具体书籍的信息
        if(bookSearchDTO.getItemId() != null){
            BookDTO bookDTO = getBookInfoById(bookSearchDTO.getItemId(), true);
            if(bookDTO == null){
                return Result.error("书籍信息不存在");
            }
            return Result.success(bookDTO);
        }
        try {
            List<BookDTO> books = bookEsService.searchBooks(bookSearchDTO);
            return Result.success(books);
        } catch (Exception e) {
            return Result.error("书籍搜索失败");
        }
    }

    /**
     * 通过ID获取图书信息
     *
     * @param id ID
     * @return 图书dto
     */
     BookDTO getBookInfoById(Long id) {
        return getBookInfoById(id, false);
    }

     BookDTO getBookInfoById(Long id, boolean recordView) {
        String bookKey = BOOK_INFO_KEY + id;
        BookDTO bookDTO;
        if(stringRedisTemplate.hasKey(bookKey)) {
            String bookJson = stringRedisTemplate.opsForValue().get(bookKey);
            Book book = JSONUtil.toBean(bookJson, Book.class);
            // avatarJson转换为List
            bookDTO = bookConversionUtil.toBookDTO(book);
        } else {
            Book book = baseMapper.selectById(id);
            if(book == null){
                return null;
            }
            bookDTO = bookConversionUtil.toBookDTO(book);
            // TODO 后续可通过新线程或者MQ优化（待测试）
            String bookJson = JSONUtil.toJsonStr(book);
            stringRedisTemplate.opsForValue().set(bookKey, bookJson, BOOK_INFO_TTL, TimeUnit.MINUTES);
        }
        if (recordView) {
            try {
                userItemInteractionRecordUtil.recordView(UserHolder.getUser().getId(), id);
            } catch (Exception ignored) {
            }
        }
        return bookDTO;
    }

    /**
     * 上传书籍商品信息
     *
     * @param bookDTO 书籍DTO类
     * @return success
     */
    @Override
    public Result uploadBookInfo(BookDTO bookDTO) {
        if(bookDTO == null){
            return Result.error("商品信息缺失");
        }
        Long userId = UserHolder.getUser().getId();
        bookDTO.setSellerId(userId);
        Book book = bookConversionUtil.toBook(bookDTO);
        baseMapper.insert(book);
        // 使用redis进行缓存
        String bookKey = BOOK_INFO_KEY + book.getItemId();
        String bookJson = JSONUtil.toJsonStr(book);
        stringRedisTemplate.opsForValue().set(bookKey, bookJson, BOOK_INFO_TTL, TimeUnit.MINUTES);
        try {
            bookEsService.saveBook(book);
            log.debug("itemId为：" + book.getItemId() + "的商品构建ES索引成功");
        } catch (Exception e) {
            return Result.error("商品已保存，但ES索引更新失败");
        }
        return Result.success();
    }

    @Override
    public Result uploadBookAvatar(MultipartFile file) {
        try {
            String avatarUrl = cosUtil.uploadImage(file);
            return Result.success(avatarUrl);
        } catch (IllegalArgumentException e) {
            return Result.error("上传数据错误");
        } catch (Exception e) {
            return Result.error("上传图片失败");
        }
    }

    /**
     * 下架图书
     *
     * @param itemId 图书ID
     * @return success
     */
    @Override
    public Result removeBookInfo(Long itemId) {
        String bookKey = BOOK_INFO_KEY + itemId;
        if(stringRedisTemplate.hasKey(bookKey)){
            stringRedisTemplate.delete(bookKey);
        }
        baseMapper.deleteById(itemId);
        try {
            bookEsService.deleteBook(itemId);
        } catch (Exception e) {
            return Result.error("商品已下架，但ES索引删除失败");
        }
        return Result.success();
    }


    /**
     * 更新图书信息
     *
     * @param bookDTO 图书DTO
     * @return success
     */
    @Override
    public Result updateBookInfo(BookDTO bookDTO) {
        if(bookDTO == null){
            return Result.error("商品信息缺失");
        }
        // TODO 后续需要测试决定是否使用延迟双删
        Book book = bookConversionUtil.toBook(bookDTO);
        baseMapper.updateById(book);
        // 若redis中存在对应的key则直接删除，下次查询填入
        String bookKey = BOOK_INFO_KEY + book.getItemId();
        if(stringRedisTemplate.hasKey(bookKey)){
            stringRedisTemplate.delete(bookKey);
        }
        try {
            bookEsService.saveBook(book);
        } catch (Exception e) {
            return Result.error("商品已更新，但ES索引更新失败");
        }
        return Result.success();
    }


    private void normalizePageLimit(BookSearchDTO bookSearchDTO) {
        if (bookSearchDTO.getLimit() == null || bookSearchDTO.getLimit() <= 0) {
            bookSearchDTO.setLimit(50);
        }
        if (bookSearchDTO.getPage() == null || bookSearchDTO.getPage() <= 0) {
            bookSearchDTO.setPage(1);
        }
    }

    private List<BookDTO> listRandomBooksFallback(int limit) {
        List<Book> books = baseMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Book>()
                .eq(Book::getStatus, 1)
                .last("order by rand() limit " + limit));
        List<BookDTO> bookDTOList = new ArrayList<>();
        for (Book book : books) {
            bookDTOList.add(bookConversionUtil.toBookDTO(book));
        }
        return bookDTOList;
    }

}
