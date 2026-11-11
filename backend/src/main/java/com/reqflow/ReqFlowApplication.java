package com.reqflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.reqflow.mapper")
@EnableScheduling
public class ReqFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReqFlowApplication.class, args);
    }
}
