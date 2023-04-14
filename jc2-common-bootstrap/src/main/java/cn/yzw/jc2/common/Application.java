package cn.yzw.jc2.common;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "cn.yzw.jc2.common")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
