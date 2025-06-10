// lib/widgets/ratemusicaltab.dart
import 'package:flutter/material.dart';
import 'package:flutter_rating_bar/flutter_rating_bar.dart';
import '../models/musical.dart';
import '../services/api_service.dart';

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
      final list = await ApiService.fetchAllMusicals();
      setState(() {
        _musicals = list;
        // 기존에 서버에 저장된 userRating 필드가 있다면 초기값으로 넣어줄 수도 있습니다:
        // for (var m in list) if (m.userRating != null) _ratings[m.id] = m.userRating!;
      });
    } catch (e) {
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
    final payload = _ratings.entries
        .map((e) => {'musicalId': e.key, 'rating': e.value})
        .toList();
    try {
      await ApiService.rateBatchMusical(payload);
      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text('평점 저장 완료')));
    } catch (e) {
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text('저장 실패: $e')));
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null) {
      return Center(child: Text('에러 발생: $_error'));
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
                  ClipRRect(
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
