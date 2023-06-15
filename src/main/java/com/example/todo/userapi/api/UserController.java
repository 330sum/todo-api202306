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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

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


    // 프로필 사진 이미지 데이터를 클라이언트에게 응답처리
    @GetMapping("/load-profile")
    public ResponseEntity<?> loadFile(
            @AuthenticationPrincipal TokenUserInfo userInfo) {
        log.info("/api/auth/load-profile GET! - user {}", userInfo.getEmail());

        try {
            // 클라이언트가 요청한 프로필 사진 응답해야함
            // 1. 프로필 사진의 경로를 얻어야 함 (디비에서 찾기)
            String filePath = userService.getProfilePath(userInfo.getUserId());

            // 2. 얻어낸 파일 경로를 통해서 실제파일 가져오기 (파일데이터 로드하기)
            // 파일객체로 포장하기
            File profileFile = new File(filePath);

            if (!profileFile.exists()) {
                return ResponseEntity.notFound().build();
            }

            // 해당 경로에 저장된 파일를 바이트배열로 직렬화해서 리턴해주는 기능 (여러개면 모아서 리스트로 줘야함...! 아니면 이런 요청을 여러번 보내거나..)
            byte[] fileData = FileCopyUtils.copyToByteArray(profileFile);

            // 3. 응답헤서에 컨텐츠 타입을 성정
            HttpHeaders headers = new HttpHeaders();
            MediaType contentType = findExtensionAndGetMediaType(filePath);

            if (contentType == null) {
                return ResponseEntity.internalServerError()
                        .body("발견된 파일은 이미지가 아님!!!!!!");
            }

            headers.setContentType(contentType);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileData);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("파일을 찾을 수 없슘둥..");

        }
    }

    private MediaType findExtensionAndGetMediaType(String filePath) {

        // 파일경로에서 확장자 추출하기
        String ext = filePath.substring(filePath.lastIndexOf(".") + 1);
        switch (ext.toUpperCase()) {
            case "JPEG":
            case "JPG":
                return MediaType.IMAGE_JPEG;
            case "PNG":
                return MediaType.IMAGE_PNG;
            case "GIF":
                return MediaType.IMAGE_GIF;
            default:
                return null;

        }

    }


}
