from dataclasses import dataclass
from typing import Optional, List
from datetime import datetime


@dataclass
class Musical:
    """뮤지컬 정보를 담는 데이터 클래스"""
    title: str
    rank: Optional[int] = None
    rating: Optional[float] = None
    image_url: Optional[str] = None
    detail_url: Optional[str] = None
    performance_period: Optional[str] = None
    booking_rate: Optional[float] = None
    cast: Optional[List[str]] = None
    venue: Optional[str] = None
    price_info: Optional[str] = None
    source: Optional[str] = None  # 'interpark' or 'yes24'
    crawled_at: Optional[datetime] = None

    def to_dict(self) -> dict:
        """딕셔너리로 변환"""
        return {
            'title': self.title,
            'rank': self.rank,
            'rating': self.rating,
            'image_url': self.image_url,
            'detail_url': self.detail_url,
            'performance_period': self.performance_period,
            'booking_rate': self.booking_rate,
            'cast': self.cast,
            'venue': self.venue,
            'price_info': self.price_info,
            'source': self.source,
            'crawled_at': self.crawled_at.isoformat() if self.crawled_at else None
        }


@dataclass
class RankingData:
    """랭킹 데이터를 담는 클래스"""
    period_type: str  # 'weekly' or 'monthly'
    ranking_date: datetime
    musicals: List[Musical]
    source: str

    def to_dict(self) -> dict:
        """딕셔너리로 변환"""
        return {
            'period_type': self.period_type,
            'ranking_date': self.ranking_date.isoformat(),
            'source': self.source,
            'musicals': [musical.to_dict() for musical in self.musicals]
        }
