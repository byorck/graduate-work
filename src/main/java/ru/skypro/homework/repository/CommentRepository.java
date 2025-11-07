package ru.skypro.homework.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import ru.skypro.homework.entity.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByAdIdOrderByCreatedAtDesc(Long adId);

    @NonNull
    @EntityGraph(attributePaths = {"user"})
    Optional<Comment> findById(Long id);

    Optional<Comment> findByAdIdAndCommentNumber(Long adId, Long commentNumber);

    long countByAdId(Long adId);
}
