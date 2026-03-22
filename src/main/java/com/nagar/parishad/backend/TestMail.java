package com.nagar.parishad.backend;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class TestMail {
    public static void main(String[] args) {
        String to = "emailnotifications26@gmail.com";
        String from = "noreply@townseva.in";
        String host = "server17213-10344.hostycare.online";

        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");

        Session session = Session.getInstance(properties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("noreply@townseva.in", "NashikLoknagar@123");
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject("SMTP Security Test - TownSeva");
            message.setText(
                    "This is an automated connection test sent via Jakarta Mail on SSL Port 465 from hostycare.online.");

            System.out.println("Attempting to securely negotiate with Hostycare SMTP server on port 465...");
            Transport.send(message);
            System.out.println("SUCCESS! TownSeva email delivered.");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
