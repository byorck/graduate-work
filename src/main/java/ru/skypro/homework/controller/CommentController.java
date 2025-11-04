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
     *
     * @param id идентификатор объявления
     * @return список комментариев к объявлению
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
     * @param id идентификатор объявления
     * @param dto DTO с текстом комментария
     * @param authentication данные аутентификации текущего пользователя
     * @return созданный комментарий или 404 если объявление не найдено
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
     * @param adId идентификатор объявления (для валидации)
     * @param commentId идентификатор комментария
     * @param authentication данные аутентификации для проверки прав доступа
     * @return 204 No Content при успешном удалении, 404 если не найдено, 403 если нет прав
     */
    @DeleteMapping("/{adId}/comments/{commentId}")
    @Operation(summary = "Удаление комментария")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long adId,
            @PathVariable Long commentId,
            Authentication authentication) {

        logger.info("Deleting comment {} by user {}", commentId, authentication.getName());

        if (!commentService.hasDeletePermission(commentId, authentication.getName())) {
            logger.warn("User {} attempted to delete comment {} without permission", authentication.getName(), commentId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boolean deleted = commentService.deleteComment(commentId, authentication.getName());
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * Обновление текста комментария
     * @param adId идентификатор объявления (для валидации)
     * @param commentId идентификатор комментария
     * @param dto DTO с новым текстом комментария
     * @param authentication данные аутентификации для проверки прав доступа
     * @return обновленный комментарий или 404 если не найдено, 403 если нет прав
     */
    @PatchMapping("/{adId}/comments/{commentId}")
    @Operation(summary = "Обновление комментария")
    public ResponseEntity<CommentDTO> updateComment(
            @PathVariable Long adId,
            @PathVariable Long commentId,
            @Valid @RequestBody CreateOrUpdateCommentDTO dto,
            Authentication authentication) {

        logger.info("Updating comment {} by user {}", commentId, authentication.getName());

        if (!commentService.hasUpdatePermission(commentId, authentication.getName())) {
            logger.warn("User {} attempted to update comment {} without permission", authentication.getName(), commentId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        CommentDTO updatedComment = commentService.updateComment(commentId, dto, authentication.getName());
        return updatedComment != null ? ResponseEntity.ok(updatedComment) : ResponseEntity.notFound().build();
    }
}
