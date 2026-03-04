package learning_exchange_platform.service;

import learning_exchange_platform.mapper.UserPostBehaviorMapper;
import learning_exchange_platform.model.UserPostBehavior;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserPostBehaviorService {
    @Autowired
    private UserPostBehaviorMapper userPostBehaviorMapper;

    public List<UserPostBehavior> getUserPostBehaviorByUserId(int user_id) {
        try {
            return userPostBehaviorMapper.selectPostBehaviorByUserId(user_id);
        }catch (Exception e) {
            System.out.println("查询用户行为失败");
            e.printStackTrace();
            return null;
        }
    }

    public List<UserPostBehavior> getNavigateBehaviorByUserId(int user_id) {
        try {
            return userPostBehaviorMapper.selectNavigateBehaviorByUserId(user_id);
        }catch (Exception e) {
            System.out.println("查询用户浏览行为失败");
            e.printStackTrace();
            return null;
        }
    }

    public List<UserPostBehavior> getUserPostBehaviorById(int id) {
        try {
            return userPostBehaviorMapper.selectUserPostBehaviorById(id);
        }catch (Exception e) {
            System.out.println("查询用户行为失败");
            e.printStackTrace();
            return null;
        }
    }

    public boolean addUserPostBehavior(UserPostBehavior userPostBehavior) {
        try {
            return userPostBehaviorMapper.insertUserPostBehavior(userPostBehavior);
        }catch (Exception e) {
            System.out.println("插入用户行为失败");
            e.printStackTrace();
            return false;
        }
    }

}
