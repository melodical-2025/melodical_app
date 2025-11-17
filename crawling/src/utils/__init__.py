import logging
import time
import json
import csv
import pandas as pd
from datetime import datetime
from functools import wraps
from pathlib import Path
from typing import List, Dict, Any


def setup_logger(name: str, level: int = logging.INFO) -> logging.Logger:
    """로거 설정"""
    logger = logging.getLogger(name)
    
    if not logger.handlers:
        logger.setLevel(level)
        
        # 파일 핸들러
        log_dir = Path("logs")
        log_dir.mkdir(exist_ok=True)
        
        file_handler = logging.FileHandler(
            log_dir / f"{name}_{datetime.now().strftime('%Y%m%d')}.log",
            encoding='utf-8'
        )
        file_handler.setLevel(level)
        
        # 콘솔 핸들러
        console_handler = logging.StreamHandler()
        console_handler.setLevel(level)
        
        # 포매터
        formatter = logging.Formatter(
            '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
        )
        file_handler.setFormatter(formatter)
        console_handler.setFormatter(formatter)
        
        logger.addHandler(file_handler)
        logger.addHandler(console_handler)
    
    return logger


def rate_limit(delay: float = 1.0):
    """요청 간격 제한 데코레이터"""
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            result = func(*args, **kwargs)
            time.sleep(delay)
            return result
        return wrapper
    return decorator


def save_to_json(data: List[Dict[Any, Any]], filename: str) -> None:
    """JSON 파일로 저장"""
    output_dir = Path("data")
    output_dir.mkdir(exist_ok=True)
    
    filepath = output_dir / f"{filename}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
    
    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    
    print(f"Data saved to {filepath}")


def save_to_csv(data: List[Dict[Any, Any]], filename: str) -> None:
    """CSV 파일로 저장"""
    if not data:
        return
    
    output_dir = Path("data")
    output_dir.mkdir(exist_ok=True)
    
    filepath = output_dir / f"{filename}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.csv"
    
    df = pd.DataFrame(data)
    df.to_csv(filepath, index=False, encoding='utf-8-sig')
    
    print(f"Data saved to {filepath}")


def combine_rankings(interpark_data: List[Dict], yes24_data: List[Dict]) -> List[Dict]:
    """두 사이트의 랭킹 데이터를 결합하여 통합 랭킹 생성"""
    combined = []
    
    # 제목 기반으로 매칭
    interpark_titles = {item['title']: item for item in interpark_data}
    yes24_titles = {item['title']: item for item in yes24_data}
    
    all_titles = set(interpark_titles.keys()) | set(yes24_titles.keys())
    
    for title in all_titles:
        interpark_item = interpark_titles.get(title, {})
        yes24_item = yes24_titles.get(title, {})
        
        # 평균 순위 계산 (없으면 큰 값 할당)
        interpark_rank = interpark_item.get('rank', 999)
        yes24_rank = yes24_item.get('rank', 999)
        
        avg_rank = (interpark_rank + yes24_rank) / 2 if interpark_rank != 999 and yes24_rank != 999 else min(interpark_rank, yes24_rank)
        
        # 통합 데이터 생성
        combined_item = {
            'title': title,
            'combined_rank': avg_rank,
            'interpark_rank': interpark_rank if interpark_rank != 999 else None,
            'yes24_rank': yes24_rank if yes24_rank != 999 else None,
            'interpark_rating': interpark_item.get('rating'),
            'yes24_rating': yes24_item.get('rating'),
            'image_url': interpark_item.get('image_url') or yes24_item.get('image_url'),
            'interpark_detail_url': interpark_item.get('detail_url'),
            'yes24_detail_url': yes24_item.get('detail_url'),
            'performance_period': interpark_item.get('performance_period') or yes24_item.get('performance_period'),
            'cast': interpark_item.get('cast') or yes24_item.get('cast'),
            'venue': interpark_item.get('venue') or yes24_item.get('venue'),
            'sources': [s for s in [interpark_item.get('source'), yes24_item.get('source')] if s]
        }
        
        combined.append(combined_item)
    
    # 통합 순위로 정렬
    combined.sort(key=lambda x: x['combined_rank'])
    
    # 최종 순위 재할당
    for idx, item in enumerate(combined, 1):
        item['final_rank'] = idx
    
    return combined


def validate_url(url: str) -> bool:
    """URL 유효성 검사"""
    import re
    url_pattern = re.compile(
        r'^https?://'  # http:// or https://
        r'(?:(?:[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?\.)+[A-Z]{2,6}\.?|'  # domain...
        r'localhost|'  # localhost...
        r'\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})'  # ...or ip
        r'(?::\d+)?'  # optional port
        r'(?:/?|[/?]\S+)$', re.IGNORECASE)
    return url_pattern.match(url) is not None


def clean_text(text: str) -> str:
    """텍스트 정리"""
    if not text:
        return ""
    
    # 공백 정리
    cleaned = " ".join(text.split())
    
    # 특수문자 제거 (필요에 따라 조정)
    import re
    cleaned = re.sub(r'[^\w\s\-\(\)\[\]\/\.\,\:]', '', cleaned)
    
    return cleaned.strip()
