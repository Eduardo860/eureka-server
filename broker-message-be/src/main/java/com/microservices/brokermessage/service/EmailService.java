package com.microservices.brokermessage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${notification.email.from}")
    private String from;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        logger.info("Email sent to={} subject={}", to, subject);
    }

    public void sendFailureEmail(String to, String jobId, String entityId, String action, String errorMessage) {
        String subject = "[RETRY FAILED] " + action + " | Job: " + jobId;
        String body = String.format(
            "Retry job has permanently failed after maximum attempts.%n%n" +
            "Job ID  : %s%n" +
            "Entity  : %s%n" +
            "Action  : %s%n" +
            "Error   : %s",
            jobId, entityId, action, errorMessage
        );
        sendEmail(to, subject, body);
    }

    public void sendPaymentReceivedEmail(String customerEmail, String orderId, Double amount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(from);
            helper.setTo(customerEmail);
            helper.setSubject("Pago recibido - Orden #" + orderId);
            
            String content = String.format(
                "<html><body>" +
                "<h1>Pago Recibido</h1>" +
                "<p>Hemos recibido tu pago de <strong>$%.2f</strong> para la orden <strong>%s</strong>.</p>" +
                "<p>Gracias por tu compra!</p>" +
                "</body></html>",
                amount, orderId
            );
            
            helper.setText(content, true);
            mailSender.send(message);
            
            logger.info("Payment received email sent for order: {}", orderId);
        } catch (Exception e) {
            logger.error("Failed to send payment received email: {}", e.getMessage());
        }
    }

    public void sendOrderConfirmationEmail(String customerEmail, String orderId, Map<String, Object> orderData) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(from);
            helper.setTo(customerEmail);
            helper.setSubject("Confirmación de Orden #" + orderId);
            
            String content = String.format(
                "<html><body>" +
                "<h1>Tu orden ha sido confirmada</h1>" +
                "<p>Orden ID: <strong>%s</strong></p>" +
                "<p>Total: <strong>$%.2f</strong></p>" +
                "<p>Estado: <strong>%s</strong></p>" +
                "<p>Gracias por tu compra!</p>" +
                "</body></html>",
                orderId,
                orderData.get("totalAmount") != null ? ((Number) orderData.get("totalAmount")).doubleValue() : 0.0,
                orderData.get("status") != null ? orderData.get("status") : "N/A"
            );
            
            helper.setText(content, true);
            mailSender.send(message);
            
            logger.info("Order confirmation email sent to: {}", customerEmail);
        } catch (Exception e) {
            logger.error("Failed to send order confirmation email: {}", e.getMessage());
        }
    }

    public void sendStatusChangeEmail(String customerEmail, String orderId, String newStatus) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(from);
            helper.setTo(customerEmail);
            helper.setSubject("Cambio de Estado - Orden #" + orderId);
            
            String content = String.format(
                "<html><body>" +
                "<h1>Tu orden ha cambiado de estado</h1>" +
                "<p>Orden ID: <strong>%s</strong></p>" +
                "<p>Nuevo Estado: <strong>%s</strong></p>" +
                "</body></html>",
                orderId, newStatus
            );
            
            helper.setText(content, true);
            mailSender.send(message);
            
            logger.info("Status change email sent to: {}", customerEmail);
        } catch (Exception e) {
            logger.error("Failed to send status change email: {}", e.getMessage());
        }
    }
}
