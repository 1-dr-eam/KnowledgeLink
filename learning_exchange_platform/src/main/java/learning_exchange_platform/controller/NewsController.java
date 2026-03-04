package learning_exchange_platform.controller;

import learning_exchange_platform.model.News;
import learning_exchange_platform.model.Result;
import learning_exchange_platform.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class NewsController {
    @Autowired
    private NewsService newsService;

    @RequestMapping("/getNewsList")
    public Result getNewsList(int limit){
        List<News> news_list=newsService.getNewsList(limit);
        if(news_list.isEmpty()){
            return Result.error("获取新闻列表失败");
        }else {
            return Result.success(news_list);
        }
    }

    @RequestMapping("/getNewsDetail")
    public Result getNewsDetail(int id){
        News news=newsService.getNewsById(id);
        if(news==null){
            return Result.error("获取新闻详情失败");
        }else {
            return Result.success(news);
        }
    }
}
