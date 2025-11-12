package ru.skypro.homework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.skypro.homework.dto.comment.CommentDTO;
import ru.skypro.homework.dto.comment.CommentsDTO;
import ru.skypro.homework.dto.comment.CreateOrUpdateCommentDTO;
import ru.skypro.homework.service.CommentService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Тестирование контроллера комментариев")
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    @Nested
    @DisplayName("Тесты получения комментариев")
    class GetCommentTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Успешное получение комментариев к объявлению")
        void getComments_Success() throws Exception {
            // Given
            CommentsDTO commentsDTO = new CommentsDTO();
            commentsDTO.setCount(2);
            commentsDTO.setResults(List.of());

            when(commentService.getCommentsByAdId(1L)).thenReturn(commentsDTO);

            // When & Then
            mockMvc.perform(get("/ads/1/comments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(2));
        }
    }

    @Nested
    @DisplayName("Тесты создания комментариев")
    class CreateCommentTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Успешное добавление комментария")
        void addComment_Success() throws Exception {
            // Given
            CreateOrUpdateCommentDTO createDTO = new CreateOrUpdateCommentDTO();
            createDTO.setText("Test comment");

            CommentDTO commentDTO = new CommentDTO();
            commentDTO.setId(1L);
            commentDTO.setText("Test comment");
            commentDTO.setAuthorFirstName("John");
            commentDTO.setAuthor(1L);
            commentDTO.setCreatedAt(System.currentTimeMillis());

            when(commentService.addComment(eq(1L), any(CreateOrUpdateCommentDTO.class), eq("testuser")))
                    .thenReturn(commentDTO);

            // When & Then
            mockMvc.perform(post("/ads/1/comments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.text").value("Test comment"))
                    .andExpect(jsonPath("$.authorFirstName").value("John"));
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Ошибка добавления комментария - объявление не найдено")
        void addComment_AdNotFound() throws Exception {
            // Given
            CreateOrUpdateCommentDTO createDTO = new CreateOrUpdateCommentDTO();
            createDTO.setText("Test comment");

            when(commentService.addComment(eq(1L), any(CreateOrUpdateCommentDTO.class), eq("testuser")))
                    .thenReturn(null);

            // When & Then
            mockMvc.perform(post("/ads/1/comments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Тесты удаления комментариев")
    class DeleteCommentTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Успешное удаление комментария")
        void deleteComment_Success() throws Exception {
            // Given
            when(commentService.deleteCommentWithPermission(1L, 1L, "testuser"))
                    .thenReturn(true);

            // When & Then
            mockMvc.perform(delete("/ads/1/comments/1")
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Ошибка удаления комментария - недостаточно прав")
        void deleteComment_Forbidden() throws Exception {
            // Given
            when(commentService.deleteCommentWithPermission(1L, 1L, "testuser"))
                    .thenReturn(false);

            // When & Then
            mockMvc.perform(delete("/ads/1/comments/1")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Тесты обновления комментариев")
    class UpdateCommentTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Успешное обновление комментария")
        void updateComment_Success() throws Exception {
            // Given
            CreateOrUpdateCommentDTO updateDTO = new CreateOrUpdateCommentDTO();
            updateDTO.setText("Updated comment");

            CommentDTO updatedComment = new CommentDTO();
            updatedComment.setId(1L);
            updatedComment.setText("Updated comment");
            updatedComment.setAuthorFirstName("John");
            updatedComment.setAuthor(1L);
            updatedComment.setCreatedAt(System.currentTimeMillis());

            when(commentService.updateCommentWithPermission(eq(1L), eq(1L), any(CreateOrUpdateCommentDTO.class), eq("testuser")))
                    .thenReturn(updatedComment);

            // When & Then
            mockMvc.perform(patch("/ads/1/comments/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.text").value("Updated comment"));
        }
    }
}