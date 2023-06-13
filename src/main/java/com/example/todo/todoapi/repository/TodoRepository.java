package com.example.todo.todoapi.repository;

import com.example.todo.todoapi.entity.Todo;
import com.example.todo.userapi.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, String> {

    // 특정회원의 할일 목록 리턴
    // SELECT * FROM tbl_todo WHERE user_id =?
    // SELECT * FROM tbl_todo WHERE user_id:userId ,nativeQuery = true
    // JPQL
    @Query("SELECT t FROM Todo t WHERE t.user = :user")
    List<Todo> findAllByUser(@Param("user") User user);
    // page 넣고싶으면 아래와 같이
//    Page<Todo> findAllByUser(@Param("zzz") User user, Pageable pageable);
}
