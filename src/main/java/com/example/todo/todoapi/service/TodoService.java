package com.example.todo.todoapi.service;

import com.example.todo.auth.TokenUserInfo;
import com.example.todo.todoapi.dto.request.TodoCreateRequestDTO;
import com.example.todo.todoapi.dto.request.TodoModifyRequestDTO;
import com.example.todo.todoapi.dto.response.TodoDetailResponseDTO;
import com.example.todo.todoapi.dto.response.TodoListResponseDTO;
import com.example.todo.todoapi.entity.Todo;
import com.example.todo.todoapi.repository.TodoRepository;
import com.example.todo.userapi.entity.Role;
import com.example.todo.userapi.entity.User;
import com.example.todo.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TodoService {

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;

    // 할 일 목록 조회
    public TodoListResponseDTO retrieve(String userId) {

        User user = getUser(userId);

        List<Todo> entityList = todoRepository.findAllByUser(user);

        List<TodoDetailResponseDTO> dtoList = entityList.stream()
                .map(TodoDetailResponseDTO::new)
                .collect(Collectors.toList());

        return TodoListResponseDTO.builder()
                .todos(dtoList)
                .build();
    }

    private User getUser(String userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new RuntimeException("회원정보가 없슈")
        );
    }


    // 할 일 등록
    public TodoListResponseDTO create(
            final TodoCreateRequestDTO createRequestDTO
            , final TokenUserInfo userInfo) throws RuntimeException {

        User foundUser = getUser(userInfo.getUserId());

        // 권한에 따른 글쓰기 제한 처리
        // 일반회원은 일정 5개만 작성 가능. 초과하면 예외 발생
        if (userInfo.getRole() == Role.COMMON && todoRepository.countByUser(foundUser) >= 5) {
            throw  new IllegalStateException("일반회원은 더 이상 일정을 작성할 수 없습니다.");
        }

        Todo todo = createRequestDTO.toEntity(foundUser);

        todoRepository.save(todo);
        log.info("할 일이 저장되었습니다. 제목 : {}", createRequestDTO.getTitle());
        return retrieve(userInfo.getUserId());
    }

    // 할 일 수정 (제목, 할일 완료여부)
    public TodoListResponseDTO update(
            final TodoModifyRequestDTO modifyRequestDTO,
            String userId) {
        Optional<Todo> targetEntity = todoRepository.findById(modifyRequestDTO.getId());

        targetEntity.ifPresent(entity -> {
            entity.setDone(modifyRequestDTO.isDone());

            todoRepository.save(entity);
        });

        return retrieve(userId);
    }

    // 할 일 삭제
    public TodoListResponseDTO delete(final String id, String userId) {

        try {
            todoRepository.deleteById(id);
        } catch (Exception e) {
            log.error("id가 존재하지 않아 삭제에 실패했습니다. - ID: {}, err: {}"
                    , id, e.getMessage());
            throw new RuntimeException("id가 존재하지 않아 삭제에 실패했습니다.");
        }
        return retrieve(userId);
    }


}
