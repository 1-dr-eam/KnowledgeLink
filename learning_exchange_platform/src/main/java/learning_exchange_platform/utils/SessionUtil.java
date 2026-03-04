package learning_exchange_platform.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
public class SessionUtil {

    static public HttpSession getSession() {
        try {
            //获取HttpSession
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            return request.getSession(false);
        } catch (Exception e) {
            e.printStackTrace();
            log.info("获取session失败");
            return null;
        }
    }
}
