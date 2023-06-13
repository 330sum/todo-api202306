package com.example.todo.auth;

import com.example.todo.userapi.entity.Role;
import com.example.todo.userapi.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

// 역할: 토큰을 발급하고, 서명 위조를 검사하는 객체
@Service
@Slf4j
public class TokenProvider {

    // 토큰을 발급하려면 서명이 필요 -> 서명에 사용할 값이 필요 (512비트(64바이트) 이상의 랜덤 문자열이 필요함 -> 해킹위험예방)
    @Value("${jwt.secret}")
    private String SECRET_KEY; // 서명은 노출되면 안되니까 yml에 설정


    // 토큰 생성 (생김새ㅋㅋㅋㅋ) - 안에 내용들을 클레임이라고 부름
        /*
               {
                    "iss": "딸긔공듀",
                    "exp": "2023-07-12",
                    "iat: "2023-06-12",
                    "email": "로그인한사람이메일",
                    "role": "Premium"

                    == 서명
               }
         */


    /**
     * Json Web Token을 생성하는 메서드
     *
     * @param userEntity - 토큰의 내용(클레임)에 포함될 유저정보
     * @return - 생성된 jwt의 json을 암호화한 토큰값
     */
    // 토큰 생성 메서드
    public String createToken(User userEntity) {

        // 토큰 만료시간 생성 (LocalDateTime을 지원하지 않고 Date만 지원함. 그래서 만듦, 일단 하루로 설정)
        Date expiry = Date.from(
                Instant.now().plus(1, ChronoUnit.DAYS)
        );


        // 추가 클레임 정의
        HashMap<String, Object> claims = new HashMap<>();
        claims.put("email", userEntity.getEmail());
        claims.put("role", userEntity.getRole().toString()); // enum으로 넣을 때는 String으로 넣기! (안그럼 에러남)

        // 토큰 생성
        return Jwts.builder()
                // token header에 들어갈 서명
                .signWith(
                        // SECRET_KEY를 암호화
                        Keys.hmacShaKeyFor(SECRET_KEY.getBytes())
                        , SignatureAlgorithm.HS512
                )
                // token payload(body같음)에 들어갈 '클레임' 설정 (자주쓰는 것은 인텔리제이에 있음)
                .setClaims(claims) // 커스텀으로 더 넣고 싶은거 (추가클레임) * 주의사항 * 추가클레임은 먼저 설정해야함
                .setIssuer("바닐라겅듀") // iss: 발급자 정보 (회사이름, 서비스이름)
                .setIssuedAt(new Date()) // iat: 토큰 발급시간
                .setExpiration(expiry) // exp: 토큰만료시간
                .setSubject(userEntity.getId()) // sub: 토큰을 식별할 수 있는 주요데이터
                .compact();
    }

    /**
     * 클라이언트가 전송한 토큰을 디코딩(암호화 해제)하여 토큰의 위조여부를 확인
     * 토큰을 json으로 파싱해서 클레임(토큰정보)를 리턴
     * @param token
     * @return - 토큰 안에 있는 인증된 유저정보를 반환
     */
    public TokenUserInfo validatedAndGetTokenUserInfo(String token) {

        // 디코딩(암호화 해제)
        Claims claims = Jwts.parserBuilder()
                // 토큰 발급자의 발급 당시의 서명을 넣어줌
                .setSigningKey(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()))
                .build()
                // 여기까지가 서명위조 검사작업 임 (위조된 경우, 예외가 발생함)
                // 위조가 되지 않은 경우, 페이로드(클레임)을 리턴 -> 페이로드의 클레임을 파싱함 -> 클레임 리턴
                .parseClaimsJws(token)
                .getBody();

        log.info("claims: {}", claims);

        return TokenUserInfo.builder()
                .userId(claims.getSubject()) // id는 Subject안에 넣었음
                .email(claims.get("email", String.class)) //map으로 들어갔으니까, String클래스로 꺼내야함
                .role(Role.valueOf(claims.get("role", String.class))) //map으로 들어갔으니까, String클래스로 꺼내야함 -> 근데 enum이니까 한번 더 변환 필요
                .build();
    }


}
