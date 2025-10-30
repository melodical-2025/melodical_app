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

  int _likedMusicals = 0;  // ⚠️ 찜 API 생기면 교체
  int _ratedMusicals = 0;
  int _ratedSongs = 0;

  // ── 리뷰 무한스크롤 상태 ───────────────────────────────────────────────
  final List<_ReviewItem> _reviews = [];
  final ScrollController _scroll = ScrollController();

  // 페이지네이션 상태
  int _page = 0;                // 0-based page index
  final int _pageSize = 10;     // 한 번에 불러올 개수
  bool _hasMore = true;         // 더 불러올 데이터 존재 여부
  bool _loadingMore = false;    // 추가 로딩 중 여부
  // ────────────────────────────────────────────────────────────────────

  @override
  void initState() {
    super.initState();
    _scroll.addListener(_onScroll);
    _bootstrap();        // 프로필/통계 + 첫 페이지 로드
  }

  @override
  void dispose() {
    _scroll.removeListener(_onScroll);
    _scroll.dispose();
    super.dispose();
  }

  Future<void> _bootstrap() async {
    // 사용자 기본 정보
    final user = Provider.of<UserProvider>(context, listen: false);
    final nickname = (user.nickname).trim();
    final email = (user.email).trim();

    // 통계값 (기존 API 재사용)
    List<Musical> ratedM = [];
    List<Song> ratedS = [];

    try {
      ratedM = await ApiService.fetchRatedMusicals();   // 사용자 평가 뮤지컬
    } catch (_) {}

    try {
      ratedS = await ApiService.fetchRatedMusicByUser(); // 사용자 평가 음악
    } catch (_) {}

    if (!mounted) return;
    setState(() {
      _nickname = nickname.isEmpty && email.isNotEmpty
          ? email.split('@').first
          : (nickname.isEmpty ? '(닉네임 없음)' : nickname);
      _email = email;
      _ratedMusicals = ratedM.length;
      _ratedSongs = ratedS.length;
      _likedMusicals = 0; // TODO(backend): 찜 개수 API로 교체
      _loading = false;
    });

    // ✅ 첫 페이지 로드
    await _reloadReviews();
  }

  // ── 스크롤 끝 근처 감지 → 다음 페이지 로드 ─────────────────────────────
  void _onScroll() {
    if (_loadingMore || !_hasMore) return;
    if (!_scroll.hasClients) return;

    final max = _scroll.position.maxScrollExtent;
    final offset = _scroll.position.pixels;

    // 끝에서 200px 남으면 다음 페이지 로드
    if (max - offset < 200) {
      _loadMoreReviews();
    }
  }

  // ── 새로고침(맨 처음 페이지부터 다시) ────────────────────────────────
  Future<void> _refresh() async {
    await _reloadReviews();
  }

  Future<void> _reloadReviews() async {
    setState(() {
      _page = 0;
      _hasMore = true;
      _reviews.clear();
    });
    await _loadMoreReviews();
  }

  // ── 다음 페이지 로드 (무한스크롤 핵심) ────────────────────────────────
  Future<void> _loadMoreReviews() async {
    if (!_hasMore || _loadingMore) return;

    setState(() => _loadingMore = true);

    try {
      // TODO(backend): 여기를 실제 API 호출로 교체
      // 예) final pageDto = await ApiService.fetchUserReviews(page: _page, size: _pageSize);
      //     final items = pageDto.items.map((e) => _ReviewItem.fromDto(e)).toList();
      //     final got = items.length;
      //     final more = pageDto.hasNext;  // 또는 got == _pageSize 로 판단
      final fetched = await _fakeFetchReviews(page: _page, size: _pageSize);

      if (!mounted) return;
      setState(() {
        _reviews.addAll(fetched.items);
        _hasMore = fetched.hasNext;
        _page += 1;
      });
    } catch (e) {
      // 필요한 경우 에러 토스트/스낵바
      // ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('후기 불러오기 실패: $e')));
      setState(() {
        _hasMore = false; // 더 이상 시도 안 함(임시)
      });
    } finally {
      if (mounted) {
        setState(() => _loadingMore = false);
      }
    }
  }

  // ── 가짜 데이터 소스(데모). 나중에 API로 교체 ─────────────────────────
  Future<_Paged<_ReviewItem>> _fakeFetchReviews({
    required int page,
    required int size,
  }) async {
    await Future.delayed(const Duration(milliseconds: 450)); // 로딩감 주기

    // 데모 전체 데이터 풀(50개 생성)
    final total = 50;
    final now = DateTime.now();

    List<_ReviewItem> all = List.generate(total, (i) {
      final day = ((i % 27) + 1);
      return _ReviewItem(
        title: i % 3 == 0 ? '뮤지컬 <햄릿>' : (i % 3 == 1 ? '뮤지컬 <레미제라블>' : '뮤지컬 <비틀쥬스>'),
        rating: 3.5 + (i % 4) * 0.5, // 3.5~5.0
        content: [
          '정말 재미있었어요! 음악이 너무 좋았어요.',
          '배우들 연기가 너무 인상 깊었습니다.',
          '조명과 무대가 멋졌어요. 다시 보고 싶어요!',
          '몰입감이 뛰어나고 스토리가 탄탄했어요.'
        ][i % 4],
        date: DateTime(now.year, now.month, (now.day - day).clamp(1, 28)),
      );
    });

    // 최신순(날짜 내림차순)
    all.sort((a, b) => b.date.compareTo(a.date));

    // 페이지 슬라이스
    final start = page * size;
    final end = (start + size).clamp(0, all.length);
    final slice = start >= all.length ? <_ReviewItem>[] : all.sublist(start, end);

    final hasNext = end < all.length;
    return _Paged(items: slice, hasNext: hasNext);
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
                return _reviewItemCard(
                  title: r.title,
                  rating: r.rating,
                  content: r.content,
                  date: r.date,
                );
              },
            ),
        ],
      ),
    );
  }

  Widget _reviewItemCard({
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

// ───── 데이터 모델(간단) ────────────────────────────────────────────────
// TODO(backend): 서버 DTO와 키를 맞춰서 fromJson 생성자로 교체하세요.
class _ReviewItem {
  final String? title;
  final double? rating;
  final String content;
  final DateTime date;

  _ReviewItem({
    this.title,
    this.rating,
    required this.content,
    required this.date,
  });

// 예: 서버 응답으로 바꿀 때
// factory _ReviewItem.fromJson(Map<String, dynamic> j) => _ReviewItem(
//   title: j['musicalTitle'] as String?,
//   rating: j['rating'] == null ? null : double.tryParse(j['rating'].toString()),
//   content: (j['content'] ?? '').toString(),
//   date: DateTime.parse(j['createdAt'].toString()),
// );
}

// 페이징 래퍼
class _Paged<T> {
  final List<T> items;
  final bool hasNext;
  _Paged({required this.items, required this.hasNext});
}
