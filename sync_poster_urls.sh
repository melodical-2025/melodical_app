#!/bin/bash

# 포스터 URL 동기화 스크립트
# 사용법: ./sync_poster_urls.sh

echo "🔄 Starting poster URL synchronization..."
echo ""

# 서버 실행 확인
if ! lsof -i :8080 > /dev/null 2>&1; then
    echo "❌ Error: Backend server is not running on port 8080"
    echo "Please start the Spring Boot application first."
    exit 1
fi

echo "✅ Backend server is running"
echo ""

# 1단계: 크롤링 데이터를 Musical 엔티티로 동기화
echo "📥 Step 1: Syncing crawled data to Musical entities..."
SYNC_RESULT=$(curl -s -X POST "http://localhost:8080/api/crawler/sync")
echo "$SYNC_RESULT" | jq '.'

if echo "$SYNC_RESULT" | jq -e '.status == "success"' > /dev/null 2>&1; then
    SYNC_COUNT=$(echo "$SYNC_RESULT" | jq -r '.syncCount')
    echo "✅ Successfully synced $SYNC_COUNT musicals"
else
    echo "❌ Sync failed"
    exit 1
fi

echo ""

# 2단계: posterUrl이 누락된 뮤지컬 복구
echo "🔧 Step 2: Fixing missing posterUrls..."
FIX_RESULT=$(curl -s -X POST "http://localhost:8080/api/crawler/fix-poster-urls")
echo "$FIX_RESULT" | jq '.'

if echo "$FIX_RESULT" | jq -e '.status == "success"' > /dev/null 2>&1; then
    FIXED_COUNT=$(echo "$FIX_RESULT" | jq -r '.fixedCount')
    echo "✅ Fixed $FIXED_COUNT musicals with missing posterUrls"
else
    echo "❌ Fix failed"
fi

echo ""

# 3단계: 결과 확인
echo "📊 Step 3: Verifying results..."
echo ""
echo "Sample musicals with posterUrls:"
curl -s "http://localhost:8080/api/musicals/monthly" | jq '.[0:3] | .[] | {title, posterUrl}'

echo ""
echo "✅ Synchronization complete!"
echo ""
echo "You can now test the app to see if poster images are displayed correctly."
