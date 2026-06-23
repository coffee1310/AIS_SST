package org.example.ais_sst.service.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    @Value("${portfolio.storage.path:storage/portfolios}")
    private String baseStoragePath;

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf",
            "application/zip",
            "application/x-zip-compressed"
    );

    private static final List<String> IMAGE_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "bmp");

    /**
     * Загрузка портфолио (PDF или ZIP с фото)
     */
    public void uploadPortfolio(MultipartFile file, Long userId) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Файл не может быть пустым");
        }

        String contentType = file.getContentType();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Поддерживаются только PDF и ZIP файлы");
        }

        Path userPortfolioDir = getUserPortfolioDirectory(userId);
        Files.createDirectories(userPortfolioDir);

        Path existingPortfolio = userPortfolioDir.resolve("portfolio.pdf");
        boolean hasExisting = Files.exists(existingPortfolio);

        Path tempFile = Files.createTempFile("upload_", file.getOriginalFilename());

        try {
            file.transferTo(tempFile);

            if (contentType != null && contentType.contains("pdf")) {
                handlePdfUpload(tempFile, existingPortfolio, hasExisting);
            } else {
                handleZipUpload(tempFile, existingPortfolio, hasExisting, userId);
            }

            log.info("Portfolio uploaded successfully for user {}", userId);

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private void handlePdfUpload(Path newPdf, Path existingPortfolio, boolean hasExisting) throws IOException {
        if (!hasExisting) {
            Files.copy(newPdf, existingPortfolio, StandardCopyOption.REPLACE_EXISTING);
        } else {
            mergePdfs(existingPortfolio, newPdf);
        }
    }

    private void handleZipUpload(Path zipFile, Path existingPortfolio, boolean hasExisting, Long userId) throws IOException {
        List<Path> extractedImages = extractImagesFromZip(zipFile);

        if (extractedImages.isEmpty()) {
            throw new IllegalArgumentException("В ZIP-архиве не найдено изображений");
        }

        Path newPdfFromImages = createPdfFromImages(extractedImages, userId);

        if (!hasExisting) {
            Files.move(newPdfFromImages, existingPortfolio, StandardCopyOption.REPLACE_EXISTING);
        } else {
            mergePdfs(existingPortfolio, newPdfFromImages);
            Files.deleteIfExists(newPdfFromImages);
        }

        // Удаляем временные изображения
        for (Path img : extractedImages) {
            Files.deleteIfExists(img);
        }
    }

    /**
     * Извлекает изображения из ZIP во временную папку
     */
    private List<Path> extractImagesFromZip(Path zipPath) throws IOException {
        List<Path> images = new ArrayList<>();
        Path tempDir = Files.createTempDirectory("portfolio_images_");

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String fileName = entry.getName().toLowerCase();
                    String extension = getFileExtension(fileName);

                    if (IMAGE_EXTENSIONS.contains(extension)) {
                        Path targetPath = tempDir.resolve(entry.getName().replace("/", "_"));
                        Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        images.add(targetPath);
                    }
                }
                zis.closeEntry();
            }
        }
        return images;
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(dotIndex + 1) : "";
    }

    /**
     * Создаёт PDF из списка изображений
     */
    private Path createPdfFromImages(List<Path> imagePaths, Long userId) throws IOException {
        Path outputPdf = Files.createTempFile("portfolio_from_images_", ".pdf");

        try (PDDocument document = new PDDocument()) {
            for (Path imagePath : imagePaths) {
                BufferedImage bufferedImage = ImageIO.read(imagePath.toFile());
                if (bufferedImage == null) continue;

                PDPage page = new PDPage(new PDRectangle(bufferedImage.getWidth(), bufferedImage.getHeight()));
                document.addPage(page);

                PDImageXObject pdImage = PDImageXObject.createFromFileByContent(imagePath.toFile(), document);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.drawImage(pdImage, 0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
                }
            }
            document.save(outputPdf.toFile());
        }
        return outputPdf;
    }

    /**
     * Объединяет два PDF файла (добавляет страницы нового в конец существующего)
     */
    private void mergePdfs(Path targetPdf, Path sourcePdf) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        merger.addSource(targetPdf.toFile());
        merger.addSource(sourcePdf.toFile());
        merger.setDestinationFileName(targetPdf.toString());
        merger.mergeDocuments(null);
    }

    private Path getUserPortfolioDirectory(Long userId) {
        return Paths.get(baseStoragePath, String.valueOf(userId));
    }

    /**
     * Возвращает портфолио пользователя как Resource
     */
    public Resource getPortfolioResource(Long userId) throws IOException {
        Path portfolioPath = getUserPortfolioDirectory(userId).resolve("portfolio.pdf");

        if (!Files.exists(portfolioPath)) {
            throw new FileNotFoundException("Портфолио не найдено для пользователя " + userId);
        }

        return new FileSystemResource(portfolioPath);
    }

    public boolean portfolioExists(Long userId) {
        Path path = getUserPortfolioDirectory(userId).resolve("portfolio.pdf");
        return Files.exists(path);
    }

    public void deletePortfolio(Long userId) throws IOException {
        Path dir = getUserPortfolioDirectory(userId);
        if (Files.exists(dir)) {
            try (var stream = Files.walk(dir)) {
                stream.sorted((a, b) -> -a.compareTo(b))
                        .forEach(path -> {
                            try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                        });
            }
        }
    }
}