package com.easy.elasticsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.easy.elasticsearch")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
