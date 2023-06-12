package com.example.todo.userapi.entity;

import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.StringTokenizer;

@Setter
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(name = "tbl_user")
public class User {

    @Id
    @Column(name = "user_id")
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id; // 계정명이 아니라 식별코드

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String userName;

    @CreationTimestamp
    private LocalDateTime joinDate;

    @Enumerated(EnumType.STRING)
//    @ColumnDefault("'COMMON'") // @ColumnDefault은 항상 "" string임. 근데enum인 경우 반드시 '' 홑따옴표로 감싸줘야함!!!!!!!!!!!!!!!!!!!!!!
    @Builder.Default
    private Role role = Role.COMMON; // 유저 권한

//    public void test() {
//        String test = "";
//        String[] s = test.split(" ");
//
//
//        StringTokenizer st = new StringTokenizer(test);
//        // 서울시 강남구 ㅇ어
//        st.nextToken();
//        String s1 = st.nextToken();
//    }

}

