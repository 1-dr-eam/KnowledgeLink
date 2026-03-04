package learning_exchange_platform.service;

import learning_exchange_platform.mapper.NewsMapper;
import learning_exchange_platform.model.News;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NewsService {
    @Autowired
    private NewsMapper newsMapper;

    public List<News> getNewsList(int limit) {
        try {
            return newsMapper.selectNewsList(limit);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public News getNewsById(int id) {
        try {
            return newsMapper.selectNewsById(id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
