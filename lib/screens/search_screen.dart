// lib/screens/search_screen.dart
import 'package:flutter/material.dart';
import '../models/musical.dart';
import '../services/api_service.dart';
import '../widgets/navigationbar.dart';
import '../widgets/searchbar.dart';
import 'detail_screen.dart';

class SearchScreen extends StatefulWidget {
  const SearchScreen({Key? key}) : super(key: key);

  @override
  State<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends State<SearchScreen> {
  final TextEditingController _searchController = TextEditingController();
  List<Musical> _allMusicals = [];
  List<Musical> _filteredMusicals = [];
  List<Musical> _popularMusicals = [];
  List<Map<String, dynamic>> _musicalExtraData = []; // 추가 데이터 저장
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _searchController.addListener(_onSearchChanged);
    _loadMusicals();
  }

  @override
  void dispose() {
    _searchController.removeListener(_onSearchChanged);
    _searchController.dispose();
    super.dispose();
  }

  void _onSearchChanged() {
    final query = _searchController.text.trim().toLowerCase();
    if (query.isEmpty) {
      setState(() {
        _filteredMusicals = [];
      });
    } else {
      // 검색어가 있으면 API를 통해 검색
      _performSearch(query);
    }
  }

  Future<void> _performSearch(String query) async {
    try {
      final results = await ApiService.searchMusicals(query, limit: 50);
      setState(() {
        _filteredMusicals = results.map((data) {
          final period = data['period']?.toString() ?? '';
          final dates = period.split('~');

          String posterUrl = data['posterUrl']?.toString() ?? '';
          if (posterUrl.isEmpty) {
            posterUrl = 'https://via.placeholder.com/150?text=No+Image';
          }

          // 추가 데이터를 Musical 객체에 저장하기 위해 Map으로 저장
          final musical = Musical(
            id: data['id'] ?? 0,
            title: data['title'] ?? '',
            cast: '',
            runtime: '',
            posterUrl: posterUrl,
            theater: data['theater'] ?? '',
            startDate: dates.isNotEmpty ? dates.first.trim() : '',
            endDate: dates.length > 1 ? dates.last.trim() : '',
          );

          return musical;
        }).toList();

        // 추가 데이터를 별도로 저장 (나중에 사용)
        _musicalExtraData = results;
      });
    } catch (e) {
      print('검색 실패: $e');
    }
  }

  Future<void> _loadMusicals() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      // integrated_monthly_dataset의 상위 10개로 인기 차트 구성
      final topList = await ApiService.fetchTopMonthlyMusicals(10);
      setState(() {
        _allMusicals = topList.map((data) {
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
        // 인기 작품은 상위 4개
        _popularMusicals = _allMusicals.length >= 4 ? _allMusicals.sublist(0, 4) : _allMusicals;
        // 추가 데이터도 저장
        _musicalExtraData = topList;
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

  @override
  Widget build(BuildContext context) {
    final isSearching = _searchController.text.trim().isNotEmpty;

    return Scaffold(
      backgroundColor: Colors.white,
      body: Padding(
        padding: const EdgeInsets.only(top: 70),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Center(child: Searchbar(controller: _searchController)),
            const SizedBox(height: 36),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 44),
              child: Text(
                isSearching ? '검색 결과' : '인기 작품 랭킹',
                style: const TextStyle(
                  color: Color(0xFFEF7B4E),
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
            const SizedBox(height: 24),
            Expanded(
              child: _loading
                  ? const Center(child: CircularProgressIndicator())
                  : _error != null
                  ? Center(child: Text('에러: $_error'))
                  : ListView.builder(
                padding: const EdgeInsets.symmetric(horizontal: 44),
                itemCount: isSearching
                    ? _filteredMusicals.length
                    : _popularMusicals.length,
                itemBuilder: (context, index) {
                  final m = isSearching
                      ? _filteredMusicals[index]
                      : _popularMusicals[index];

                  // 추가 데이터 가져오기
                  final extraData = index < _musicalExtraData.length
                      ? _musicalExtraData[index]
                      : {};

                  return Padding(
                    padding: const EdgeInsets.only(bottom: 16),
                    child: GestureDetector(
                      onTap: () async {
                        // API를 통해 최신 데이터 가져오기 (URL 포함)
                        try {
                          final detailData = await ApiService.getMusicalDetail(m.id);
                          if (!context.mounted) return;
                          
                          // 상세 화면으로 이동
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
                          
                          // 에러 시에도 기존 데이터로 이동 (URL 없이)
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (context) => DetailScreen(
                                musicalData: {
                                  'id': m.id,
                                  'title': m.title,
                                  'posterUrl': m.posterUrl,
                                  'theater': m.theater,
                                  'period': '${m.startDate} ~ ${m.endDate}',
                                  'genre': extraData['genre'] ?? '',
                                  'rank': extraData['rank'],
                                  'interparkRating': extraData['interparkRating'],
                                  'yes24Rating': extraData['yes24Rating'],
                                  'rating': extraData['rating'],
                                },
                              ),
                            ),
                          );
                        }
                      },
                      child: Row(
                        children: [
                          ClipRRect(
                            borderRadius: BorderRadius.circular(8),
                            child: Image.network(
                              m.posterUrl,
                              width: 80,
                              height: 120,
                              fit: BoxFit.cover,
                              errorBuilder: (_, __, ___) =>
                                  Container(
                                    width: 80,
                                    height: 120,
                                    color: Colors.grey.shade200,
                                    child: const Icon(Icons.broken_image),
                                  ),
                            ),
                          ),
                          const SizedBox(width: 16),
                          Expanded(
                            child: Text(
                              m.title,
                              style: const TextStyle(
                                fontSize: 15,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: const BottomNavBar(currentIndex: 1),
    );
  }
}
