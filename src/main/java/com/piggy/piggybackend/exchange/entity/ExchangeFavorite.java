package com.piggy.piggybackend.exchange.entity;

import com.piggy.piggybackend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "exchange_favorites",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "currency_code"}))
public class ExchangeFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;
}