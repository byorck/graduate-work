package ru.skypro.homework.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.skypro.homework.entity.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByAdIdOrderByCreatedAtDesc(Long adId);

    @EntityGraph(attributePaths = {"user"})
    Optional<Comment> findById(Long id);

    void deleteByAdId(Long adId);

    boolean existsByIdAndUserUsername(Long id, String username);

    boolean existsByAdIdAndUserUsername(Long adId, String username);
}
