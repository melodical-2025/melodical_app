#!/usr/bin/env python3
"""
Musical Ranking Crawler
뮤지컬 랭킹 데이터를 크롤링하는 메인 애플리케이션
"""

import asyncio
import time
from datetime import datetime
import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from crawlers.interpark_improved import InterparkImprovedCrawler
from crawlers.yes24_new import Yes24Crawler
from integrator import MusicalDataIntegrator
from utils import setup_logger, save_to_json, save_to_csv, combine_rankings


def main():
    """메인 실행 함수"""
    logger = setup_logger("main")
    logger.info("Starting musical ranking crawler")
    
    try:
        # 크롤러 인스턴스 생성
        interpark = InterparkImprovedCrawler()
        yes24 = Yes24Crawler()
        
        # 주간 랭킹 수집
        logger.info("Collecting weekly rankings...")
        
        interpark_weekly = interpark.get_weekly_ranking()
        yes24_weekly = yes24.get_weekly_ranking()
        
        # 월간 랭킹 수집
        logger.info("Collecting monthly rankings...")
        
        interpark_monthly = interpark.get_monthly_ranking()
        yes24_monthly = yes24.get_monthly_ranking()
        
        # 상세 정보 수집 (평점 정보 포함)
        logger.info("Collecting detailed information and ratings for all musicals...")
        
        # 인터파크 주간 뮤지컬의 평점 수집
        for musical in interpark_weekly.musicals:
            interpark.get_musical_details(musical)
            time.sleep(0.5)  # 요청 간격 조절
        
        # 인터파크 월간 뮤지컬의 평점 수집
        for musical in interpark_monthly.musicals:
            interpark.get_musical_details(musical)
            time.sleep(0.5)  # 요청 간격 조절
        
        # Yes24 주간 상위 10개 뮤지컬의 평점 수집
        for musical in yes24_weekly.musicals[:10]:
            yes24.get_musical_details(musical)
            time.sleep(0.3)  # 요청 간격 조절
        
        # Yes24 월간 상위 10개 뮤지컬의 평점 수집
        for musical in yes24_monthly.musicals[:10]:
            yes24.get_musical_details(musical)
            time.sleep(0.3)  # 요청 간격 조절
        
        # 데이터 저장
        logger.info("Saving collected data...")
        
        # 개별 사이트 데이터 저장
        interpark_weekly_data = [m.to_dict() for m in interpark_weekly.musicals]
        yes24_weekly_data = [m.to_dict() for m in yes24_weekly.musicals]
        
        interpark_monthly_data = [m.to_dict() for m in interpark_monthly.musicals]
        yes24_monthly_data = [m.to_dict() for m in yes24_monthly.musicals]
        
        # JSON 저장
        save_to_json(interpark_weekly_data, "interpark_weekly")
        save_to_json(yes24_weekly_data, "yes24_weekly")
        save_to_json(interpark_monthly_data, "interpark_monthly")
        save_to_json(yes24_monthly_data, "yes24_monthly")
        
        # CSV 저장
        save_to_csv(interpark_weekly_data, "interpark_weekly")
        save_to_csv(yes24_weekly_data, "yes24_weekly")
        save_to_csv(interpark_monthly_data, "interpark_monthly")
        save_to_csv(yes24_monthly_data, "yes24_monthly")
        
        # 통합 랭킹 생성 및 저장
        logger.info("Creating combined rankings...")
        
        combined_weekly = combine_rankings(interpark_weekly_data, yes24_weekly_data)
        combined_monthly = combine_rankings(interpark_monthly_data, yes24_monthly_data)
        
        save_to_json(combined_weekly, "combined_weekly_ranking")
        save_to_json(combined_monthly, "combined_monthly_ranking")
        save_to_csv(combined_weekly, "combined_weekly_ranking")
        save_to_csv(combined_monthly, "combined_monthly_ranking")
        
        # 새로운 통합 데이터셋 생성
        logger.info("Creating integrated dataset...")
        integrator = MusicalDataIntegrator()
        
        # 주간 통합 데이터
        integrated_weekly = integrator.integrate_data(
            interpark_weekly_data, 
            yes24_weekly_data
        )
        
        # 월간 통합 데이터
        integrated_monthly = integrator.integrate_data(
            interpark_monthly_data,
            yes24_monthly_data
        )
        
        # 통합 데이터 저장
        save_to_json(integrated_weekly, "integrated_weekly_dataset")
        save_to_csv(integrated_weekly, "integrated_weekly_dataset")
        save_to_json(integrated_monthly, "integrated_monthly_dataset")
        save_to_csv(integrated_monthly, "integrated_monthly_dataset")
        
        logger.info("Crawling completed successfully!")
        
        # 요약 정보 출력
        print(f"\n=== Crawling Summary ===")
        print(f"Interpark Weekly: {len(interpark_weekly.musicals)} musicals")
        print(f"Yes24 Weekly: {len(yes24_weekly.musicals)} musicals")
        print(f"Interpark Monthly: {len(interpark_monthly.musicals)} musicals")
        print(f"Yes24 Monthly: {len(yes24_monthly.musicals)} musicals")
        print(f"Combined Weekly Ranking: {len(combined_weekly)} musicals")
        print(f"Combined Monthly Ranking: {len(combined_monthly)} musicals")
        print(f"\n=== Integrated Dataset ===")
        print(f"Integrated Weekly: {len(integrated_weekly)} musicals")
        print(f"  - Both sources: {sum(1 for m in integrated_weekly if m['source'] == 'both')}")
        print(f"  - Interpark only: {sum(1 for m in integrated_weekly if m['source'] == 'interpark')}")
        print(f"  - Yes24 only: {sum(1 for m in integrated_weekly if m['source'] == 'yes24')}")
        print(f"Integrated Monthly: {len(integrated_monthly)} musicals")
        print(f"Data saved to 'data/' directory")
            
    except Exception as e:
        logger.error(f"Crawling failed: {e}")
        raise


if __name__ == "__main__":
    main()
