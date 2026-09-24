package com.rikkeibank.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkeibank.common.constant.SecurityConstants;
import com.rikkeibank.common.event.TransferCompletedEvent;
import com.rikkeibank.common.event.TransferRollbackEvent;
import com.rikkeibank.notification.model.Notification;
import com.rikkeibank.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionKafkaConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = SecurityConstants.TOPIC_TRANSACTION_EVENTS, groupId = "notification-group", autoStartup = "${spring.kafka.consumer.auto-startup:true}")
    public void consumeTransactionEvent(String messagePayload) {
        log.info("Kafka Consumer received event from topic '{}': {}", SecurityConstants.TOPIC_TRANSACTION_EVENTS, messagePayload);

        try {
            if (messagePayload.contains("senderNewBalance") || messagePayload.contains("receiverNewBalance")) {
                TransferCompletedEvent event = objectMapper.readValue(messagePayload, TransferCompletedEvent.class);
                handleTransferCompleted(event);
            } else if (messagePayload.contains("TransferRollbackEvent") || messagePayload.contains("reason")) {
                TransferRollbackEvent event = objectMapper.readValue(messagePayload, TransferRollbackEvent.class);
                handleTransferRollback(event);
            }
        } catch (Exception e) {
            log.error("Failed to parse Kafka message payload: {}", e.getMessage());
        }
    }

    public void handleTransferCompleted(TransferCompletedEvent event) {
        // 1. Notify Sender (Debit fluctuation)
        Notification senderNotification = Notification.builder()
                .id(UUID.randomUUID().toString())
                .customerId(event.getSenderCustomerId())
                .accountNumber(event.getFromAccountNumber())
                .type("BALANCE_DECREASE")
                .title("Biến động số dư: Trừ tiền")
                .message(String.format("Tài khoản %s vừa trừ -%,.0f %s. Nội dung: %s. Số dư mới: %,.0f %s. Mã GD: %s",
                        event.getFromAccountNumber(),
                        event.getAmount(),
                        event.getCurrency(),
                        event.getDescription(),
                        event.getSenderNewBalance() != null ? event.getSenderNewBalance().doubleValue() : 0.0,
                        event.getCurrency(),
                        event.getTransactionId()))
                .amount(event.getAmount().negate())
                .balanceAfter(event.getSenderNewBalance())
                .timestamp(LocalDateTime.now())
                .build();
        notificationService.processAndSendNotification(senderNotification);

        // 2. Notify Receiver (Credit fluctuation)
        Notification receiverNotification = Notification.builder()
                .id(UUID.randomUUID().toString())
                .customerId(event.getReceiverCustomerId())
                .accountNumber(event.getToAccountNumber())
                .type("BALANCE_INCREASE")
                .title("Biến động số dư: Nhận tiền")
                .message(String.format("Tài khoản %s vừa nhận +%,.0f %s từ tài khoản %s. Nội dung: %s. Số dư mới: %,.0f %s. Mã GD: %s",
                        event.getToAccountNumber(),
                        event.getAmount(),
                        event.getCurrency(),
                        event.getFromAccountNumber(),
                        event.getDescription(),
                        event.getReceiverNewBalance() != null ? event.getReceiverNewBalance().doubleValue() : 0.0,
                        event.getCurrency(),
                        event.getTransactionId()))
                .amount(event.getAmount())
                .balanceAfter(event.getReceiverNewBalance())
                .timestamp(LocalDateTime.now())
                .build();
        notificationService.processAndSendNotification(receiverNotification);
    }

    public void handleTransferRollback(TransferRollbackEvent event) {
        Notification rollbackNotification = Notification.builder()
                .id(UUID.randomUUID().toString())
                .customerId(event.getSenderCustomerId())
                .accountNumber(event.getFromAccountNumber())
                .type("ROLLBACK_REFUND")
                .title("Hoàn tiền giao dịch thất bại (Saga Rollback)")
                .message(String.format("Giao dịch %s thất bại (%s). Hệ thống đã hoàn tiền +%,.0f VND vào tài khoản %s của bạn.",
                        event.getTransactionId(),
                        event.getReason(),
                        event.getAmount(),
                        event.getFromAccountNumber()))
                .amount(event.getAmount())
                .timestamp(LocalDateTime.now())
                .build();
        notificationService.processAndSendNotification(rollbackNotification);
    }
}
