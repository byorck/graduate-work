package ru.skypro.homework.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.skypro.homework.dto.Role;
import ru.skypro.homework.dto.comment.CommentDTO;
import ru.skypro.homework.dto.comment.CommentsDTO;
import ru.skypro.homework.dto.comment.CreateOrUpdateCommentDTO;
import ru.skypro.homework.entity.Ad;
import ru.skypro.homework.entity.Comment;
import ru.skypro.homework.entity.User;
import ru.skypro.homework.repository.CommentRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для работы с комментариями к объявлениям.
 * Обрабатывает бизнес-логику создания, получения, обновления и удаления комментариев
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final AdService adService;
    private final UserService userService;

    /**
     * Получение всех комментариев для объявления
     *
     * @param adId идентификатор объявления
     * @return DTO с количеством и списком комментариев
     */
    public CommentsDTO getCommentsByAdId(Long adId) {
        log.debug("Getting comments for ad id: {}", adId);
        List<Comment> comments = commentRepository.findByAdIdOrderByCreatedAtDesc(adId);
        List<CommentDTO> commentDTOS = comments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        CommentsDTO response = new CommentsDTO();
        response.setCount(commentDTOS.size());
        response.setResults(commentDTOS);
        return response;
    }

    /**
     * Добавление нового комментария к объявлению
     *
     * @param adId     идентификатор объявления
     * @param dto      DTO с данными комментария
     * @param username имя пользователя-автора
     * @return DTO созданного комментария или null при ошибке
     */
    public CommentDTO addComment(Long adId, CreateOrUpdateCommentDTO dto, String username) {
        log.debug("Adding comment to ad {} by user {}", adId, username);
        Ad ad = adService.getAdEntityById(adId);
        User user = userService.findUser(username);

        if (ad == null || user == null) {
            log.warn("Failed to add comment: ad {} or user {} not found", adId, username);
            return null;
        }

        Comment comment = new Comment();
        comment.setText(dto.getText());
        comment.setUser(user);
        comment.setAd(ad);

        Comment savedComment = commentRepository.save(comment);
        log.info("Comment {} added successfully to ad {}", savedComment.getId(), adId);
        return convertToDto(savedComment);
    }

    /**
     * Удаление комментария с проверкой прав доступа
     *
     * @param commentId идентификатор комментария
     * @param username  имя пользователя, выполняющего операцию
     * @return true если удаление успешно, false если нет прав
     */
    public boolean deleteCommentWithPermission(Long commentId, String username) {
        log.debug("Deleting comment {} by user {}", commentId, username);

        if (!hasDeletePermission(commentId, username)) {
            log.warn("User {} attempted to delete comment {} without permission", username, commentId);
            return false;
        }

        return deleteComment(commentId, username);
    }

    /**
     * Обновление комментария с проверкой прав доступа
     *
     * @param commentId идентификатор комментария
     * @param dto       DTO с обновленными данными
     * @param username  имя пользователя, выполняющего операцию
     * @return DTO обновленного комментария или null если нет прав
     */
    public CommentDTO updateCommentWithPermission(Long commentId, CreateOrUpdateCommentDTO dto, String username) {
        log.debug("Updating comment {} by user {}", commentId, username);

        if (!hasUpdatePermission(commentId, username)) {
            log.warn("User {} attempted to update comment {} without permission", username, commentId);
            return null;
        }

        return updateComment(commentId, dto, username);
    }

    /**
     * Удаление комментария (без проверки прав)
     *
     * @param commentId идентификатор комментария
     * @param username  имя пользователя, выполняющего операцию
     * @return true если удаление успешно, false если комментарий не найден
     */
    public boolean deleteComment(Long commentId, String username) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            log.warn("Comment {} not found for deletion", commentId);
            return false;
        }

        commentRepository.deleteById(commentId);
        log.info("Comment {} deleted successfully by user {}", commentId, username);
        return true;
    }

    /**
     * Обновление комментария (без проверки прав)
     *
     * @param commentId идентификатор комментария
     * @param dto       DTO с обновленными данными
     * @param username  имя пользователя, выполняющего операцию
     * @return DTO обновленного комментария или null если комментарий не найден
     */
    public CommentDTO updateComment(Long commentId, CreateOrUpdateCommentDTO dto, String username) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            log.warn("Comment {} not found for update", commentId);
            return null;
        }

        comment.setText(dto.getText());
        Comment updatedComment = commentRepository.save(comment);
        log.info("Comment {} updated successfully by user {}", commentId, username);
        return convertToDto(updatedComment);
    }

    /**
     * Проверка прав доступа для редактирования комментария
     *
     * @param commentId идентификатор комментария
     * @param username  имя пользователя
     * @return true только если пользователь является автором комментария
     */
    public boolean hasUpdatePermission(Long commentId, String username) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            return false;
        }

        boolean isCommentAuthor = comment.getUser().getUsername().equals(username);

        log.debug("User {} update permission for comment {}: isCommentAuthor={}",
                username, commentId, isCommentAuthor);

        return isCommentAuthor;
    }

    /**
     * Проверка прав доступа для удаления комментария
     *
     * @param commentId идентификатор комментария
     * @param username  имя пользователя
     * @return true если пользователь является админом, автором комментария или автором объявления
     */
    public boolean hasDeletePermission(Long commentId, String username) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            return false;
        }

        User user = userService.findUser(username);
        if (user == null) {
            return false;
        }

        if (user.getRole() == Role.ADMIN) {
            log.debug("User {} is ADMIN, has delete permission for comment {}", username, commentId);
            return true;
        }

        boolean isCommentAuthor = comment.getUser().getUsername().equals(username);

        boolean isAdAuthor = comment.getAd().getUser().getUsername().equals(username);

        boolean hasPermission = isCommentAuthor || isAdAuthor;

        if (hasPermission) {
            log.debug("User {} has delete permission for comment {}: isCommentAuthor={}, isAdAuthor={}",
                    username, commentId, isCommentAuthor, isAdAuthor);
        } else {
            log.debug("User {} has NO delete permission for comment {}: isCommentAuthor={}, isAdAuthor={}",
                    username, commentId, isCommentAuthor, isAdAuthor);
        }

        return hasPermission;
    }

    /**
     * Преобразование сущности комментария в DTO
     */
    private CommentDTO convertToDto(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setText(comment.getText());
        dto.setAuthor(comment.getUser().getId());
        dto.setAuthorFirstName(comment.getUser().getFirstName());
        dto.setCreatedAt(comment.getCreatedAt());

        String avatarUrl = "/users/" + comment.getUser().getId() + "/avatar";
        dto.setAuthorImage(avatarUrl);

        return dto;
    }
}
