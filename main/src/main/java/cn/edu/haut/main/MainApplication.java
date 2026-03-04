package cn.edu.haut.main;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = {
		"cn.edu.haut.main",
		"learning_exchange_platform",
		"com.liuyi.fateqq",
		"org.example.book"
})
@MapperScan(basePackages = {
		"cn.edu.haut.main",
		"learning_exchange_platform",
		"com.liuyi.fateqq",
		"org.example.book"
})
@SpringBootConfiguration
@EnableAutoConfiguration
public class MainApplication {
	public static void main(String[] args) {
		SpringApplication.run(MainApplication.class, args);
	}
}