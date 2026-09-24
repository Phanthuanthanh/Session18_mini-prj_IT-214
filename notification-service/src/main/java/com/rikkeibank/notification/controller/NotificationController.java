package com.rikkeibank.notification.controller;

import com.rikkeibank.common.dto.ApiResponse;
import com.rikkeibank.common.event.TransferCompletedEvent;
import com.rikkeibank.common.event.TransferRollbackEvent;
import com.rikkeibank.notification.consumer.TransactionKafkaConsumer;
import com.rikkeibank.notification.model.Notification;
import com.rikkeibank.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final TransactionKafkaConsumer kafkaConsumer;

    // WebFlux Reactive Server-Sent Events (SSE) stream for Realtime Balance Notifications
    @GetMapping(value = "/stream/{customerId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Notification>> streamNotifications(@PathVariable Long customerId) {
        return notificationService.getNotificationStream(customerId)
                .map(notification -> ServerSentEvent.<Notification>builder()
                        .id(notification.getId())
                        .event("balance-update")
                        .data(notification)
                        .build())
                .mergeWith(Flux.interval(Duration.ofSeconds(15))
                        .map(seq -> ServerSentEvent.<Notification>builder()
                                .event("heartbeat")
                                .comment("keep-alive")
                                .build()));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<Notification>>> getCustomerNotifications(@PathVariable Long customerId) {
        List<Notification> list = notificationService.getNotificationsForCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getAllNotifications() {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getAllNotifications()));
    }

    // Direct event ingestion endpoint (supports test/demo without live Kafka broker)
    @PostMapping("/mock/completed-event")
    public ResponseEntity<ApiResponse<String>> triggerCompletedEvent(@RequestBody TransferCompletedEvent event) {
        kafkaConsumer.handleTransferCompleted(event);
        return ResponseEntity.ok(ApiResponse.success("Đã kích hoạt xử lý sự kiện TransferCompletedEvent thành công", null));
    }

    @PostMapping("/mock/rollback-event")
    public ResponseEntity<ApiResponse<String>> triggerRollbackEvent(@RequestBody TransferRollbackEvent event) {
        kafkaConsumer.handleTransferRollback(event);
        return ResponseEntity.ok(ApiResponse.success("Đã kích hoạt xử lý sự kiện TransferRollbackEvent thành công", null));
    }
}
