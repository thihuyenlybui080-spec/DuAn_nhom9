package org.example.loginregister.server.common.exception;

public class DuplicateUsernameException extends Exception {
    public DuplicateUsernameException(String message){
        super(message);
    }
}
