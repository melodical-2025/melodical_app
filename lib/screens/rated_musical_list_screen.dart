import 'package:flutter/material.dart';
import '../models/musical.dart';
import '../services/api_service.dart';
import 'detail_screen.dart';

class RatedMusicalListScreen extends StatefulWidget {
  const RatedMusicalListScreen({super.key});

  @override
  State<RatedMusicalListScreen> createState() => _RatedMusicalListScreenState();
}

class _RatedMusicalListScreenState extends State<RatedMusicalListScreen> {
  List<Musical> ratedMusicals = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    fetchRatedMusicals();
  }

  void fetchRatedMusicals() async {
    print('🎭 Fetching rated musicals...');
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final musicals = await ApiService.fetchRatedMusicals();
      print('✅ Loaded ${musicals.length} rated musicals');

      setState(() {
        ratedMusicals = musicals;
        _loading = false;
      });
    } catch (e) {
      print('❌ Error loading rated musicals: $e');
      setState(() {
        _error = e.toString();
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: const Text('평가한 뮤지컬'),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black,
        elevation: 1,
      ),
      body: _loading
          ? const Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  CircularProgressIndicator(
                    color: Color(0xFFE17951),
                  ),
                  SizedBox(height: 16),
                  Text(
                    '평가한 뮤지컬을 불러오는 중...',
                    style: TextStyle(fontSize: 16),
                  ),
                ],
              ),
            )
          : _error != null
              ? Center(
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
                          style: const TextStyle(
                            fontSize: 14,
                            color: Colors.black54,
                          ),
                        ),
                        const SizedBox(height: 24),
                        ElevatedButton(
                          onPressed: fetchRatedMusicals,
                          style: ElevatedButton.styleFrom(
                            backgroundColor: const Color(0xFFE17951),
                            padding: const EdgeInsets.symmetric(
                              horizontal: 32,
                              vertical: 12,
                            ),
                          ),
                          child: const Text(
                            '다시 시도',
                            style: TextStyle(
                              fontSize: 16,
                              color: Colors.white,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                )
              : ratedMusicals.isEmpty
                  ? const Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(
                            Icons.music_note,
                            size: 64,
                            color: Colors.grey,
                          ),
                          SizedBox(height: 16),
                          Text(
                            '평가한 뮤지컬이 없습니다.',
                            style: TextStyle(
                              fontSize: 18,
                              color: Colors.grey,
                            ),
                          ),
                        ],
                      ),
                    )
                  : Padding(
                      padding: const EdgeInsets.all(12.0),
                      child: GridView.builder(
                        itemCount: ratedMusicals.length,
                        gridDelegate:
                            const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 3,
                          mainAxisSpacing: 12,
                          crossAxisSpacing: 12,
                          childAspectRatio: 0.55,
                        ),
                        itemBuilder: (context, index) {
                          final m = ratedMusicals[index];
                          return GestureDetector(
                            onTap: () async {
                              // API를 통해 최신 데이터 가져오기 (URL 포함)
                              try {
                                final detailData = await ApiService.getMusicalDetail(m.id);
                                if (!context.mounted) return;
                                
                                // 뮤지컬 상세페이지로 이동
                                Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                    builder: (context) => DetailScreen(
                                      musicalData: detailData,
                                    ),
                                  ),
                                );
                              } catch (e) {
                                print('❌ Failed to fetch musical detail: $e');
                                if (!context.mounted) return;
                                
                                // 에러 시에도 기존 데이터로 이동 (Musical 객체를 Map으로 변환)
                                Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                    builder: (context) => DetailScreen(
                                      musicalData: {
                                        'id': m.id,
                                        'title': m.title,
                                        'posterUrl': m.posterUrl,
                                        'startDate': m.startDate,
                                        'endDate': m.endDate,
                                        'cast': m.cast,
                                        'runtime': m.runtime,
                                        'theater': m.theater,
                                        'period': m.period,
                                        'interparkRating': m.interparkRating,
                                        'yes24Rating': m.yes24Rating,
                                        'interparkUrl': m.interparkUrl,
                                        'yes24Url': m.yes24Url,
                                      },
                                    ),
                                  ),
                                );
                              }
                            },
                            child: Column(
                              children: [
                                Expanded(
                                  child: ClipRRect(
                                    borderRadius: BorderRadius.circular(8),
                                    child: Image.network(
                                      m.posterUrl,
                                      width: double.infinity,
                                      fit: BoxFit.cover,
                                      errorBuilder: (_, __, ___) => Container(
                                        width: double.infinity,
                                        color: Colors.grey.shade200,
                                        child: const Icon(
                                          Icons.theaters,
                                          size: 40,
                                          color: Colors.grey,
                                        ),
                                      ),
                                    ),
                                  ),
                                ),
                                const SizedBox(height: 8),
                                Text(
                                  m.title,
                                  textAlign: TextAlign.center,
                                  style: const TextStyle(
                                    fontSize: 14,
                                    fontWeight: FontWeight.bold,
                                  ),
                                  maxLines: 2,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ],
                            ),
                          );
                        },
                      ),
                    ),
    );
  }
}

