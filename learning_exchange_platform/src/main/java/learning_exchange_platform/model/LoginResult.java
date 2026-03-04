package learning_exchange_platform.model;

import lombok.Data;

@Data
public class LoginResult {
    private String token;
    private User userInfo;

    public LoginResult(String token, User userInfo) {
        this.token = token;
        this.userInfo = userInfo;
    }
}
