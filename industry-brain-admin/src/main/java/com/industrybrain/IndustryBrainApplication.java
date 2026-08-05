package com.industrybrain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(IndustryBrainApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(IndustryBrainApplication.class, args);
        log.info("产业链智能分析与招商推荐平台 启动成功！");
    }
}
