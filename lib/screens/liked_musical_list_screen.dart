import 'package:flutter/material.dart';
import '../models/musical.dart';
import '../services/api_service.dart';
import 'detail_screen.dart';

class LikedMusicalListScreen extends StatefulWidget {
  const LikedMusicalListScreen({super.key});

  @override
  State<LikedMusicalListScreen> createState() => _LikedMusicalListScreenState();
}

class _LikedMusicalListScreenState extends State<LikedMusicalListScreen> {
  List<Map<String, dynamic>> likedMusicals = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    fetchLikedMusicals();
  }

  void fetchLikedMusicals() async {
    print('💙 Fetching liked musicals...');
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      // 실제 찜한 뮤지컬 API 호출
      final musicals = await ApiService.fetchLikedMusicals();
      print('✅ Loaded ${musicals.length} liked musicals');

      setState(() {
        likedMusicals = musicals;
        _loading = false;
      });
    } catch (e) {
      print('❌ Error loading liked musicals: $e');
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
        title: const Text('관심있는 뮤지컬'),
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
                    '관심있는 뮤지컬을 불러오는 중...',
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
                          onPressed: fetchLikedMusicals,
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
              : likedMusicals.isEmpty
                  ? const Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(
                            Icons.favorite_border,
                            size: 64,
                            color: Colors.grey,
                          ),
                          SizedBox(height: 16),
                          Text(
                            '관심있는 뮤지컬이 없습니다.',
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
                        itemCount: likedMusicals.length,
                        gridDelegate:
                            const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 3,
                          mainAxisSpacing: 12,
                          crossAxisSpacing: 12,
                          childAspectRatio: 0.55, // 0.6에서 0.55로 감소 (높이 증가)
                        ),
                        itemBuilder: (context, index) {
                          final m = likedMusicals[index];
                          return GestureDetector(
                            onTap: () async {
                              // API를 통해 최신 데이터 가져오기 (URL 포함)
                              final musicalId = m['id'];
                              if (musicalId == null) {
                                print('❌ No musical ID in liked musical data');
                                return;
                              }
                              
                              try {
                                final detailData = await ApiService.getMusicalDetail(musicalId);
                                if (!context.mounted) return;
                                
                                // 뮤지컬 상세페이지로 이동
                                Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                    builder: (context) => DetailScreen(musicalData: detailData),
                                  ),
                                );
                              } catch (e) {
                                print('❌ Failed to fetch musical detail: $e');
                                if (!context.mounted) return;
                                
                                // 에러 시에도 기존 데이터로 이동 (URL 없이)
                                Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                    builder: (context) => DetailScreen(musicalData: m),
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
                                      m['posterUrl'] ?? '',
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
                                  m['title'] ?? '',
                                  textAlign: TextAlign.center,
                                  style: const TextStyle(
                                    fontSize: 14, // 16에서 14로 감소
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
