package com.mycompany.ecommerce.mail;

public interface EmailService {
    void sendEmail(String to, String subject, String content);
}