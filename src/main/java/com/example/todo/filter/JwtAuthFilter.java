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
public class JwtAuthFilter extends OncePerRequestFilter { // OncePerRequestFilter 모든 요청마다 한번씩 검사하는 필터

    private final TokenProvider tokenProvider;

    // 필터가 해야할 작업을 기술
    // 필터 설정 (1~8), 필터체인에 내 필터 연결하기(9~13)
    @Override
    protected void doFilterInternal( // 필터의 매개변수는 세개가 있음! (요청정보, 응답정보, 필터체인)
            HttpServletRequest request, // 요청방식(GET, POST 등), 요청 URI, 요청 클라이언트 IP, 물음표 파라미터(request.getParameter), 세션, 요청헤더파싱(토큰), 요청바디에 들어있는 json파싱도 가능
            HttpServletResponse response, // 응답정보생성(쿠키 실어보내기, 리다이렉트 응답 등)
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // 1.
            String token = parseBearerToken(request);
            log.info("파싱한 토큰(Bearer 뗀 토큰)!!!! Jwt Token Filter is running... - token: {},", token);

            // 2. 토큰 위조검사 및 인증 완료 처리
            if (token != null) {

                // 3. 토큰 서명위조 검사와 토큰을 파싱해서 클레임(유저정보)을 얻어내는 작업 -> tokenProvider에서 작업하기 (SECRET_KEY 가지고 있어서 -> 객체지향) -> tokenProvider에게 의존하기
                TokenUserInfo userInfo = tokenProvider.validatedAndGetTokenUserInfo(token);

                // 5. 인가 정보 리스트
                List<SimpleGrantedAuthority> authorityList = new ArrayList<>();
                authorityList.add(new SimpleGrantedAuthority(userInfo.getRole().toString()));


                // 4. 인증 완료처리 [인증정보 설정 생성]
                // - 스프링 시큐리티에게 인증정보를 전달해서 전역적으로 앱에서 인증정보를 활용할 수 있게 설정
                // (추상클래스 AbstractAuthenticationToken : 추상화된 인증 토큰)
                AbstractAuthenticationToken auth = new UsernamePasswordAuthenticationToken( // 매개변수 (인증정보, 널가능, 인가정보)
                        userInfo, // 컨트롤러에서 활용할 유저 정보
                        null, // null로 고정 (인증된 사용자의 비밀번호 - 보통 null 값 설정 (위험하기 때문에, 토큰 안에 안넣음)
                        authorityList // 이건 옵션임: 인가정보(권한정보) - 실버, 골드같이 등급 안나누면 안 써도 됨 -> 근데, 들어갈 때 list형식으로 들어감!(왜? 대규모 서비스에서는 권한이 여러개인 경우가 많기때문 (예: 관리자회원이면서 vip인 경우, 멜론 스트리밍 회원이면서 다운로드 회원) -> 그래서 5.인가정보 리스트를 만듦
                );

                // 6. 인증 완료 처리시 클라이언트의 요청 정보 셋팅 (즉, 4.인증정보설정 생성 시, 사용자의 디테일한 것도 설정해둔다는 의미 (예: 의심되는 사용자(해커) ip 등록))
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 7. 스프링 시큐리티 컨테이너에 인증정보 객체 등록 [인증정보 등록] ->  (4.인증완료처리 된 객체를 등록하는 것)
                SecurityContextHolder.getContext().setAuthentication(auth); // 4번의 auth를 넣어주면 등록 됨

            }
            
            // 8. tokenProvider에서 위조 검사시, 토큰 위조된거 발견하는 경우 익셉션터짐. 트롸이캐치로 잡기
        } catch (Exception e) {
            e.printStackTrace();
            log.error("토큰이 위조되었습니다. - tokenProvider에서 위조 검사하는데, 토큰이 위조된 걸 발견!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }


        // 9. 필터 체인에 내가 만든 필터도 끼워넣기! (내가 만든 필터 실행 명령)
        filterChain.doFilter(request, response);

        // 필터가 이미 여러개 존재함. 그 필터들은 순서대로 진행되는데, 내가 만든 이 필터를 언제 실행할지말지를 정해야함 -> config -> 10. WebSecurityConfig로 가기!
        // (예: 지금 만든 커스텀 필터는 토큰검사용 필터임. 이건 로그인시도, 회원가입 할때는 필요없음. 그니까 실행 안해도 됨) 

    }

    
    
    
    private String parseBearerToken(HttpServletRequest request) {
        // 클라이언트의 요청 헤더에서 토큰 가져오기 (토큰 검사하려면 헤더뒤져야함)
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
