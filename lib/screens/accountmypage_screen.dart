import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../widgets/navigationbar.dart';
import '../widgets/stat_box.dart';
import '../models/user_provider.dart';
import '../services/api_service.dart';
import '../models/musical.dart';
import '../models/song.dart';


class AccountMyPageScreen extends StatefulWidget {
  const AccountMyPageScreen({super.key});

  @override
  State<AccountMyPageScreen> createState() => _AccountMyPageScreenState();
}

class _AccountMyPageScreenState extends State<AccountMyPageScreen> {
  static const primary = Color(0xFFE17951);
  static const line = Color(0xFFFFE5B6);

  bool _loading = true;
  String _nickname = '';
  String _email = '';

  int _likedMusicals = 0;   // ⚠️ 백엔드 API가 준비되면 실제 값으로 교체
  int _ratedMusicals = 0;
  int _ratedSongs = 0;

  @override
  void initState() {
    super.initState();
    _bootstrap();
  }

  Future<void> _bootstrap() async {
    // 사용자 기본 정보: Provider에서 가져옵니다 (signup/login 후 UserProvider에 세팅되어 있다고 가정)
    final user = Provider.of<UserProvider>(context, listen: false);
    final nickname = (user.nickname).trim();
    final email = (user.email).trim();

    // 통계값은 기존 HomeScreen에서 쓰던 API를 재사용합니다.
    List<Musical> ratedM = [];
    List<Song> ratedS = [];

    try {
      ratedM = await ApiService.fetchRatedMusicals();   // 사용자 평가 뮤지컬
    } catch (e) {
      // 필요시 로깅
    }

    try {
      ratedS = await ApiService.fetchRatedMusicByUser(); // 사용자 평가 음악
    } catch (e) {
      // 필요시 로깅
    }

    // ⚠️ 찜한 뮤지컬 수는 아직 전용 API가 없다고 가정 → 0으로 표시(나중에 API 생기면 교체)
    // 만약 ApiService.fetchLikedMusicals()가 있다면 아래처럼 교체하세요:
    // try {
    //   final likedList = await ApiService.fetchLikedMusicals();
    //   _likedMusicals = likedList.length;
    // } catch (_) {}

    if (!mounted) return;
    setState(() {
      _nickname = nickname.isEmpty && email.isNotEmpty ? email.split('@').first : (nickname.isEmpty ? '(닉네임 없음)' : nickname);
      _email = email;
      _ratedMusicals = ratedM.length;
      _ratedSongs = ratedS.length;
      _likedMusicals = 0; // TODO: 전용 API 연동 시 변경
      _loading = false;
    });
  }

  Future<void> _refresh() async => _bootstrap();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      bottomNavigationBar: const BottomNavBar(currentIndex: 3),

      // 상단 헤더: Home과 통일 + 제목 살짝 아래 + 폰트 24 + 우측 설정 버튼
      appBar: PreferredSize(
        preferredSize: const Size.fromHeight(110),
        child: Container(
          height: 110,
          decoration: const BoxDecoration(
            color: Colors.white,
            boxShadow: [
              BoxShadow(
                color: Color(0xFFE17951),
                blurRadius: 4,
                offset: Offset(5, 0),
              ),
            ],
            border: Border(
              bottom: BorderSide(color: line, width: 1),
            ),
          ),
          child: SafeArea(
            bottom: false,
            child: Padding(
              padding: const EdgeInsets.only(bottom: 16, left: 12, right: 8),
              child: Stack(
                alignment: Alignment.bottomCenter,
                children: [
                  const Positioned(
                    bottom: 4,
                    left: 0,
                    right: 0,
                    child: Center(
                      child: Text(
                        '마이페이지',
                        style: TextStyle(
                          color: primary,
                          fontWeight: FontWeight.bold,
                          fontSize: 24,
                          height: 1.1,
                        ),
                      ),
                    ),
                  ),
                  Positioned(
                    right: 0,
                    bottom: 0,
                    child: IconButton(
                      icon: const Icon(Icons.settings_outlined, color: primary),
                      onPressed: () => Navigator.pushNamed(context, '/account'),
                      tooltip: '설정',
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),

      body: _loading
          ? const Center(child: CircularProgressIndicator(color: primary))
          : RefreshIndicator(
        color: primary,
        onRefresh: _refresh,
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Padding(
                padding: const EdgeInsets.fromLTRB(20, 16, 20, 24),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _profileHeader(),
                    const SizedBox(height: 16),
                    _statsRow(),
                    const SizedBox(height: 20),
                    _recentReviewCard(),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  // ────────────── UI ──────────────
  Widget _profileHeader() {
    return Row(
      children: [
        const CircleAvatar(
          radius: 26,
          backgroundColor: Color(0xFFFFD9A3),
          child: Icon(Icons.music_note, color: primary, size: 22),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                _nickname.isEmpty ? '(닉네임 없음)' : _nickname,
                style: const TextStyle(
                  color: primary,
                  fontWeight: FontWeight.w700,
                  fontSize: 16,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                _email,
                style: const TextStyle(color: Colors.grey, fontSize: 12),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _statsRow() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: [
        GestureDetector(
          onTap: () => Navigator.pushNamed(context, '/liked-musicals'),
          child: StatBox(
            icon: Icons.favorite,
            label1: '관심있는',
            label2: '뮤지컬',
            count: _likedMusicals,
            textColor: Colors.black,
          ),
        ),
        GestureDetector(
          onTap: () => Navigator.pushNamed(context, '/rated-musicals'),
          child: StatBox(
            assetPath: 'assets/musicalicon.png',
            label1: '평가한',
            label2: '뮤지컬',
            count: _ratedMusicals,
            textColor: Colors.black,
          ),
        ),
        GestureDetector(
          onTap: () => Navigator.pushNamed(context, '/rated-songs'),
          child: StatBox(
            assetPath: 'assets/musicicon.png',
            label1: '평가한',
            label2: '음악',
            count: _ratedSongs,
            textColor: Colors.black,
          ),
        ),
      ],
    );
  }



  Widget _recentReviewCard() {
    // TODO: 백엔드 후기 API 연결 시 최신 1건 표시로 교체
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white,
        border: const Border.fromBorderSide(BorderSide(color: line)),
        borderRadius: BorderRadius.circular(12),
        boxShadow: const [
          BoxShadow(
            color: Color(0x14000000),
            blurRadius: 6,
            offset: Offset(0, 1),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '나의 최근 후기',
            style: TextStyle(
              color: primary,
              fontWeight: FontWeight.bold,
              fontSize: 13,
            ),
          ),
          const SizedBox(height: 8),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: const Color(0xFFFDF4E3),
              borderRadius: BorderRadius.circular(8),
            ),
            child: const Text(
              '샘플 후기 내용입니다.\n(여기에 실제 최신 후기를 연결합니다)',
              style: TextStyle(color: primary, fontSize: 12, height: 1.4),
            ),
          ),
          const SizedBox(height: 6),
          Align(
            alignment: Alignment.centerRight,
            child: Text(
              _yyyymmdd(DateTime.now()),
              style: const TextStyle(color: Colors.grey, fontSize: 10),
            ),
          ),
        ],
      ),
    );
  }

  String _yyyymmdd(DateTime d) =>
      '${d.year}.${d.month.toString().padLeft(2, '0')}.${d.day.toString().padLeft(2, '0')}';
}
