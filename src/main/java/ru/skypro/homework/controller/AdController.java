package ru.skypro.homework.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.skypro.homework.dto.ad.AdFullResponseDTO;
import ru.skypro.homework.dto.ad.AdUpdateRequestDTO;
import ru.skypro.homework.service.AdService;

import java.io.IOException;
import java.util.Map;

/**
 * REST контроллер для управления объявлениями.
 * Обрабатывает операции CRUD для объявлений.
 */
@Slf4j
@RestController
@RequestMapping("/ads")
@RequiredArgsConstructor
@Tag(name = "Объявления")
public class AdController {
    private final AdService adService;

    /**
     * Создание нового объявления
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Добавление объявления")
    public ResponseEntity<AdFullResponseDTO> createAd(
            @RequestParam String title,
            @RequestParam Integer price,
            @RequestParam String description,
            @RequestPart("image") MultipartFile imageFile,
            Authentication authentication) throws IOException {

        log.info("Creating ad for user: {}", authentication.getName());
        AdFullResponseDTO response = adService.createAd(authentication.getName(), title, price, description, imageFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получение списка всех объявлений
     */
    @GetMapping
    @Operation(summary = "Получение всех объявлений")
    public ResponseEntity<Map<String, Object>> getAllAds() {
        Map<String, Object> response = adService.getAllAds();
        return ResponseEntity.ok(response);
    }

    /**
     * Получение полной информации об объявлении по ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Получение информации об объявлении")
    public ResponseEntity<AdFullResponseDTO> getAdById(@PathVariable Long id) {
        AdFullResponseDTO ad = adService.getAdById(id);
        return ad != null ? ResponseEntity.ok(ad) : ResponseEntity.notFound().build();
    }

    /**
     * Удаление объявления по ID
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Удаление объявления")
    public ResponseEntity<Void> deleteAd(@PathVariable Long id, Authentication authentication) {
        boolean deleted = adService.deleteAd(id, authentication.getName());
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * Обновление информации об объявлении по ID
     */
    @PatchMapping("/{id}")
    @Operation(summary = "Обновление информации об объявлении")
    public ResponseEntity<Void> updateAd(@PathVariable Long id,
                                         @RequestBody @Valid AdUpdateRequestDTO updateRequest,
                                         Authentication authentication) {
        boolean updated = adService.updateAd(id, updateRequest, authentication.getName());
        return updated ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * Получение объявлений текущего авторизованного пользователя
     */
    @GetMapping("/me")
    @Operation(summary = "Получение объявлений авторизованного пользователя")
    public ResponseEntity<Map<String, Object>> getMyAds(Authentication auth) {
        Map<String, Object> response = adService.getUserAds(auth.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Обновление изображения объявления
     */
    @PatchMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Обновление картинки объявления")
    public ResponseEntity<Void> updateAdImage(
            @PathVariable Long id,
            @RequestPart("imageFile") MultipartFile imageFile,
            Authentication auth) throws IOException {

        boolean updated = adService.updateAdImage(id, imageFile, auth.getName());
        return updated ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}