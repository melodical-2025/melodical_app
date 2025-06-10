import 'package:flutter/material.dart';
import '../models/song.dart';
import '../services/api_service.dart';
import '../widgets/navigationbar.dart';
import '../widgets/ratemusicaltab.dart';
import '../widgets/ratemusictab.dart';

class RateScreen extends StatefulWidget {
  const RateScreen({super.key});
  @override
  State<RateScreen> createState() => _RateScreenState();
}

class _RateScreenState extends State<RateScreen> {
  List<Song> _songs = [];
  bool _loading = true;
  String? _error;
  final Map<String,double> _local = {};

  @override
  void initState() {
    super.initState();
    _loadTop();
  }

  Future<void> _loadTop() async {
    setState(() { _loading = true; _error = null; });
    try {
      final songs = await ApiService.fetchTopMusic();
      setState(() { _songs = songs; });
    } catch (e) {
      _error = e.toString();
    } finally {
      setState(() { _loading = false; });
    }
  }

  Future<void> _submitRatings() async {
    final ratings = _local.entries
        .map((e) => {'songId': e.key, 'rating': e.value})
        .toList();
    try {
      await ApiService.rateBatchMusic(ratings);
      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text('저장 완료')));
    } catch (e) {
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text('저장 실패: $e')));
    }
  }

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
                  RateMusicalTab(),  // 뮤지컬 평가 로직을 여기에 분리
                  RateMusicTab(),    // 음악 평가 로직을 여기에 분리
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