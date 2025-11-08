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
import ru.skypro.homework.repository.AdRepository;
import ru.skypro.homework.repository.CommentRepository;
import ru.skypro.homework.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тестирование CommentService")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private AdRepository adRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentService commentService;

    private User testUser;
    private Ad testAd;
    private Comment testComment;

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
        @DisplayName("Получение комментариев по ID объявления")
        void getCommentsByAdId_ShouldReturnCommentsDTO() {
            // Arrange
            List<Comment> comments = List.of(testComment);
            when(commentRepository.findByAdIdOrderByCreatedAtDesc(1L)).thenReturn(comments);

            // Act
            CommentsDTO result = commentService.getCommentsByAdId(1L);

            // Assert
            assertNotNull(result, "Результат не должен быть null");
            assertEquals(1, result.getCount(), "Количество комментариев должно быть 1");
            assertEquals(1, result.getResults().size(), "Размер списка комментариев должен быть 1");
            verify(commentRepository, times(1)).findByAdIdOrderByCreatedAtDesc(1L);
        }
    }

    @Nested
    @DisplayName("Тесты добавления комментариев")
    class AddCommentTests {

        @Test
        @DisplayName("Успешное добавление комментария с валидными данными")
        void addComment_WithValidData_ShouldReturnCommentDTO() {
            // Arrange
            CreateOrUpdateCommentDTO dto = new CreateOrUpdateCommentDTO();
            dto.setText("Test comment");

            when(adRepository.findById(1L)).thenReturn(Optional.of(testAd));
            when(userRepository.findByUsername("test@mail.ru")).thenReturn(Optional.of(testUser));
            when(commentRepository.countByAdId(1L)).thenReturn(0L);
            when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

            // Act
            CommentDTO result = commentService.addComment(1L, dto, "test@mail.ru");

            // Assert
            assertNotNull(result, "Результат не должен быть null");
            assertEquals("Test comment", result.getText(), "Текст комментария должен совпадать");
            verify(commentRepository, times(1)).save(any(Comment.class));
        }

        @Test
        @DisplayName("Добавление комментария к несуществующему объявлению должно возвращать null")
        void addComment_WithNonExistentAd_ShouldReturnNull() {
            // Arrange
            CreateOrUpdateCommentDTO dto = new CreateOrUpdateCommentDTO();
            dto.setText("Test comment");

            when(adRepository.findById(1L)).thenReturn(Optional.empty());

            // Act
            CommentDTO result = commentService.addComment(1L, dto, "test@mail.ru");

            // Assert
            assertNull(result, "Результат должен быть null для несуществующего объявления");
            verify(commentRepository, never()).save(any(Comment.class));
        }
    }

    @Nested
    @DisplayName("Тесты удаления комментариев")
    class DeleteCommentTests {

        @Test
        @DisplayName("Удаление комментария автором должно возвращать true")
        void deleteCommentWithPermission_ByAuthor_ShouldReturnTrue() {
            // Arrange
            when(commentRepository.findByAdIdAndCommentNumber(1L, 1L)).thenReturn(Optional.of(testComment));
            when(userRepository.findByUsername("test@mail.ru")).thenReturn(Optional.of(testUser));

            // Act
            boolean result = commentService.deleteCommentWithPermission(1L, 1L, "test@mail.ru");

            // Assert
            assertTrue(result, "Результат должен быть true при удалении автором");
            verify(commentRepository, times(1)).delete(testComment);
        }

        @Test
        @DisplayName("Удаление комментария без прав доступа должно возвращать false")
        void deleteCommentWithPermission_WithoutPermission_ShouldReturnFalse() {
            // Arrange
            when(commentRepository.findByAdIdAndCommentNumber(1L, 1L)).thenReturn(Optional.of(testComment));
            when(userRepository.findByUsername("other@mail.ru")).thenReturn(Optional.of(createOtherUser()));

            // Act
            boolean result = commentService.deleteCommentWithPermission(1L, 1L, "other@mail.ru");

            // Assert
            assertFalse(result, "Результат должен быть false без прав доступа");
            verify(commentRepository, never()).delete(any(Comment.class));
        }
    }

    @Nested
    @DisplayName("Тесты обновления комментариев")
    class UpdateCommentTests {

        @Test
        @DisplayName("Обновление комментария автором должно возвращать обновленный комментарий")
        void updateCommentWithPermission_ByAuthor_ShouldReturnUpdatedComment() {
            // Arrange
            CreateOrUpdateCommentDTO dto = new CreateOrUpdateCommentDTO();
            dto.setText("Updated comment");

            when(commentRepository.findByAdIdAndCommentNumber(1L, 1L)).thenReturn(Optional.of(testComment));
            when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

            // Act
            CommentDTO result = commentService.updateCommentWithPermission(1L, 1L, dto, "test@mail.ru");

            // Assert
            assertNotNull(result, "Результат не должен быть null");
            verify(commentRepository, times(1)).save(testComment);
        }
    }

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("test@mail.ru");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(Role.USER);
        return user;
    }

    private User createOtherUser() {
        User user = new User();
        user.setId(2L);
        user.setUsername("other@mail.ru");
        user.setFirstName("Other");
        user.setLastName("User");
        user.setRole(Role.USER);
        return user;
    }

    private Ad createTestAd() {
        Ad ad = new Ad();
        ad.setId(1L);
        ad.setUser(testUser);
        return ad;
    }

    private Comment createTestComment() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setText("Test comment");
        comment.setUser(testUser);
        comment.setAd(testAd);
        comment.setCommentNumber(1L);
        comment.setCreatedAt(LocalDateTime.now());
        return comment;
    }
}