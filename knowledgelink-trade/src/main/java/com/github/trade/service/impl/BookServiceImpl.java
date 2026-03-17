package com.github.trade.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.common.dto.Result;
import com.github.common.utils.CosUtil;
import com.github.trade.dto.BookDTO;
import com.github.trade.dto.BookSearchDTO;
import com.github.trade.entity.Book;
import com.github.trade.mapper.BookMapper;
import com.github.trade.service.BookEsService;
import com.github.trade.service.IBookService;
import com.github.trade.util.BookConversionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.trade.util.RedisConstant.BOOK_INFO_KEY;
import static com.github.trade.util.RedisConstant.BOOK_INFO_TTL;

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

    // TODO 推荐系统算法完善后补充
    @Override
    public Result getRecommendedBooks() {
        return null;
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
        // id 不为空直接返回具体书籍的信息
        if(bookSearchDTO.getId() != null){
            String bookKey = BOOK_INFO_KEY + bookSearchDTO.getId();
            if(stringRedisTemplate.hasKey(bookKey)) {
                String bookJson = stringRedisTemplate.opsForValue().get(bookKey);
                Book book = JSONUtil.toBean(bookJson, Book.class);
                // avatarJson转换为List
                BookDTO bookDTO = bookConversionUtil.toBookDTO(book);
                return Result.success(bookDTO);
            } else {
                Book book = baseMapper.selectById(bookSearchDTO.getId());
                if(book == null){
                    return Result.error("查找的书籍信息不存在");
                }
                BookDTO bookDTO = bookConversionUtil.toBookDTO(book);
                // TODO 后续可通过新线程或者MQ优化（待测试）
                String bookJson = JSONUtil.toJsonStr(book);
                stringRedisTemplate.opsForValue().set(bookKey, bookJson, BOOK_INFO_TTL, TimeUnit.MINUTES);
                return Result.success(bookDTO);
            }
        }
        try {
            List<BookDTO> books = bookEsService.searchBooks(bookSearchDTO);
            return Result.success(books);
        } catch (Exception e) {
            return Result.error("书籍搜索失败");
        }
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
        Book book = bookConversionUtil.toBook(bookDTO);
        baseMapper.insert(book);
        // 使用redis进行缓存
        String bookKey = BOOK_INFO_KEY + book.getId();
        String bookJson = JSONUtil.toJsonStr(book);
        stringRedisTemplate.opsForValue().set(bookKey, bookJson, BOOK_INFO_TTL, TimeUnit.MINUTES);
        try {
            bookEsService.saveBook(book);
            log.debug("id为：" + book.getId() + "的商品构建ES索引成功");
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
     * @param id ID
     * @return success
     */
    @Override
    public Result removeBookInfo(Integer id) {
        String bookKey = BOOK_INFO_KEY + id;
        if(stringRedisTemplate.hasKey(bookKey)){
            stringRedisTemplate.delete(bookKey);
        }
        baseMapper.deleteById(id);
        try {
            bookEsService.deleteBook(id);
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
        String bookKey = BOOK_INFO_KEY + book.getId();
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


}
