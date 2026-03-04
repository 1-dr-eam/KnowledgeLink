package learning_exchange_platform.controller;

import learning_exchange_platform.model.Result;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import jakarta.servlet.http.HttpSession;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

@RestController
public class CaptchaController {
    private static final Random random = new Random();

    @RequestMapping("/captcha")
    public Result getCaptcha(HttpSession session) throws IOException {
        // 创建验证码
        String captcha = generateCaptcha(5);
        session.setAttribute("captcha", captcha);

        // 创建图片
        int width = 200;
        int height = 50;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = image.getGraphics();

        // 设置背景色
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);

        // 设置字体
        graphics.setFont(new Font("Arial", Font.BOLD, 30));

        // 绘制验证码
        graphics.setColor(Color.BLACK);
        graphics.drawString(captcha, 50, 35);

        // 绘制干扰线
        graphics.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < 5; i++) {
            int x1 = random.nextInt(width);
            int y1 = random.nextInt(height);
            int x2 = random.nextInt(width);
            int y2 = random.nextInt(height);
            graphics.drawLine(x1, y1, x2, y2);
        }

        // 将图片转换为字节数组
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "JPEG", baos);
        byte[] imageBytes = baos.toByteArray();

        // 设置响应头，避免浏览器缓存
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl("no-store, no-cache, must-revalidate");
        headers.setPragma("no-cache");
        //告诉浏览器这是一个图片，不要当成二进制文件
        headers.setContentType(MediaType.IMAGE_JPEG);

        return Result.success(new ResponseEntity<>(imageBytes, headers, HttpStatus.OK));
    }

    private String generateCaptcha(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder captchaBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            captchaBuilder.append(characters.charAt(index));
        }
        return captchaBuilder.toString();
    }

    public static boolean validateCaptcha(String input, HttpSession session) {
        String captcha = (String) session.getAttribute("captcha");
        if (captcha != null && captcha.equalsIgnoreCase(input)) {
            return true;
        } else {
            return false;
        }
    }
}