package org.example.ais_sst.event;

import lombok.Getter;
import org.example.ais_sst.entity.User;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserAccountCreatingRequestEvent extends ApplicationEvent {

    private final User user;

    public UserAccountCreatingRequestEvent(Object source, User user) {
        super(source);
        this.user = user;
    }
}
