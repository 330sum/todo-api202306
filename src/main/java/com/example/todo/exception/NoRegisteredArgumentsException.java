package com.example.todo.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class NoRegisteredArgumentsException extends RuntimeException {

    // 기본생성자 + 에러메시지를 받는 생성자
    // @NoArgsConstructor옆에서 alt+insert
    public NoRegisteredArgumentsException(String message) {
        super(message);
    }


}
