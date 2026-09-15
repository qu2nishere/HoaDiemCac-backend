package com.hoadiemcat.config;

import com.hoadiemcat.entity.User;
import com.hoadiemcat.entity.enums.Role;
import com.hoadiemcat.entity.enums.Status;
import com.hoadiemcat.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        String adminEmail = "kqtthings@gmail.com";
        Optional<User> existingUser = userRepository.findByEmail(adminEmail);
        
        if (existingUser.isEmpty()) {
            User admin = User.builder()
                    .username("admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("Abc@1234"))
                    .fullName("Quản Trị Viên")
                    .role(Role.ADMIN)
                    .status(Status.ACTIVE)
                    .build();
            userRepository.save(admin);
            System.out.println("=========================================================");
            System.out.println("✅ TẠO TÀI KHOẢN ADMIN THÀNH CÔNG");
            System.out.println("Email: " + adminEmail);
            System.out.println("Password: Đã mã hóa và lưu vào Database");
            System.out.println("=========================================================");
        } else {
            // Force reset password to Abc@1234 encoded
            User admin = existingUser.get();
            admin.setPassword(passwordEncoder.encode("Abc@1234"));
            userRepository.save(admin);
            System.out.println("=========================================================");
            System.out.println("⚡ Tài khoản Admin (" + adminEmail + ") đã tồn tại. ĐÃ RESET LẠI MẬT KHẨU THÀNH: Abc@1234");
            System.out.println("=========================================================");
        }
    }
}
