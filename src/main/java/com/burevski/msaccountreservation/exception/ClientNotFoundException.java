package com.burevski.msaccountreservation.exception;

import java.util.UUID;

public class ClientNotFoundException extends RuntimeException {
    public ClientNotFoundException(UUID clientId) {
        super("Клиент с ID " + clientId + " не найден");
    }
}
