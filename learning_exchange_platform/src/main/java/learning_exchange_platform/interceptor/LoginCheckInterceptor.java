package learning_exchange_platform.interceptor;

import com.alibaba.fastjson.JSON;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import learning_exchange_platform.model.Result;
import learning_exchange_platform.utils.JWTUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Component
public class LoginCheckInterceptor implements HandlerInterceptor {
    //Controller方法运行前
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String url=request.getRequestURL().toString();
        log.info("请求的url:{}",url);
        String jwt=request.getHeader("token");

        if(jwt.isEmpty())
        {
            log.info("请求头token为空，未登录");
            Result error=Result.error("NOT_LOGIN");
            String notLogin=JSON.toJSONString(error);
            response.getWriter().write(notLogin);
            return false;
        }

        try {
            Claims claims=JWTUtil.parseJWT(jwt);
            int user_id=Integer.parseInt(claims.get("user_id").toString());
            HttpSession session=request.getSession();
            session.setAttribute("user_id",user_id);
        } catch (Exception e) {
            e.printStackTrace();
            log.info("解析令牌失败，令牌非法");
            Result error=Result.error("NOT_LOGIN");
            //手动转阿里巴巴高速JSON
            String notLogin=JSON.toJSONString(error);
            response.getWriter().write(notLogin);
            return false;
        }

        log.info("令牌合法，放行");
        return true;
    }

    //Controller方法运行后
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        System.out.println("postHandle");
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    //页面渲染完成后
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        System.out.println("afterCompletion");
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
