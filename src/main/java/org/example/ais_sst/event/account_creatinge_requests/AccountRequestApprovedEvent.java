package org.example.ais_sst.event.account_creatinge_requests;

import lombok.Getter;
import org.example.ais_sst.entity.AccountCreatingRequest;
import org.example.ais_sst.entity.User;
import org.springframework.context.ApplicationEvent;

/**
 * Событие, которое публикуется при одобрении заявки на создание аккаунта.
 * Используется для отправки уведомления создателю заявки.
 */
@Getter
public class AccountRequestApprovedEvent extends ApplicationEvent {

    private final AccountCreatingRequest request;
    private final User createdUser;

    public AccountRequestApprovedEvent(Object source, AccountCreatingRequest request, User createdUser) {
        super(source);
        this.request = request;
        this.createdUser = createdUser;
    }
}
