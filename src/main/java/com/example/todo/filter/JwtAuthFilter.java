package com.example.todo.filter;

import com.example.todo.auth.TokenProvider;
import com.example.todo.auth.TokenUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// 필터(인증) > 인터셉터(인가)
// 클라이언트가 전송한 토큰을 검사하는 필터
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter { // 요청마다 한번씩 필터

    private final TokenProvider tokenProvider;

    // 필터가 해야할 작업을 기술
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, // 세션, 요청헤더(토큰), 리퀘스트바디(json파싱도 가능) ...
            HttpServletResponse response, // 응답정보생성(쿠키, 리다이렉트 등)
            FilterChain filterChain) throws ServletException, IOException {

        try {

            String token = parseBearerToken(request);
            log.info("파싱한 토큰!!!! Jwt Token Filter is running... - token: {},", token);

            // 토큰 위조검사 및 인증 완료 처리
            if (token != null) {

                // 토큰 서명위조 검사와 토큰을 파싱해서 클레임을 얻어내는 작업 -> tokenProvider에서 작업하기
                TokenUserInfo userInfo = tokenProvider.validatedAndGetTokenUserInfo(token);

                // 인가 정보 리스트
                List<SimpleGrantedAuthority> authorityList = new ArrayList<>();
                authorityList.add(new SimpleGrantedAuthority(userInfo.getRole().toString()));


                // 인증 완료처리 -> [[[[[[[[[[  객체정보 생성한것!  ]]]]]]]]]]]
                // - 스프링 시큐리티에게 인증정보를 전달해서 전역적으로 앱에서 인증정보를 활용할 수 있게 설정
                // (클래스 AbstractAuthenticationToken) 추상화된 인증 토큰
                AbstractAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userInfo, // 1번째 param은 컨트롤러에서 활용할 유저 정보
                        null, // 2번째 param은 null로 고정 (인증된 사용자의 비밀번호 - 보통 null 값 설정 (위험하기 떄문에 토큰 안에 안넣음)
                        authorityList // 3번째 param은 옵션: 인가정보(권한정보)
                );

                // 인증 완료 처리시 클라이언트의 요청 정보 셋팅
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 스프링 시큐리티 컨테이너에 인증정보 객체 등록 ->  [[[[[[[[[[  객체정보 등록하는 것!  ]]]]]]]]]]]
                SecurityContextHolder.getContext().setAuthentication(auth);

            }

        } catch (Exception e) {
            e.printStackTrace();
            log.error("토큰 위조됨!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }


        // 필터 체인에 내가 만든 필터도 끼워넣기! (내가 만든 필터 실행 명령)
        filterChain.doFilter(request, response);

        // 이 만든 필터를 언제 실행할지, 말지 정해야함  -> config -> WebSecurityConfig로 가기!

    }

    private String parseBearerToken(HttpServletRequest request) {
        // 요청 헤더에서 토큰 가져오기 (토큰 검사하려면 헤더뒤져야함)
        // http request header 안에 담을 수 있는 것
        // -- Content-type : application/json
        // -- Authorization: Bearer ajsgjja/3g#@%kasd(토큰)

        String bearerToken = request.getHeader("Authorization");
        // 요청 헤더에 가져온 토큰은 순수토큰이 아니라 Bearer 붙어 있어서 짤라줘야함.
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer")) {
            return bearerToken.substring(7); //Bearer+공백1칸 = 7개 잘라줘야함
        }
        return null;
    }
}
