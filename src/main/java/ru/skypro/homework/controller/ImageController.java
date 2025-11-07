package ru.skypro.homework.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.skypro.homework.repository.AvatarRepository;
import ru.skypro.homework.repository.AdRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Контроллер для получения изображений (аватаров пользователей и картинок объявлений)
 * Обеспечивает доступ к загруженным файлам через REST API
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Изображения", description = "API для работы с изображениями пользователей и объявлений")
public class ImageController {

    private final AvatarRepository avatarRepository;
    private final AdRepository adRepository;

    /**
     * Получение аватара пользователя по ID пользователя
     *
     * @param userId ID пользователя
     * @return массив байтов изображения аватара
     * @throws IOException при ошибках чтения файла
     */
    @GetMapping(value = "/users/{userId}/avatar",
            produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_GIF_VALUE})
    @Operation(summary = "Получение аватара пользователя", description = "Возвращает аватар пользователя по его ID")
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long userId) throws IOException {
        log.debug("Getting avatar for user ID: {}", userId);

        var avatar = avatarRepository.findByUser_Id(userId).orElse(null);
        if (avatar == null) {
            log.warn("Avatar not found for user ID: {}", userId);
            return ResponseEntity.notFound().build();
        }

        byte[] image = Files.readAllBytes(Path.of(avatar.getFilePath()));
        log.debug("Successfully retrieved avatar for user ID: {}, size: {} bytes", userId, image.length);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatar.getMediaType()))
                .body(image);
    }

    /**
     * Получение изображения объявления по ID объявления
     *
     * @param adId ID объявления
     * @return массив байтов изображения объявления
     * @throws IOException при ошибках чтения файла
     */
    @GetMapping(value = "/ads/{adId}/image",
            produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_GIF_VALUE})
    @Operation(summary = "Получение изображения объявления", description = "Возвращает изображение объявления по его ID")
    public ResponseEntity<byte[]> getAdImage(@PathVariable Long adId) throws IOException {
        log.debug("Getting image for ad ID: {}", adId);

        var ad = adRepository.findById(adId).orElse(null);
        if (ad == null || ad.getFilePath() == null) {
            log.warn("Ad image not found for ad ID: {}", adId);
            return ResponseEntity.notFound().build();
        }

        byte[] image = Files.readAllBytes(Path.of(ad.getFilePath()));
        log.debug("Successfully retrieved image for ad ID: {}, size: {} bytes", adId, image.length);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ad.getMediaType()))
                .body(image);
    }
}