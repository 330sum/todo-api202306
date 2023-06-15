package com.example.todo.userapi.api;

import com.example.todo.auth.TokenUserInfo;
import com.example.todo.exception.DuplcatedEmailException;
import com.example.todo.exception.NoRegisteredArgumentsException;
import com.example.todo.userapi.dto.request.LoginRequestDTO;
import com.example.todo.userapi.dto.request.UserRequestSignUpDTO;
import com.example.todo.userapi.dto.response.LoginResponseDTO;
import com.example.todo.userapi.dto.response.UserSignUpResponseDTO;
import com.example.todo.userapi.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
        if (email.trim().equals("")) {
            return ResponseEntity.badRequest()
                    .body("이메일이 없습니다!!!!!!!!");
        }
        boolean resultFlag = userService.isDuplicate(email);
        log.info("{} 중복인강?????? -{}", email, resultFlag);

        return ResponseEntity.ok().body(resultFlag);
    }


    // 회원가입 요청처리
    // POST: /api/auth
    @PostMapping()
    public ResponseEntity<?> signUp(
            @Validated @RequestPart("user") UserRequestSignUpDTO dto
            , @RequestPart(value = "profileImage", required = false) MultipartFile profileImg
            , BindingResult result) {
        log.info("/api/auth POST! - {}", dto);
        if (result.hasErrors()) {
            log.warn(result.toString());
            return ResponseEntity.badRequest()
                    .body(result.getFieldError());
        }

        try {
            String uploadedFilePath = null;

            if (profileImg != null) {
                log.info("attached file name: {}", profileImg.getOriginalFilename());

                uploadedFilePath = userService.uploadProfileImage(profileImg);

            }
            UserSignUpResponseDTO responseDTO = userService.create(dto, uploadedFilePath);
            return ResponseEntity.ok().body(responseDTO);
//            return null;
        } catch (NoRegisteredArgumentsException e) {
            log.warn("필수 가입정보를 전달받지 못했습니다!!!!!!!!!!!!!!!");
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (DuplcatedEmailException e) {
            log.warn("이메일 중복입니다!!!!!!!!!!!!!!!");
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.warn("기타 예외(파일업로드에러)가 발생했습니다! ㅠㅠ");
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }

        // 415에러는 전송데이터 타입이 안맞아서 그런거임! (Network -> Fetch/XHR 보기)


    }

    // 로그인 요청 처리
    @PostMapping("/signin")
    public ResponseEntity<?> signIn(@Validated @RequestBody LoginRequestDTO dto) {

        try {
            LoginResponseDTO responseDTO = userService.authenticate(dto);
            return ResponseEntity.ok().body(responseDTO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }

    }


    // 일반 회원을 프리미엄회원으로 승격하는 요청 처리
    @PutMapping("/promote")
    // 권한검사 필요한곳에 @PreAuthorize - 해당권한이 아니면 인가처리 거부함 403리터함
    @PreAuthorize("hasRole('ROLE_COMMON')") // 커먼이 아닌 사람이 보내면 403보냄. 그래서 서비스에서 따로 예외처리 안만들어도 됨
    public ResponseEntity<?> promote(@AuthenticationPrincipal TokenUserInfo userInfo) {
        log.info("/api/auth/promote PUT!");

        try {
            LoginResponseDTO responseDTO = userService.promoteToPremium(userInfo);
            // 서비스가 할 일 1. 인증처리필요 (->TokenUserInfo 넘겨주기), 2. 프리미엄이 또 프리미엄해달라고 하면 안됨. (커먼인지 확인하기)
            // 권한바꾸기 (localStorage변경 -> jwt 토큰 재생성해서 다시 줘야함 -> 토큰들어있는 DTO로 리턴받아야함)
            return ResponseEntity.ok().body(responseDTO);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            log.warn(e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }


}
