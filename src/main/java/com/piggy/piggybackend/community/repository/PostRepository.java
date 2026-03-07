package com.piggy.piggybackend.community.repository;

import com.piggy.piggybackend.community.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // 1. 카테고리별 게시글 목록 최신순 조회
    List<Post> findAllByCategoryOrderByCreatedAtDesc(String category);

    // 2. 특정 카테고리 내에서 제목이나 내용에 검색어가 포함된 게시글 조회
    @Query("SELECT p FROM Post p WHERE p.category = :category " +
            "AND (p.title LIKE %:search% OR p.content LIKE %:search%) " +
            "ORDER BY p.createdAt DESC")
    List<Post> searchByCategoryAndKeyword(@Param("category") String category,
                                          @Param("search") String search);

    // 3. 특정 사용자가 쓴 글 목록 조회 (마이페이지용)
    List<Post> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
