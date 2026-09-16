package com.hoadiemcat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class HoaDiemCatApplication {

    public static void main(String[] args) {
        SpringApplication.run(HoaDiemCatApplication.class, args);
    }
}
