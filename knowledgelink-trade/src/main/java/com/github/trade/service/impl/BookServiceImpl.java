package com.github.trade.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.common.dto.Result;
import com.github.common.utils.CosUtil;
import com.github.common.utils.UserHolder;
import com.github.trade.dto.BatchIdRequest;
import com.github.trade.dto.BookDTO;
import com.github.trade.dto.BookSearchDTO;
import com.github.trade.entity.Book;
import com.github.trade.feign.RecommendFeignClient;
import com.github.trade.mapper.BookMapper;
import com.github.trade.service.BookEsService;
import com.github.trade.service.IBookService;
import com.github.trade.util.UserItemInteractionRecordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private CosUtil cosUtil;

    @Autowired
    private BookEsService bookEsService;

    @Autowired
    private RecommendFeignClient recommendFeignClient;
    @Autowired
    private UserItemInteractionRecordUtil userItemInteractionRecordUtil;

    @Override
    public Result getRecommendedBooks() {
//        try {
//            String health = recommendFeignClient.health();
//            RecommendHealthResponse recommendHealthResponse = JSONUtil.toBean(health, RecommendHealthResponse.class);
//            if (recommendHealthResponse == null || !"healthy".equals(recommendHealthResponse.getStatus())) {
//                return Result.success(listRandomBooksFallback(50));
//            }
//            int hour = java.time.LocalTime.now(java.time.ZoneId.of("Asia/Shanghai")).getHour();
//            RecommendationRequest recommendationRequest = new RecommendationRequest();
//            recommendationRequest.setUserId(54L);
//            recommendationRequest.setHour(hour);
//            recommendationRequest.setWeekend(false);
//            recommendationRequest.setHoliday(false);
//            String recommend = recommendFeignClient.recommend(recommendationRequest);
//            RecommendIdsResponse recommendIdsResponse = JSONUtil.toBean(recommend, RecommendIdsResponse.class);
//            if (recommendIdsResponse == null || recommendIdsResponse.getRecommendations() == null || recommendIdsResponse.getRecommendations().isEmpty()) {
//                return Result.success(listRandomBooksFallback(50));
//            }
//            List<Long> recommendations = Stream.of(recommendIdsResponse.getRecommendations().toArray(new Object[0]))
//                    .map(Object::toString)
//                    .map(Long::parseLong)
//                    .limit(50)
//                    .toList();
//            List<BookDTO> bookDTOList = new ArrayList<>();
//            for (Long recommendation : recommendations) {
//                BookDTO bookDTO = getBookInfoById(recommendation, false);
//                if (bookDTO == null) {
//                    continue;
//                }
//                bookDTOList.add(bookDTO);
//            }
//            if (bookDTOList.isEmpty()) {
//                return Result.success(listRandomBooksFallback(50));
//            }
//            return Result.success(bookDTOList);
//        } catch (Exception e) {
//            return Result.success(listRandomBooksFallback(50));
//        }
        List<BookDTO> recommendList = listRandomBooksFallback(50);
        cacheMissedBooks(recommendList);
        return Result.success(recommendList);
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
        try {
            List<BookDTO> books = bookEsService.searchBooks(bookSearchDTO);
            cacheMissedBooks(books);
            return Result.success(books);
        } catch (Exception e) {
            return Result.error("书籍搜索失败");
        }
    }

    @Override
    public Result getBookById(Long itemId) {
        if (itemId == null) {
            return Result.error("书籍ID缺失");
        }

        BookDTO bookDTO = getBookInfoById(itemId, true);
        if (bookDTO == null) {
            return Result.error("书籍信息不存在");
        }
        return Result.success(bookDTO);
    }

    @Override
    public Result getMyBooks() {
        Long userId = UserHolder.getUser().getId();
        List<Book> myBooks = baseMapper.selectList(new LambdaQueryWrapper<Book>()
                .eq(Book::getSellerId, userId)
                .orderByDesc(Book::getUpdateTime));
        List<BookDTO> result = myBooks.stream()
                .map(book -> BeanUtil.copyProperties(book, BookDTO.class))
                .collect(Collectors.toList());
        cacheMissedBooks(result);
        return Result.success(result);
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
        // 先读 Redis，未命中再回源数据库并回填缓存（旁路缓存）
        String bookJson = stringRedisTemplate.opsForValue().get(bookKey);
        if (StrUtil.isNotBlank(bookJson)) {
            Book book = JSONUtil.toBean(bookJson, Book.class);
            bookDTO = BeanUtil.copyProperties(book, BookDTO.class);
        } else {
            Book book = baseMapper.selectById(id);
            if(book == null){
                return null;
            }
            bookDTO = BeanUtil.copyProperties(book, BookDTO.class);
            // TODO 后续可通过新线程或者MQ优化（待测试）
            String cacheBookJson = JSONUtil.toJsonStr(book);
            stringRedisTemplate.opsForValue().set(bookKey, cacheBookJson, BOOK_INFO_TTL, TimeUnit.MINUTES);
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
        Book book = BeanUtil.copyProperties(bookDTO, Book.class);
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

    @Override
    public Result removeBookInfoBatch(BatchIdRequest batchIdRequest) {
        if (batchIdRequest == null || batchIdRequest.getIds() == null || batchIdRequest.getIds().isEmpty()) {
            return Result.error("批量下架ID不能为空");
        }
        Long userId = UserHolder.getUser().getId();
        List<Long> itemIds = batchIdRequest.getIds().stream()
                .map(this::parseLongId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (itemIds.isEmpty()) {
            return Result.error("批量下架ID非法");
        }

        List<Book> ownedBooks = baseMapper.selectList(new LambdaQueryWrapper<Book>()
                .eq(Book::getSellerId, userId)
                .in(Book::getItemId, itemIds));
        if (ownedBooks.isEmpty()) {
            return Result.error("未找到可下架的商品");
        }
        List<Long> ownedItemIds = ownedBooks.stream().map(Book::getItemId).collect(Collectors.toList());
        int removed = baseMapper.delete(new LambdaQueryWrapper<Book>()
                .eq(Book::getSellerId, userId)
                .in(Book::getItemId, ownedItemIds));
        for (Long itemId : ownedItemIds) {
            stringRedisTemplate.delete(BOOK_INFO_KEY + itemId);
        }
        try {
            for (Long itemId : ownedItemIds) {
                bookEsService.deleteBook(itemId);
            }
        } catch (Exception e) {
            return Result.error("商品已下架，但部分ES索引删除失败");
        }
        return Result.success(removed);
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
        Book book = BeanUtil.copyProperties(bookDTO, Book.class);
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
        try {
            List<Book> books = baseMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Book>()
                    .eq(Book::getStatus, 1)
                    .last("order by rand() limit " + limit));
            List<BookDTO> bookDTOList = new ArrayList<>();
            for (Book book : books) {
                bookDTOList.add(BeanUtil.copyProperties(book, BookDTO.class));
            }
            return bookDTOList;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 将列表中未命中缓存的书籍写入 Redis，避免后续详情查询回源数据库。
     */
    private void cacheMissedBooks(List<BookDTO> bookDTOList) {
        if (bookDTOList == null || bookDTOList.isEmpty()) {
            return;
        }
        for (BookDTO bookDTO : bookDTOList) {
            if (bookDTO == null || bookDTO.getItemId() == null) {
                continue;
            }
            String bookKey = BOOK_INFO_KEY + bookDTO.getItemId();
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(bookKey))) {
                continue;
            }
            Book book = BeanUtil.copyProperties(bookDTO, Book.class);
            String bookJson = JSONUtil.toJsonStr(book);
            stringRedisTemplate.opsForValue().set(bookKey, bookJson, BOOK_INFO_TTL, TimeUnit.MINUTES);
        }
    }

    private Long parseLongId(String idText) {
        try {
            return Long.parseLong(idText);
        } catch (Exception e) {
            return null;
        }
    }

}
