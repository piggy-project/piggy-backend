package com.piggy.piggybackend.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider", "providerId"}),
                @UniqueConstraint(columnNames = "nickname")
        })
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128)
    private String email; // 소셜은 null 가능

    @Column(length = 128)
    private String password; // LOCAL만 사용

    @Column( length = 20)
    private String name;

    @Column( length = 50)
    private String nickname;

    @Column
    private LocalDate birth;

    @Column(length = 256)
    private String phoneNum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Provider provider; // 소셜로그인


    @Column(nullable = true)
    private String providerId; // 소셜 고유 ID

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}

