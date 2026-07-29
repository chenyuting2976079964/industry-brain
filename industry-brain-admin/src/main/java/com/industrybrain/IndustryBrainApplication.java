package com.industrybrain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 产业链智能分析与招商推荐平台 - 启动入口
 * 基于若依（RuoYi）框架扩展
 */
@EnableAsync
@SpringBootApplication
public class IndustryBrainApplication {

    public static void main(String[] args) {
        SpringApplication.run(IndustryBrainApplication.class, args);
        System.out.println("========================================");
        System.out.println("  产业链智能分析与招商推荐平台 启动成功！");
        System.out.println("========================================");
    }
}
