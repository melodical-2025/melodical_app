import 'package:capstone/models/musical.dart';
import 'package:flutter/material.dart';
import '../models/song.dart';
import '../widgets/navigationbar.dart';
import '../widgets/stat_box.dart';            // ✅ 공용 허드 위젯
import '../services/api_service.dart';

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
      // DB에서 현재 userId에 해당하는 평가된 음악만 가져오는 API 호출
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
    final int ratedSongs = _ratedSongs.length;
    final int ratedMusicals = _ratedMusicals.length;
    // 예시: 찜 개수는 임시로 두 리스트 합 (실제로는 찜 API/DB로 교체 권장)
    final int likedcounts = _ratedMusicals.length + _ratedSongs.length;

    return Scaffold(
      backgroundColor: Colors.white,
      body: Column(
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

          // ─ 나의 음악 취향 ─
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 20.0, vertical: 16.0),
            child: Text(
              '나의 음악 취향',
              style: TextStyle(
                fontSize: 25,
                fontWeight: FontWeight.bold,
                color: Colors.black,
              ),
            ),
          ),
          SizedBox(
            height: 150,
            child: _loadingSongs
                ? const Center(child: CircularProgressIndicator())
                : _errorSongs != null
                ? Center(child: Text('에러: $_errorSongs'))
                : ListView.builder(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 16.0),
              itemCount: _ratedSongs.length,
              itemBuilder: (context, index) {
                final song = _ratedSongs[index];
                return Padding(
                  padding: const EdgeInsets.only(right: 18.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      ClipRRect(
                        borderRadius: BorderRadius.circular(10),
                        child: Image.network(
                          song.artworkUrl,
                          width: 100,
                          height: 100,
                          fit: BoxFit.cover,
                        ),
                      ),
                      const SizedBox(height: 8),
                      SizedBox(
                        width: 100,
                        child: Text(
                          song.title,
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

          const SizedBox(height: 24),

          // ─ 나의 뮤지컬 취향 ─
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

          const Spacer(),

          // ─ 통계 허드(공용 위젯) ─
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 20),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                StatBox(
                  icon: Icons.favorite,
                  label1: '찜한',
                  label2: '뮤지컬',
                  count: likedcounts,
                  textColor: Colors.black,
                ),
                StatBox(
                  assetPath: 'assets/musicalicon.png',
                  label1: '평가한',
                  label2: '뮤지컬',
                  count: ratedMusicals,
                  textColor: Colors.black,
                ),
                StatBox(
                  assetPath: 'assets/musicicon.png',
                  label1: '평가한',
                  label2: '음악',
                  count: ratedSongs,
                  textColor: Colors.black,
                ),
              ],
            ),
          ),
        ],
      ),
      bottomNavigationBar: const BottomNavBar(currentIndex: 0),
    );
  }
}
