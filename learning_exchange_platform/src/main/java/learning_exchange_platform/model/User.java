package learning_exchange_platform.model;

import lombok.Data;

import java.math.BigInteger;

@Data
public class User {
    private Integer id;
    private String phone;
    private String username;
    private String password;
    private String avatar;
    private String summary;
    private String grade;
    private String major;
    private double balance;

    public User(String phone, String username, String password, String grade,String major) {
        this.phone = phone;
        this.username = username;
        this.password = password;
        this.grade = grade;
        this.major = major;
    }

    public User(Integer id, String phone, String username, String password, String avatar, String summary, String grade, String major,double balance) {
        this.id = id;
        this.phone = phone;
        this.username = username;
        this.password = password;
        this.avatar = avatar;
        this.summary = summary;
        this.grade = grade;
        this.major = major;
        this.balance = balance;
    }
    public User() {}

}
