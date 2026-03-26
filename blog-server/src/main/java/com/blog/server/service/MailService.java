package com.blog.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@blog.local}")
    private String fromEmail;

    @Value("${app.site-url:http://localhost:3000}")
    private String siteUrl;

    @Async
    public void sendCommentReplyNotification(String toEmail, String parentAuthor, String replyAuthor, String articleTitle, String articleSlug) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("有人在博客中回复了您");

            String text = String.format(
                    "亲爱的 %s:\n\n用户 %s 在文章《%s》中回复了您的评论。\n\n点击此处查看回复详情: %s/article/%s#comments\n\n(此邮件由系统自动发出，请勿回复)",
                    parentAuthor, replyAuthor, articleTitle, siteUrl, articleSlug
            );

            message.setText(text);
            mailSender.send(message);
            log.info("Sent reply notification email to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send reply notification email to {}. Reason: {}", toEmail, e.getMessage());
        }
    }
}
