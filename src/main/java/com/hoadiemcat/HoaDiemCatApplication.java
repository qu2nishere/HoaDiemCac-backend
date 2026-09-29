package com.hoadiemcat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class HoaDiemCatApplication {

    private static final Logger log = LoggerFactory.getLogger(HoaDiemCatApplication.class);

    private final Environment env;

    public HoaDiemCatApplication(Environment env) {
        this.env = env;
    }

    public static void main(String[] args) {
        SpringApplication.run(HoaDiemCatApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String port = env.getProperty("server.port", "8080");
        String activeProfile = env.getProperty("spring.profiles.active", "dev");

        String banner = """
            \n----------------------------------------------------------
            \t🚀 HỎA DIỆM CÁC BACKEND SERVER ĐÃ KHỞI ĐỘNG THÀNH CÔNG!
            \t👉 Local API URL:    http://localhost:%s
            \t👉 Swagger UI Doc:   http://localhost:%s/swagger-ui.html
            \t👉 Active Profile:   %s
            \t🟢 Hệ thống đã sẵn sàng nhận kết nối và xử lý yêu cầu!
            ----------------------------------------------------------
            """.formatted(port, port, activeProfile);

        // Dùng System.out.println để nổi bật hẳn so với các dòng log thông thường
        System.out.println(banner);
    }
}