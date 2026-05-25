package com.piggy.piggybackend.shopping.util.entity;

import com.piggy.piggybackend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "shopping_favorites",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_shopping_favorite_user_source_product",
                        columnNames = {"user_id", "source", "external_product_id"})
        },
        indexes = {
                @Index(name = "idx_shopping_favorite_user", columnList = "user_id"),
                @Index(name = "idx_shopping_favorite_product", columnList = "source, external_product_id")
        })
public class ShoppingFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shopping_favorite_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "external_product_id", nullable = false, length = 500)
    private String externalProductId;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(length = 1000)
    private String imageUrl;

    @Column(length = 1000)
    private String productUrl;

    @Column(length = 100)
    private String shopName;

    @Column(length = 10)
    private String currencyCode;

    private Double exchangeRate;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
