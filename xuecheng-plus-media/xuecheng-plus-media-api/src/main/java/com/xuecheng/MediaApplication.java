package com.xuecheng;

import com.spring4all.swagger.EnableSwagger2Doc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableSwagger2Doc
@SpringBootApplication
@EnableScheduling // 启用Spring定时任务
public class MediaApplication {
	public static void main(String[] args) {
		SpringApplication.run(MediaApplication.class, args);
	}
}