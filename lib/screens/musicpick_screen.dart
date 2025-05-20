import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/user_provider.dart';
import 'home_screen.dart';

class MusicPickScreen extends StatefulWidget {
  const MusicPickScreen({super.key});

  @override
  State<MusicPickScreen> createState() => _MusicPickScreenState();
}

class _MusicPickScreenState extends State<MusicPickScreen> {
  final List<String> musicList = [
    '아이유 - 라일락',
    'BTS - 봄날',
    'Day6 - Love me or Leave me',
    '뉴진스 - Ditto',
    '이무진 - 신호등',
    '백예린 - 그건 아마 우리의 잘못은 아닐 거야',
    '클래식 - EASY',
    '정미라 - 살아있는 것들',
    '이소라 - 바람이 분다',
  ];

  final Set<String> selectedMusics = {};

  void toggleMusic(String music) {
    setState(() {
      if (selectedMusics.contains(music)) {
        selectedMusics.remove(music);
      } else {
        selectedMusics.add(music);
      }
    });
  }

  void proceed() {
    if (selectedMusics.length < 3) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('음악을 3개 이상 선택해주세요.')),
      );
      return;
    }

    // ✅ Provider에 음악 리스트 저장
    final user = Provider.of<UserProvider>(context, listen: false);
    user.updateMusics(selectedMusics.toList());

    // ✅ Home 화면으로 이동
    Navigator.pushReplacementNamed(context, '/home');
  }

  @override
  Widget build(BuildContext context) {
    final user = Provider.of<UserProvider>(context);

    return Scaffold(
      backgroundColor: const Color(0xFFFFF2DB),
      appBar: AppBar(
        title: const Text('좋아하는 음악 선택'),
        backgroundColor: const Color(0xFFFFAD75),
        foregroundColor: const Color(0xFFE17951),
        elevation: 0,
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Text(
              '${user.nickname}님, 좋아하는 음악을 3개 이상 선택해주세요!',
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: Color(0xFFE17951),
              ),
            ),
            const SizedBox(height: 16),
            Expanded(
              child: GridView.count(
                crossAxisCount: 2,
                crossAxisSpacing: 12,
                mainAxisSpacing: 12,
                children: musicList.map((music) {
                  final isSelected = selectedMusics.contains(music);
                  return GestureDetector(
                    onTap: () => toggleMusic(music),
                    child: Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: isSelected ? const Color(0xFFFFD9A3) : Colors.white,
                        border: Border.all(
                          color: const Color(0xFFFFAD75),
                          width: 2,
                        ),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Center(
                        child: Text(
                          music,
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                            fontSize: 14,
                            color: isSelected ? const Color(0xFFE17951) : Colors.black,
                          ),
                        ),
                      ),
                    ),
                  );
                }).toList(),
              ),
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: proceed,
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFFFAD75),
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 16),
                ),
                child: const Text('다음', style: TextStyle(fontSize: 16)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
