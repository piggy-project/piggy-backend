package com.piggy.piggybackend.shopping.util.repository;

import com.piggy.piggybackend.shopping.util.entity.ShoppingFavorite;
import com.piggy.piggybackend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShoppingFavoriteRepository extends JpaRepository<ShoppingFavorite, Long> {
    List<ShoppingFavorite> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<ShoppingFavorite> findByUserAndSourceAndExternalProductId(User user, String source, String externalProductId);
    boolean existsByUserAndSourceAndExternalProductId(User user, String source, String externalProductId);
}
