import requests
from bs4 import BeautifulSoup
import re
import time
import logging
from typing import List, Optional
from datetime import datetime
import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from crawlers import BaseCrawler
from models import Musical, RankingData
from utils import setup_logger, rate_limit


class Yes24Crawler(BaseCrawler):
    """Yes24 티켓 크롤러 - 개선 버전"""
    
    def __init__(self):
        super().__init__("yes24")
        self.base_url = "https://ticket.yes24.com"
        # 실제 뮤지컬 목록이 있는 Genre 페이지 사용
        self.ranking_url = f"{self.base_url}/Genre/Musical"
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
            'Accept-Language': 'ko-KR,ko;q=0.9',
            'Referer': 'https://ticket.yes24.com/',
        })
        self.logger = setup_logger(f"{self.site_name}_crawler")
        
        # 초기 쿠키 얻기
        try:
            self.session.get(self.base_url, timeout=10)
        except:
            pass
    
    @rate_limit(delay=0.5)
    def _fetch_page(self, url: str) -> BeautifulSoup:
        """페이지 요청 및 파싱"""
        try:
            response = self.session.get(url, timeout=15)
            response.raise_for_status()
            return BeautifulSoup(response.content, 'html.parser')
        except requests.RequestException as e:
            self.logger.error(f"Failed to fetch {url}: {e}")
            raise
    
    def extract_musicals_from_page(self, soup: BeautifulSoup) -> List[dict]:
        """페이지에서 뮤지컬 정보 추출 (실제 페이지에서 ID 추출 + 다중 소스)"""
        self.logger.info("Yes24에서 최대한 많은 뮤지컬 ID 수집 시작")
        
        all_ids = set()
        
        # 1. Genre/Musical 페이지에서 ID 추출
        links = soup.find_all('a', href=True)
        for link in links:
            href = link.get('href', '')
            match = re.search(r'/Perf[^/]*/(\d+)', href, re.IGNORECASE)
            if match:
                all_ids.add(match.group(1))
        
        self.logger.info(f"Genre/Musical 페이지: {len(all_ids)}개 발견")
        
        # 2. 메인 페이지에서 추가 ID 추출
        try:
            main_soup = self._fetch_page(self.base_url)
            main_links = main_soup.find_all('a', href=True)
            before = len(all_ids)
            for link in main_links:
                href = link.get('href', '')
                match = re.search(r'/Perf[^/]*/(\d+)', href, re.IGNORECASE)
                if match:
                    all_ids.add(match.group(1))
            if len(all_ids) > before:
                self.logger.info(f"메인 페이지 추가: +{len(all_ids)-before}개 (총 {len(all_ids)}개)")
        except Exception as e:
            self.logger.warning(f"메인 페이지 수집 실패: {e}")
        
        # 3. Special 페이지에서 추가 수집 (31334번 등)
        try:
            special_url = f"{self.base_url}/Special/31334"
            special_soup = self._fetch_page(special_url)
            special_links = special_soup.find_all('a', href=True)
            before = len(all_ids)
            for link in special_links:
                href = link.get('href', '')
                match = re.search(r'/Perf[^/]*/(\d+)', href, re.IGNORECASE)
                if match:
                    all_ids.add(match.group(1))
            if len(all_ids) > before:
                self.logger.info(f"Special 페이지 추가: +{len(all_ids)-before}개 (총 {len(all_ids)}개)")
        except Exception as e:
            self.logger.warning(f"Special 페이지 수집 실패: {e}")
        
        # 최신순 정렬
        sorted_ids = sorted(list(all_ids), key=lambda x: int(x), reverse=True)
        
        # 사용 가능한 모든 ID 사용 (최대 50개)
        popular_musical_ids = sorted_ids[:50]
        
        self.logger.info(f"✅ 최종 선택: {len(popular_musical_ids)}개 뮤지컬")
        
        musicals = []
        
        for rank, musical_id in enumerate(popular_musical_ids, 1):
            try:
                detail_url = f"{self.base_url}/Perf/{musical_id}"
                
                # 상세 페이지에서 제목과 정보 가져오기
                detail_soup = self._fetch_page(detail_url)
                
                # 제목 추출
                title_selectors = [
                    '.rn-big-title',
                    'h1',
                    '.title',
                    '[class*="title"]'
                ]
                
                title = None
                for selector in title_selectors:
                    title_elem = detail_soup.select_one(selector)
                    if title_elem:
                        title = title_elem.get_text(strip=True)
                        if title and len(title) > 2:
                            break
                
                if not title:
                    title = f"뮤지컬 {musical_id}"
                
                # 평점 추출
                rating = self._extract_rating(detail_soup)
                
                # 이미지 추출
                img_elem = detail_soup.select_one('img[alt*="포스터"], .poster img, [class*="poster"] img')
                image_url = img_elem.get('src') if img_elem else None
                if image_url and not image_url.startswith('http'):
                    image_url = self.base_url + image_url
                
                musical_info = {
                    'rank': rank,
                    'title': title,
                    'rating': rating,
                    'image_url': image_url,
                    'detail_url': detail_url,
                    'performance_period': None,
                    'booking_rate': None,
                    'venue': None
                }
                
                musicals.append(musical_info)
                self.logger.info(f"[{rank}] {title} - 평점: {rating}")
                
                time.sleep(0.3)  # 요청 간격
                
            except Exception as e:
                self.logger.warning(f"Failed to fetch musical {musical_id}: {e}")
                continue
        
        return musicals
    
    def _extract_rating(self, soup: BeautifulSoup) -> Optional[float]:
        """상세 페이지에서 평점 추출"""
        # 제공된 정확한 경로
        rating_selectors = [
            'form#mainForm .renew-wrap.rw2 .renew-content .rn-03 .rn-03-left .rn-product-social .rn-product-star#rnProductStar span em',
            '.rn-product-star em',
            '#rnProductStar em',
            '.rn-product-star span em',
            '[class*="star"] em',
        ]
        
        for selector in rating_selectors:
            elements = soup.select(selector)
            for elem in elements:
                text = elem.get_text(strip=True)
                # 숫자만 추출
                if re.match(r'^\d+\.?\d*$', text):
                    try:
                        rating = float(text)
                        # Yes24는 5점 만점
                        if 0 <= rating <= 5:
                            return round(rating, 1)
                    except ValueError:
                        continue
        
        return None
    
    def get_weekly_ranking(self) -> RankingData:
        """주간 랭킹 데이터 수집"""
        self.logger.info("Collecting weekly ranking from Yes24")
        
        try:
            soup = self._fetch_page(self.ranking_url)
            musicals_data = self.extract_musicals_from_page(soup)
        except Exception as e:
            self.logger.error(f"Failed to fetch rankings: {e}")
            musicals_data = []
        
        musicals = []
        for data in musicals_data:  # 모든 뮤지컬 사용 (최대 50개)
            musical = Musical(
                title=data.get('title', 'Unknown'),
                rank=data.get('rank', 0),
                rating=data.get('rating'),
                image_url=data.get('image_url'),
                detail_url=data.get('detail_url'),
                performance_period=data.get('performance_period'),
                booking_rate=data.get('booking_rate'),
                venue=data.get('venue'),
                source=self.site_name,
                crawled_at=datetime.now()
            )
            musicals.append(musical)
        
        self.logger.info(f"Successfully extracted {len(musicals)} musicals")
        
        return RankingData(
            period_type='weekly',
            ranking_date=datetime.now(),
            musicals=musicals,
            source=self.site_name
        )
    
    def get_monthly_ranking(self) -> RankingData:
        """월간 랭킹 데이터 수집"""
        self.logger.info("Collecting monthly ranking from Yes24")
        
        # 주간과 동일한 방식으로 수집
        ranking_data = self.get_weekly_ranking()
        ranking_data.period_type = 'monthly'
        
        return ranking_data
    
    def get_musical_details(self, musical: Musical) -> Musical:
        """뮤지컬 상세 정보 수집 (평점이 이미 있으면 스킵)"""
        if musical.rating:
            return musical
        
        if not musical.detail_url:
            return musical
        
        try:
            soup = self._fetch_page(musical.detail_url)
            rating = self._extract_rating(soup)
            if rating:
                musical.rating = rating
                self.logger.info(f"✅ {musical.title} 평점: {rating}점")
        except Exception as e:
            self.logger.warning(f"Failed to get rating for {musical.title}: {e}")
        
        return musical
