package ru.skypro.homework.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.skypro.homework.dto.Role;
import ru.skypro.homework.dto.user.UserProfileResponse;
import ru.skypro.homework.dto.user.UserProfileUpdateRequest;
import ru.skypro.homework.entity.Ad;
import ru.skypro.homework.entity.Avatar;
import ru.skypro.homework.entity.User;
import ru.skypro.homework.repository.AvatarRepository;
import ru.skypro.homework.repository.UserRepository;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.CREATE_NEW;

/**
 * Сервис для работы с пользователями.
 * Обрабатывает бизнес-логику управления профилями пользователей, аватарами и паролями
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    @Value("${path.to.avatars.folder}")
    private String avatarsDir;

    private final UserRepository userRepository;
    private final AvatarRepository avatarRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Поиск пользователя по имени
     *
     * @param username имя пользователя
     * @return сущность пользователя или null если не найден
     */
    public User findUser(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    /**
     * Получение профиля пользователя
     *
     * @param username имя пользователя
     * @return DTO профиля пользователя или null если пользователь не найден
     */
    public UserProfileResponse getUserProfile(String username) {
        User user = findUser(username);
        if (user == null) {
            return null;
        }
        UserProfileResponse profile = new UserProfileResponse();
        profile.setId(user.getId());
        profile.setEmail(user.getUsername());
        profile.setFirstName(user.getFirstName());
        profile.setLastName(user.getLastName());
        profile.setPhone(user.getPhone());
        profile.setRole(user.getRole().name());

        Avatar avatar = findAvatar(username);
        if (avatar != null) {
            profile.setImage("/users/" + user.getId() + "/avatar");
        }
        return profile;
    }

    /**
     * Обновление профиля пользователя
     *
     * @param username имя пользователя
     * @param request  DTO с обновленными данными
     * @return true если обновление успешно, false если пользователь не найден
     */
    public boolean updateUserProfile(String username, UserProfileUpdateRequest request) {
        User user = findUser(username);
        if (user == null) {
            return false;
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        userRepository.save(user);
        return true;
    }

    /**
     * Проверка прав доступа к объявлению
     *
     * @param ad       сущность объявления
     * @param username имя пользователя
     * @return true если пользователь является админом или автором объявления
     */
    public boolean hasPermission(Ad ad, String username) {
        User user = findUser(username);
        if (user == null) {
            return false;
        }

        if (user.getRole() == Role.ADMIN) {
            return true;
        }

        return ad.getUser().getUsername().equals(username);
    }

    /**
     * Загрузка аватара пользователя
     *
     * @param username имя пользователя
     * @param file     файл аватара
     * @return true если загрузка успешна, false если пользователь не найден
     * @throws IOException              при ошибках работы с файловой системой
     * @throws IllegalArgumentException если файл слишком большой
     */
    public boolean uploadAvatar(String username, MultipartFile file) throws IOException {
        User user = findUser(username);
        if (user == null) {
            return false;
        }

        if (file.getSize() >= 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File is too big");
        }

        uploadAvatarInternal(user, file);
        return true;
    }

    /**
     * Внутренний метод загрузки аватара
     *
     * @param user сущность пользователя
     * @param file файл аватара
     * @throws IOException при ошибках работы с файловой системой
     */
    private void uploadAvatarInternal(User user, MultipartFile file) throws IOException {
        log.info("Uploading avatar for user: {}", user.getUsername());

        String extension = getExtension(file.getOriginalFilename());
        String filename = user.getUsername() + "." + extension;
        Path filePath = Path.of(avatarsDir, filename);

        Files.createDirectories(filePath.getParent());
        Files.deleteIfExists(filePath);

        try (InputStream inputStream = file.getInputStream();
             OutputStream outputStream = Files.newOutputStream(filePath, CREATE_NEW);
             BufferedInputStream bis = new BufferedInputStream(inputStream, 1024);
             BufferedOutputStream bos = new BufferedOutputStream(outputStream, 1024)) {
            bis.transferTo(bos);
        }

        Avatar avatar = findAvatar(user.getUsername());
        if (avatar == null) {
            avatar = new Avatar();
        }
        avatar.setUser(user);
        avatar.setFilePath(filePath.toString());
        avatar.setFileSize(file.getSize());
        avatar.setMediaType(file.getContentType());
        avatar.setData(generateAvatarPreview(filePath));

        avatarRepository.save(avatar);
        log.info("Avatar uploaded successfully for user: {}", user.getUsername());
    }

    /**
     * Поиск аватара пользователя
     *
     * @param username имя пользователя
     * @return сущность аватара или null если не найден
     */
    public Avatar findAvatar(String username) {
        log.debug("Finding avatar for user: {}", username);
        return avatarRepository.findByUser_Username(username).orElse(null);
    }

    /**
     * Генерация превью аватара
     *
     * @param filePath путь к файлу аватара
     * @return массив байтов превью изображения
     * @throws IOException при ошибках чтения/записи изображения
     */
    private byte[] generateAvatarPreview(Path filePath) throws IOException {
        log.debug("Generating avatar preview for: {}", filePath);

        try (InputStream inputStream = Files.newInputStream(filePath);
             BufferedInputStream bis = new BufferedInputStream(inputStream, 1024);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            BufferedImage image = ImageIO.read(bis);
            if (image == null) {
                throw new IOException("Unsupported image format");
            }

            int height = image.getHeight() / (image.getWidth() / 100);
            BufferedImage preview = new BufferedImage(100, height, image.getType());
            Graphics2D graphics = preview.createGraphics();
            graphics.drawImage(image, 0, 0, 100, height, null);
            graphics.dispose();

            ImageIO.write(preview, getExtension(filePath.getFileName().toString()), baos);
            return baos.toByteArray();
        }
    }

    /**
     * Получение расширения файла из имени
     */
    private String getExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * Проверка старого пароля пользователя
     *
     * @param username    имя пользователя
     * @param oldPassword старый пароль для проверки
     * @return true если старый пароль верный, false если нет или пользователь не найден
     */
    public boolean checkOldPassword(String username, String oldPassword) {
        User user = findUser(username);
        return user != null && passwordEncoder.matches(oldPassword, user.getPassword());
    }

    /**
     * Смена пароля пользователя
     *
     * @param username        имя пользователя
     * @param currentPassword текущий пароль
     * @param newPassword     новый пароль
     * @return true если смена успешна, false если текущий пароль неверен
     */
    public boolean changePassword(String username, String currentPassword, String newPassword) {
        if (!checkOldPassword(username, currentPassword)) {
            return false;
        }

        User user = findUser(username);
        if (user != null) {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
        }
        return true;
    }
}