package com.melodical.backend.service.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melodical.backend.entity.CrawledMusicalRanking;
import com.melodical.backend.repository.CrawledMusicalRankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 뮤지컬 데이터 크롤링 스케줄러 서비스
 * - 시스템 시작 시 1회 실행
 * - 이후 24시간마다 자동 실행
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MusicalCrawlerService {

    private final CrawledMusicalRankingRepository crawledRepository;
    private final ObjectMapper objectMapper;
    private final MusicalSyncService musicalSyncService;

    private static final String CRAWLER_BASE_PATH = "crawling";
    private static final String CRAWLER_SCRIPT = "src/main.py";
    private static final String DATA_DIR = "data";

    /**
     * 시스템 시작 시 기존 데이터 로드 및 Musical 동기화
     * ApplicationReadyEvent를 사용하여 서버가 완전히 준비된 후 비동기로 실행
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void initializeCrawling() {
        log.info("System ready - Loading existing crawled data asynchronously...");
        try {
            // 최신 크롤링 데이터 파일 찾기
            String latestVersion = findLatestDataVersion();

            if (latestVersion != null) {
                log.info("Found latest data version: {}", latestVersion);

                // integrated_weekly_dataset 저장
                String weeklyFile = String.format("%s/%s/integrated_weekly_dataset_%s.json",
                        CRAWLER_BASE_PATH, DATA_DIR, latestVersion);
                saveIntegratedDataset(weeklyFile, "WEEKLY", latestVersion);

                // integrated_monthly_dataset 저장
                String monthlyFile = String.format("%s/%s/integrated_monthly_dataset_%s.json",
                        CRAWLER_BASE_PATH, DATA_DIR, latestVersion);
                saveIntegratedDataset(monthlyFile, "MONTHLY", latestVersion);

                log.info("Successfully loaded existing crawled data");

                // 크롤링 데이터를 Musical 엔티티와 동기화
                log.info("Starting Musical entity synchronization...");
                int syncCount = musicalSyncService.syncCrawledDataToMusicals();
                log.info("Musical synchronization completed: {} musicals synced", syncCount);

            } else {
                log.warn("No existing crawled data found. Please run crawler manually.");
            }
        } catch (Exception e) {
            log.error("Failed to load existing crawled data", e);
        }
    }

    /**
     * 24시간마다 크롤링 실행 (매일 오전 3시)
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledCrawling() {
        log.info("Scheduled crawling started at {}", LocalDateTime.now());
        try {
            runCrawlerAndSaveData();
        } catch (Exception e) {
            log.error("Scheduled crawling failed", e);
        }
    }

    /**
     * 크롤러 실행 및 데이터 저장
     */
    @Transactional
    public void runCrawlerAndSaveData() {
        try {
            log.info("Starting musical data crawling...");

            // 1. Python 크롤러 실행
            boolean crawlSuccess = executePythonCrawler();

            if (!crawlSuccess) {
                log.error("Crawler execution failed");
                return;
            }

            // 2. 최신 크롤링 데이터 파일 찾기
            String latestVersion = findLatestDataVersion();

            if (latestVersion == null) {
                log.error("No crawled data files found");
                return;
            }

            log.info("Found latest data version: {}", latestVersion);

            // 3. integrated_weekly_dataset 저장
            String weeklyFile = String.format("%s/%s/integrated_weekly_dataset_%s.json",
                    CRAWLER_BASE_PATH, DATA_DIR, latestVersion);
            saveIntegratedDataset(weeklyFile, "WEEKLY", latestVersion);

            // 4. integrated_monthly_dataset 저장
            String monthlyFile = String.format("%s/%s/integrated_monthly_dataset_%s.json",
                    CRAWLER_BASE_PATH, DATA_DIR, latestVersion);
            saveIntegratedDataset(monthlyFile, "MONTHLY", latestVersion);

            // 5. 오래된 데이터 정리 (최근 3개 버전만 유지)
            cleanupOldData(3);

            log.info("Crawling and data saving completed successfully");

        } catch (Exception e) {
            log.error("Error during crawling process", e);
            throw new RuntimeException("Crawling process failed", e);
        }
    }

    /**
     * Python 크롤러 실행
     */
    private boolean executePythonCrawler() {
        try {
            // 크롤러 디렉토리로 이동하여 실행
            ProcessBuilder pb = new ProcessBuilder(
                    "python3",
                    CRAWLER_SCRIPT
            );

            pb.directory(new File(CRAWLER_BASE_PATH + "/src"));
            pb.redirectErrorStream(true);

            log.info("Executing Python crawler...");
            Process process = pb.start();

            // 실행 로그 출력
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("Crawler output: {}", line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("Crawler execution completed successfully");
                return true;
            } else {
                log.error("Crawler execution failed with exit code: {}", exitCode);
                return false;
            }

        } catch (Exception e) {
            log.error("Failed to execute Python crawler", e);
            return false;
        }
    }

    /**
     * 최근 7일 내 가장 많은 데이터를 포함한 버전 찾기
     * 
     * 방법론:
     * 1. 최근 7일 내 크롤링된 데이터셋 파일들을 탐색
     * 2. 각 버전의 데이터 개수를 카운트
     * 3. 가장 많은 데이터를 포함한 버전을 선택
     */
    private String findLatestDataVersion() {
        try {
            Path dataPath = Paths.get(CRAWLER_BASE_PATH, DATA_DIR);

            if (!Files.exists(dataPath)) {
                log.error("Data directory does not exist: {}", dataPath);
                return null;
            }

            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            
            String bestVersion = null;
            int maxDataCount = 0;

            // integrated_weekly_dataset 파일들 찾기
            try (Stream<Path> paths = Files.list(dataPath)) {
                List<Path> weeklyFiles = paths
                        .filter(path -> path.getFileName().toString().startsWith("integrated_weekly_dataset_"))
                        .filter(path -> path.getFileName().toString().endsWith(".json"))
                        .toList();

                log.info("🔍 Scanning {} data versions within last 7 days...", weeklyFiles.size());

                for (Path file : weeklyFiles) {
                    String filename = file.getFileName().toString();
                    // integrated_weekly_dataset_20251104_163553.json -> 20251104_163553
                    String version = filename.replace("integrated_weekly_dataset_", "")
                            .replace(".json", "");

                    try {
                        // 버전에서 날짜/시간 파싱
                        LocalDateTime fileTime = LocalDateTime.parse(version, formatter);

                        // 7일 이내 데이터만 고려
                        if (fileTime.isBefore(sevenDaysAgo)) {
                            log.debug("⏭️  Skipping old version: {} (older than 7 days)", version);
                            continue;
                        }

                        // JSON 파일의 데이터 개수 카운트
                        String jsonContent = Files.readString(file);
                        JsonNode rootNode = objectMapper.readTree(jsonContent);
                        
                        int dataCount = 0;
                        if (rootNode.isArray()) {
                            dataCount = rootNode.size();
                        }

                        log.info("📊 Version {}: {} items", version, dataCount);

                        // 가장 많은 데이터를 가진 버전 선택
                        if (dataCount > maxDataCount) {
                            maxDataCount = dataCount;
                            bestVersion = version;
                        }

                    } catch (Exception e) {
                        log.warn("⚠️  Failed to parse or read file: {}", filename, e);
                    }
                }

                if (bestVersion != null) {
                    log.info("✅ Selected best data version: {} with {} items", bestVersion, maxDataCount);
                } else {
                    log.warn("⚠️  No valid data version found within last 7 days");
                }

                return bestVersion;
            }

        } catch (Exception e) {
            log.error("Error finding latest data version", e);
            return null;
        }
    }

    /**
     * 통합 데이터셋 저장
     */
    private void saveIntegratedDataset(String filePath, String rankingType, String dataVersion) {
        try {
            Path path = Paths.get(filePath);

            if (!Files.exists(path)) {
                log.warn("Data file not found: {}", filePath);
                return;
            }

            log.info("Loading data from: {}", filePath);

            // JSON 파일 읽기
            String jsonContent = Files.readString(path);
            JsonNode rootNode = objectMapper.readTree(jsonContent);

            List<CrawledMusicalRanking> rankings = new ArrayList<>();
            LocalDateTime crawledAt = LocalDateTime.now();

            // JSON 배열 파싱
            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
                    CrawledMusicalRanking ranking = parseMusicalData(node, rankingType, dataVersion, crawledAt);
                    if (ranking != null) {
                        rankings.add(ranking);
                    }
                }
            }

            // DB에 저장
            if (!rankings.isEmpty()) {
                crawledRepository.saveAll(rankings);
                log.info("Saved {} {} rankings to database", rankings.size(), rankingType);
            } else {
                log.warn("No data to save for {}", rankingType);
            }

        } catch (Exception e) {
            log.error("Error saving integrated dataset: {}", filePath, e);
        }
    }

    /**
     * JSON 노드를 CrawledMusicalRanking 엔티티로 변환
     */
    private CrawledMusicalRanking parseMusicalData(JsonNode node, String rankingType,
                                                   String dataVersion, LocalDateTime crawledAt) {
        try {
            // rank 또는 final_rank 사용
            Integer combinedRank = getIntValue(node, "rank");
            if (combinedRank == null) {
                combinedRank = getIntValue(node, "final_rank");
            }

            // posterUrl 처리
            String imageUrl = getStringValue(node, "image_url");
            log.debug("Processing musical: {} - image_url: {}",
                     getStringValue(node, "title"), imageUrl);

            // Interpark URL에서 ID 추출 (마지막 8자리 숫자)
            String interparkUrl = getStringValue(node, "detail_url");
            String interparkId = extractInterparkId(interparkUrl);

            return CrawledMusicalRanking.builder()
                    .title(getStringValue(node, "title"))
                    .interparkId(interparkId)
                    .normalizedTitle(getStringValue(node, "normalized_title"))
                    .interparkTitle(getStringValue(node, "interpark_title"))
                    .yes24Title(getStringValue(node, "yes24_title"))
                    .rankingType(rankingType)
                    .combinedRank(combinedRank)
                    .interparkRank(getIntValue(node, "interpark_rank"))
                    .yes24Rank(getIntValue(node, "yes24_rank"))
                    .averageRating(getDoubleValue(node, "average_rating"))
                    .interparkRating(getDoubleValue(node, "interpark_rating"))
                    .yes24Rating(getDoubleValue(node, "yes24_rating"))
                    .totalReviews(getIntValue(node, "total_reviews"))
                    .interparkReviews(getIntValue(node, "interpark_reviews"))
                    .yes24Reviews(getIntValue(node, "yes24_reviews"))
                    .theaterName(getStringValue(node, "venue"))
                    .performancePeriod(getStringValue(node, "performance_period"))
                    .genre(getStringValue(node, "genre"))
                    .description(getStringValue(node, "description"))
                    .posterUrl(imageUrl)
                    .interparkUrl(interparkUrl)
                    .yes24Url(getStringValue(node, "yes24_detail_url"))
                    .isAvailable(getBooleanValue(node, "is_available"))
                    .crawledAt(crawledAt)
                    .dataVersion(dataVersion)
                    .build();
        } catch (Exception e) {
            log.error("Error parsing musical data", e);
            return null;
        }
    }

    /**
     * Interpark URL에서 마지막 8자리 ID 추출
     * 예: http://ticket.interpark.com/goods/24012345 -> 24012345
     */
    private String extractInterparkId(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        try {
            // URL에서 숫자만 추출
            String[] parts = url.split("/");
            String lastPart = parts[parts.length - 1];
            
            // 쿼리 파라미터 제거
            if (lastPart.contains("?")) {
                lastPart = lastPart.substring(0, lastPart.indexOf("?"));
            }
            
            // 숫자만 추출
            String numbers = lastPart.replaceAll("[^0-9]", "");
            
            // 8자리 이상이면 마지막 8자리 반환
            if (numbers.length() >= 8) {
                return numbers.substring(numbers.length() - 8);
            } else if (numbers.length() > 0) {
                return numbers; // 8자리보다 짧으면 전체 반환
            }
            
            return null;
        } catch (Exception e) {
            log.warn("Failed to extract Interpark ID from URL: {}", url, e);
            return null;
        }
    }

    // JSON 파싱 헬퍼 메서드들
    private String getStringValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : null;
    }

    private Integer getIntValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asInt() : null;
    }

    private Double getDoubleValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asDouble() : null;
    }

    private Boolean getBooleanValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asBoolean() : null;
    }

    /**
     * 오래된 데이터 정리
     */
    @Transactional
    public void cleanupOldData(int keepVersions) {
        try {
            List<String> allVersions = crawledRepository.findAllDataVersions();

            if (allVersions.size() <= keepVersions) {
                log.info("No old data to cleanup. Current versions: {}", allVersions.size());
                return;
            }

            // 최신 N개 제외하고 삭제
            List<String> versionsToDelete = allVersions.subList(keepVersions, allVersions.size());

            for (String version : versionsToDelete) {
                log.info("Deleting old data version: {}", version);
                crawledRepository.deleteByDataVersion(version);
            }

            log.info("Cleanup completed. Kept {} versions, deleted {} versions",
                    keepVersions, versionsToDelete.size());

        } catch (Exception e) {
            log.error("Error during data cleanup", e);
        }
    }
}

