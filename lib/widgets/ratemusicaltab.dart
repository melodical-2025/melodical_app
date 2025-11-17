// lib/widgets/ratemusicaltab.dart
import 'package:flutter/material.dart';
import 'package:flutter_rating_bar/flutter_rating_bar.dart';
import '../models/musical.dart';
import '../services/api_service.dart';
import '../screens/detail_screen.dart';

class RateMusicalTab extends StatefulWidget {
  const RateMusicalTab({super.key});

  @override
  State<RateMusicalTab> createState() => _RateMusicalTabState();
}

class _RateMusicalTabState extends State<RateMusicalTab> {
  List<Musical> _musicals = [];
  final Map<int, double> _ratings = {}; // musical.id → rating
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadMusicals();
  }

  Future<void> _loadMusicals() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      print('📥 Loading musicals for rating...');
      // integrated_monthly_dataset 사용
      final list = await ApiService.fetchMonthlyMusicals();
      print('✅ Loaded ${list.length} musicals');

      setState(() {
        _musicals = list.map((data) {
          final period = data['period']?.toString() ?? '';
          final dates = period.split('~');

          String posterUrl = data['posterUrl']?.toString() ?? '';
          if (posterUrl.isEmpty) {
            posterUrl = 'https://via.placeholder.com/150?text=No+Image';
          }

          return Musical(
            id: data['id'] ?? 0,
            title: data['title'] ?? '',
            cast: '',
            runtime: '',
            posterUrl: posterUrl,
            theater: data['theater'] ?? '',
            startDate: dates.isNotEmpty ? dates.first.trim() : '',
            endDate: dates.length > 1 ? dates.last.trim() : '',
          );
        }).toList();
      });
      
      // 사용자가 이미 평가한 평점 로드
      try {
        print('🔍 Loading user ratings...');
        final myRatings = await ApiService.getMyRatings();
        print('✅ Loaded ${myRatings.length} user ratings');
        setState(() {
          _ratings.addAll(myRatings);
        });
      } catch (e) {
        print('⚠️ Failed to load user ratings (may not be logged in): $e');
        // 로그인하지 않았거나 평점이 없는 경우는 무시
      }
    } catch (e) {
      print('❌ Error loading musicals: $e');
      setState(() {
        _error = e.toString();
      });
    } finally {
      setState(() {
        _loading = false;
      });
    }
  }

  Future<void> _submitRatings() async {
    if (_ratings.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('평가할 뮤지컬을 선택해주세요'),
          backgroundColor: Color(0xFFE17951),
        ),
      );
      return;
    }

    // 로딩 다이얼로그
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => const Center(
        child: Card(
          child: Padding(
            padding: EdgeInsets.all(20),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                CircularProgressIndicator(color: Color(0xFFE17951)),
                SizedBox(height: 16),
                Text('평가 저장 중...', style: TextStyle(fontSize: 16)),
              ],
            ),
          ),
        ),
      ),
    );

    final payload = _ratings.entries
        .map((e) => {'musicalId': e.key, 'rating': e.value})
        .toList();

    print('💾 Submitting ${payload.length} ratings...');
    print('Payload: $payload');

    try {
      await ApiService.rateBatchMusical(payload);
      print('✅ Ratings saved successfully!');

      if (mounted) {
        Navigator.of(context).pop(); // 다이얼로그 닫기

        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('${_ratings.length}개의 평가가 저장되었습니다!'),
            backgroundColor: Colors.green,
          ),
        );

        // 평가 초기화
        setState(() {
          _ratings.clear();
        });
      }
    } catch (e) {
      print('❌ Failed to save ratings: $e');

      if (mounted) {
        Navigator.of(context).pop(); // 다이얼로그 닫기

        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('저장 실패: $e'),
            backgroundColor: Colors.red,
            duration: const Duration(seconds: 3),
            action: SnackBarAction(
              label: '다시 시도',
              textColor: Colors.white,
              onPressed: _submitRatings,
            ),
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            CircularProgressIndicator(color: Color(0xFFE17951)),
            SizedBox(height: 16),
            Text(
              '뮤지컬 목록을 불러오는 중...',
              style: TextStyle(fontSize: 16),
            ),
          ],
        ),
      );
    }

    if (_error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(
                Icons.error_outline,
                size: 64,
                color: Color(0xFFE17951),
              ),
              const SizedBox(height: 16),
              const Text(
                '데이터를 불러오는데 실패했습니다',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                _error!,
                textAlign: TextAlign.center,
                style: const TextStyle(fontSize: 14, color: Colors.black54),
              ),
              const SizedBox(height: 24),
              ElevatedButton(
                onPressed: _loadMusicals,
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFE17951),
                  padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 12),
                ),
                child: const Text(
                  '다시 시도',
                  style: TextStyle(fontSize: 16, color: Colors.white),
                ),
              ),
            ],
          ),
        ),
      );
    }

    if (_musicals.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(
                Icons.music_note,
                size: 64,
                color: Color(0xFFE17951),
              ),
              const SizedBox(height: 16),
              const Text(
                '평가할 뮤지컬이 없습니다',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 24),
              ElevatedButton(
                onPressed: _loadMusicals,
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFE17951),
                  padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 12),
                ),
                child: const Text(
                  '새로고침',
                  style: TextStyle(fontSize: 16, color: Colors.white),
                ),
              ),
            ],
          ),
        ),
      );
    }

    return Stack(
      children: [
        ListView.builder(
          padding: const EdgeInsets.only(bottom: 80, left: 18, right: 18, top: 18),
          itemCount: _musicals.length,
          itemBuilder: (context, index) {
            final m = _musicals[index];
            final current = _ratings[m.id] ?? 0.0;
            return Padding(
              padding: const EdgeInsets.symmetric(vertical: 16.0),
              child: Row(
                children: [
                  GestureDetector(
                    onTap: () async {
                      print('🎭 Opening musical detail: ${m.title}');
                      // 캐시된 데이터에서 뮤지컬 찾기 (빠른 탐색)
                      try {
                        // 캐시된 데이터 사용 (새로 fetch하지 않음)
                        final monthlyMusicals = await ApiService.fetchMonthlyMusicals();
                        final musicalData = monthlyMusicals.firstWhere(
                          (musical) => musical['id'] == m.id,
                          orElse: () => {
                            'id': m.id,
                            'title': m.title,
                            'posterUrl': m.posterUrl,
                            'theater': m.theater,
                            'period': '${m.startDate} ~ ${m.endDate}',
                            'cast': '',
                            'runtime': '',
                          },
                        );
                        
                        if (context.mounted) {
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (context) => DetailScreen(
                                musicalData: musicalData,
                              ),
                            ),
                          );
                        }
                      } catch (e) {
                        print('❌ Error opening detail: $e');
                        if (context.mounted) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            SnackBar(
                              content: Text('상세 정보를 불러올 수 없습니다: $e'),
                              backgroundColor: Colors.red,
                            ),
                          );
                        }
                      }
                    },
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(8),
                      child: Image.network(
                        m.posterUrl,
                        width: 80,
                        height: 110,
                        fit: BoxFit.cover,
                        errorBuilder: (_, __, ___) => Container(
                          width: 80,
                          height: 110,
                          color: Colors.grey.shade200,
                          child: const Icon(Icons.broken_image),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 18),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          m.title,
                          style: const TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 18,
                          ),
                        ),
                        const SizedBox(height: 4),
                        RatingBar.builder(
                          initialRating: current,
                          minRating: 0,
                          allowHalfRating: true,
                          itemCount: 5,
                          itemSize: 32,
                          itemPadding: const EdgeInsets.symmetric(horizontal: 2),
                          unratedColor: Colors.grey.shade300,
                          itemBuilder: (_, __) => const Icon(
                            Icons.star,
                            color: Colors.orange,
                          ),
                          onRatingUpdate: (r) {
                            setState(() {
                              _ratings[m.id] = r;
                            });
                          },
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            );
          },
        ),

        // 저장 버튼
        Positioned(
          bottom: 16,
          left: 16,
          right: 16,
          child: ElevatedButton(
            onPressed: _ratings.isNotEmpty ? _submitRatings : null,
            style: ElevatedButton.styleFrom(
              padding: const EdgeInsets.symmetric(vertical: 14),
              backgroundColor: const Color(0xFFFFAD75),
              foregroundColor: const Color(0xFFE17951),
              disabledBackgroundColor: Colors.grey.shade300,
              disabledForegroundColor: Colors.grey,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
            ),
            child: const Text('저장하기', style: TextStyle(fontSize: 16)),
          ),
        ),
      ],
    );
  }
}
