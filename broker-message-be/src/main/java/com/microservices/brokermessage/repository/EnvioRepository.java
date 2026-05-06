package com.microservices.brokermessage.repository;

import com.microservices.brokermessage.model.Envio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnvioRepository extends JpaRepository<Envio, UUID> {
    
    Optional<Envio> findByOrderId(String orderId);
    
    List<Envio> findByStatusAndNextRunAtBefore(String status, LocalDateTime now);
    
    List<Envio> findByStatus(String status);
}
