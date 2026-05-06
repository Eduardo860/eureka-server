package com.microservices.brokermessage.kafka;

import com.microservices.brokermessage.service.EmailService;
import com.microservices.brokermessage.service.EnvioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OrderStatusChangedEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderStatusChangedEventListener.class);
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private EnvioService envioService;
    
    @KafkaListener(topics = "order_status_changed_events", groupId = "order-status-group")
    public void consumeOrderStatusChangedEvent(Map<String, Object> event) {
        logger.info("Consumed order_status_changed_events: {}", event);
        
        try {
            String orderId = (String) event.get("orderId");
            String newStatus = (String) event.get("newStatus");
            String customerEmail = (String) event.get("customerEmail");
            
            if (orderId == null || newStatus == null) {
                logger.warn("Invalid event data: orderId={}, newStatus={}", orderId, newStatus);
                return;
            }
            
            // 1. Enviar correo de notificación
            if (customerEmail != null && !customerEmail.isEmpty()) {
                emailService.sendStatusChangeEmail(customerEmail, orderId, newStatus);
                logger.info("Status change email sent for order: {}", orderId);
            }
            
            // 2. Si status = PAGADO, guardar en envios
            if ("PAGADO".equalsIgnoreCase(newStatus)) {
                logger.info("Order {} marked as PAGADO, creating envio", orderId);
                
                if (customerEmail != null && !customerEmail.isEmpty()) {
                    envioService.createEnvio(orderId, customerEmail);
                } else {
                    logger.warn("Cannot create envio for order {} - missing customer email", orderId);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error processing order_status_changed_events: {}", e.getMessage(), e);
        }
    }
}
