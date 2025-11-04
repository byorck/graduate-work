package ru.skypro.homework.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.skypro.homework.dto.comment.CommentDTO;
import ru.skypro.homework.dto.comment.CommentsDTO;
import ru.skypro.homework.dto.comment.CreateOrUpdateCommentDTO;
import ru.skypro.homework.service.CommentService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ads")
@Tag(name = "Комментарии")
public class CommentController {

    private final CommentService commentService;
    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);

    /**
     * Получение всех комментариев для указанного объявления
     */
    @GetMapping("/{id}/comments")
    @Operation(summary = "Получение комментариев объявления")
    public ResponseEntity<CommentsDTO> getComments(@PathVariable Long id) {
        logger.info("Getting comments for ad with id: {}", id);
        CommentsDTO comments = commentService.getCommentsByAdId(id);
        return ResponseEntity.ok(comments);
    }

    /**
     * Добавление нового комментария к объявлению
     */
    @PostMapping("/{id}/comments")
    @Operation(summary = "Добавление комментария к объявлению")
    public ResponseEntity<CommentDTO> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateOrUpdateCommentDTO dto,
            Authentication authentication) {

        logger.info("Adding comment to ad {} by user {}", id, authentication.getName());
        CommentDTO comment = commentService.addComment(id, dto, authentication.getName());
        return comment != null ? ResponseEntity.ok(comment) : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /**
     * Удаление комментария
     */
    @DeleteMapping("/{adId}/comments/{commentId}")
    @Operation(summary = "Удаление комментария")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long adId,
            @PathVariable Long commentId,
            Authentication authentication) {

        logger.info("Deleting comment {} by user {}", commentId, authentication.getName());
        boolean deleted = commentService.deleteCommentWithPermission(commentId, authentication.getName());

        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Обновление текста комментария
     */
    @PatchMapping("/{adId}/comments/{commentId}")
    @Operation(summary = "Обновление комментария")
    public ResponseEntity<CommentDTO> updateComment(
            @PathVariable Long adId,
            @PathVariable Long commentId,
            @Valid @RequestBody CreateOrUpdateCommentDTO dto,
            Authentication authentication) {

        logger.info("Updating comment {} by user {}", commentId, authentication.getName());
        CommentDTO updatedComment = commentService.updateCommentWithPermission(commentId, dto, authentication.getName());

        if (updatedComment != null) {
            return ResponseEntity.ok(updatedComment);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}