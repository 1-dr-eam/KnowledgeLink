package com.liuyi.fateqq;

import com.liuyi.fateqq.mapper.MessageRecordMapper;
import com.liuyi.fateqq.model.MessageRecord;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.InputStream;
import java.util.List;

class MyBatisTests {
    public static void main(String[] args) throws Exception {

        String Id="123";
        //1.加载mybatis的核心配置文件，获取SqlSessionFactory
        String resource = "mybatis.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        //2.获取SqlSession对象，用它执行SQL
        SqlSession sqlSession = sqlSessionFactory.openSession();

        //3.执行SQL
        //List<User> users = sqlSession.selectList("test.selectAll");
        //3.获取UserMapper接口的代理对象
        MessageRecordMapper Mapper = sqlSession.getMapper(MessageRecordMapper.class);
        MessageRecord msg= Mapper. selectMessageById(Id);

        System.out.println();

        //4.释放 SqlSession
        sqlSession.close();
    }
}