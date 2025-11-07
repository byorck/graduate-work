package ru.skypro.homework.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.skypro.homework.dto.Role;
import ru.skypro.homework.dto.comment.CommentDTO;
import ru.skypro.homework.dto.comment.CommentsDTO;
import ru.skypro.homework.dto.comment.CreateOrUpdateCommentDTO;
import ru.skypro.homework.entity.Ad;
import ru.skypro.homework.entity.Comment;
import ru.skypro.homework.entity.User;
import ru.skypro.homework.repository.CommentRepository;
import ru.skypro.homework.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тестовый класс для CommentService.
 * Проверяет бизнес-логику работы с комментариями
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тестирование сервиса комментариев")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private AdService adService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentService commentService;

    private final Long AD_ID = 1L;
    private final Long COMMENT_ID = 1L;
    private final String USERNAME = "user@example.com";
    private final String TEXT = "Test comment";

    private Comment testComment;
    private Ad testAd;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
        testAd = createTestAd();
        testComment = createTestComment();
    }

    @Nested
    @DisplayName("Тесты получения комментариев")
    class GetCommentsTests {

        @Test
        @DisplayName("Успешное получение комментариев для объявления")
        void getCommentsByAdId_ShouldReturnComments() {
            // Given
            when(commentRepository.findByAdIdOrderByCreatedAtDesc(AD_ID))
                    .thenReturn(List.of(testComment));

            // When
            CommentsDTO result = commentService.getCommentsByAdId(AD_ID);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getCount());
            assertEquals(1, result.getResults().size());

            CommentDTO commentDTO = result.getResults().get(0);
            assertEquals(COMMENT_ID, commentDTO.getId());
            assertEquals(TEXT, commentDTO.getText());

            verify(commentRepository).findByAdIdOrderByCreatedAtDesc(AD_ID);
        }

        @Test
        @DisplayName("Получение пустого списка при отсутствии комментариев")
        void getCommentsByAdId_WhenNoComments_ShouldReturnEmptyList() {
            // Given
            when(commentRepository.findByAdIdOrderByCreatedAtDesc(AD_ID))
                    .thenReturn(List.of());

            // When
            CommentsDTO result = commentService.getCommentsByAdId(AD_ID);

            // Then
            assertNotNull(result);
            assertEquals(0, result.getCount());
            assertTrue(result.getResults().isEmpty());
        }
    }

    @Nested
    @DisplayName("Тесты добавления комментариев")
    class AddCommentTests {

        @Test
        @DisplayName("Успешное добавление комментария")
        void addComment_ShouldSuccessfullyAddComment() {
            // Given
            CreateOrUpdateCommentDTO dto = new CreateOrUpdateCommentDTO();
            dto.setText(TEXT);

            when(adService.getAdEntityById(AD_ID)).thenReturn(testAd);
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(commentRepository.countByAdId(AD_ID)).thenReturn(0L);
            when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
                Comment savedComment = invocation.getArgument(0);
                savedComment.setId(1L);
                return savedComment;
            });

            // When
            CommentDTO result = commentService.addComment(AD_ID, dto, USERNAME);

            // Then
            assertNotNull(result);
            assertEquals(TEXT, result.getText());
            assertEquals(1L, result.getId());

            verify(commentRepository).save(any(Comment.class));
            verify(commentRepository).countByAdId(AD_ID);
        }

        @Test
        @DisplayName("Неудачное добавление комментария - объявление не найдено")
        void addComment_WhenAdNotFound_ShouldReturnNull() {
            // Given
            CreateOrUpdateCommentDTO dto = new CreateOrUpdateCommentDTO();
            dto.setText(TEXT);

            when(adService.getAdEntityById(AD_ID)).thenReturn(null);

            // When
            CommentDTO result = commentService.addComment(AD_ID, dto, USERNAME);

            // Then
            assertNull(result);
            verify(commentRepository, never()).save(any(Comment.class));
        }

        @Test
        @DisplayName("Неудачное добавление комментария - пользователь не найден")
        void addComment_WhenUserNotFound_ShouldReturnNull() {
            // Given
            CreateOrUpdateCommentDTO dto = new CreateOrUpdateCommentDTO();
            dto.setText(TEXT);

            when(adService.getAdEntityById(AD_ID)).thenReturn(testAd);
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

            // When
            CommentDTO result = commentService.addComment(AD_ID, dto, USERNAME);

            // Then
            assertNull(result);
            verify(commentRepository, never()).save(any(Comment.class));
        }
    }

    @Nested
    @DisplayName("Тесты удаления комментариев с проверкой прав доступа")
    class DeleteCommentTests {

        @Test
        @DisplayName("Успешное удаление комментария автором")
        void deleteCommentWithPermission_AsCommentAuthor_ShouldSuccess() {
            // Given
            when(commentRepository.findByAdIdAndCommentNumber(AD_ID, COMMENT_ID))
                    .thenReturn(Optional.of(testComment));
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.ofNullable(testComment.getUser()));

            // When
            boolean result = commentService.deleteCommentWithPermission(AD_ID, COMMENT_ID, USERNAME);

            // Then
            assertTrue(result);
            verify(commentRepository).delete(testComment);
        }

        @Test
        @DisplayName("Успешное удаление комментария администратором")
        void deleteCommentWithPermission_AsAdmin_ShouldSuccess() {
            // Given
            User adminUser = createTestUser();
            adminUser.setRole(Role.ADMIN);
            adminUser.setUsername("admin@example.com");

            when(commentRepository.findByAdIdAndCommentNumber(AD_ID, COMMENT_ID))
                    .thenReturn(Optional.of(testComment));
            when(userRepository.findByUsername("admin@example.com")).thenReturn(Optional.of(adminUser));

            // When
            boolean result = commentService.deleteCommentWithPermission(AD_ID, COMMENT_ID, "admin@example.com");

            // Then
            assertTrue(result);
            verify(commentRepository).delete(testComment);
        }

        @Test
        @DisplayName("Успешное удаление комментария автором объявления")
        void deleteCommentWithPermission_AsAdAuthor_ShouldSuccess() {
            // Given
            User adAuthor = createTestUser();
            adAuthor.setUsername("adauthor@example.com");
            testComment.getAd().getUser().setUsername("adauthor@example.com");
            testComment.getUser().setUsername("commentauthor@example.com");

            when(commentRepository.findByAdIdAndCommentNumber(AD_ID, COMMENT_ID))
                    .thenReturn(Optional.of(testComment));
            when(userRepository.findByUsername("adauthor@example.com")).thenReturn(Optional.of(adAuthor));

            // When
            boolean result = commentService.deleteCommentWithPermission(AD_ID, COMMENT_ID, "adauthor@example.com");

            // Then
            assertTrue(result);
            verify(commentRepository).delete(testComment);
        }

        @Test
        @DisplayName("Неудачное удаление комментария - недостаточно прав")
        void deleteCommentWithPermission_WithoutPermission_ShouldFail() {
            // Given
            User unauthorizedUser = createTestUser();
            unauthorizedUser.setUsername("unauthorized@example.com");
            unauthorizedUser.setRole(Role.USER);
            testComment.getUser().setUsername("different@example.com");
            testComment.getAd().getUser().setUsername("different@example.com");

            when(commentRepository.findByAdIdAndCommentNumber(AD_ID, COMMENT_ID))
                    .thenReturn(Optional.of(testComment));
            when(userRepository.findByUsername("unauthorized@example.com")).thenReturn(Optional.of(unauthorizedUser));

            // When
            boolean result = commentService.deleteCommentWithPermission(AD_ID, COMMENT_ID, "unauthorized@example.com");

            // Then
            assertFalse(result);
            verify(commentRepository, never()).delete(testComment);
        }

        @Test
        @DisplayName("Неудачное удаление комментария - комментарий не найден")
        void deleteCommentWithPermission_WhenCommentNotFound_ShouldFail() {
            // Given
            when(commentRepository.findByAdIdAndCommentNumber(AD_ID, COMMENT_ID))
                    .thenReturn(Optional.empty());

            // When
            boolean result = commentService.deleteCommentWithPermission(AD_ID, COMMENT_ID, USERNAME);

            // Then
            assertFalse(result);
            verify(commentRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Тесты обновления комментариев с проверкой прав доступа")
    class UpdateCommentTests {

        @Test
        @DisplayName("Успешное обновление комментария автором")
        void updateCommentWithPermission_AsCommentAuthor_ShouldSuccess() {
            // Given
            CreateOrUpdateCommentDTO dto = new CreateOrUpdateCommentDTO();
            dto.setText("Updated text");

            when(commentRepository.findByAdIdAndCommentNumber(AD_ID, COMMENT_ID))
                    .thenReturn(Optional.of(testComment));
            when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

            // When
            CommentDTO result = commentService.updateCommentWithPermission(AD_ID, COMMENT_ID, dto, USERNAME);

            // Then
            assertNotNull(result);
            assertEquals("Updated text", result.getText());
            verify(commentRepository).save(testComment);
        }

        @Test
        @DisplayName("Неудачное обновление комментария - недостаточно прав")
        void updateCommentWithPermission_WithoutPermission_ShouldReturnNull() {
            // Given
            testComment.getUser().setUsername("different@example.com");
            CreateOrUpdateCommentDTO dto = new CreateOrUpdateCommentDTO();
            dto.setText("Updated text");

            when(commentRepository.findByAdIdAndCommentNumber(AD_ID, COMMENT_ID))
                    .thenReturn(Optional.of(testComment));

            // When
            CommentDTO result = commentService.updateCommentWithPermission(AD_ID, COMMENT_ID, dto, USERNAME);

            // Then
            assertNull(result);
            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Неудачное обновление комментария - комментарий не найден")
        void updateCommentWithPermission_WhenCommentNotFound_ShouldReturnNull() {
            // Given
            CreateOrUpdateCommentDTO dto = new CreateOrUpdateCommentDTO();
            dto.setText("Updated text");

            when(commentRepository.findByAdIdAndCommentNumber(AD_ID, COMMENT_ID))
                    .thenReturn(Optional.empty());

            // When
            CommentDTO result = commentService.updateCommentWithPermission(AD_ID, COMMENT_ID, dto, USERNAME);

            // Then
            assertNull(result);
            verify(commentRepository, never()).save(any());
        }
    }

    /**
     * Создает тестовый комментарий
     */
    private Comment createTestComment() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setText(TEXT);
        comment.setCommentNumber(COMMENT_ID);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUser(testUser);
        comment.setAd(testAd);
        return comment;
    }

    /**
     * Создает тестовое объявление
     */
    private Ad createTestAd() {
        Ad ad = new Ad();
        ad.setId(AD_ID);

        User adUser = createTestUser();
        adUser.setUsername("adowner@example.com");
        ad.setUser(adUser);

        return ad;
    }

    /**
     * Создает тестового пользователя
     */
    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername(USERNAME);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setRole(Role.USER);
        return user;
    }
}