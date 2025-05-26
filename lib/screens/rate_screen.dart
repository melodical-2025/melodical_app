import 'package:flutter/material.dart';
import '../widgets/ratemusicaltab.dart';
import '../widgets/ratemusictab.dart';
import '../widgets/navigationbar.dart';

class RateScreen extends StatelessWidget {
  const RateScreen({super.key});

  @override
  Widget build(BuildContext context) {
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
                      fontSize: 24,
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
            const Expanded(
              child: TabBarView(
                children: [
                  RateMusicalTab(),
                  RateMusicTab(),
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
