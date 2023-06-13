package com.example.todo.config;

import com.example.todo.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.CorsFilter;

//@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtAuthFilter jwtAuthFilter; // 10. 내가 만든 토큰검사용 필터 셋팅

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors()
                .and()
                .csrf().disable()
                .httpBasic().disable()
                // ↓ 11. 세션인증을 사용하지 않겠다고 해주기 (우린 이제부터 토큰 사용할거임)
                .sessionManagement() // 세션관리
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 세션생성정책(무상태)
                .and() // 그리고
                // ↓ 12. 어떤 요청에서 인가를 할것인지, 안할 것인지 설정
                .authorizeRequests() // 인가요청들
                    .antMatchers("/", "/api/auth/**").permitAll() // 이 요청은 그냥 허용한다 (.denyAll()은 모두 거절한다)
                    //.antMatchers(HttpMethod.POST, "/api/todos").hasRole("ADMIN") // 이 요청은 ADMIN 역할만 허용
                .anyRequest().authenticated() // 그 외 나머지 요청들은 다 인증받아라
        ;

        // 13. 필터 연결하기 ( jwtAuthFilter 우리가 만든 토큰 인증필터는 CorsFilter와 LogoutFilter 사이에 배치
        http.addFilterAfter(
                jwtAuthFilter
                , CorsFilter.class // CorsFilter뒤에 배치하겠다 (import주의 : 스프링꺼!!))
        );
        // 이렇게 하고, 톰켓돌리면 필터작동함. 인증,인가만 안하는거지. 필터를 추가해서 그냥 필터는 돌아감
        // 44번라인에 있는 경로로 들어가면 403 인증에러가 웹브라우저에 뜸

        return http.build();
    }

}
