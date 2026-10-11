package com.burevski.msaccountreservation.exception;

public class ClientAlreadyExistsException extends RuntimeException {

    public ClientAlreadyExistsException(Throwable cause) {
        super("Клиент с таким mdmId уже существует", cause);
    }
}
