package com.example.todo.todoapi.api;

import com.example.todo.auth.TokenUserInfo;
import com.example.todo.todoapi.dto.request.TodoCreateRequestDTO;
import com.example.todo.todoapi.dto.request.TodoModifyRequestDTO;
import com.example.todo.todoapi.dto.response.TodoListResponseDTO;
import com.example.todo.todoapi.service.TodoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/todos")
//@CrossOrigin(origins =  "http://localhost:3000")
public class TodoController {

    private final TodoService todoService;

    // 할 일 목록요청 (GET)
    @GetMapping
    public ResponseEntity<?> retrieveTodoList(
            // 토큰에 인증된 사용자 정보를 불러올 수 있음
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {
        log.info("/api/todos GET request!");

        TodoListResponseDTO responseDTO = todoService.retrieve(userInfo.getUserId());

        return ResponseEntity.ok().body(responseDTO);
    }

    // 할 일 등록 요청
    @PostMapping
    public ResponseEntity<?> createTodo(
            @AuthenticationPrincipal TokenUserInfo userInfo
            , @Validated @RequestBody TodoCreateRequestDTO requestDTO
            , BindingResult result
    ) {
        if (result.hasErrors()) {
            log.warn("DTO 검증 에러 발생 : {}", result.getFieldError());
            return ResponseEntity
                    .badRequest()
                    .body(result.getFieldError());
        }

        try {
            TodoListResponseDTO responseDTO = todoService.create(requestDTO, userInfo);
            return ResponseEntity
                    .ok()
                    .body(responseDTO);
        } catch (IllegalStateException e) {
            // 권한때문에 발생한 예외
            log.warn(e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
                    // 인가되지 않음(UNAUTHORIZED) - 401번
        } catch (RuntimeException e) {
            log.error(e.getMessage());
            return ResponseEntity
                    .internalServerError()
                    .body(TodoListResponseDTO.builder().error(e.getMessage()));
        }

    }

    // 할 일 삭제 요청
    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('ROLE_PREMIUM') or hasROLE('ROLE_ADMIN')")
    public ResponseEntity<?> deleteTodo(
            @AuthenticationPrincipal TokenUserInfo userInfo
            ,@PathVariable("id") String todoId) {
        log.info("/api/todos/{} DELETE request!", todoId);

        if (todoId == null || todoId.trim().equals("")) {
            return ResponseEntity
                    .badRequest()
                    .body(TodoListResponseDTO.builder().error("ID를 전달해주세요"));
        }

        try {
            TodoListResponseDTO responseDTO = todoService.delete(todoId, userInfo.getUserId());
            return ResponseEntity.ok().body(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(TodoListResponseDTO.builder().error(e.getMessage()));
        }
    }

    // 할 일 수정요청 (PUT, PATCH)
    @RequestMapping(method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<?> updateTodo(
            @AuthenticationPrincipal TokenUserInfo userInfo
            ,@Validated @RequestBody TodoModifyRequestDTO requestDTO
            , BindingResult result
            , HttpServletRequest request
    ) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(result.getFieldError());
        }

        log.info("/api/todos {} request", request.getMethod());
        log.info("modifying dto : {}", requestDTO);

        try {
            TodoListResponseDTO responseDTO = todoService.update(requestDTO, userInfo.getUserId());
            return ResponseEntity.ok().body(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(TodoListResponseDTO.builder().error(e.getMessage()));
        }
    }

}
