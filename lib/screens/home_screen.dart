import 'package:capstone/models/musical.dart';
import 'package:flutter/material.dart';
import '../models/song.dart';
import '../widgets/navigationbar.dart';
import '../services/api_service.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final int likedCount = 0;

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

  Future<void> _loadRatedMusicals() async{
    setState(() {
      _loadingMusicals = true;
      _errorMusicals = null;
    });
    try{
      final list = await ApiService.fetchRatedMusicals();
      setState(() {
        _ratedMusicals = list;
      });
    }catch (e){
      setState(() {
        _errorMusicals = e.toString();
      });
    }finally{
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
    final int likedcounts = _ratedMusicals.length + _ratedSongs.length;

    return Scaffold(
      backgroundColor: Colors.white,
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 앱 바
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

          // '나의 음악 및 뮤지컬 취향' 타이틀
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

          // 나의 음악 취향 리스트
          SizedBox(
            height: 150,
            child: _loadingSongs
                ? const Center(child: CircularProgressIndicator())
                : _errorSongs != null
                ? Center(child: Text('에러: \$_errorSongs'))
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

          // ─── '나의 뮤지컬 취향' 타이틀 ────────────────────
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

          // ─── 나의 평가한 뮤지컬 리스트 ─────────────────
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

          // 통계 박스
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 20),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                buildStatBoxWidget(
                  icon: Icons.favorite,
                  assetPath: null,
                  label1: '찜한',
                  label2: '뮤지컬',
                  count: likedcounts,
                  textColor: Colors.black,
                ),
                buildStatBoxWidget(
                  icon: null,
                  assetPath: 'assets/musicalicon.png',
                  label1: '평가한',
                  label2: '뮤지컬',
                  count: ratedMusicals,
                  textColor: Colors.black,
                ),
                buildStatBoxWidget(
                  icon: null,
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

  Widget buildStatBoxWidget({
    IconData? icon,
    String? assetPath,
    required String label1,
    required String label2,
    required int count,
    required Color textColor,
  }) {
    return Container(
      width: 120,
      height: 65,
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF2DB),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Row(
        children: [
          if (assetPath != null)
            Image.asset(
              assetPath,
              width: 25,
              height: 25,
              fit: BoxFit.contain,
            )
          else if (icon != null)
            Icon(
              icon,
              color: Colors.red,
              size: 25,
            ),
          const SizedBox(width: 8),
          Flexible(
            child: Text(
              '$label1\n$label2 $count개',
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.bold,
                color: textColor,
                height: 1.2,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

