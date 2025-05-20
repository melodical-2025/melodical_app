import 'package:flutter/material.dart';
import '../widgets/navigationbar.dart';

class SearchScreen extends StatefulWidget {
  const SearchScreen({super.key});

  @override
  State<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends State<SearchScreen> {
  int _currentIndex = 1; // 현재 페이지가 Search이므로 1로 설정

  void _onTabTapped(int index) {
    if (index == 0) {
      Navigator.pushNamed(context, '/home');
    } else if (index == 2) {
      Navigator.pushNamed(context, '/ratemusical');
    } else if (index == 3) {
      Navigator.pushNamed(context, '/account');
    }
  }

  final TextEditingController _searchController = TextEditingController();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFFF2DB),
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        title: const Text('검색', style: TextStyle(color: Color(0xFFE17951))),
        centerTitle: true,
        iconTheme: const IconThemeData(color: Color(0xFFE17951)),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            // 검색창
            TextField(
              controller: _searchController,
              decoration: InputDecoration(
                hintText: '검색어를 입력하세요...',
                hintStyle: const TextStyle(color: Color(0xFFE17951)),
                prefixIcon: const Icon(Icons.search, color: Color(0xFFE17951)),
                filled: true,
                fillColor: Colors.white,
                border: OutlineInputBorder(
                  borderSide: const BorderSide(color: Color(0xFFFFAD75)),
                  borderRadius: BorderRadius.circular(12),
                ),
                focusedBorder: OutlineInputBorder(
                  borderSide: const BorderSide(color: Color(0xFFE17951), width: 2),
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
            ),
            const SizedBox(height: 24),

            // 향후 검색 결과나 추천 UI를 여기에 추가
            const Expanded(
              child: Center(
                child: Text('여기에 검색 결과가 표시됩니다'),
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: BottomNavBar(
        currentIndex: _currentIndex,
        onTabTapped: _onTabTapped,
      ),
    );
  }
}
