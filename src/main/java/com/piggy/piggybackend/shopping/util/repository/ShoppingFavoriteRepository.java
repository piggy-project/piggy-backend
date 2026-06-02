package com.piggy.piggybackend.shopping.util.repository;

import com.piggy.piggybackend.shopping.util.entity.ShoppingFavorite;
import com.piggy.piggybackend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShoppingFavoriteRepository extends JpaRepository<ShoppingFavorite, Long> {
    List<ShoppingFavorite> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<ShoppingFavorite> findByUserAndSourceAndExternalProductId(User user, String source, String externalProductId);
    boolean existsByUserAndSourceAndExternalProductId(User user, String source, String externalProductId);

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO shopping_favorites
            (created_at, currency_code, exchange_rate, external_product_id, image_url,
             name, price, product_url, shop_name, source, user_id)
            VALUES
            (NOW(), :currencyCode, :exchangeRate, :externalProductId, :imageUrl,
             :name, :price, :productUrl, :shopName, :source, :userId)
            """, nativeQuery = true)
    int insertIgnore(
            @Param("userId") Long userId,
            @Param("externalProductId") String externalProductId,
            @Param("source") String source,
            @Param("name") String name,
            @Param("price") int price,
            @Param("imageUrl") String imageUrl,
            @Param("productUrl") String productUrl,
            @Param("shopName") String shopName,
            @Param("currencyCode") String currencyCode,
            @Param("exchangeRate") Double exchangeRate
    );
}
