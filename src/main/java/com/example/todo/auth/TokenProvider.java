package com.example.todo.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

// 역할: 토큰을 발급하고, 서명 위조를 검사하는 객체
@Service
@Slf4j
public class TokenProvider {

    // 토큰을 발급하려면 서명이 필요 -> 서명에 사용할 값이 필요 (512바이트 이상의 랜덤 문자열이 필요함 -> 해킹위험예방)
    private String SECRET_KEY;

}
