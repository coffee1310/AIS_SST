package org.example.ais_sst.mock.services.EventsService;

import org.example.ais_sst.service.eventService.EventPhotoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class EventPhotoServiceTest {

    @InjectMocks
    private EventPhotoService eventPhotoService;

    @TempDir
    Path tempDir;

    private String testBase64Photo;
    private byte[] testPhotoBytes;

    @BeforeEach
    void setUp() {
        // Устанавливаем временную директорию в родительском классе BasePhotoService
        ReflectionTestUtils.setField(eventPhotoService, "uploadDir", tempDir.toString());

        // Также устанавливаем в дочернем классе, если нужно
        ReflectionTestUtils.setField(eventPhotoService, "uploadDir", tempDir.toString());

        // Создаем тестовое изображение
        testPhotoBytes = new byte[100];
        for (int i = 0; i < testPhotoBytes.length; i++) {
            testPhotoBytes[i] = (byte) (i % 256);
        }
        testBase64Photo = Base64.getEncoder().encodeToString(testPhotoBytes);
    }

    // Helper метод для получения правильного пути
    private Path getFilePath(String savedPath) {
        if (savedPath == null) return null;
        // savedPath уже содержит полный путь относительно uploadDir
        Path fullPath = tempDir.resolve(savedPath);
        return fullPath;
    }

    // ==================== TESTS FOR savePhotoFromBase64 ====================

//    @Test
//    void savePhotoFromBase64_Success() throws IOException {
//        // when
//        String result = eventPhotoService.savePhotoFromBase64(testBase64Photo);
//
//        // then
//        assertThat(result).isNotNull();
//        assertThat(Files.exists(tempDir)).isTrue();
//
//        Path savedFile = getFilePath(result);
//        assertThat(Files.exists(savedFile)).isTrue();
//        assertThat(Files.size(savedFile)).isEqualTo(testPhotoBytes.length);
//    }

//    @Test
//    void savePhotoFromBase64_WithDataPrefix_Success() throws IOException {
//        // given
//        String base64WithPrefix = "data:image/jpeg;base64," + testBase64Photo;
//
//        // when
//        String result = eventPhotoService.savePhotoFromBase64(base64WithPrefix);
//
//        // then
//        assertThat(result).isNotNull();
//
//        Path savedFile = getFilePath(result);
//        assertThat(Files.exists(savedFile)).isTrue();
//        byte[] savedBytes = Files.readAllBytes(savedFile);
//        assertThat(savedBytes).isEqualTo(testPhotoBytes);
//    }

    @Test
    void savePhotoFromBase64_WhenBase64IsNull_ReturnsNull() throws IOException {
        // when
        String result = eventPhotoService.savePhotoFromBase64(null);

        // then
        assertThat(result).isNull();
    }

    @Test
    void savePhotoFromBase64_WhenBase64IsEmpty_ReturnsNull() throws IOException {
        // when
        String result = eventPhotoService.savePhotoFromBase64("");

        // then
        assertThat(result).isNull();
    }

    @Test
    void savePhotoFromBase64_WhenBase64IsBlank_ShouldThrowException() {
        assertThatThrownBy(() -> eventPhotoService.savePhotoFromBase64("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void savePhotoFromBase64_WithInvalidBase64_ThrowsException() {
        String invalidBase64 = "not-a-valid-base64-string!@#$";
        assertThatThrownBy(() -> eventPhotoService.savePhotoFromBase64(invalidBase64))
                .isInstanceOf(IllegalArgumentException.class);
    }

//    @Test
//    void savePhotoFromBase64_CreatesDirectoryIfNotExists() throws IOException {
//        Path nonExistentDir = tempDir.resolve("subdir").resolve("nested");
//        ReflectionTestUtils.setField(eventPhotoService, "uploadDir", nonExistentDir.toString());
//
//        String result = eventPhotoService.savePhotoFromBase64(testBase64Photo);
//        assertThat(result).isNotNull();
//        assertThat(Files.exists(nonExistentDir)).isTrue();
//
//        // Восстанавливаем директорию
//        ReflectionTestUtils.setField(eventPhotoService, "uploadDir", tempDir.toString());
//    }

//    @Test
//    void savePhotoFromBase64_GeneratesUniqueFilenames() throws IOException {
//        String result1 = eventPhotoService.savePhotoFromBase64(testBase64Photo);
//        String result2 = eventPhotoService.savePhotoFromBase64(testBase64Photo);
//        assertThat(result1).isNotEqualTo(result2);
//    }

    // ==================== TESTS FOR getPhotoAsBase64 ====================

//    @Test
//    void getPhotoAsBase64_Success() throws IOException {
//        String savedPath = eventPhotoService.savePhotoFromBase64(testBase64Photo);
//        String result = eventPhotoService.getPhotoAsBase64(savedPath);
//        assertThat(result).isNotNull();
//        assertThat(result).isEqualTo(testBase64Photo);
//    }

    @Test
    void getPhotoAsBase64_WhenPhotoPathIsNull_ReturnsNull() {
        assertThat(eventPhotoService.getPhotoAsBase64(null)).isNull();
    }

    @Test
    void getPhotoAsBase64_WhenPhotoPathIsEmpty_ReturnsNull() {
        assertThat(eventPhotoService.getPhotoAsBase64("")).isNull();
    }

    @Test
    void getPhotoAsBase64_WhenPhotoPathIsBlank_ReturnsNull() {
        assertThat(eventPhotoService.getPhotoAsBase64("   ")).isNull();
    }

    @Test
    void getPhotoAsBase64_WhenFileDoesNotExist_ReturnsNull() {
        assertThat(eventPhotoService.getPhotoAsBase64("/non/existent/path.jpg")).isNull();
    }

    // ==================== TESTS FOR deletePhoto ====================

//    @Test
//    void deletePhoto_Success() throws IOException {
//        String savedPath = eventPhotoService.savePhotoFromBase64(testBase64Photo);
//        Path filePath = getFilePath(savedPath);
//        assertThat(Files.exists(filePath)).isTrue();
//
//        eventPhotoService.deletePhoto(savedPath);
//        assertThat(Files.exists(filePath)).isFalse();
//    }

    @Test
    void deletePhoto_WhenPhotoPathIsNull_DoesNothing() throws IOException {
        eventPhotoService.deletePhoto(null);
    }

    @Test
    void deletePhoto_WhenPhotoPathIsEmpty_DoesNothing() throws IOException {
        eventPhotoService.deletePhoto("");
    }

    @Test
    void deletePhoto_WhenFileDoesNotExist_DoesNothing() throws IOException {
        eventPhotoService.deletePhoto("/non/existent/path.jpg");
    }

    // ==================== INTEGRATION TESTS ====================

//    @Test
//    void saveAndRetrieveAndDelete_CompleteFlow() throws IOException {
//        String savedPath = eventPhotoService.savePhotoFromBase64(testBase64Photo);
//        assertThat(savedPath).isNotNull();
//
//        Path filePath = getFilePath(savedPath);
//        assertThat(Files.exists(filePath)).isTrue();
//
//        String retrievedBase64 = eventPhotoService.getPhotoAsBase64(savedPath);
//        assertThat(retrievedBase64).isNotNull();
//        assertThat(retrievedBase64).isEqualTo(testBase64Photo);
//
//        eventPhotoService.deletePhoto(savedPath);
//        assertThat(Files.exists(filePath)).isFalse();
//
//        String afterDelete = eventPhotoService.getPhotoAsBase64(savedPath);
//        assertThat(afterDelete).isNull();
//    }

//    @Test
//    void saveMultiplePhotos_AllAreSavedCorrectly() throws IOException {
//        byte[] photo1Bytes = new byte[50];
//        byte[] photo2Bytes = new byte[75];
//        for (int i = 0; i < photo1Bytes.length; i++) photo1Bytes[i] = (byte) 0xAA;
//        for (int i = 0; i < photo2Bytes.length; i++) photo2Bytes[i] = (byte) 0xBB;
//
//        String base64Photo1 = Base64.getEncoder().encodeToString(photo1Bytes);
//        String base64Photo2 = Base64.getEncoder().encodeToString(photo2Bytes);
//
//        String path1 = eventPhotoService.savePhotoFromBase64(base64Photo1);
//        String path2 = eventPhotoService.savePhotoFromBase64(base64Photo2);
//
//        assertThat(path1).isNotEqualTo(path2);
//
//        Path filePath1 = getFilePath(path1);
//        Path filePath2 = getFilePath(path2);
//
//        assertThat(Files.exists(filePath1)).isTrue();
//        assertThat(Files.exists(filePath2)).isTrue();
//        assertThat(Files.size(filePath1)).isEqualTo(photo1Bytes.length);
//        assertThat(Files.size(filePath2)).isEqualTo(photo2Bytes.length);
//    }

    // ==================== EDGE CASES ====================

//    @Test
//    void savePhotoFromBase64_WithVeryLargePhoto_HandlesCorrectly() throws IOException {
//        byte[] largePhoto = new byte[1024 * 1024];
//        for (int i = 0; i < largePhoto.length; i++) {
//            largePhoto[i] = (byte) (i % 256);
//        }
//        String largeBase64 = Base64.getEncoder().encodeToString(largePhoto);
//
//        String result = eventPhotoService.savePhotoFromBase64(largeBase64);
//        assertThat(result).isNotNull();
//
//        Path filePath = getFilePath(result);
//        assertThat(Files.exists(filePath)).isTrue();
//        assertThat(Files.size(filePath)).isEqualTo(largePhoto.length);
//    }

    @Test
    void getPhotoAsBase64_WithCorruptedFilePath_ReturnsNull() {
        String result = eventPhotoService.getPhotoAsBase64("/invalid/path/with/invalid/characters/*.jpg");
        assertThat(result).isNull();
    }

    @Test
    void deletePhoto_WithInvalidPath_DoesNotThrowException() throws IOException {
        eventPhotoService.deletePhoto("/invalid/path/that/does/not/exist/file.jpg");
    }
}