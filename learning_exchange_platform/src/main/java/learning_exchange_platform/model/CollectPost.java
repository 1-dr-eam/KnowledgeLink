package learning_exchange_platform.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CollectPost {
    private Integer user_id;
    private Integer post_id;
    private String username;
    private String user_avatar;
    private String post_title;
    private LocalDate collect_date;
}
