package com.melodical.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 파일 저장 서비스
 * 프로필 이미지 등의 파일 업로드를 처리합니다.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private final Path fileStorageLocation;

    public FileStorageService(@Value("${file.upload-dir:uploads/profile-images}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        
        log.info("🔵 FileStorageService 초기화");
        log.info("파일 저장 경로: {}", this.fileStorageLocation);
        
        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("✅ 파일 저장 디렉토리 생성 완료");
        } catch (Exception ex) {
            log.error("❌ 파일 업로드 디렉토리 생성 실패", ex);
            throw new RuntimeException("파일 업로드 디렉토리를 생성할 수 없습니다.", ex);
        }
    }

    /**
     * 파일 저장
     * @param file 업로드할 파일
     * @return 저장된 파일의 URL 경로
     */
    public String storeFile(MultipartFile file) {
        log.info("🔵 파일 저장 시작");
        log.info("원본 파일명: {}", file.getOriginalFilename());
        log.info("파일 크기: {} bytes", file.getSize());
        log.info("Content-Type: {}", file.getContentType());
        
        // 원본 파일명 검증
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.contains("..")) {
            log.error("❌ 잘못된 파일명: {}", originalFileName);
            throw new IllegalArgumentException("잘못된 파일명입니다.");
        }

        // 파일 확장자 추출
        String fileExtension = "";
        if (originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        log.info("파일 확장자: {}", fileExtension);

        // 고유한 파일명 생성 (UUID + 확장자)
        String fileName = UUID.randomUUID().toString() + fileExtension;
        log.info("생성된 파일명: {}", fileName);

        try {
            // 파일 저장
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            log.info("저장 경로: {}", targetLocation);
            
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("✅ 파일 저장 완료");

            // 저장된 파일의 URL 경로 반환
            String urlPath = "/uploads/profile-images/" + fileName;
            log.info("반환 URL: {}", urlPath);
            return urlPath;
        } catch (IOException ex) {
            log.error("❌ 파일 저장 실패: {}", fileName, ex);
            throw new RuntimeException("파일 저장에 실패했습니다: " + fileName, ex);
        }
    }

    /**
     * 파일 삭제
     * @param fileUrl 삭제할 파일의 URL 경로
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            log.info("삭제할 파일 URL이 없음");
            return;
        }

        log.info("🔵 파일 삭제 시작: {}", fileUrl);
        
        try {
            // URL에서 파일명 추출
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            
            log.info("삭제할 파일 경로: {}", filePath);
            
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("✅ 파일 삭제 완료");
            } else {
                log.warn("⚠️ 파일이 존재하지 않음");
            }
        } catch (IOException ex) {
            log.error("❌ 파일 삭제 실패: {}", fileUrl, ex);
            throw new RuntimeException("파일 삭제에 실패했습니다: " + fileUrl, ex);
        }
    }

    /**
     * 이미지 파일 유효성 검증
     * @param file 검증할 파일
     * @return 유효한 이미지 파일이면 true
     */
    public boolean isValidImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            log.warn("⚠️ 파일이 비어있음");
            return false;
        }

        String contentType = file.getContentType();
        boolean isValid = contentType != null && (
                contentType.equals("image/jpeg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/gif") ||
                contentType.equals("image/webp")
        );
        
        log.info("파일 유효성 검증: {} (Content-Type: {})", isValid, contentType);
        return isValid;
    }
}
