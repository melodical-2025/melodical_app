import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../widgets/navigationbar.dart';
import '../widgets/stat_box.dart';
import '../models/user_provider.dart';
import '../services/api_service.dart';
import '../models/musical.dart';
import '../models/song.dart';
import '../models/comment.dart';
import '../repositories/comment_repository.dart';
import 'detail_screen.dart';  // DetailScreen import 추가

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

  int _likedMusicals = 0;  // ⚠️ 찜 API 생기면 교체
  int _ratedMusicals = 0;
  int _ratedSongs = 0;

  // ── 리뷰(댓글) 상태 ───────────────────────────────────────────────
  final CommentRepository _commentRepository = CommentRepository();
  final List<Comment> _reviews = [];
  final ScrollController _scroll = ScrollController();

  bool _hasMore = false;
  bool _loadingMore = false;
  // ────────────────────────────────────────────────────────────────────

  @override
  void initState() {
    super.initState();
    _scroll.addListener(_onScroll);
    _bootstrap();        // 프로필/통계 + 첫 페이지 로드
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    // 화면이 다시 표시될 때마다 사용자 정보 새로고침
    _refreshUserInfo();
  }

  @override
  void dispose() {
    _scroll.removeListener(_onScroll);
    _scroll.dispose();
    super.dispose();
  }

  Future<void> _refreshUserInfo() async {
    try {
      final userInfo = await ApiService.getCurrentUser();
      if (!mounted) return;
      
      setState(() {
        _nickname = userInfo['nickname'] ?? userInfo['name'] ?? '(닉네임 없음)';
        _email = userInfo['email'] ?? '';
      });
    } catch (e) {
      print('⚠️ Failed to refresh user info: $e');
    }
  }

  Future<void> _bootstrap() async {
    setState(() {
      _loading = true;
    });

    // 통계값
    List<Musical> ratedM = [];
    List<Song> ratedS = [];
    int likedCount = 0;

    try {
      // 최신 사용자 정보를 API에서 가져오기
      final userInfo = await ApiService.getCurrentUser();
      _nickname = userInfo['nickname'] ?? userInfo['name'] ?? '(닉네임 없음)';
      _email = userInfo['email'] ?? '';
      print('✅ Loaded user info: nickname=$_nickname, email=$_email');
    } catch (e) {
      print('⚠️ Failed to load user info: $e');
      // Fallback to UserProvider
      final user = Provider.of<UserProvider>(context, listen: false);
      _nickname = user.nickname.trim();
      _email = user.email.trim();
      if (_nickname.isEmpty && _email.isNotEmpty) {
        _nickname = _email.split('@').first;
      }
      if (_nickname.isEmpty) {
        _nickname = '(닉네임 없음)';
      }
    }

    try {
      ratedM = await ApiService.fetchRatedMusicals();   // 사용자 평가 뮤지컬
    } catch (e) {
      print('⚠️ Failed to load rated musicals: $e');
    }

    try {
      ratedS = await ApiService.fetchRatedMusicByUser(); // 사용자 평가 음악
    } catch (e) {
      print('⚠️ Failed to load rated songs: $e');
    }

    try {
      likedCount = await ApiService.getFavoriteCount(); // 찜 개수
    } catch (e) {
      print('⚠️ Failed to load favorite count: $e');
    }

    if (!mounted) return;
    setState(() {
      _ratedMusicals = ratedM.length;
      _ratedSongs = ratedS.length;
      _likedMusicals = likedCount;
      _loading = false;
    });

    // ✅ 댓글 로드
    await _loadReviews();
  }

  // ── 스크롤 끝 근처 감지 (향후 페이지네이션 시 사용 가능) ─────────────────
  void _onScroll() {
    // 현재는 모든 댓글을 한 번에 로드하므로 무한스크롤 불필요
  }

  // ── 새로고침 ────────────────────────────────────────────────────────
  Future<void> _refresh() async {
    await _loadReviews();
  }

  Future<void> _loadReviews() async {
    try {
      final comments = await _commentRepository.getMyComments();
      if (!mounted) return;
      setState(() {
        _reviews.clear();
        _reviews.addAll(comments);
        _hasMore = false;
      });
    } catch (e) {
      print('❌ Error loading my comments: $e');
      if (!mounted) return;
      setState(() {
        _reviews.clear();
        _hasMore = false;
      });
    }
  }

  // ────────────────────────────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      bottomNavigationBar: const BottomNavBar(currentIndex: 3),

      // 상단 헤더 (기존 스타일 유지)
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
          controller: _scroll, // ✅ 무한스크롤을 위한 컨트롤러 연결
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
                    _reviewsSection(),
                    const SizedBox(height: 12),
                    _infiniteFooter(), // ✅ 로딩/더 없음 표시
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

  // ── 리뷰 섹션(리스트 + 무한스크롤) ────────────────────────────────────
  Widget _reviewsSection() {
    final count = _reviews.length;

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
          Row(
            children: [
              const Expanded(
                child: Text(
                  '내가 쓴 후기',
                  style: TextStyle(
                    color: primary,
                    fontWeight: FontWeight.bold,
                    fontSize: 13,
                  ),
                ),
              ),
              Text(
                '총 $count개',
                style: const TextStyle(color: Colors.grey, fontSize: 12),
              ),
            ],
          ),
          const SizedBox(height: 8),

          if (count == 0)
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: const Color(0xFFFDF4E3),
                borderRadius: BorderRadius.circular(8),
              ),
              child: const Text(
                '아직 작성한 후기가 없어요.',
                style: TextStyle(color: primary, fontSize: 12, height: 1.4),
              ),
            )
          else
            ListView.separated(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: _reviews.length,
              separatorBuilder: (_, __) => const SizedBox(height: 8),
              itemBuilder: (context, index) {
                final r = _reviews[index];
                return GestureDetector(
                  onTap: () async {
                    // 뮤지컬 상세페이지로 이동
                    if (r.musicalId != null) {
                      try {
                        // musicalId로 뮤지컬 데이터 가져오기
                        final musicalData = await ApiService.fetchMonthlyMusicals();
                        final musical = musicalData.firstWhere(
                          (m) => m['id'] == r.musicalId,
                          orElse: () => {},
                        );
                        
                        if (musical.isNotEmpty && mounted) {
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (context) => DetailScreen(musicalData: musical),
                            ),
                          );
                        }
                      } catch (e) {
                        print('Error navigating to musical: $e');
                      }
                    }
                  },
                  child: _reviewItemCard(
                    musicalTitle: r.musicalTitle,  // 뮤지컬 제목 추가
                    title: null,  // 댓글에는 제목이 없음
                    rating: null,  // 댓글에는 평점이 없음
                    content: r.content,
                    date: r.createdAt,
                  ),
                );
              },
            ),
        ],
      ),
    );
  }

  Widget _reviewItemCard({
    String? musicalTitle,  // 뮤지컬 제목 추가
    String? title,
    double? rating,
    required String content,
    required DateTime date,
  }) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: const Color(0xFFFDF4E3),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (musicalTitle != null && musicalTitle.isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(bottom: 6),
              child: Row(
                children: [
                  const Icon(Icons.theater_comedy, size: 14, color: primary),
                  const SizedBox(width: 4),
                  Expanded(
                    child: Text(
                      musicalTitle,
                      style: const TextStyle(
                        color: primary,
                        fontWeight: FontWeight.w700,
                        fontSize: 13,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
            ),
          if (title != null && title.isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(bottom: 6),
              child: Text(
                title,
                style: const TextStyle(
                  color: primary,
                  fontWeight: FontWeight.w700,
                  fontSize: 12,
                ),
              ),
            ),
          if (rating != null)
            Padding(
              padding: const EdgeInsets.only(bottom: 6),
              child: Row(
                children: [
                  const Icon(Icons.star, size: 14, color: primary),
                  const SizedBox(width: 4),
                  Text(
                    '${rating.toStringAsFixed(1)} / 5.0',
                    style: const TextStyle(color: primary, fontSize: 12),
                  ),
                ],
              ),
            ),
          Text(
            content,
            style: const TextStyle(color: primary, fontSize: 12, height: 1.4),
          ),
          const SizedBox(height: 6),
          Align(
            alignment: Alignment.centerRight,
            child: Text(
              _yyyymmdd(date),
              style: const TextStyle(color: Colors.grey, fontSize: 10),
            ),
          ),
        ],
      ),
    );
  }

  // 하단 로딩/끝 표시
  Widget _infiniteFooter() {
    if (_loadingMore) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 12),
        child: Center(child: CircularProgressIndicator(color: primary)),
      );
    }
    if (!_hasMore && _reviews.isNotEmpty) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 12),
        child: Center(
          child: Text('마지막 후기까지 다 봤어요', style: TextStyle(color: Colors.grey)),
        ),
      );
    }
    return const SizedBox.shrink();
  }

  String _yyyymmdd(DateTime d) =>
      '${d.year}.${d.month.toString().padLeft(2, '0')}.${d.day.toString().padLeft(2, '0')}';
}
