import 'package:flutter/material.dart';
import '../widgets/navigationbar.dart';
import '../widgets/searchbar.dart';

class SearchScreen extends StatelessWidget {
  const SearchScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final TextEditingController _searchController = TextEditingController();

    final List<Map<String, String>> popularMusicals = [
      {'title': '도리안 그레이', 'image': 'assets/poster.png'},
      {'title': '모리스', 'image': 'assets/poster.png'},
      {'title': '알라딘', 'image': 'assets/poster.png'},
      {'title': '매디슨 카운티의 다리', 'image': 'assets/poster.png'},
    ];

    return Scaffold(
      backgroundColor: Colors.white,
      body: Padding(
        padding: const EdgeInsets.only(top: 70),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Center(child: Searchbar(controller: _searchController)),
            const SizedBox(height: 36),
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 44),
              child: Text(
                '인기 작품 랭킹',
                style: TextStyle(
                  color: Color(0xFFEF7B4E),
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
            const SizedBox(height: 24),

            // 뮤지컬 목록 세로 나열
            Expanded(
              child: ListView.builder(
                padding: const EdgeInsets.symmetric(horizontal: 44),
                itemCount: popularMusicals.length,
                itemBuilder: (context, index) {
                  final musical = popularMusicals[index];
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 16),
                    child: Row(
                      children: [
                        ClipRRect(
                          borderRadius: BorderRadius.circular(8),
                          child: Image.asset(
                            musical['image']!,
                            width: 80,
                            height: 120,
                            fit: BoxFit.cover,
                          ),
                        ),
                        const SizedBox(width: 16),
                        Text(
                          musical['title']!,
                          style: const TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.bold,
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
      bottomNavigationBar: BottomNavBar(currentIndex: 1),
    );
  }
}
