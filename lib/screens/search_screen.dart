// lib/screens/search_screen.dart
import 'package:flutter/material.dart';
import '../models/musical.dart';
import '../services/api_service.dart';
import '../widgets/navigationbar.dart';
import '../widgets/searchbar.dart';

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
      setState(() {
        _filteredMusicals = _allMusicals
            .where((m) => m.title.toLowerCase().contains(query))
            .toList();
      });
    }
  }

  Future<void> _loadMusicals() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final list = await ApiService.fetchAllMusicals();
      setState(() {
        _allMusicals = list;
        // 인기 작품은 처음 4개를 사용
        _popularMusicals = list.length >= 4 ? list.sublist(0, 4) : list;
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
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 16),
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
