package learning_exchange_platform.mapper;

import learning_exchange_platform.model.News;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NewsMapper {
    public List<News> selectNewsList(int limit);
    public News selectNewsById(int id);

}
