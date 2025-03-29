package com.example.financeservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

  private final JavaMailSender emailSender;

  public void sendSimpleMessage(String to, String subject, String text) {
    var message = new SimpleMailMessage();
    message.setTo(to);
    message.setSubject(subject);
    message.setText(text);
    emailSender.send(message);
    log.info("EmailService: Email sent to: {}", to);
  }

  public void sendPasswordResetEmail(String email, String token) {
    var resetUrl = "http://yourdomain.com/reset-password?token=" + token;
    var subject = "Password Reset Request";
    var message = "To reset your password, click the link below:\n\n" + resetUrl +
        "\n\nIf you did not request a password reset, please ignore this email.";

    sendSimpleMessage(email, subject, message);
  }
}
