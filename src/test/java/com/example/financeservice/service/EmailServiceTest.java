package com.example.financeservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

  @Mock
  private JavaMailSender javaMailSender;

  @InjectMocks
  private EmailService emailService;

  @BeforeEach
  void setUp() {
    // Configurar comportamento padrão
    doNothing().when(javaMailSender).send(any(SimpleMailMessage.class));
  }

  @Test
  void sendSimpleMessage_ShouldSendEmailWithCorrectParameters() {
    // Act
    emailService.sendSimpleMessage("test@example.com", "Test Subject", "Test Message");

    // Assert
    verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
  }

  @Test
  void sendPasswordResetEmail_ShouldSendEmailWithResetToken() {
    // Act
    emailService.sendPasswordResetEmail("test@example.com", "reset-token");

    // Assert
    verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
  }
}
