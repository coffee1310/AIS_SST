package org.example.ais_sst.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.entity.CustomUserDetails;
import org.example.ais_sst.service.portfolio.PortfolioService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
@Tag(name = "Portfolio", description = "Загрузка и выгрузка портфолио пользователя")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @PostMapping("/upload")
    @Operation(summary = "Загрузить портфолио",
            description = "Принимает PDF или ZIP с изображениями. Если портфолио уже существует — новые страницы добавляются в конец.")
    public ResponseEntity<String> uploadPortfolio(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            Long userId = userDetails.getId();
            portfolioService.uploadPortfolio(file, userId);
            return ResponseEntity.ok("Портфолио успешно загружено и сохранено");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            log.error("Error uploading portfolio for user {}", userDetails.getId(), e);
            return ResponseEntity.internalServerError().body("Ошибка при обработке файла");
        }
    }

    @GetMapping("/download")
    @Operation(summary = "Скачать портфолио")
    public ResponseEntity<Resource> downloadPortfolio(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            Long userId = userDetails.getId();
            Resource resource = portfolioService.getPortfolioResource(userId);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"portfolio.pdf\"")
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    @Operation(summary = "Проверить наличие портфолио")
    public ResponseEntity<Boolean> hasPortfolio(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getId();
        return ResponseEntity.ok(portfolioService.portfolioExists(userId));
    }

    @DeleteMapping
    @Operation(summary = "Удалить портфолио")
    public ResponseEntity<String> deletePortfolio(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Long userId = userDetails.getId();
            portfolioService.deletePortfolio(userId);
            return ResponseEntity.ok("Портфолио удалено");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Ошибка при удалении");
        }
    }
}