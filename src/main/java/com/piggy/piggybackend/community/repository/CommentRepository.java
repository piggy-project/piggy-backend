package com.piggy.piggybackend.community.repository;

import com.piggy.piggybackend.community.entity.Comment;
import com.piggy.piggybackend.community.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    // 특정 게시글의 모든 댓글을 작성 순서대로 가져오기
    List<Comment> findAllByPostIdOrderByCreatedAtAsc(Long postId);
    List<Comment> findByPostOrderByCreatedAtAsc(Post post);
}
