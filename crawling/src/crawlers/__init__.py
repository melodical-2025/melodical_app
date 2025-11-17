from abc import ABC, abstractmethod
from typing import List
import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from models import Musical, RankingData


class BaseCrawler(ABC):
    """크롤러 기본 클래스"""
    
    def __init__(self, site_name: str):
        self.site_name = site_name
        self.session = None
    
    @abstractmethod
    def get_weekly_ranking(self) -> RankingData:
        """주간 랭킹 데이터 수집"""
        pass
    
    @abstractmethod
    def get_monthly_ranking(self) -> RankingData:
        """월간 랭킹 데이터 수집"""
        pass
    
    @abstractmethod
    def get_musical_details(self, musical: Musical) -> Musical:
        """뮤지컬 상세 정보 수집"""
        pass
    
    def __enter__(self):
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.session:
            self.session.close()
