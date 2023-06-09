package com.example.todo.userapi.service;

import com.example.todo.exception.DuplcatedEmailException;
import com.example.todo.exception.NoRegisteredArgumentsException;
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

    // 회원가입 처리
    public UserSignUpResponseDTO create(final UserRequestSignUpDTO dto) throws RuntimeException {

        String email = dto.getEmail();

        if(dto == null || email.equals("")) {
            throw new NoRegisteredArgumentsException("가입정보가 없습니다.");
            // throw 에러를 발생시키는 것
            // throws 에러를 던지는 것
        }

        // 이건 이메일 최종검증
        if(userRepository.existsByEmail(dto.getEmail())) {
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
}
