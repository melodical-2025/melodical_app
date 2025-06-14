// lib/screens/rate_screen.dart
import 'package:flutter/material.dart';
import '../widgets/navigationbar.dart';
import '../widgets/ratemusicaltab.dart';
import '../widgets/ratemusictab.dart';

class RateScreen extends StatelessWidget {
  const RateScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        backgroundColor: Colors.white,
        body: Column(
          children: [
            // ─── 상단 탭 헤더 ───────────────────────────────
            Container(
              height: 130,
              decoration: BoxDecoration(
                color: Colors.white,
                boxShadow: [
                  BoxShadow(
                    color: const Color(0xFFD55D2E),
                    blurRadius: 4,
                    offset: const Offset(5, 0),
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
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
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
            // ─── 탭뷰 ──────────────────────────────────────
            const Expanded(
              child: TabBarView(
                children: [
                  RateMusicalTab(),  // 뮤지컬 평가를 처리하는 탭
                  RateMusicTab(),    // 음악 평가를 처리하는 탭
                ],
              ),
            ),
          ],
        ),
        bottomNavigationBar: const BottomNavBar(currentIndex: 2),
      ),
    );
  }
}
