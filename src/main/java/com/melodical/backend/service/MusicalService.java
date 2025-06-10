package com.melodical.backend.service;

import com.melodical.backend.dto.MusicalResponseDto;
import com.melodical.backend.entity.Musical;
import com.melodical.backend.entity.RatedMusical;
import com.melodical.backend.repository.MusicalRepository;
import com.melodical.backend.repository.RatedMusicalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MusicalService {
    private final MusicalRepository repo;
    private final RatedMusicalRepository ratedRepo;

    public List<MusicalResponseDto> fetchAllMusicals() {
        return repo.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<MusicalResponseDto> fetchRatedMusicals(Long userId) {
        List<RatedMusical> rated = ratedRepo.findByUserId(userId);
        if (rated.isEmpty()) {
            return Collections.emptyList();
        }
        // 3) DTO로 변환
        return rated.stream()
                .map(RatedMusical::getMusical)
                .filter(m -> m != null)
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    private MusicalResponseDto toDto(Musical m) {
        return MusicalResponseDto.builder()
                .id(m.getId())
                .cast(m.getCast())
                .endDate(m.getEndDate())
                .posterUrl(m.getPosterUrl())
                .runtime(m.getRuntime())
                .startDate(m.getStartDate())
                .theater(m.getTheater())
                .title(m.getTitle())
                .build();
    }
}
