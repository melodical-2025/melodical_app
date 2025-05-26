import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:flutter_rating_bar/flutter_rating_bar.dart';
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
                children: const [
                  Text(
                    '평가',
                    style: TextStyle(
                      fontFamily: 'Urbanist',
                      color: Color(0xFFD55D2E),
                      fontSize: 22,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  SizedBox(height: 0),
                  TabBar(
                    labelColor: Color(0xFFD55D2E),
                    unselectedLabelColor: Colors.black,
                    labelStyle: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                    unselectedLabelStyle: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.normal,
                    ),
                    indicatorSize: TabBarIndicatorSize.tab,
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
                  // 뮤지컬 평가 탭
                  ListView.builder(
                    padding: const EdgeInsets.all(16),
                    itemCount: musicalList.length,
                    itemBuilder: (context, index) {
                      final musical = musicalList[index];
                      final rating =
                          provider.musicalRatings[musical['title']] ?? 0.0;

                      return Padding(
                        padding: const EdgeInsets.symmetric(vertical: 16.0),
                        child: Row(
                          children: [
                            ClipRRect(
                              borderRadius: BorderRadius.circular(8),
                              child: Image.asset(
                                musical['image'],
                                width: 80,
                                height: 110,
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
                                      fontSize: 18,
                                    ),
                                  ),
                                  const SizedBox(height: 4),
                                  RatingBar.builder(
                                    initialRating: rating,
                                    minRating: 0,
                                    allowHalfRating: true,
                                    itemCount: 5,
                                    itemSize: 32,
                                    itemPadding: const EdgeInsets.symmetric(horizontal: 2),
                                    unratedColor: Colors.grey.shade300,
                                    itemBuilder: (context, _) => const Icon(
                                      Icons.star,
                                      color: Colors.orange,
                                    ),
                                    onRatingUpdate: (newRating) {
                                      provider.rateMusical(
                                          musical['title'], newRating);
                                    },
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      );
                    },
                  ),

                  // 음악 평가 탭 (미구현)
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
