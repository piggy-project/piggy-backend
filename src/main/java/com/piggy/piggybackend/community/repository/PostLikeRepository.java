package com.piggy.piggybackend.community.repository;

import com.piggy.piggybackend.community.entity.Post;
import com.piggy.piggybackend.community.entity.PostLike;
import com.piggy.piggybackend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostAndUser(Post post, User user);
}