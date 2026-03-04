package org.example.book.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import learning_exchange_platform.service.UserService;
import learning_exchange_platform.utils.SessionUtil;
import org.example.book.entity.Book;
import org.example.book.entity.Result;
import learning_exchange_platform.model.User;
import org.example.book.service.BookService;
import org.example.book.service.BookUserService;
import org.example.book.util.SessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.example.book.util.BookOSSUtil;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Slf4j
public class BookUserController {

    @Autowired
    private BookService bookService;
    @Autowired
    private UserService userService;
    @Autowired
    private BookUserService bookUserService;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private BookOSSUtil bookOssUtil;

    @Operation(summary = "用户充值")
    @PostMapping("/recharge")
    public Result recharge(@RequestParam Double amount) {
        try {
            User currentUser = sessionManager.getCurrentUser();
            if (currentUser == null) {
                return Result.error("用户信息获取失败");
            }

            if (amount == null || amount <= 0) {
                return Result.error("充值金额必须大于0");
            }

            int result = bookUserService.recharge(currentUser.getId(), amount);
            if (result > 0) {
                // 更新Session中的用户信息
                User updatedUser = userService.selectUserById(currentUser.getId());
                if (updatedUser != null) {
                    sessionManager.setUserSession(updatedUser);
                }
                return Result.success("充值成功");
            } else {
                return Result.error("充值失败");
            }
        } catch (Exception e) {
            return Result.error("充值失败: " + e.getMessage());
        }
    }

