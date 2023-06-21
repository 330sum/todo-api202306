package com.example.todo.userapi.service;

import com.example.todo.auth.TokenProvider;
import com.example.todo.auth.TokenUserInfo;
import com.example.todo.aws.S3Service;
import com.example.todo.exception.DuplcatedEmailException;
import com.example.todo.exception.NoRegisteredArgumentsException;
import com.example.todo.userapi.dto.request.LoginRequestDTO;
import com.example.todo.userapi.dto.request.UserRequestSignUpDTO;
import com.example.todo.userapi.dto.response.LoginResponseDTO;
import com.example.todo.userapi.dto.response.UserSignUpResponseDTO;
import com.example.todo.userapi.entity.Role;
import com.example.todo.userapi.entity.User;
import com.example.todo.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final TokenProvider tokenProvider; // ocp원칙과 dip원칙 지킨 것
    private final S3Service s3Service;

//    @Value("${upload.path}")
//    private String uploadRootPath;

    // 회원가입 처리
    public UserSignUpResponseDTO create(
            final UserRequestSignUpDTO dto
            , String uploadedFilePath)
            throws RuntimeException {

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
        User user = dto.toEntity(uploadedFilePath);

        User saved = userRepository.save(user);

        log.info("회원가입 정상 수행됨 - saved user - {}", saved);

        return new UserSignUpResponseDTO(saved);

    }

    // 이건 이메일실시간 검증!
    public boolean isDuplicate(String email) {

        return userRepository.existsByEmail(email);
    }


    // 회원 인증
    public LoginResponseDTO authenticate(final LoginRequestDTO dto) {

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
        String token = tokenProvider.createToken(user);
        // 이미 암호화 되어있기 때문에 바로 클라이언트에게 줘도 됨
        return new LoginResponseDTO(user, token);


    }

    // 프리미엄으로 등업
    public LoginResponseDTO promoteToPremium(TokenUserInfo userInfo) throws NoRegisteredArgumentsException, IllegalStateException {

        // 수정하는건 일단 예외처리부터 해야함
        // JPA 수정 : 조회 -> 세터 -> 저장
        User foundUser = userRepository.findById(userInfo.getUserId())
                .orElseThrow(() -> new NoRegisteredArgumentsException("회원조회에 실패!"));

        // 일반회원이 아니면 예외 -> 이거는 씹힘 (controller에서 @PreAuthorize 여기에서 403으로 쳐냄)
        if (userInfo.getRole() != Role.COMMON) {
            throw new IllegalStateException("일반회원이 아니면 등급을 상승시킬 수 없습니당!");
        }

        // 등급 변경
        // *실무측면에서는 세터쓰면 안됨 -> 메서드 만들기!
        //foundUser.setRole(); ㄴㄴ
        foundUser.changeRole(Role.PREMIUM);
        User saved = userRepository.save(foundUser);

        // 변경된 권한에 맞는 토큰을 재발급
        String token = tokenProvider.createToken(saved);

        return new LoginResponseDTO(saved, token);
    }

    // 파일 저장처리 메서드

    /**
     * 업로드된 파일을 서버에 저장하고 저장경로를 리턴
     *
     * @param originalFile - 업로드된 파일의 정보
     * @return 실제로 저장된 이미지의 경로
     */
    public String uploadProfileImage(MultipartFile originalFile) throws IOException {

        // 루트 디렉토리가 존재하는지 확인 후 존재하지 않으면 생성
//        File rootDir = new File(uploadRootPath);
//        if (!rootDir.exists()) rootDir.mkdir();

        // 파일명을 유니크하게 변경
        String uniqueFileName = UUID.randomUUID() + "_" + originalFile.getOriginalFilename();

        // 파일을 저장
//        File uploadFile = new File(uploadRootPath + "/" + uniqueFileName);
//        originalFile.transferTo(uploadFile);

        // 파일을 s3 버킷에 저장
        String uploadUrl = s3Service.uploadToS3Bucket(originalFile.getBytes(), uniqueFileName);

        return uploadUrl;
    }

    public String getProfilePath(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow();
//        return uploadRootPath + "/" + user.getProfileImg();
            return user.getProfileImg();

    }

    public String getProfilePath(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow();
        return uploadRootPath + "/" + user.getProfileImg();

    }
}
