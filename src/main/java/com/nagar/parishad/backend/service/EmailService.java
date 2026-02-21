package com.nagar.parishad.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendEmail(String to, String subject, String body) {
        sendEmail(to, subject, body, null);
    }

    @Async
    public void sendEmail(String to, String subject, String body, java.util.List<java.io.File> attachments) {
        try {
            jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                    message, true);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true indicates HTML content

            if (attachments != null && !attachments.isEmpty()) {
                System.out.println("DEBUG: EmailService received " + attachments.size() + " attachments.");
                for (java.io.File file : attachments) {
                    if (file.exists() && file.isFile()) {
                        System.out.println("DEBUG: Attaching file: " + file.getName());
                        helper.addAttachment(file.getName(), file);
                    } else {
                        System.out.println(
                                "DEBUG: EmailService could not find file to attach: " + file.getAbsolutePath());
                    }
                }
            } else {
                System.out.println("DEBUG: EmailService received NO attachments.");
            }

            mailSender.send(message);
            System.out.println("Email sent successfully to " + to);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
        }
    }

    // Removed @Async to allow error propagation to the controller
    public void sendEmailWithAttachment(String to, String subject, String body, byte[] attachmentData,
            String attachmentName) throws Exception {
        try {
            jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                    message, true);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);

            if (attachmentData != null && attachmentData.length > 0) {
                helper.addAttachment(attachmentName, new org.springframework.core.io.ByteArrayResource(attachmentData));
            }

            mailSender.send(message);
            System.out.println("Email with attachment sent successfully to " + to);
        } catch (Exception e) {
            System.err.println("Failed to send email with attachment to " + to + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Async
    public void sendWelcomeEmail(String to, String name, String password) {
        String subject = "Welcome to Nagar Parishad - Your Admin Credentials";
        String body = "<div style=\"font-family: Arial, sans-serif; color: #333;\">"
                + "<h2>Welcome to Nagar Parishad, " + name + "!</h2>"
                + "<p>Your Admin account has been successfully created. Below are your login credentials:</p>"
                + "<div style=\"background-color: #f5f5f5; padding: 15px; border-radius: 5px; border: 1px solid #ddd; margin: 20px 0;\">"
                + "<p><strong>Email:</strong> " + to + "</p>"
                + "<p><strong>Password:</strong> " + password + "</p>"
                + "</div>"
                + "<p>Please login and change your password immediately for security purposes.</p>"
                + "<br>"
                + "<p>Best Regards,<br>The Nagar Parishad Team</p>"
                + "</div>";

        sendEmail(to, subject, body);
    }
}
