import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/user_provider.dart';

class MusicalPickScreen extends StatefulWidget {
  const MusicalPickScreen({super.key});

  @override
  State<MusicalPickScreen> createState() => _MusicalPickScreenState();
}

class _MusicalPickScreenState extends State<MusicalPickScreen> {
  final List<String> musicalList = [
    '레미제라블',
    '오페라의 유령',
    '위키드',
    '맨 오브 라만차',
    '지킬 앤 하이드',
    '시카고',
    '킹키부츠',
    '모차르트!',
    '웃는 남자',
  ];

  final Set<String> selectedMusicals = {};

  void toggleMusical(String musical) {
    setState(() {
      if (selectedMusicals.contains(musical)) {
        selectedMusicals.remove(musical);
      } else {
        selectedMusicals.add(musical);
      }
    });
  }

  void proceed() {
    if (selectedMusicals.length < 3) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('뮤지컬을 3개 이상 선택해주세요.')),
      );
      return;
    }

    // ✅ Provider에 저장
    final user = Provider.of<UserProvider>(context, listen: false);
    user.updateMusicals(selectedMusicals.toList());

    // ✅ 다음 화면으로 이동
    Navigator.pushReplacementNamed(context, '/musicpick');
  }

  @override
  Widget build(BuildContext context) {
    final user = Provider.of<UserProvider>(context);

    return Scaffold(
      backgroundColor: const Color(0xFFFFF2DB),
      appBar: AppBar(
        title: const Text('좋아하는 뮤지컬 선택'),
        backgroundColor: const Color(0xFFFFAD75),
        foregroundColor: const Color(0xFFE17951),
        elevation: 0,
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Text(
              '${user.nickname}님, 좋아하는 뮤지컬을 3개 이상 선택해주세요!',
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
                children: musicalList.map((musical) {
                  final isSelected = selectedMusicals.contains(musical);
                  return GestureDetector(
                    onTap: () => toggleMusical(musical),
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
                          musical,
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
