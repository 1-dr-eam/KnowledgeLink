package com.github.trade.service;

import com.github.common.dto.Result;
import com.github.trade.dto.BookDTO;
import com.github.trade.mapper.BookMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
public class BookServiceFlowTest {
    @Autowired
    private IBookService bookService;

    @Autowired
    private BookMapper bookMapper;

    @Test
    public void testBookCrudAndSearchFlow() throws Exception {
        List<String> avatarUrlList = uploadImageList();
        String uniqueName = "ES测试书籍-" + System.currentTimeMillis();
        BookDTO bookDTO = new BookDTO();
        bookDTO.setSellerId(10001L);
        bookDTO.setName(uniqueName);
        bookDTO.setAuthor("测试作者");
        bookDTO.setPublisher("测试出版社");
        bookDTO.setVersion("第1版");
        bookDTO.setPrice(39.9);
        bookDTO.setType("教材");
        bookDTO.setClassify("工学");
        bookDTO.setSubClassify("计算机");
        bookDTO.setNote(Boolean.TRUE);
        bookDTO.setDescription("用于测试上传、修改、删除与搜索流程");
        bookDTO.setStatus(1);
        bookDTO.setAvatar(avatarUrlList);
        Result uploadResult = bookService.uploadBookInfo(bookDTO);
        Assertions.assertEquals(1, uploadResult.getCode());
    }

    private List<String> uploadImageList() throws Exception {
        List<String> localImagePaths = Arrays.asList("C:/Users/ningn/Pictures/动漫/111.jpg", "C:/Users/ningn/Pictures/动漫/111.jpg");
        List<String> avatarUrlList = new ArrayList<>();
        for (String localImagePath : localImagePaths) {
            if (localImagePath == null || localImagePath.isBlank()) {
                continue;
            }
            try (FileInputStream inputStream = new FileInputStream(localImagePath)) {
                MultipartFile file = new MockMultipartFile(
                        "file",
                        Path.of(localImagePath).getFileName().toString(),
                        "image/jpeg",
                        inputStream
                );
                Result uploadAvatarResult = bookService.uploadBookAvatar(file);
                if (uploadAvatarResult.getCode() != null && uploadAvatarResult.getCode() == 1 && uploadAvatarResult.getData() != null) {
                    avatarUrlList.add(uploadAvatarResult.getData().toString());
                }
            }
        }
        return avatarUrlList;
    }
}
