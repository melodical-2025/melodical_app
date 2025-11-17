package com.melodical.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링 설정
 * 사용자 프로필 정규화, 뮤지컬 팬 프로필 업데이트 등의 배치 작업 활성화
 */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {
    // 기본 설정으로 충분함
    // 필요시 커스텀 TaskScheduler나 TaskExecutor 빈 추가 가능
}
