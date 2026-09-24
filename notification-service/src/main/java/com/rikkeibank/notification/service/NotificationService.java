package com.rikkeibank.notification.service;

import com.rikkeibank.notification.model.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotificationService {

    // Reactive WebFlux multicast sink for pushing real-time Server-Sent Events (SSE)
    private final Sinks.Many<Notification> notificationSink = Sinks.many().multicast().onBackpressureBuffer();

    // In-memory persistent history storage for demo/runtime
    private final List<Notification> notificationHistory = new CopyOnWriteArrayList<>();

    public void processAndSendNotification(Notification notification) {
        notificationHistory.add(0, notification);
        log.info("Sending notification to Customer [{}]: {} - {}",
                notification.getCustomerId(), notification.getTitle(), notification.getMessage());

        Sinks.EmitResult result = notificationSink.tryEmitNext(notification);
        if (result.isFailure()) {
            log.warn("Failed to emit notification to reactive stream: {}", result);
        }
    }

    public Flux<Notification> getNotificationStream(Long customerId) {
        return notificationSink.asFlux()
                .filter(notification -> customerId == null || customerId.equals(notification.getCustomerId()));
    }

    public List<Notification> getNotificationsForCustomer(Long customerId) {
        return notificationHistory.stream()
                .filter(n -> customerId == null || customerId.equals(n.getCustomerId()))
                .collect(Collectors.toList());
    }

    public List<Notification> getAllNotifications() {
        return List.copyOf(notificationHistory);
    }
}
