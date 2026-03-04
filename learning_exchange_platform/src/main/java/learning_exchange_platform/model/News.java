package learning_exchange_platform.model;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class News {
    private int id;
    private String title;
    //年月日
    private LocalDate publish_date;
    private int click_count;
    private String summary;
    private String content;
    private String avatar;//附件链接

}

