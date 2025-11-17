"""
뮤지컬 데이터 통합 모듈
인터파크와 Yes24 데이터를 통합하여 하나의 데이터셋으로 구성
"""

import json
import logging
from typing import List, Dict, Optional
from datetime import datetime
from difflib import SequenceMatcher
import re


class MusicalDataIntegrator:
    """뮤지컬 데이터 통합 클래스"""
    
    def __init__(self):
        self.logger = logging.getLogger("integrator")
    
    def normalize_title(self, title: str) -> str:
        """제목 정규화 (비교를 위해)"""
        if not title:
            return ""
        
        normalized = title
        
        # 1. 지역 정보 제거 ([서울], [부산] 등)
        normalized = re.sub(r'\[.*?지역.*?\]', '', normalized)
        normalized = re.sub(r'\[(서울|부산|대구|인천|광주|대전|울산|경기|강원|충북|충남|전북|전남|경북|경남|제주|용인|창원|김해)\]', '', normalized)
        
        # 2. 특수 정보 제거 (날짜, 이벤트 정보 등)
        normalized = re.sub(r'\d{4}\s*\.\s*\d{1,2}\s*\.\s*\d{1,2}.*', '', normalized)
        normalized = re.sub(r'YES24DAY', '', normalized, flags=re.IGNORECASE)
        
        # 3. 괄호 내용은 제거하되, 괄호 안의 핵심 제목은 보존
        # 예: "태양의서커스 [쿠자]" → "태양의서커스 쿠자"
        normalized = normalized.replace('[', ' ').replace(']', ' ')
        normalized = normalized.replace('(', ' ').replace(')', ' ')
        normalized = normalized.replace('〈', ' ').replace('〉', ' ')
        normalized = normalized.replace('<', ' ').replace('>', ' ')
        
        # 4. "뮤지컬" 단어 제거
        normalized = re.sub(r'뮤지컬', '', normalized, flags=re.IGNORECASE)
        normalized = re.sub(r'musical', '', normalized, flags=re.IGNORECASE)
        
        # 5. 특수문자 제거 (단, 한글/영문/숫자/공백은 유지)
        normalized = re.sub(r'[^\w\s가-힣a-zA-Z0-9]', ' ', normalized)
        
        # 6. 연속된 공백을 하나로
        normalized = re.sub(r'\s+', ' ', normalized)
        
        # 7. 소문자 변환 및 앞뒤 공백 제거
        return normalized.lower().strip()
    
    def calculate_similarity(self, title1: str, title2: str) -> float:
        """두 제목 간의 유사도 계산 (개선된 버전)"""
        norm1 = self.normalize_title(title1)
        norm2 = self.normalize_title(title2)
        
        # 빈 문자열 처리
        if not norm1 or not norm2:
            return 0.0
        
        # 완전 일치
        if norm1 == norm2:
            return 1.0
        
        # 단어 분리
        words1 = set(norm1.split())
        words2 = set(norm2.split())
        
        # 공통 단어가 있는지 확인
        common_words = words1 & words2
        if common_words:
            # 공통 단어가 있으면 높은 점수
            # 두 제목의 핵심 단어가 겹치면 매칭으로 간주
            shorter_word_count = min(len(words1), len(words2))
            if len(common_words) >= shorter_word_count * 0.5:  # 50% 이상 겹치면
                return 0.85 + (len(common_words) / max(len(words1), len(words2))) * 0.15
        
        # 부분 일치 (한쪽이 다른 쪽을 포함)
        if norm1 in norm2 or norm2 in norm1:
            shorter = min(len(norm1), len(norm2))
            longer = max(len(norm1), len(norm2))
            return shorter / longer * 0.9  # 부분일치는 90% 점수
        
        # SequenceMatcher로 유사도 계산
        seq_ratio = SequenceMatcher(None, norm1, norm2).ratio()
        
        # 단어 기반 유사도
        if words1 and words2:
            word_jaccard = len(common_words) / len(words1 | words2)
            # 두 점수의 가중 평균
            return seq_ratio * 0.6 + word_jaccard * 0.4
        
        return seq_ratio
    
    def find_matching_musical(self, target_title: str, musicals_list: List[Dict], 
                             threshold: float = 0.8) -> Optional[Dict]:
        """제목이 유사한 뮤지컬 찾기"""
        best_match = None
        best_similarity = 0.0
        
        for musical in musicals_list:
            similarity = self.calculate_similarity(target_title, musical.get('title', ''))
            if similarity > best_similarity and similarity >= threshold:
                best_similarity = similarity
                best_match = musical
        
        return best_match
    
    def integrate_data(self, interpark_data: List[Dict], yes24_data: List[Dict]) -> List[Dict]:
        """
        인터파크와 Yes24 데이터를 통합
        
        개선된 규칙:
        1. 제목 매칭으로 같은 뮤지컬 찾기
        2. 매칭 성공 시:
           - 기본 정보: 인터파크 데이터 사용 (제목, 이미지, 공연기간, 예매율, 공연장 등)
           - Yes24 추가: 평점(yes24_rating), 상세URL(yes24_detail_url)만 저장
        3. 매칭 실패 시: 각 사이트 데이터를 별도로 보존
        """
        integrated_musicals = []
        used_yes24_indices = set()
        
        self.logger.info(f"통합 시작: 인터파크 {len(interpark_data)}개, Yes24 {len(yes24_data)}개")
        
        # 1단계: 인터파크 데이터 기반으로 통합
        for interpark_musical in interpark_data:
            # 기본 정보는 모두 인터파크 것 사용
            integrated = {
                'title': interpark_musical.get('title'),
                'rank': interpark_musical.get('rank'),
                'interpark_rank': interpark_musical.get('rank'),
                'yes24_rank': None,
                'interpark_rating': interpark_musical.get('rating'),
                'yes24_rating': None,
                'image_url': interpark_musical.get('image_url'),
                'detail_url': interpark_musical.get('detail_url'),  # 인터파크 URL
                'yes24_detail_url': None,  # Yes24 URL은 별도 저장
                'performance_period': interpark_musical.get('performance_period'),
                'booking_rate': interpark_musical.get('booking_rate'),
                'venue': interpark_musical.get('venue'),
                'source': 'interpark',
                'crawled_at': interpark_musical.get('crawled_at'),
            }
            
            # Yes24에서 매칭되는 뮤지컬 찾기
            yes24_match = self.find_matching_musical(
                interpark_musical.get('title', ''), 
                yes24_data,
                threshold=0.75  # 75% 이상 유사도
            )
            
            if yes24_match:
                # 매칭 성공: Yes24 평점과 상세URL만 추가
                integrated['yes24_rating'] = yes24_match.get('rating')
                integrated['yes24_rank'] = yes24_match.get('rank')
                integrated['yes24_detail_url'] = yes24_match.get('detail_url')
                integrated['source'] = 'both'
                
                # Yes24 데이터 사용 표시
                yes24_idx = yes24_data.index(yes24_match)
                used_yes24_indices.add(yes24_idx)
                
                self.logger.info(
                    f"✅ 매칭: '{interpark_musical.get('title')}' ↔ '{yes24_match.get('title')}' "
                    f"(유사도: {self.calculate_similarity(interpark_musical.get('title'), yes24_match.get('title')):.2f}) "
                    f"[인터파크 {integrated['interpark_rank']}위 / Yes24 {integrated['yes24_rank']}위]"
                )
            
            integrated_musicals.append(integrated)
        
        # 2단계: Yes24에만 있는 뮤지컬 추가 (매칭되지 않은 것만)
        for idx, yes24_musical in enumerate(yes24_data):
            if idx not in used_yes24_indices:
                integrated = {
                    'title': yes24_musical.get('title'),
                    'rank': len(integrated_musicals) + 1,
                    'interpark_rank': None,
                    'yes24_rank': yes24_musical.get('rank'),
                    'interpark_rating': None,
                    'yes24_rating': yes24_musical.get('rating'),
                    'image_url': yes24_musical.get('image_url'),  # Yes24 이미지 (있으면)
                    'detail_url': yes24_musical.get('detail_url'),  # Yes24 URL
                    'yes24_detail_url': yes24_musical.get('detail_url'),  # 동일하게
                    'performance_period': yes24_musical.get('performance_period'),
                    'booking_rate': None,
                    'venue': yes24_musical.get('venue'),
                    'source': 'yes24',
                    'crawled_at': yes24_musical.get('crawled_at'),
                }
                
                integrated_musicals.append(integrated)
                self.logger.info(
                    f"📌 Yes24 전용: '{yes24_musical.get('title')}' (Yes24 {integrated['yes24_rank']}위)"
                )
        
        # 순위 재정렬 (인터파크 순위 우선, 없으면 Yes24 순위)
        integrated_musicals.sort(
            key=lambda x: (
                x['interpark_rank'] if x['interpark_rank'] is not None else 999,
                x['yes24_rank'] if x['yes24_rank'] is not None else 999
            )
        )
        
        # 최종 순위 부여
        for idx, musical in enumerate(integrated_musicals, 1):
            musical['final_rank'] = idx
        
        # 통합 결과 통계
        both_count = sum(1 for m in integrated_musicals if m['source'] == 'both')
        interpark_only = sum(1 for m in integrated_musicals if m['source'] == 'interpark')
        yes24_only = sum(1 for m in integrated_musicals if m['source'] == 'yes24')
        
        self.logger.info(
            f"\n{'='*70}\n"
            f"통합 완료: 총 {len(integrated_musicals)}개 뮤지컬\n"
            f"  - 양쪽 매칭: {both_count}개\n"
            f"  - 인터파크만: {interpark_only}개\n"
            f"  - Yes24만: {yes24_only}개\n"
            f"{'='*70}"
        )
        
        return integrated_musicals
    
    def save_integrated_data(self, integrated_data: List[Dict], 
                           output_path: str, format: str = 'json'):
        """통합 데이터 저장"""
        if format == 'json':
            with open(output_path, 'w', encoding='utf-8') as f:
                json.dump(integrated_data, f, ensure_ascii=False, indent=2, default=str)
        elif format == 'csv':
            import csv
            with open(output_path, 'w', encoding='utf-8', newline='') as f:
                if integrated_data:
                    fieldnames = integrated_data[0].keys()
                    writer = csv.DictWriter(f, fieldnames=fieldnames)
                    writer.writeheader()
                    writer.writerows(integrated_data)
        
        self.logger.info(f"통합 데이터 저장 완료: {output_path}")
