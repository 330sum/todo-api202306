package com.example.todo.todoapi.dto.request;

import lombok.*;

@Setter
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodoModifyRequestDTO {

    private String id;
    private boolean done; // 완료 취소선 긋는거

}
