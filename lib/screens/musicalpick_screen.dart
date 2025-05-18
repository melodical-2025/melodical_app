import 'package:flutter/material.dart';
import 'musicpick_screen.dart';


class MusicalpickScreen extends StatefulWidget {
  const MusicalpickScreen({super.key});

  @override
  State<MusicalpickScreen> createState() => _MusicalpickScreenState();
}

class _MusicalpickScreenState extends State<MusicalpickScreen> {
  final List<Map<String, String>> musicals = List.generate(
    20,
        (index) => {
      'title': '뮤지컬 ${index + 1}',
      'image': 'assets/poster.png',
    },
  );

  final Set<int> selectedIndexes = {};
  final ScrollController _scrollController = ScrollController();


  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFFF2DB),
      body: SafeArea(
        child: Stack(
          children: [
            // 콘텐츠 전체 스크롤
            Column(
              children: [
                const Padding(
                  padding: EdgeInsets.fromLTRB(30, 90, 16, 16),
                  child: Align(
                    alignment: Alignment.centerLeft,
                    child: Text.rich(
                      TextSpan(
                        style: TextStyle(
                          fontSize: 30,
                          fontWeight: FontWeight.bold,
                          height: 1.4,
                        ),
                        children: [
                          TextSpan(
                            text: '좋아하는 ',
                            style: TextStyle(color: Colors.black),
                          ),
                          TextSpan(
                            text: '뮤지컬',
                            style: TextStyle(color: Color(0xFFE17951)),
                          ),
                          TextSpan(
                            text: '을\n3개 이상 선택하세요',
                            style: TextStyle(color: Colors.black),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),

                // 리스트 스크롤 영역
                Expanded(
                  child: Stack(
                    children: [
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 16),
                        child: GridView.builder(
                          controller: _scrollController,
                          itemCount: musicals.length,
                          gridDelegate:
                          const SliverGridDelegateWithFixedCrossAxisCount(
                            crossAxisCount: 3,
                            crossAxisSpacing: 12,
                            mainAxisSpacing: 4,
                            childAspectRatio: 0.65,
                          ),
                          padding: const EdgeInsets.only(bottom: 100),
                          itemBuilder: (context, index) {
                            final musical = musicals[index];
                            final isSelected = selectedIndexes.contains(index);

                            return GestureDetector(
                              onTap: () {
                                setState(() {
                                  if (isSelected) {
                                    selectedIndexes.remove(index);
                                  } else {
                                    selectedIndexes.add(index);
                                  }
                                });
                              },
                              child: Column(
                                children: [
                                  Container(
                                    width: 90,
                                    height: 90,
                                    decoration: BoxDecoration(
                                      shape: BoxShape.circle,
                                      border: Border.all(
                                        color: isSelected
                                            ? const Color(0xFFE17951)
                                            : Colors.transparent,
                                        width: 3,
                                      ),
                                      image: DecorationImage(
                                        image: AssetImage(musical['image']!),
                                        fit: BoxFit.cover,
                                      ),
                                    ),
                                  ),
                                  const SizedBox(height: 8),
                                  Text(
                                    musical['title']!,
                                    textAlign: TextAlign.center,
                                    style: const TextStyle(
                                      color: Colors.black,
                                      fontSize: 15,
                                      fontWeight: FontWeight.w500,
                                    ),
                                  ),
                                ],
                              ),
                            );
                          },
                        ),
                      ),

                      // 하단 그라데이션
                        Positioned(
                          left: 0,
                          right: 0,
                          bottom: 0,
                          height: 95,
                          child: IgnorePointer(
                            child: Container(
                              decoration: const BoxDecoration(
                                gradient: LinearGradient(
                                  begin: Alignment.topCenter,
                                  end: Alignment.bottomCenter,
                                  colors: [
                                    Colors.transparent,
                                    Colors.black45,
                                  ],
                                ),
                              ),
                            ),
                          ),
                        ),
                    ],
                  ),
                ),
              ],
            ),

            // 선택 완료 버튼 (화면 하단 고정)
            Positioned(
              left: 16,
              right: 16,
              bottom: 24,
              child: ElevatedButton(
                onPressed: selectedIndexes.length >= 3
                    ? () {
                  Navigator.pushNamed(context, '/musicpick');
                }
                    : null, // 눌렀을때 동작 정의안됨 아직
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFFFAD75),
                  foregroundColor: const Color(0xFFE17951),
                  disabledBackgroundColor: Colors.grey.shade300,
                  disabledForegroundColor: Colors.grey,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                  ),
                ),
                child: selectedIndexes.length >= 3
                    ? Stack(
                  children: [
                    // 외곽선 텍스트 (Stroke)
                    Text(
                      '선택 완료',
                      style: TextStyle(
                        fontSize: 16,
                        foreground: Paint()
                          ..style = PaintingStyle.stroke
                          ..strokeWidth = 2
                          ..color = const Color(0xFFFFE5B6),
                      ),
                    ),
                    // 내부 텍스트
                    const Text(
                      '선택 완료',
                      style: TextStyle(
                        fontSize: 16,
                        color: Color(0xFFE17951),
                      ),
                    ),
                  ],
                )
                : const Text(
                  '선택 완료',
                  style: TextStyle(fontSize: 16),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
