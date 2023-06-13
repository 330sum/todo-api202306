package com.example.todo.todoapi.dto.request;

import com.example.todo.todoapi.entity.Todo;
import com.example.todo.userapi.entity.User;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Setter @Getter
@ToString @EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodoCreateRequestDTO {

    @NotBlank
    @Size(min = 2, max = 10)
    private String title;

    // dto를 엔터티로 변환
    public Todo toEntity() {
        return Todo.builder()
                .title(this.title)
                .build();
    }

    // 오버로딩으로 만듦 (테스트 다 망가지는 경우떄문에) 안정화되면 위에꺼 삭제
    public Todo toEntity(User user) {
        return Todo.builder()
                .title(this.title)
                .user(user) // 유저id 넣는게 아니라, user 객체
                .build();
    }
}
