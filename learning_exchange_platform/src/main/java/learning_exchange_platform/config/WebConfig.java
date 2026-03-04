package learning_exchange_platform.config;

import learning_exchange_platform.interceptor.LoginCheckInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginCheckInterceptor loginCheckInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> list = new ArrayList<String>();
        list.add("/login");
        list.add("/register");
        list.add("/captcha");
        list.add("/home");
        list.add("/getNewsList");
        list.add("/getNewsDetail");
        list.add("/payment/alipay/notify");

        registry.addInterceptor(loginCheckInterceptor).addPathPatterns("/**").excludePathPatterns(list);
    }
}
