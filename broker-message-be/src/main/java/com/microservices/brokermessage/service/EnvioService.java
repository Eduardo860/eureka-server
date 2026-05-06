package com.microservices.brokermessage.service;

import com.microservices.brokermessage.model.Envio;
import com.microservices.brokermessage.repository.EnvioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class EnvioService {
    
    private static final Logger logger = LoggerFactory.getLogger(EnvioService.class);
    
    @Autowired
    private EnvioRepository envioRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${ORDER_SERVICE_URL:http://apigateway:8080/orders-api}")
    private String orderServiceUrl;
    
    public Envio createEnvio(String orderId, String customerEmail) {
        try {
            // Verificar si ya existe
            var existing = envioRepository.findByOrderId(orderId);
            if (existing.isPresent()) {
                logger.warn("Envio already exists for order: {}", orderId);
                return existing.get();
            }
            
            Envio envio = new Envio();
            envio.setOrderId(orderId);
            envio.setCustomerEmail(customerEmail);
            envio.setStatus("SCHEDULED");
            envio.setNextRunAt(LocalDateTime.now());
            
            Envio saved = envioRepository.save(envio);
            logger.info("Envio created for order: {}", orderId);
            return saved;
        } catch (Exception e) {
            logger.error("Error creating envio for order {}: {}", orderId, e.getMessage());
            throw e;
        }
    }
    
    public void processEnvio(Envio envio) {
        try {
            logger.info("Processing envio for order: {}", envio.getOrderId());
            
            // Obtener detalles de la orden
            Map<String, Object> orderResponse = restTemplate.getForObject(
                orderServiceUrl + "/" + envio.getOrderId(),
                Map.class
            );
            
            if (orderResponse == null || !orderResponse.containsKey("data")) {
                throw new RuntimeException("Order not found: " + envio.getOrderId());
            }
            
            Map<String, Object> orderData = (Map<String, Object>) orderResponse.get("data");
            
            // Enviar correo de confirmación
            emailService.sendOrderConfirmationEmail(
                envio.getCustomerEmail(),
                envio.getOrderId(),
                orderData
            );
            
            // Actualizar estado a SENT
            envio.setStatus("SENT");
            envio.setSentAt(LocalDateTime.now());
            envioRepository.save(envio);
            
            logger.info("Envio processed successfully for order: {}", envio.getOrderId());
            
        } catch (Exception e) {
            logger.error("Failed to process envio for order: {}", envio.getOrderId(), e);
            
            // Incrementar intento y reprogramar
            envio.setAttempt(envio.getAttempt() + 1);
            envio.setErrorMessage(e.getMessage());
            
            if (envio.getAttempt() >= 5) {
                envio.setStatus("FAILED");
                logger.error("Envio failed after 5 attempts for order: {}", envio.getOrderId());
            } else {
                // Backoff exponencial
                long delayMinutes = (long) Math.pow(2, envio.getAttempt());
                envio.setNextRunAt(LocalDateTime.now().plusMinutes(delayMinutes));
                logger.info("Envio rescheduled for order {} in {} minutes", envio.getOrderId(), delayMinutes);
            }
            
            envioRepository.save(envio);
        }
    }
}
