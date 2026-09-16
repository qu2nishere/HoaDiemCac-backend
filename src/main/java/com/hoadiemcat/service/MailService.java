package com.hoadiemcat.service;

import java.util.concurrent.CompletableFuture;

public interface MailService {
    CompletableFuture<Void> sendWelcomeEmail(String toEmail, String fullName, String password);
}
