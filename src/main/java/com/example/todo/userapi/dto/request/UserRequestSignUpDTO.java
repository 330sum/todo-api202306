package com.example.todo.userapi.dto.request;

import com.example.todo.userapi.entity.User;
import lombok.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "email")
@Builder
public class UserRequestSignUpDTO {

    @NotBlank
    @Email // 패턴검증
    private String email;

    @NotBlank
    @Size(min = 8, max = 20)
    private String password;

    @NotBlank
    @Size(min = 2, max = 5)
    private String userName;

    // 엔터티로 변경하는 메서드 (세터 사용하지 말고 빌더로 사용하기! -> 왜? enum이 builder로 기본값들어가기 때문)
    public User toEntity() {
        return User.builder()
                .email(this.email)
                .password(this.password)
                .userName(this.userName)
                .build();
    }
}
