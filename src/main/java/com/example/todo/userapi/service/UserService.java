package com.example.todo.userapi.service;

import com.example.todo.auth.TokenProvider;
import com.example.todo.exception.DuplcatedEmailException;
import com.example.todo.exception.NoRegisteredArgumentsException;
import com.example.todo.userapi.dto.LoginRequestDTO;
import com.example.todo.userapi.dto.request.UserRequestSignUpDTO;
import com.example.todo.userapi.dto.response.UserSignUpResponseDTO;
import com.example.todo.userapi.entity.User;
import com.example.todo.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final TokenProvider tokenProvider; // ocp원칙과 dip원칙 지킨 것

    // 회원가입 처리
    public UserSignUpResponseDTO create(final UserRequestSignUpDTO dto) throws RuntimeException {

        String email = dto.getEmail();

        if (dto == null || email.equals("")) {
            throw new NoRegisteredArgumentsException("가입정보가 없습니다.");
            // throw 에러를 발생시키는 것
            // throws 에러를 던지는 것
        }

        // 이건 이메일 최종검증
        if (userRepository.existsByEmail(dto.getEmail())) {
            // if (isDuplicate(email)) {
            log.warn("이메일이 중복되었습니다 - {} ", email);
            throw new DuplcatedEmailException("중복된 이메일입니다");
        }

        // 패스워드 인코딩
        String encoded = encoder.encode(dto.getPassword());
        dto.setPassword(encoded);

        // 유저 엔터티로 변환
        User user = dto.toEntity();

        User saved = userRepository.save(user);

        log.info("회원가입 정상 수행됨 - saved user - {}", saved);

        return new UserSignUpResponseDTO(saved);

    }

    // 이건 이메일실시간 검증!
    public boolean isDuplicate(String email) {

        return userRepository.existsByEmail(email);
    }


    // 회원 인증
    public void authenticate(final LoginRequestDTO dto) {

        // 이메일을 통해서 회원 정보를 조회
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(
                        () -> new RuntimeException("가입된 회원이 아닙니다!")
                );
        // if (user != null) throw new RuntimeException 이랑 같은거!


        // 패스워드 검증
        String rawPassword = dto.getPassword(); // 입력 비번 (클라이언트가 보낸 것)
        String encodedPassword = user.getPassword(); // DB에 저장된 비번
        if (!encoder.matches(rawPassword, encodedPassword)) {
            throw new RuntimeException("비밀번호가 틀렸습니다.");
        }

        log.info("{}님 로그인 성공! ", user.getUserName());


        // 로그인 성공 후 클라이언트에게 뭘 리턴할 것인가?
        // -> JWT(Json Web Token)을 클라이언트에게 발급해줘야함
        // 여기서 토큰발급 코드를 쓰면 SRP위반, 객체지향적이지 않기 떄문에, 토큰발급객체 따로 생성(auth 패키지)



    }


}