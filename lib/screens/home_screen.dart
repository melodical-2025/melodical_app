import 'package:capstone/models/musical.dart';
import 'package:flutter/material.dart';
import '../models/song.dart';
import '../widgets/navigationbar.dart';
import '../services/api_service.dart';
import '../widgets/bubblechart.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  List<Musical> _ratedMusicals = [];
  bool _loadingMusicals = true;
  String? _errorMusicals;

  List<Song> _ratedSongs = [];
  bool _loadingSongs = true;
  String? _errorSongs;

  // 예시: 장르별 선호도 데이터 (0~1)
  Map<String, double> _genrePreferences = {
    '팝': 0.9,
    '재즈': 0.5,
    '하우스': 0.8,
    '알앤비': 0.6,
    '락': 0.4,
  };

  @override
  void initState() {
    super.initState();
    _loadRatedSongs();
    _loadRatedMusicals();
  }

  Future<void> _loadRatedMusicals() async {
    setState(() {
      _loadingMusicals = true;
      _errorMusicals = null;
    });
    try {
      final list = await ApiService.fetchRatedMusicals();
      setState(() {
        _ratedMusicals = list;
      });
    } catch (e) {
      setState(() {
        _errorMusicals = e.toString();
      });
    } finally {
      setState(() {
        _loadingMusicals = false;
      });
    }
  }

  Future<void> _loadRatedSongs() async {
    setState(() {
      _loadingSongs = true;
      _errorSongs = null;
    });
    try {
      final songs = await ApiService.fetchRatedMusicByUser();
      setState(() {
        _ratedSongs = songs;
      });
    } catch (e) {
      setState(() {
        _errorSongs = e.toString();
      });
    } finally {
      setState(() {
        _loadingSongs = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // ─ AppBar 대용 헤더 ─
            Container(
              height: 110,
              decoration: BoxDecoration(
                color: Colors.white,
                boxShadow: [
                  BoxShadow(
                    color: const Color(0xFFE17951),
                    blurRadius: 4,
                    offset: const Offset(5, 0),
                  ),
                ],
              ),
              child: const Padding(
                padding: EdgeInsets.only(bottom: 16),
                child: Align(
                  alignment: Alignment.bottomCenter,
                  child: Text(
                    'Melodical',
                    style: TextStyle(
                      color: Color(0xFFE17951),
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ),
            ),

            // ─ 나의 음악 장르 취향 ─
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 20.0, vertical: 16.0),
              child: Text(
                '나의 장르 취향',
                style: TextStyle(
                  fontSize: 25,
                  fontWeight: FontWeight.bold,
                  color: Colors.black,
                ),
              ),
            ),

            // ─ 장르 버블차트 ─
            GenreBubbleChart(genrePreferences: _genrePreferences),

            const SizedBox(height: 28),

            // ─ 나의 뮤지컬 리스트 ─
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 20.0, vertical: 16.0),
              child: Text(
                '나의 뮤지컬 취향',
                style: TextStyle(
                  fontSize: 25,
                  fontWeight: FontWeight.bold,
                  color: Colors.black,
                ),
              ),
            ),
            SizedBox(
              height: 150,
              child: _loadingMusicals
                  ? const Center(child: CircularProgressIndicator())
                  : _errorMusicals != null
                  ? Center(child: Text('에러: $_errorMusicals'))
                  : ListView.builder(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 16.0),
                itemCount: _ratedMusicals.length,
                itemBuilder: (context, index) {
                  final m = _ratedMusicals[index];
                  return Padding(
                    padding: const EdgeInsets.only(right: 18.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        ClipRRect(
                          borderRadius: BorderRadius.circular(10),
                          child: Image.network(
                            m.posterUrl,
                            width: 100,
                            height: 100,
                            fit: BoxFit.cover,
                            errorBuilder: (_, __, ___) => Container(
                              width: 100,
                              height: 100,
                              color: Colors.grey.shade200,
                              child: const Icon(Icons.broken_image),
                            ),
                          ),
                        ),
                        const SizedBox(height: 8),
                        SizedBox(
                          width: 100,
                          child: Text(
                            m.title,
                            style: const TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.bold,
                            ),
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),
            const SizedBox(height: 40), // 하단 여유
          ],
        ),
      ),
      bottomNavigationBar: const BottomNavBar(currentIndex: 0),
    );
  }
}

