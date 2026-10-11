package com.burevski.msaccountreservation.exception;

import java.util.UUID;

public class ClientHasActiveAccountsException extends RuntimeException {
    public ClientHasActiveAccountsException(UUID clientId) {
        super("У клиента с ID " + clientId
                + " есть активные счета или счета в процессе создания");
    }
}
