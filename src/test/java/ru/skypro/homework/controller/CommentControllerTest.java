package ru.skypro.homework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.skypro.homework.dto.comment.CommentDTO;
import ru.skypro.homework.dto.comment.CommentsDTO;
import ru.skypro.homework.dto.comment.CreateOrUpdateCommentDTO;
import ru.skypro.homework.service.CommentService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тестовый класс для CommentController.
 * Проверяет endpoints для работы с комментариями
 */
@WebMvcTest(CommentController.class)
@DisplayName("Тестирование контроллера комментариев")
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    private final Long AD_ID = 1L;
    private final Long COMMENT_ID = 1L;

    @Nested
    @DisplayName("Тесты получения комментариев")
    class GetCommentsTests {

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("Должен возвращать комментарии")
        void getComments_ShouldReturnComments() throws Exception {
            // Given
            CommentsDTO commentsDTO = new CommentsDTO();
            commentsDTO.setCount(1);
            commentsDTO.setResults(List.of(createTestCommentDTO()));

            when(commentService.getCommentsByAdId(AD_ID)).thenReturn(commentsDTO);

            // When & Then
            mockMvc.perform(get("/ads/{id}/comments", AD_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(1))
                    .andExpect(jsonPath("$.results[0].id").value(COMMENT_ID))
                    .andExpect(jsonPath("$.results[0].text").value("Test comment"));

            verify(commentService).getCommentsByAdId(AD_ID);
        }
    }

    @Nested
    @DisplayName("Тесты добавления комментариев")
    class AddCommentTests {

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("Должен создавать комментарий")
        void addComment_ShouldReturnCreatedComment() throws Exception {
            // Given
            CreateOrUpdateCommentDTO requestDto = new CreateOrUpdateCommentDTO();
            requestDto.setText("New comment");

            CommentDTO responseDto = createTestCommentDTO();
            responseDto.setText("New comment");

            when(commentService.addComment(eq(AD_ID), any(CreateOrUpdateCommentDTO.class), eq("user@example.com")))
                    .thenReturn(responseDto);

            // When & Then
            mockMvc.perform(post("/ads/{id}/comments", AD_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(COMMENT_ID))
                    .andExpect(jsonPath("$.text").value("New comment"));

            verify(commentService).addComment(eq(AD_ID), any(CreateOrUpdateCommentDTO.class), eq("user@example.com"));
        }

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("Должен возвращать 404 если объявление не найдено")
        void addComment_WhenAdNotFound_ShouldReturnNotFound() throws Exception {
            // Given
            CreateOrUpdateCommentDTO requestDto = new CreateOrUpdateCommentDTO();
            requestDto.setText("New comment");

            when(commentService.addComment(eq(AD_ID), any(CreateOrUpdateCommentDTO.class), eq("user@example.com")))
                    .thenReturn(null);

            // When & Then
            mockMvc.perform(post("/ads/{id}/comments", AD_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isNotFound());

            verify(commentService).addComment(eq(AD_ID), any(CreateOrUpdateCommentDTO.class), eq("user@example.com"));
        }

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("Должен возвращать 400 при невалидных данных")
        void addComment_WithInvalidData_ShouldReturnBadRequest() throws Exception {
            // Given
            CreateOrUpdateCommentDTO requestDto = new CreateOrUpdateCommentDTO();
            requestDto.setText(""); // empty text - invalid

            // When & Then
            mockMvc.perform(post("/ads/{id}/comments", AD_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest());

            verify(commentService, never()).addComment(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Тесты удаления комментариев")
    class DeleteCommentTests {

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("Должен удалять комментарий с правами доступа")
        void deleteComment_WithPermission_ShouldReturnNoContent() throws Exception {
            // Given
            when(commentService.deleteCommentWithPermission(AD_ID, COMMENT_ID, "user@example.com"))
                    .thenReturn(true);

            // When & Then
            mockMvc.perform(delete("/ads/{adId}/comments/{commentId}", AD_ID, COMMENT_ID)
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(commentService).deleteCommentWithPermission(AD_ID, COMMENT_ID, "user@example.com");
        }

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("Должен возвращать 403 без прав доступа")
        void deleteComment_WithoutPermission_ShouldReturnForbidden() throws Exception {
            // Given
            when(commentService.deleteCommentWithPermission(AD_ID, COMMENT_ID, "user@example.com"))
                    .thenReturn(false);

            // When & Then
            mockMvc.perform(delete("/ads/{adId}/comments/{commentId}", AD_ID, COMMENT_ID)
                            .with(csrf()))
                    .andExpect(status().isForbidden());

            verify(commentService).deleteCommentWithPermission(AD_ID, COMMENT_ID, "user@example.com");
        }
    }

    @Nested
    @DisplayName("Тесты обновления комментариев")
    class UpdateCommentTests {

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("Должен обновлять комментарий с правами доступа")
        void updateComment_WithPermission_ShouldReturnUpdatedComment() throws Exception {
            // Given
            CreateOrUpdateCommentDTO requestDto = new CreateOrUpdateCommentDTO();
            requestDto.setText("Updated text");

            CommentDTO responseDto = createTestCommentDTO();
            responseDto.setText("Updated text");

            when(commentService.updateCommentWithPermission(eq(AD_ID), eq(COMMENT_ID), any(CreateOrUpdateCommentDTO.class), eq("user@example.com")))
                    .thenReturn(responseDto);

            // When & Then
            mockMvc.perform(patch("/ads/{adId}/comments/{commentId}", AD_ID, COMMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.text").value("Updated text"));

            verify(commentService).updateCommentWithPermission(eq(AD_ID), eq(COMMENT_ID), any(CreateOrUpdateCommentDTO.class), eq("user@example.com"));
        }

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("Должен возвращать 403 без прав доступа")
        void updateComment_WithoutPermission_ShouldReturnForbidden() throws Exception {
            // Given
            CreateOrUpdateCommentDTO requestDto = new CreateOrUpdateCommentDTO();
            requestDto.setText("Updated text");

            when(commentService.updateCommentWithPermission(eq(AD_ID), eq(COMMENT_ID), any(CreateOrUpdateCommentDTO.class), eq("user@example.com")))
                    .thenReturn(null);

            // When & Then
            mockMvc.perform(patch("/ads/{adId}/comments/{commentId}", AD_ID, COMMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isForbidden());

            verify(commentService).updateCommentWithPermission(eq(AD_ID), eq(COMMENT_ID), any(CreateOrUpdateCommentDTO.class), eq("user@example.com"));
        }

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("Должен возвращать 400 при невалидных данных")
        void updateComment_WithInvalidData_ShouldReturnBadRequest() throws Exception {
            // Given
            CreateOrUpdateCommentDTO requestDto = new CreateOrUpdateCommentDTO();
            requestDto.setText(""); // empty text - invalid

            // When & Then
            mockMvc.perform(patch("/ads/{adId}/comments/{commentId}", AD_ID, COMMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest());

            verify(commentService, never()).updateCommentWithPermission(any(), any(), any(), any());
        }
    }

    private CommentDTO createTestCommentDTO() {
        CommentDTO dto = new CommentDTO();
        dto.setId(COMMENT_ID);
        dto.setText("Test comment");
        dto.setAuthor(1L);
        dto.setAuthorFirstName("John");
        dto.setAuthorImage("/users/1/avatar");
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }
}