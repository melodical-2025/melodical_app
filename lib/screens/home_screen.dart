import 'package:capstone/models/musical.dart';
import 'package:flutter/material.dart';
import '../models/song.dart';
import '../widgets/navigationbar.dart';
import '../services/api_service.dart';
import '../widgets/bubblechart.dart';
import '../widgets/custom_header.dart';

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
            const CustomHeader(title: 'Melodical'),

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

            // ─ 뮤지컬추천 ─
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 20.0, vertical: 16.0),
              child: Text(
                '당신을 위한 뮤지컬 추천',
                style: TextStyle(
                  fontSize: 25,
                  fontWeight: FontWeight.bold,
                  color: Colors.black,
                ),
              ),
            ),
            SizedBox(
              height: 200,
              child: ListView.builder(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 16.0),
                itemCount: 5, // 테스트용으로 5개만 표시
                itemBuilder: (context, index) {
                  final sampleImages = [
                    'https://picsum.photos/200?random=1',
                    'https://picsum.photos/200?random=2',
                    'https://picsum.photos/200?random=3',
                    'https://picsum.photos/200?random=4',
                    'https://picsum.photos/200?random=5',
                  ];
                  final imageUrl = sampleImages[index % sampleImages.length];
                  return Padding(
                    padding: const EdgeInsets.only(right: 18.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        ClipRRect(
                          borderRadius: BorderRadius.circular(10),
                          child: Image.network(
                            imageUrl,
                            width: 120,
                            height: 160,
                            fit: BoxFit.cover,
                            errorBuilder: (_, __, ___) => Container(
                              width: 120,
                              height: 160,
                              color: Colors.grey.shade200,
                              child: const Icon(Icons.broken_image),
                            ),
                          ),
                        ),
                        const SizedBox(height: 8),
                        const SizedBox(
                          width: 100,
                          child: Text(
                            '뮤지컬 제목',
                            style: TextStyle(
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

            // ─ 내가 관심있는 뮤지컬 ─
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 20.0, vertical: 16.0),
              child: Text(
                '내가 관심있는 뮤지컬',
                style: TextStyle(
                  fontSize: 25,
                  fontWeight: FontWeight.bold,
                  color: Colors.black,
                ),
              ),
            ),
            SizedBox(
              height: 200,
              child: ListView.builder(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 16.0),
                itemCount: 5, // 테스트용으로 5개만 표시
                itemBuilder: (context, index) {
                  final sampleImages = [
                    'https://picsum.photos/200?random=1',
                    'https://picsum.photos/200?random=2',
                    'https://picsum.photos/200?random=3',
                    'https://picsum.photos/200?random=4',
                    'https://picsum.photos/200?random=5',
                  ];
                  final imageUrl = sampleImages[index % sampleImages.length];
                  return Padding(
                    padding: const EdgeInsets.only(right: 18.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        ClipRRect(
                          borderRadius: BorderRadius.circular(10),
                          child: Image.network(
                            imageUrl,
                            width: 120,
                            height: 160,
                            fit: BoxFit.cover,
                            errorBuilder: (_, __, ___) => Container(
                              width: 120,
                              height: 160,
                              color: Colors.grey.shade200,
                              child: const Icon(Icons.broken_image),
                            ),
                          ),
                        ),
                        const SizedBox(height: 8),
                        const SizedBox(
                          width: 100,
                          child: Text(
                            '뮤지컬 제목',
                            style: TextStyle(
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
          ],
        ),
      ),

      bottomNavigationBar: const BottomNavBar(currentIndex: 0),
    );
  }
}
