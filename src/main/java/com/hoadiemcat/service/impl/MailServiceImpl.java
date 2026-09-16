package com.hoadiemcat.service.impl;

import com.hoadiemcat.service.MailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {

    private final JavaMailSender javaMailSender;

    @Override
    @Async
    public CompletableFuture<Void> sendWelcomeEmail(String toEmail, String fullName, String password) {
        log.info("Sending welcome email to: {}", toEmail);
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Tài khoản nhân viên - Hỏa Diệm Các");

            String htmlMsg = "<div style='font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; border: 1px solid #ddd; border-radius: 8px; overflow: hidden;'>" +
                    "<div style='background-color: #A31D1D; color: #fff; padding: 20px; text-align: center;'>" +
                    "  <h2 style='margin: 0;'>Chào mừng đến với Hỏa Diệm Các</h2>" +
                    "</div>" +
                    "<div style='padding: 20px;'>" +
                    "  <p>Xin chào <strong>" + fullName + "</strong>,</p>" +
                    "  <p>Tài khoản nhân viên của bạn đã được quản trị viên cấp phát thành công. Dưới đây là thông tin đăng nhập hệ thống của bạn:</p>" +
                    "  <div style='background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin-bottom: 20px;'>" +
                    "    <p style='margin: 0;'><strong>Tên đăng nhập / Email:</strong> " + toEmail + "</p>" +
                    "    <p style='margin: 0;'><strong>Mật khẩu:</strong> <span style='color: #A31D1D; font-weight: bold;'>" + password + "</span></p>" +
                    "  </div>" +
                    "  <p><em>Vui lòng thay đổi mật khẩu sau khi đăng nhập lần đầu tiên để đảm bảo an toàn.</em></p>" +
                    "  <br/>" +
                    "  <p>Trân trọng,</p>" +
                    "  <p><strong>Ban Quản Trị Hỏa Diệm Các</strong></p>" +
                    "</div>" +
                    "</div>";

            helper.setText(htmlMsg, true);
            javaMailSender.send(message);
            log.info("Welcome email sent successfully to: {}", toEmail);
            
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", toEmail, e);
        }
        return CompletableFuture.completedFuture(null);
    }
}
