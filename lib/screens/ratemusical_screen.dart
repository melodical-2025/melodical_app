import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../widgets/navigationbar.dart';
import '../models/user_provider.dart';

class RatemusicalScreen extends StatefulWidget {
  const RatemusicalScreen({super.key});

  @override
  State<RatemusicalScreen> createState() => _RatemusicalScreenState();
}

class _RatemusicalScreenState extends State<RatemusicalScreen> {
  final List<Map<String, dynamic>> musicalList = [
    {'title': '도리안 그레이', 'image': 'assets/poster.png'},
    {'title': '모리스', 'image': 'assets/poster.png'},
    {'title': '알라딘', 'image': 'assets/poster.png'},
    {'title': '매디슨 카운티의 다리', 'image': 'assets/poster.png'},
  ];

  @override
  Widget build(BuildContext context) {
    final provider = Provider.of<UserProvider>(context);

    return DefaultTabController(
      length: 2,
      child: Scaffold(
        backgroundColor: Colors.white,
        body: Column(
          children: [
            Container(
              height: 130,
              decoration: BoxDecoration(
                color: Colors.white,
                boxShadow: [
                  BoxShadow(
                    color: const Color(0xFFD55D2E),
                    blurRadius: 4,
                    offset: const Offset(5, 0),
                    spreadRadius: 0,
                  ),
                ],
              ),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  const Text(
                    '평가',
                    style: TextStyle(
                      fontFamily: 'Urbanist',
                      color: Color(0xFFD55D2E),
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 8),
                  const TabBar(
                    labelColor: Color(0xFFD55D2E),
                    unselectedLabelColor: Colors.black,
                    labelStyle: TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.bold,
                    ),
                    unselectedLabelStyle: TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.normal,
                    ),
                    indicatorSize: TabBarIndicatorSize.tab,  // 탭 전체 너비에 밑줄
                    indicatorColor: Color(0xFFD55D2E),
                    indicatorWeight: 2,
                    tabs: [
                      Tab(text: '뮤지컬'),
                      Tab(text: '음악'),
                    ],
                  ),

                ],
              ),
            ),

            const Divider(color: Color(0xFFE17951), height: 1, thickness: 1),

            Expanded(
              child: TabBarView(
                children: [
                  // 뮤지컬 평가 탭 내용
                  ListView.builder(
                    padding: const EdgeInsets.all(16),
                    itemCount: musicalList.length,
                    itemBuilder: (context, index) {
                      final musical = musicalList[index];
                      final rating =
                          provider.musicalRatings[musical['title']] ?? 0;

                      return Padding(
                        padding: const EdgeInsets.symmetric(vertical: 8.0),
                        child: Row(
                          children: [
                            ClipRRect(
                              borderRadius: BorderRadius.circular(8),
                              child: Image.asset(
                                musical['image'],
                                width: 60,
                                height: 80,
                                fit: BoxFit.cover,
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    '<${musical['title']}>',
                                    style: const TextStyle(
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                  const SizedBox(height: 4),
                                  Row(
                                    children: List.generate(5, (star) {
                                      return IconButton(
                                        iconSize: 20,
                                        padding: EdgeInsets.zero,
                                        onPressed: () {
                                          provider.rateMusical(
                                              musical['title'], star + 1);
                                        },
                                        icon: Icon(
                                          star < rating
                                              ? Icons.star
                                              : Icons.star_border,
                                          color: Colors.orange,
                                        ),
                                      );
                                    }),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      );
                    },
                  ),

                  // 음악 평가 탭 (빈 화면 예시)
                  const Center(
                    child: Text('음악 평가 기능은 준비 중입니다.'),
                  ),
                ],
              ),
            ),
          ],
        ),
        bottomNavigationBar: BottomNavBar(currentIndex: 2),
      ),
    );
  }
}
