import 'package:flutter/material.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

import '../widgets/navigationbar.dart';
import '../widgets/stat_box.dart';

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

  int _likedMusicals = 0;
  int _ratedMusicals = 0;
  int _ratedSongs = 0;

  @override
  void initState() {
    super.initState();
    _bootstrap();
  }

  Future<void> _bootstrap() async {
    final c = Supabase.instance.client;
    final user = c.auth.currentUser;

    if (user == null) {
      if (!mounted) return;
      Navigator.pushReplacementNamed(context, '/login');
      return;
    }

    String email = user.email ?? '';
    String nickname = '';

    try {
      final row = await c
          .from('profiles')
          .select('nickname')
          .eq('id', user.id)
          .maybeSingle();

      nickname = (row?['nickname'] as String?)?.trim() ?? '';
      if (nickname.isEmpty) {
        nickname = (user.userMetadata?['nickname'] as String?)?.trim() ?? '';
      }
      if (nickname.isEmpty && email.isNotEmpty) {
        nickname = email.split('@').first;
      }

      _likedMusicals = await _countByUser(c, 'liked_musicals', user.id, 'user_id');
      _ratedMusicals = await _countByUser(c, 'rated_musicals', user.id, 'user_id');
      _ratedSongs = await _countByUser(c, 'rated_songs', user.id, 'user_id');
    } catch (e) {
      debugPrint('AccountMyPage load error: $e');
    }

    if (!mounted) return;
    setState(() {
      _nickname = nickname;
      _email = email;
      _loading = false;
    });
  }

  Future<int> _countByUser(
      SupabaseClient client,
      String table,
      String userId,
      String userIdColumn,
      ) async {
    final data = await client.from(table).select('id').eq(userIdColumn, userId);
    return (data as List).length;
  }

  Future<void> _refresh() async => _bootstrap();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      bottomNavigationBar: const BottomNavBar(currentIndex: 3),

      // 🔶 상단: Home과 동일 + 제목 위치 조정 + 폰트 살짝 크게
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
                  // ✅ 살짝 아래로 내린 마이페이지 제목
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
                          fontSize: 24, // 🔼 폰트 키움
                          height: 1.1,
                        ),
                      ),
                    ),
                  ),
                  // ✅ 우측 설정 버튼 유지
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

  // ────────────── 이하 동일 ──────────────
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
        StatBox(
          icon: Icons.favorite,
          label1: '찜한',
          label2: '뮤지컬',
          count: _likedMusicals,
          textColor: Colors.black,
        ),
        StatBox(
          assetPath: 'assets/musicalicon.png',
          label1: '평가한',
          label2: '뮤지컬',
          count: _ratedMusicals,
          textColor: Colors.black,
        ),
        StatBox(
          assetPath: 'assets/musicicon.png',
          label1: '평가한',
          label2: '음악',
          count: _ratedSongs,
          textColor: Colors.black,
        ),
      ],
    );
  }

  Widget _recentReviewCard() {
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
