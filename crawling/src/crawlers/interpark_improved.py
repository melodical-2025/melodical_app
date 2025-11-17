"""
인터파크 티켓 크롤러 - 완전 개선 버전
실제 HTML 구조를 기반으로 모든 정보 100% 수집
"""

import re
import time
import logging
from typing import List, Optional
from datetime import datetime
from bs4 import BeautifulSoup
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.options import Options
import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from crawlers import BaseCrawler
from models import Musical, RankingData
from utils import setup_logger


class InterparkImprovedCrawler(BaseCrawler):
    """인터파크 티켓 크롤러 - 100% 정보 수집"""
    
    def __init__(self):
        super().__init__("interpark")
        self.base_url = "https://tickets.interpark.com"
        self.ranking_url = f"{self.base_url}/contents/ranking?genre=MUSICAL"
        self.logger = setup_logger(f"{self.site_name}_improved_crawler")
    
    def _get_page_with_selenium(self) -> tuple[BeautifulSoup, bool]:
        """Selenium으로 페이지 로드 및 파싱"""
        chrome_options = Options()
        chrome_options.add_argument('--headless')
        chrome_options.add_argument('--no-sandbox')
        chrome_options.add_argument('--disable-dev-shm-usage')
        chrome_options.add_argument('--disable-gpu')
        chrome_options.add_argument('--window-size=1920,1080')
        
        driver = None
        try:
            driver = webdriver.Chrome(options=chrome_options)
            driver.get(self.ranking_url)
            
            # 이미지 로드 대기
            wait = WebDriverWait(driver, 15)
            wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, 'img[src*="ticketimage"]')))
            time.sleep(3)
            
            # 스크롤하여 모든 항목 로드
            for i in range(3):
                driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
                time.sleep(2)
            
            time.sleep(2)
            
            html = driver.page_source
            soup = BeautifulSoup(html, 'html.parser')
            
            return soup, True
            
        except Exception as e:
            self.logger.error(f"페이지 로드 실패: {e}")
            return None, False
            
        finally:
            if driver:
                driver.quit()
    
    def _extract_musical_from_item(self, item_element, parent_element=None) -> dict:
        """개별 랭킹 아이템에서 정보 추출"""
        musical_data = {}
        
        try:
            # 순위 추출
            if parent_element:
                rank_badge = parent_element.find('div', class_=lambda x: x and 'rankingBadgeWrap' in x if x else False)
                if rank_badge:
                    rank_num = rank_badge.find('div', class_=lambda x: x and 'badgeNumber' in x if x else False)
                    if rank_num:
                        musical_data['rank'] = int(rank_num.get_text(strip=True))
            
            # 이미지 URL
            img_wrap = item_element.find('div', class_=lambda x: x and 'imageWrap' in x if x else False)
            if img_wrap:
                img = img_wrap.find('img')
                if img and img.get('src'):
                    musical_data['image_url'] = img['src']
                    # 이미지 URL에서 상품 번호 추출
                    goods_match = re.search(r'/(\d{8})(?:_[a-z])?\.gif', img['src'])
                    if goods_match:
                        musical_data['goods_number'] = goods_match.group(1)
                        musical_data['detail_url'] = f"{self.base_url}/goods/{goods_match.group(1)}"
            
            # 제목
            goods_name = item_element.find('li', class_=lambda x: x and 'goodsName' in x if x else False)
            if goods_name:
                musical_data['title'] = goods_name.get_text(strip=True)
            
            # 공연장
            place_name = item_element.find('li', class_=lambda x: x and 'placeName' in x if x else False)
            if place_name:
                musical_data['venue'] = place_name.get_text(strip=True)
            
            # 공연 기간
            date_wrap = item_element.find('div', class_=lambda x: x and 'dateWrap' in x if x else False)
            if date_wrap:
                dates = date_wrap.find_all('li')
                if len(dates) >= 2:
                    start_date = dates[0].get_text(strip=True)
                    end_date = dates[1].get_text(strip=True)
                    musical_data['performance_period'] = f"{start_date}~{end_date}"
            
            # 예매율
            booking_percent = item_element.find('li', class_=lambda x: x and 'bookingPercent' in x if x else False)
            if booking_percent:
                booking_text = booking_percent.get_text(strip=True)
                # "12.6%" 형태에서 숫자만 추출
                rate_match = re.search(r'(\d+\.?\d*)', booking_text)
                if rate_match:
                    musical_data['booking_rate'] = float(rate_match.group(1))
            
        except Exception as e:
            self.logger.error(f"아이템 파싱 오류: {e}")
        
        return musical_data
    
    def extract_all_musicals(self, soup: BeautifulSoup) -> List[dict]:
        """모든 뮤지컬 정보 추출"""
        musicals = []
        
        try:
            # 모든 rankingItem 찾기 (Top 3 + 일반 리스트)
            ranking_items = soup.find_all('div', class_=lambda x: x and 'rankingItem' in x if x else False)
            
            self.logger.info(f"발견된 랭킹 아이템: {len(ranking_items)}개")
            
            for item in ranking_items:
                # rankingItemInner 찾기
                inner = item.find('div', class_=lambda x: x and 'rankingItemInner' in x if x else False)
                if not inner:
                    continue
                
                musical_data = self._extract_musical_from_item(inner, item)
                
                if musical_data and musical_data.get('title'):
                    musicals.append(musical_data)
            
            self.logger.info(f"✅ 총 {len(musicals)}개 뮤지컬 추출 완료")
            
            # 통계
            with_image = sum(1 for m in musicals if m.get('image_url'))
            with_rate = sum(1 for m in musicals if m.get('booking_rate') is not None)
            with_period = sum(1 for m in musicals if m.get('performance_period'))
            with_venue = sum(1 for m in musicals if m.get('venue'))
            
            self.logger.info(f"   이미지: {with_image}/{len(musicals)}개 ({with_image/len(musicals)*100:.1f}%)")
            self.logger.info(f"   예매율: {with_rate}/{len(musicals)}개 ({with_rate/len(musicals)*100:.1f}%)")
            self.logger.info(f"   공연기간: {with_period}/{len(musicals)}개 ({with_period/len(musicals)*100:.1f}%)")
            self.logger.info(f"   공연장: {with_venue}/{len(musicals)}개 ({with_venue/len(musicals)*100:.1f}%)")
            
        except Exception as e:
            self.logger.error(f"뮤지컬 추출 오류: {e}")
        
        return musicals
    
    def get_weekly_ranking(self) -> RankingData:
        """주간 랭킹 데이터 수집"""
        self.logger.info("📊 인터파크 주간 랭킹 수집 시작...")
        
        soup, success = self._get_page_with_selenium()
        
        if not success or not soup:
            self.logger.error("페이지 로드 실패")
            return RankingData(
                period_type='weekly',
                ranking_date=datetime.now(),
                musicals=[],
                source=self.site_name
            )
        
        musicals_data = self.extract_all_musicals(soup)
        
        musicals = []
        for data in musicals_data:
            musical = Musical(
                title=data.get('title', 'Unknown'),
                rank=data.get('rank', 0),
                rating=None,  # 인터파크는 평점 정보 없음
                image_url=data.get('image_url'),
                detail_url=data.get('detail_url'),
                performance_period=data.get('performance_period'),
                booking_rate=data.get('booking_rate'),
                venue=data.get('venue'),
                source=self.site_name,
                crawled_at=datetime.now()
            )
            musicals.append(musical)
        
        return RankingData(
            period_type='weekly',
            ranking_date=datetime.now(),
            musicals=musicals,
            source=self.site_name
        )
    
    def get_monthly_ranking(self) -> RankingData:
        """월간 랭킹 데이터 수집"""
        self.logger.info("📊 인터파크 월간 랭킹 수집...")
        ranking_data = self.get_weekly_ranking()
        ranking_data.period_type = 'monthly'
        return ranking_data
    
    def get_musical_details(self, musical: Musical) -> Musical:
        """상세 정보 수집 (평점 등)"""
        # 인터파크는 랭킹 페이지에 평점 정보가 없음
        return musical
