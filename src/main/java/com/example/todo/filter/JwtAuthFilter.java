package com.example.todo.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// 필터(인증) > 인터셉터(인가)
// 클라이언트가 전송한 토큰을 검사하는 필터
@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter { // 요청마다 한번씩 필터

    // 필터가 해야할 작업을 기술
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, // 세션, 요청헤더(토큰), 리퀘스트바디(json파싱도 가능) ...
            HttpServletResponse response, // 응답정보생성(쿠키, 리다이렉트 등)
            FilterChain filterChain) throws ServletException, IOException {

        String token = parseBearerToken(request);
        log.info("파싱한 토큰!!!! Jwt Token Filter is running... - token: {},", token);


    }

    private String parseBearerToken(HttpServletRequest request) {
        // 요청 헤더에서 토큰 가져오기 (토큰 검사하려면 헤더뒤져야함)
        // http request header 안에 담을 수 있는 것
        // -- Content-type : application/json
        // -- Authorization: Bearer ajsgjja/3g#@%kasd(토큰)

        String bearerToken = request.getHeader("Authorization");
        // 요청 헤더에 가져온 토큰은 순수토큰이 아니라 Bearer 붙어 있어서 짤라줘야함.
        if (StringUtils.hasText(bearerToken)&& bearerToken.startsWith("Bearer")) {
            return bearerToken.substring(7); //Bearer+공백1칸 = 7개 잘라줘야함
        }
        return null;
    }
}
