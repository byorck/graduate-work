package ru.skypro.homework.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.skypro.homework.dto.comment.CommentDTO;
import ru.skypro.homework.dto.comment.CommentsDTO;
import ru.skypro.homework.dto.comment.CreateOrUpdateCommentDTO;
import ru.skypro.homework.service.CommentService;

/**
 * Контроллер для работы с комментариями к объявлениям.
 * Обеспечивает REST API для создания, получения, обновления и удаления комментариев.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ads")
@Tag(name = "Комментарии")
public class CommentController {
    private final CommentService commentService;

    /**
     * Получение всех комментариев для указанного объявления
     *
     * @param id идентификатор объявления
     * @return список комментариев и их количество
     */
    @GetMapping("/{id}/comments")
    public ResponseEntity<CommentsDTO> getComments(@PathVariable Long id) {
        CommentsDTO comments = commentService.getCommentsByAdId(id);
        return ResponseEntity.ok(comments);
    }

    /**
     * Добавление нового комментария к объявлению
     *
     * @param id идентификатор объявления
     * @param dto данные для создания комментария
     * @param authentication данные аутентификации пользователя
     * @return созданный комментарий
     */
    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentDTO> addComment(@PathVariable Long id,
                                                 @Valid @RequestBody CreateOrUpdateCommentDTO dto, Authentication authentication) {
        CommentDTO comment = commentService.addComment(id, dto, authentication.getName());
        return comment != null ? ResponseEntity.ok(comment) : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /**
     * Удаление комментария по его номеру в рамках объявления
     *
     * @param adId идентификатор объявления
     * @param commentId(commentNumber) номер комментария в рамках объявления
     * @param authentication данные аутентификации пользователя
     * @return статус операции удаления
     */
    @DeleteMapping("/{adId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long adId, @PathVariable Long commentId,
                                              Authentication authentication) {
        boolean deleted = commentService.deleteCommentWithPermission(adId, commentId, authentication.getName());
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * Обновление текста комментария
     *
     * @param adId идентификатор объявления
     * @param commentId(commentNumber) номер комментария в рамках объявления
     * @param dto новые данные комментария
     * @param authentication данные аутентификации пользователя
     * @return обновленный комментарий
     */
    @PatchMapping("/{adId}/comments/{commentId}")
    public ResponseEntity<CommentDTO> updateComment(@PathVariable Long adId, @PathVariable Long commentId,
                                                    @Valid @RequestBody CreateOrUpdateCommentDTO dto, Authentication authentication) {
        CommentDTO updatedComment = commentService.updateCommentWithPermission(adId, commentId, dto, authentication.getName());
        return updatedComment != null ? ResponseEntity.ok(updatedComment) : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}