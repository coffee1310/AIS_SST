//package org.example.ais_sst.event;
//
//import lombok.Getter;
//import org.example.ais_sst.event.enums.NotificationType;
//import org.springframework.context.ApplicationEvent;
//
//import java.time.LocalDateTime;
//
//@Getter
//public abstract class NotificationEvent<T> extends ApplicationEvent {
//
//    private final T payload;
//    private final String eventType;
//    private final NotificationType notificationType;
//    private final LocalDateTime timestamp;
//
//    public NotificationEvent(Object source, T payload, String eventType, NotificationType notificationType) {
//        super(source);
//        this.payload = payload;
//        this.eventType = eventType;
//        this.notificationType = notificationType;
//        this.timestamp = LocalDateTime.now();
//    }
//}
