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

    private final JwtAuthFilter jwtAuthFilter; // 이 필터(내가만든것)를 언제 실행할 것인지에 대한 셋팅

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
                // ↓ 세션인증을 사용하지 않겠다고 해주기
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // ↓ 어떤 요청에서 인가를 할것인지, 안할 것인지 설정
                .authorizeRequests()
                    .antMatchers("/", "/api/auth/**").permitAll() // 이런요청은 그냥 허용한다
                    //.antMatchers(HttpMethod.POST, "/api/todos").hasAnyRole("ADMIN")
                .anyRequest().authenticated() // 그거말고 다 인증받아라
        ;

        // 토큰인증 필터 연결 ( jwtAuthFilter 우리가 만든 인증필터는 CorsFilter와 LogoutFilter 사이에 배치
        http.addFilterAfter(
                jwtAuthFilter
                , CorsFilter.class // CorsFilter뒤에 배치하겠다 (impor주의 : 스프링꺼!!))

        );


        return http.build();
    }

}