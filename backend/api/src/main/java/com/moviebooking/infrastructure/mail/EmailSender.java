package com.moviebooking.infrastructure.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/** Gửi email HTML. Lỗi gửi chỉ ghi log — email là tác dụng phụ, không làm hỏng giao dịch (L6). */
@Slf4j
@Component
public class EmailSender {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final MailProperties properties;

    public EmailSender(ObjectProvider<JavaMailSender> mailSender, MailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void send(String to, String subject, String html) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!properties.enabled() || sender == null) {
            log.info("[mail disabled] to={} subject={} ({} ký tự HTML)", to, subject, html.length());
            return;
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(properties.from());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            sender.send(message);
        } catch (MessagingException | RuntimeException ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage());
        }
    }
}
