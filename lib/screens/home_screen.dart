import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/user_provider.dart';
import '../widgets/navigationbar.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _currentIndex = 0;

  void _onTabTapped(int index) {
    if (index == 0) return; // 현재 페이지이므로 이동 없음
    if (index == 1) Navigator.pushNamed(context, '/search');
    if (index == 2) Navigator.pushNamed(context, '/ratemusical'); // ✅ 평가탭 연결
    if (index == 3) Navigator.pushNamed(context, '/account');
  }

  @override
  Widget build(BuildContext context) {
    final user = Provider.of<UserProvider>(context);

    return Scaffold(
      backgroundColor: const Color(0xFFFFF2DB),
      appBar: AppBar(
        backgroundColor: const Color(0xFFFFF2DB),
        elevation: 0,
        centerTitle: true,
        title: const Text(
          'Melodical',
          style: TextStyle(color: Color(0xFFE17951), fontWeight: FontWeight.bold),
        ),
        iconTheme: const IconThemeData(color: Color(0xFFE17951)),
      ),
      body: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '나의 음악 및 뮤지컬 취향',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              children: [
                ...user.selectedMusicals.map((tag) => _buildTag(tag)),
                ...user.selectedMusics.map((tag) => _buildTag(tag)),
              ],
            ),
            const SizedBox(height: 24),
            const Text(
              '당신의 취향에 꼭 맞는 뮤지컬',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            // 추천 뮤지컬 카드(임시)
            Expanded(
              child: ListView.builder(
                itemCount: 3,
                itemBuilder: (context, index) {
                  return Card(
                    margin: const EdgeInsets.symmetric(vertical: 8),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: ListTile(
                      leading: const Icon(Icons.music_note, color: Color(0xFFE17951)),
                      title: Text('추천 뮤지컬 ${index + 1}'),
                      subtitle: const Text('당신의 취향을 반영한 작품'),
                      trailing: const Icon(Icons.chevron_right),
                    ),
                  );
                },
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

  Widget _buildTag(String label) {
    return Chip(
      label: Text(label),
      backgroundColor: const Color(0xFFFFD9A3),
      labelStyle: const TextStyle(color: Color(0xFFE17951)),
    );
  }
}