    @Operation(summary = "用户提现")
    @PostMapping("/withdraw")
    public Result withdraw(@RequestParam Double amount) {
        try {
            User currentUser = sessionManager.getCurrentUser();
            if (currentUser == null) {
                return Result.error("用户信息获取失败");
            }

            if (amount == null || amount <= 0) {
                return Result.error("提现金额必须大于0");
            }

            if (amount > currentUser.getBalance()) {
                return Result.error("余额不足");
            }

            int result = bookUserService.withdraw(currentUser.getId(), amount);
            if (result > 0) {
                // 更新Session中的用户信息
                User updatedUser = userService.selectUserById(currentUser.getId());
                if (updatedUser != null) {
                    sessionManager.setUserSession(updatedUser);
                }
                return Result.success("提现成功");
            } else {
                return Result.error("提现失败，请检查余额是否充足");
            }
        } catch (Exception e) {
            return Result.error("提现失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取用户余额")
    @GetMapping("/balance")
    public Result getBalance() {
        try {
            int user_id= (int)SessionUtil.getSession().getAttribute("user_id");
            User currentUser = userService.getUserInfo(user_id);
            if (currentUser == null) {
                return Result.error("用户信息获取失败");
            }

            // 从数据库获取最新余额信息
            User latestUser = userService.selectUserById(currentUser.getId());
            if (latestUser != null) {
                return Result.success(latestUser.getBalance());
            }
            return Result.success(currentUser.getBalance());
        } catch (Exception e) {
            return Result.error("获取余额失败: " + e.getMessage());
        }
    }

    @Operation(summary = "检查登录状态")
    @GetMapping("/checkLogin")
    public Result checkLogin() {
        try {
            if (sessionManager.isUserLoggedIn()) {
                User currentUser = sessionManager.getCurrentUser();
                if (currentUser != null) {
                    // 返回用户基本信息
                    return Result.success(currentUser);
                }
            }
            return Result.error("用户未登录");
        } catch (Exception e) {
            return Result.error("检查登录状态失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/update")
    public Result updateUser(@RequestBody User user) {
        try {
            User currentUser = sessionManager.getCurrentUser();
            if (currentUser == null || !currentUser.getId().equals(user.getId())) {
                return Result.error("无权修改其他用户信息");
            }

            // 这里需要UserService添加updateUser方法
            // int result = userService.updateUser(user);
            // if (result > 0) {
            //     // 更新Session中的用户信息
            //     User updatedUser = userService.getUserById(user.getId());
            //     if (updatedUser != null) {
            //         sessionManager.setUserSession(updatedUser);
            //     }
            //     return Result.success("用户信息更新成功");
            // } else {
            //     return Result.error("用户信息更新失败");
            // }
            return Result.error("此功能暂未实现，请联系管理员");
        } catch (Exception e) {
            return Result.error("更新用户信息失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据用户ID获取用户信息")
    @GetMapping("/getUserById")
    public Result getUserById(@RequestParam Integer userId) {
        try {
            if (userId == null || userId <= 0) {
                return Result.error("用户ID不能为空且必须大于0");
            }

            User user = userService.selectUserById(userId);
            if (user != null) {
                // 只返回安全的用户信息，使用现有字段
                User safeUser = new User();
                safeUser.setId(user.getId());
                safeUser.setUsername(user.getUsername());
                safeUser.setPhone(user.getPhone());
                safeUser.setGrade(user.getGrade());
                safeUser.setMajor(user.getMajor());
                safeUser.setAvatar(user.getAvatar());
                safeUser.setSummary(user.getSummary());
                safeUser.setBalance(user.getBalance());

                return Result.success(safeUser);
            } else {
                return Result.error("用户不存在");
            }
        } catch (Exception e) {
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }

    // 以下是图书相关功能（需要BookService支持）

    /**
     * 新增图书信息（支持多图上传）
     * @param bookJson 图书信息的JSON字符串
     * @param images 图片数组（第一张作为封面）
     * @return 操作结果
     */
    @Operation(summary = "新增图书信息")
    @PostMapping("/addBook")
    public Result addBook(@RequestPart("book") String bookJson,
                          @RequestPart(value = "images", required = false) MultipartFile[] images) {
        try {
            // 1. 获取当前登录用户
            User currentUser = sessionManager.getCurrentUser();
            if (currentUser == null) {
                log.error("用户未登录，尝试添加图书");
                return new Result(0, "用户未登录，请先登录", null);
            }

            log.info("接收添加图书请求，用户ID：{}", currentUser.getId());
            log.info("接收的bookJson：{}", bookJson);

            // 记录接收到的图片信息
            if (images != null) {
                log.info("接收图片数量：{}", images.length);
                for (int i = 0; i < images.length; i++) {
                    MultipartFile image = images[i];
                    if (image != null && !image.isEmpty()) {
                        log.info("图片[{}]: 文件名={}, 大小={}字节, 类型={}",
                                i, image.getOriginalFilename(), image.getSize(), image.getContentType());
                    } else {
                        log.info("图片[{}]: 为空", i);
                    }
                }
            } else {
                log.info("未接收到图片");
            }

            // 2. 解析图书信息
            ObjectMapper objectMapper = new ObjectMapper();
            Book book;
            try {
                book = objectMapper.readValue(bookJson, Book.class);
                log.info("JSON解析成功：{}", book);
            } catch (Exception e) {
                log.error("JSON解析失败：{}", e.getMessage());
                return new Result(0, "图书数据格式错误：" + e.getMessage(), null);
            }

            // 3. 设置图书发布者为当前用户
            book.setSellerId(currentUser.getId());

            // 4. 处理所有图片
            List<String> allImageUrls = new ArrayList<>();

            if (images != null && images.length > 0) {
                log.info("开始处理 {} 张图片", images.length);

                for (int i = 0; i < images.length; i++) {
                    MultipartFile image = images[i];
                    if (image != null && !image.isEmpty()) {
                        try {
                            // 验证文件类型
                            String originalFilename = image.getOriginalFilename();
                            if (originalFilename != null && !originalFilename.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")) {
                                log.error("图片[{}]格式不支持：{}", i, originalFilename);
                                continue; // 跳过不支持的格式
                            }

                            // 验证文件大小（限制为5MB）
                            long fileSize = image.getSize();
                            if (fileSize > 5 * 1024 * 1024) {
                                log.error("图片[{}]大小超过限制：{}字节", i, fileSize);
                                continue; // 跳过超大的图片
                            }

                            // 上传到阿里云OSS
                            log.info("上传图片[{}]到OSS，文件名：{}，大小：{}字节",
                                    i, originalFilename, fileSize);
                            String imageUrl = bookOssUtil.OSSUpload(image);

                            if (imageUrl == null || imageUrl.isEmpty()) {
                                log.error("图片[{}]上传失败，OSS返回URL为空", i);
                                continue;
                            }

                            log.info("图片[{}]上传成功：{}", i, imageUrl);

                            // 添加到图片URL列表
                            allImageUrls.add(imageUrl);

                        } catch (Exception e) {
                            log.error("图片[{}]上传异常：", i, e);
                            // 继续处理其他图片
                        }
                    }
                }
            }

            // 5. 将所有图片URL按格式保存到avatar字段
            if (!allImageUrls.isEmpty()) {
                // 格式：[url1],[url2],[url3]
                StringBuilder avatarBuilder = new StringBuilder();
                for (String url : allImageUrls) {
                    avatarBuilder.append("[").append(url).append("],");
                }
                // 删除最后一个逗号
                if (avatarBuilder.length() > 0) {
                    avatarBuilder.deleteCharAt(avatarBuilder.length() - 1);
                }
                book.setAvatar(avatarBuilder.toString());
                log.info("设置avatar字段（{}张图片）：{}", allImageUrls.size(), book.getAvatar());

                // 注意：不再调用 setCoverImage，因为Book实体没有这个方法
                // 封面就是第一张图片，已经在avatar字段中了
                log.info("第一张图片作为封面：{}", allImageUrls.get(0));
            } else {
                // 没有上传任何图片，设置默认封面
                log.info("未上传任何图片，设置默认封面");
                book.setAvatar("[https://haut-campus-knowledge-sharing-and-trading-platform.oss-cn-beijing.aliyuncs.com/default-book-cover.jpg]");
            }

            // 6. 设置默认状态为1（可售）
            book.setStatus(1);

            log.info("准备插入数据库，图书信息：sellerId={}, name={}, avatar={}",
                    book.getSellerId(), book.getName(), book.getAvatar());

            // 7. 插入图书信息到数据库
            int result = bookService.addBook(book);
            log.info("数据库插入结果：{}", result);

            if (result > 0) {
                // 返回图书ID和avatar信息
                Map<String, Object> data = new HashMap<>();
                data.put("bookId", book.getId());
                data.put("avatar", book.getAvatar());
                return new Result(1, "图书添加成功", data);
            } else {
                return new Result(0, "图书添加失败，数据库操作异常", null);
            }
        } catch (Exception e) {
            log.error("添加图书异常：", e);
            return new Result(0, "发生错误: " + e.getMessage(), null);
        }
    }

    @Operation(summary = "上传图书封面")
    @PostMapping("/uploadBook")
    public Result uploadBook(@RequestParam("bookId") Integer bookId,
                                           @RequestParam("image") MultipartFile image) {
        try {
            User currentUser = sessionManager.getCurrentUser();
            if (currentUser == null) {
                return Result.error("用户未登录");
            }

            // 验证图书是否属于当前用户
            List<Book> existingBooks = bookService.getBookInfoById(bookId);
            if (existingBooks.isEmpty() || !existingBooks.get(0).getSellerId().equals(currentUser.getId())) {
                return Result.error("无权修改该图书");
            }

            // 验证文件
            if (image.isEmpty()) {
                return Result.error("请选择要上传的图片");
            }

            // 上传到阿里云OSS
            String coverUrl = bookOssUtil.OSSUpload(image);
            if (coverUrl == null || coverUrl.isEmpty()) {
                return Result.error("封面上传失败");
            }

            // 立即更新图书封面（调用Service的更新封面方法）
            int updateResult = bookService.updateBookAvatar(bookId, coverUrl);
            if (updateResult > 0) {
                Map<String, Object> data = new HashMap<>();
                data.put("bookId", bookId);
                data.put("coverUrl", coverUrl);
                data.put("message", "封面更新成功");
                return Result.success(data);
            } else {
                return Result.error("封面更新失败");
            }
        } catch (Exception e) {
            log.error("上传图书封面异常：", e);
            return Result.error("封面上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除自己发布的图书信息")
    @DeleteMapping("/deleteBook/{id}")
    public Result deleteBook(@PathVariable Integer id) {
        try {
            User currentUser = sessionManager.getCurrentUser();
            if (currentUser == null) {
                return Result.error("用户信息获取失败");
            }

            // 验证图书是否属于当前用户
            List<Book> existingBooks = bookService.getBookInfoById(id);
            if (existingBooks.isEmpty() || !existingBooks.get(0).getSellerId().equals(currentUser.getId())) {
                return Result.error("无权删除其他用户发布的图书");
            }

            int result = bookService.deleteBook(id, currentUser.getId());
            if (result > 0) {
                return Result.success("图书删除成功");
            } else {
                return Result.error("图书删除失败");
            }
        } catch (Exception e) {
            return Result.error("发生错误: " + e.getMessage());
        }
    }

    @Operation(summary = "根据用户ID获取用户发布的图书")
    @GetMapping("/getBooksByUserId/{userId}")
    public Result getBooksByUserId(@PathVariable Integer userId) {
        try {
            if (userId == null || userId <= 0) {
                return Result.error("用户ID不能为空且必须大于0");
            }

            List<Book> books = bookService.getBooksByUserId(userId);
            return Result.success(books);
        } catch (Exception e) {
            return Result.error("获取用户书籍失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据书籍ID获取书籍信息")
    @GetMapping("/getBookInfoById")
    public Result getBookInfoById(@RequestParam Integer id) {
        try {
            if (id == null || id <= 0) {
                return Result.error("书籍ID不能为空且必须大于0");
            }

            List<Book> books = bookService.getBookInfoById(id);
            if (books != null && !books.isEmpty()) {
                return Result.success(books);
            } else {
                return Result.error("未找到该书籍信息");
            }
        } catch (Exception e) {
            return Result.error("获取书籍信息失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查看所有图书信息")
    @GetMapping("/getAllBooks")
    public Result getAllBooks() {
        try {
            List<Book> books = bookService.getAllBooks();
            return Result.success(books);
        } catch (Exception e) {
            return Result.error("发生错误: " + e.getMessage());
        }
    }

    @Operation(summary = "根据图书名称查找图书信息")
    @GetMapping("/getBooksByName/{name}")
    public Result getBooksByName(@PathVariable String name) {
        try {
            List<Book> books = bookService.getBooksByName(name);
            return Result.success(books);
        } catch (Exception e) {
            return Result.error("发生错误: " + e.getMessage());
        }
    }

    @Operation(summary = "根据图书类型查看该类图书信息")
    @GetMapping("/getBooksByType/{type}")
    public Result getBooksByType(@PathVariable String type) {
        try {
            List<Book> books = bookService.getBooksByType(type);
            return Result.success(books);
        } catch (Exception e) {
            return Result.error("发生错误: " + e.getMessage());
        }
    }

    @Operation(summary = "获取教材信息")
    @GetMapping("/getBookByInfo")
    public Result getBookByInfo(@RequestParam(defaultValue = "15") Integer size,
                                @RequestParam(required = false) String bookName,
                                @RequestParam(required = false) String version,
                                @RequestParam(required = false) Integer note,
                                @RequestParam(required = false) String author,
                                @RequestParam(required = false) String publisher,
                                @RequestParam(required = false) String classify,
                                @RequestParam(required = false) String subClassify) {
        try {
            // 处理null字符串
            bookName = "null".equals(bookName) ? null : bookName;
            version = "null".equals(version) ? null : version;
            author = "null".equals(author) ? null : author;
            publisher = "null".equals(publisher) ? null : publisher;
            classify = "null".equals(classify) ? null : classify;
            subClassify = "null".equals(subClassify) ? null : subClassify;

            List<Book> books = bookService.getTextbooksByInfo(bookName, version, note, author, publisher, classify, subClassify, size);
            return Result.success(books);
        } catch (Exception e) {
            return Result.error("获取教材信息失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取非教材信息")
    @GetMapping("/getOtherBookByInfo")
    public Result getOtherBookByInfo(@RequestParam(defaultValue = "15") Integer size,
                                     @RequestParam(required = false) String bookName,
                                     @RequestParam(required = false) String version,
                                     @RequestParam(required = false) Integer note,
                                     @RequestParam(required = false) String author,
                                     @RequestParam(required = false) String publisher,
                                     @RequestParam(required = false) String classify,
                                     @RequestParam(required = false) String subClassify) {
        try {
            // 处理null字符串
            bookName = "null".equals(bookName) ? null : bookName;
            version = "null".equals(version) ? null : version;
            author = "null".equals(author) ? null : author;
            publisher = "null".equals(publisher) ? null : publisher;
            classify = "null".equals(classify) ? null : classify;
            subClassify = "null".equals(subClassify) ? null : subClassify;

            List<Book> books = bookService.getOtherBooksByInfo(bookName, version, note, author, publisher, classify, subClassify, size);
            return Result.success(books);
        } catch (Exception e) {
            return Result.error("获取非教材信息失败: " + e.getMessage());
        }
    }

    @Operation(summary = "教材根据价格排序")
    @GetMapping("/bookSortByPrice")
    public Result bookSortByPrice(@RequestParam(defaultValue = "15") Integer size,
                                  @RequestParam(required = false) String bookName,
                                  @RequestParam(required = false) String version,
                                  @RequestParam(required = false) Integer note,
                                  @RequestParam(required = false) String author,
                                  @RequestParam(required = false) String publisher,
                                  @RequestParam(required = false) String classify,
                                  @RequestParam(required = false) String subClassify) {
        try {
            // 处理null字符串
            bookName = "null".equals(bookName) ? null : bookName;
            version = "null".equals(version) ? null : version;
            author = "null".equals(author) ? null : author;
            publisher = "null".equals(publisher) ? null : publisher;
            classify = "null".equals(classify) ? null : classify;
            subClassify = "null".equals(subClassify) ? null : subClassify;

            List<Book> books = bookService.getTextbooksSortByPrice(bookName, version, note, author, publisher, classify, subClassify, size);
            return Result.success(books);
        } catch (Exception e) {
            return Result.error("获取教材信息失败: " + e.getMessage());
        }
    }

    @Operation(summary = "非教材根据价格排序")
    @GetMapping("/otherBookSortByPrice")
    public Result otherBookSortByPrice(@RequestParam(defaultValue = "15") Integer size,
                                       @RequestParam(required = false) String bookName,
                                       @RequestParam(required = false) String version,
                                       @RequestParam(required = false) Integer note,
                                       @RequestParam(required = false) String author,
                                       @RequestParam(required = false) String publisher,
                                       @RequestParam(required = false) String classify,
                                       @RequestParam(required = false) String subClassify) {
        try {
            // 处理null字符串
            bookName = "null".equals(bookName) ? null : bookName;
            version = "null".equals(version) ? null : version;
            author = "null".equals(author) ? null : author;
            publisher = "null".equals(publisher) ? null : publisher;
            classify = "null".equals(classify) ? null : classify;
            subClassify = "null".equals(subClassify) ? null : subClassify;

            List<Book> books = bookService.getOtherBooksSortByPrice(bookName, version, note, author, publisher, classify, subClassify, size);
            return Result.success(books);
        } catch (Exception e) {
            return Result.error("获取非教材信息失败: " + e.getMessage());
        }
    }
}