package learning_exchange_platform.model;


import lombok.Data;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Data
public class UserPostBehavior {
    private BigInteger id;
    private Integer user_id;
    private Integer post_id;
    private Integer behavior_type;
    private LocalDateTime behavior_time;
    private Integer navigate_time;

    public UserPostBehavior(BigInteger id, Integer user_id, Integer post_id, Integer behavior_type, LocalDateTime behavior_time, Integer navigate_time) {
        this.id = id;
        this.user_id = user_id;
        this.post_id = post_id;
        this.behavior_type = behavior_type;
        this.behavior_time = behavior_time;
        this.navigate_time = navigate_time;
    }

    public UserPostBehavior(Integer user_id, Integer post_id, Integer behavior_type, LocalDateTime behavior_time, Integer navigate_time) {
        this.user_id = user_id;
        this.post_id = post_id;
        this.behavior_type = behavior_type;
        this.behavior_time = behavior_time;
        this.navigate_time = navigate_time;
    }
}
