package com.example.todo.userapi.api;

import com.example.todo.exception.DuplcatedEmailException;
import com.example.todo.exception.NoRegisteredArgumentsException;
import com.example.todo.userapi.dto.request.UserRequestSignUpDTO;
import com.example.todo.userapi.dto.response.UserSignUpResponseDTO;
import com.example.todo.userapi.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;


    // 이메일 중복확인 요청처리
    // GET: /api/auth/check?email=zzzz@xxxx.com
    @GetMapping("/check")
    public ResponseEntity<?> check(String email) {
        if(email.trim().equals("")) {
            return ResponseEntity.badRequest()
                    .body("이메일이 없습니다!!!!!!!!");
        }
        boolean resultFlag = userService.isDuplicate(email);
        log.info("{} 중복인강?????? -{}", email, resultFlag);

        return ResponseEntity.ok().body(resultFlag);
    }


    // 회원가입 요청처리
    // POST: /api/auth
    @PostMapping
    public ResponseEntity<?> signUp(@Validated @RequestBody UserRequestSignUpDTO dto, BindingResult result) {
        log.info("/api/auth POST! - {}", dto);

        if(result.hasErrors()) {
            log.warn(result.toString());
            return ResponseEntity.badRequest()
                    .body(result.getFieldError());
        }

        try {
            UserSignUpResponseDTO responseDTO = userService.create(dto);
            return ResponseEntity.ok().body(responseDTO);
        } catch (NoRegisteredArgumentsException e) {
            log.warn("필수 가입정보를 전달받지 못했습니다!!!!!!!!!!!!!!!");
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (DuplcatedEmailException e) {
            log.warn("이메일 중복입니다!!!!!!!!!!!!!!!");
            return  ResponseEntity.badRequest().body(e.getMessage());
        }

    }



}
