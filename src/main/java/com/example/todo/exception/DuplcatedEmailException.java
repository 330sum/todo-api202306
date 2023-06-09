package com.example.todo.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class DuplcatedEmailException extends RuntimeException{

    public DuplcatedEmailException(String message) {
        super(message);
    }
}
