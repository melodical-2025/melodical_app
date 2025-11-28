package com.melodical.backend.service.recommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.melodical.backend.dto.CandidateItem;
import com.melodical.backend.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RecommendationStageLogger {

    private static final Path LOG_DIR = Paths.get("logs");
    private final ObjectMapper mapper = new ObjectMapper();

    public void logStage1Lists(User user,
                               List<CandidateItem> musicOnly,
                               List<CandidateItem> musicalOnly,
                               List<CandidateItem> popularityOnly) {
        try {
            if (!Files.exists(LOG_DIR)) {
                Files.createDirectories(LOG_DIR);
            }

            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("recommendation_stage1_user_%d_%s.json", user.getId(), ts);
            Path out = LOG_DIR.resolve(filename);

            Map<String, Object> root = new HashMap<>();
            root.put("userId", user.getId());
            root.put("timestamp", ts);
            root.put("musicOnly", musicOnly.stream().map(this::toSimpleMap).collect(Collectors.toList()));
            root.put("musicalOnly", musicalOnly.stream().map(this::toSimpleMap).collect(Collectors.toList()));
            root.put("popularityOnly", popularityOnly.stream().map(this::toSimpleMap).collect(Collectors.toList()));

            ObjectNode node = mapper.valueToTree(root);
            Files.writeString(out, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node));

            log.info("Wrote Stage-1 recommendation lists to {}", out.toAbsolutePath());

        } catch (IOException e) {
            log.warn("Failed to write recommendation stage log", e);
        }
    }

    private Map<String, Object> toSimpleMap(CandidateItem c) {
        Map<String,Object> m = new HashMap<>();
        m.put("musicalId", c.getMusicalId());
        m.put("title", c.getTitle());
        m.put("stage1Score", c.getStage1Score());
        m.put("popularityScore", c.getPopularityScore());
        m.put("source", c.getSource());
        return m;
    }
}
