package com.rikkeibank.transaction.service;

import com.rikkeibank.common.constant.SecurityConstants;
import com.rikkeibank.common.event.TransferCompletedEvent;
import com.rikkeibank.common.event.TransferRollbackEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTransferCompleted(TransferCompletedEvent event) {
        try {
            log.info("Publishing TransferCompletedEvent to Kafka topic '{}': txId={}",
                    SecurityConstants.TOPIC_TRANSACTION_EVENTS, event.getTransactionId());
            kafkaTemplate.send(SecurityConstants.TOPIC_TRANSACTION_EVENTS, event.getTransactionId(), event);
        } catch (Exception e) {
            log.warn("Kafka broker unreachable or error sending event: {}. Continuing without blocking transaction.", e.getMessage());
        }
    }

    public void publishTransferRollback(TransferRollbackEvent event) {
        try {
            log.info("Publishing TransferRollbackEvent to Kafka topic '{}': txId={}",
                    SecurityConstants.TOPIC_TRANSACTION_EVENTS, event.getTransactionId());
            kafkaTemplate.send(SecurityConstants.TOPIC_TRANSACTION_EVENTS, event.getTransactionId(), event);
        } catch (Exception e) {
            log.warn("Kafka broker unreachable or error sending event: {}. Continuing without blocking transaction.", e.getMessage());
        }
    }
}
