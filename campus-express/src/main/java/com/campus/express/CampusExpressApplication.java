package com.campus.express;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 校园快递点导航与空闲度查询平台 —— 启动类
 *
 * @author 郑俊霖
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@MapperScan("com.campus.express.mapper")
public class CampusExpressApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusExpressApplication.class, args);
    }
}
