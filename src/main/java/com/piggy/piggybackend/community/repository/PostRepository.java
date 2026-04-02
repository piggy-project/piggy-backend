package com.piggy.piggybackend.community.repository;

import com.piggy.piggybackend.community.entity.BoardCategory;
import com.piggy.piggybackend.community.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // 1. 카테고리별 조회 (전체)
    List<Post> findAllByCategoryOrderByCreatedAtDesc(BoardCategory category);

    // 2. 카테고리 + 통화 코드별 조회 (환율 게시판 전용)
    List<Post> findAllByCategoryAndCodeOrderByCreatedAtDesc(BoardCategory category, String code);

    // 3. 검색 (카테고리 + 통화코드 선택적 + 키워드)
    @Query("SELECT p FROM Post p WHERE p.category = :category " +
            "AND (:code IS NULL OR p.code = :code) " +
            "AND (p.title LIKE %:search% OR p.content LIKE %:search%) " +
            "ORDER BY p.createdAt DESC")
    List<Post> searchByCategoryAndCodeAndKeyword(@Param("category") BoardCategory category,
                                                 @Param("code") String code,
                                                 @Param("search") String search);

    // 4. 특정 사용자가 쓴 글
    List<Post> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}