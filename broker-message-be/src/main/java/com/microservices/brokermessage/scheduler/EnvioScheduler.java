package com.microservices.brokermessage.scheduler;

import com.microservices.brokermessage.model.Envio;
import com.microservices.brokermessage.repository.EnvioRepository;
import com.microservices.brokermessage.service.EnvioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class EnvioScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(EnvioScheduler.class);
    
    @Autowired
    private EnvioRepository envioRepository;
    
    @Autowired
    private EnvioService envioService;
    
    @Scheduled(fixedDelay = 10000) // cada 10 segundos
    public void processScheduledEmails() {
        logger.debug("EnvioScheduler: Processing scheduled emails...");
        
        try {
            List<Envio> pending = envioRepository.findByStatusAndNextRunAtBefore(
                "SCHEDULED",
                LocalDateTime.now()
            );
            
            if (!pending.isEmpty()) {
                logger.info("Found {} pending envios to process", pending.size());
                
                for (Envio envio : pending) {
                    logger.info("Processing envio for order: {}", envio.getOrderId());
                    envioService.processEnvio(envio);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in EnvioScheduler: {}", e.getMessage(), e);
        }
    }
}
