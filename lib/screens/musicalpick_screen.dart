import 'package:flutter/material.dart';
import '../models/musical.dart';
import '../services/api_service.dart';
import 'musicpick_screen.dart';

class MusicalpickScreen extends StatefulWidget {
  const MusicalpickScreen({super.key});

  @override
  State<MusicalpickScreen> createState() => _MusicalpickScreenState();
}

class _MusicalpickScreenState extends State<MusicalpickScreen> {
  List<Musical> _musicals = [];
  final Set<int> _selectedIndexes = {};
  bool _loading = true;
  String? _error;
  final ScrollController _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    _fetchMusicals();
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _fetchMusicals() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final list = await ApiService.fetchMonthlyMusicals();
      print('📥 Fetched ${list.length} musicals from API');

      setState(() {
        _musicals = list.map((data) {
          final period = data['period']?.toString() ?? '';
          final dates = period.split('~');

          // posterUrl 처리 및 디버깅
          String posterUrl = data['posterUrl']?.toString() ?? '';
          print('🖼️ Original posterUrl for "${data['title']}": $posterUrl');

          // posterUrl이 비어있거나 null이면 기본 이미지 URL 사용
          if (posterUrl.isEmpty) {
            posterUrl = 'https://via.placeholder.com/150?text=No+Image';
            print('⚠️ Using placeholder for "${data['title']}"');
          }

          return Musical(
            id: data['id'] ?? 0,
            title: data['title'] ?? '',
            cast: '',
            runtime: '',
            posterUrl: posterUrl,
            theater: data['theater'] ?? '',
            startDate: dates.isNotEmpty ? dates.first.trim() : '',
            endDate: dates.length > 1 ? dates.last.trim() : '',
          );
        }).toList();
      });
      print('✅ Successfully loaded ${_musicals.length} musicals');
    } catch (e) {
      print('❌ Error fetching musicals: $e');
      setState(() {
        _error = e.toString();
      });
    } finally {
      setState(() {
        _loading = false;
      });
    }
  }

  Future<void> _submitSelection() async {
    if (_selectedIndexes.length < 3) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('최소 3개 이상의 뮤지컬을 선택해주세요'),
          backgroundColor: Color(0xFFE17951),
        ),
      );
      return;
    }

    // 로딩 다이얼로그 표시
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => const Center(
        child: Card(
          child: Padding(
            padding: EdgeInsets.all(20),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                CircularProgressIndicator(
                  color: Color(0xFFE17951),
                ),
                SizedBox(height: 16),
                Text(
                  '저장 중...',
                  style: TextStyle(fontSize: 16),
                ),
              ],
            ),
          ),
        ),
      ),
    );

    final payload = _selectedIndexes
        .map((i) => {'musicalId': _musicals[i].id, 'rating': 5.0})
        .toList();

    print('💾 Saving ${payload.length} musicals...');
    print('Payload: $payload');

    try {
      await ApiService.rateBatchMusical(payload);
      print('✅ Save successful!');

      // 로딩 다이얼로그 닫기
      if (mounted) {
        Navigator.of(context).pop();

        // 성공 메시지
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('${_selectedIndexes.length}개의 뮤지컬 취향이 저장되었습니다!'),
            backgroundColor: Colors.green,
            duration: const Duration(seconds: 2),
          ),
        );

        // 다음 화면으로 이동
        await Future.delayed(const Duration(milliseconds: 500));
        if (mounted) {
          Navigator.of(context).pushReplacement(
            MaterialPageRoute(builder: (_) => const MusicpickScreen()),
          );
        }
      }
    } catch (e) {
      print('❌ Save failed: $e');

      // 로딩 다이얼로그 닫기
      if (mounted) {
        Navigator.of(context).pop();

        // 에러 메시지
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('저장 실패: $e'),
            backgroundColor: Colors.red,
            duration: const Duration(seconds: 3),
            action: SnackBarAction(
              label: '다시 시도',
              textColor: Colors.white,
              onPressed: _submitSelection,
            ),
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return Scaffold(
        backgroundColor: const Color(0xFFFFF2DB),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: const [
              CircularProgressIndicator(
                color: Color(0xFFE17951),
              ),
              SizedBox(height: 16),
              Text(
                '뮤지컬 목록을 불러오는 중...',
                style: TextStyle(
                  fontSize: 16,
                  color: Colors.black87,
                ),
              ),
            ],
          ),
        ),
      );
    }

    if (_error != null) {
      return Scaffold(
        backgroundColor: const Color(0xFFFFF2DB),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(
                  Icons.error_outline,
                  size: 64,
                  color: Color(0xFFE17951),
                ),
                const SizedBox(height: 16),
                const Text(
                  '데이터를 불러오는데 실패했습니다',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  _error!,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontSize: 14,
                    color: Colors.black54,
                  ),
                ),
                const SizedBox(height: 24),
                ElevatedButton(
                  onPressed: _fetchMusicals,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFE17951),
                    padding: const EdgeInsets.symmetric(
                      horizontal: 32,
                      vertical: 12,
                    ),
                  ),
                  child: const Text(
                    '다시 시도',
                    style: TextStyle(
                      fontSize: 16,
                      color: Colors.white,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    }

    if (_musicals.isEmpty) {
      return Scaffold(
        backgroundColor: const Color(0xFFFFF2DB),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(
                  Icons.music_note,
                  size: 64,
                  color: Color(0xFFE17951),
                ),
                const SizedBox(height: 16),
                const Text(
                  '표시할 뮤지컬이 없습니다',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 8),
                const Text(
                  '서버에 데이터가 없거나\n크롤링이 아직 실행되지 않았습니다',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 14,
                    color: Colors.black54,
                  ),
                ),
                const SizedBox(height: 24),
                ElevatedButton(
                  onPressed: _fetchMusicals,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFE17951),
                    padding: const EdgeInsets.symmetric(
                      horizontal: 32,
                      vertical: 12,
                    ),
                  ),
                  child: const Text(
                    '새로고침',
                    style: TextStyle(
                      fontSize: 16,
                      color: Colors.white,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    }

    return Scaffold(
      backgroundColor: const Color(0xFFFFF2DB),
      body: SafeArea(
        child: Stack(
          children: [
            Column(
              children: [
                const Padding(
                  padding: EdgeInsets.fromLTRB(30, 90, 16, 16),
                  child: Align(
                    alignment: Alignment.centerLeft,
                    child: Text.rich(
                      TextSpan(
                        style: TextStyle(
                          fontSize: 30,
                          fontWeight: FontWeight.bold,
                          height: 1.4,
                        ),
                        children: [
                          TextSpan(
                              text: '관심있는',
                              style: TextStyle(color: Colors.black)),
                          TextSpan(
                              text: '뮤지컬',
                              style: TextStyle(color: Color(0xFFE17951))),
                          TextSpan(
                              text: '을\n3개 이상 선택하세요',
                              style: TextStyle(color: Colors.black)),
                        ],
                      ),
                    ),
                  ),
                ),
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    child: GridView.builder(
                      controller: _scrollController,
                      itemCount: _musicals.length,
                      gridDelegate:
                          const SliverGridDelegateWithFixedCrossAxisCount(
                        crossAxisCount: 3,
                        crossAxisSpacing: 12,
                        mainAxisSpacing: 4,
                        childAspectRatio: 0.65,
                      ),
                      padding: const EdgeInsets.only(bottom: 100),
                      itemBuilder: (context, index) {
                        final m = _musicals[index];
                        final isSelected = _selectedIndexes.contains(index);
                        return GestureDetector(
                          onTap: () => setState(() {
                            if (isSelected) {
                              _selectedIndexes.remove(index);
                            } else {
                              _selectedIndexes.add(index);
                            }
                          }),
                          child: Column(
                            children: [
                              Container(
                                width: 90,
                                height: 90,
                                decoration: BoxDecoration(
                                  shape: BoxShape.circle,
                                  border: Border.all(
                                    color: isSelected
                                        ? const Color(0xFFE17951)
                                        : Colors.transparent,
                                    width: 3,
                                  ),
                                ),
                                child: ClipOval(
                                  child: m.posterUrl.isNotEmpty
                                      ? Image.network(
                                          m.posterUrl,
                                          fit: BoxFit.cover,
                                          errorBuilder: (context, error, stackTrace) {
                                            return Container(
                                              color: Colors.grey.shade300,
                                              child: const Icon(
                                                Icons.music_note,
                                                color: Colors.grey,
                                                size: 40,
                                              ),
                                            );
                                          },
                                          loadingBuilder: (context, child, loadingProgress) {
                                            if (loadingProgress == null) return child;
                                            return Center(
                                              child: CircularProgressIndicator(
                                                value: loadingProgress.expectedTotalBytes != null
                                                    ? loadingProgress.cumulativeBytesLoaded /
                                                        loadingProgress.expectedTotalBytes!
                                                    : null,
                                              ),
                                            );
                                          },
                                        )
                                      : Container(
                                          color: Colors.grey.shade300,
                                          child: const Icon(
                                            Icons.music_note,
                                            color: Colors.grey,
                                            size: 40,
                                          ),
                                        ),
                                ),
                              ),
                              const SizedBox(height: 8),
                              SizedBox(
                                width: 100,
                                child: Text(
                                  m.title,
                                  textAlign: TextAlign.center,
                                  style: const TextStyle(
                                    color: Colors.black,
                                    fontSize: 15,
                                    fontWeight: FontWeight.w500,
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
                ),
              ],
            ),
            Positioned(
              left: 0,
              right: 0,
              bottom: 0,
              height: 95,
              child: IgnorePointer(
                child: Container(
                  decoration: const BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.topCenter,
                      end: Alignment.bottomCenter,
                      colors: [Colors.transparent, Colors.black45],
                    ),
                  ),
                ),
              ),
            ),
            Positioned(
              left: 16,
              right: 16,
              bottom: 24,
              child: ElevatedButton(
                onPressed:
                    _selectedIndexes.length >= 3 ? _submitSelection : null,
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFFFAD75),
                  foregroundColor: const Color(0xFFE17951),
                  disabledBackgroundColor: Colors.grey.shade300,
                  disabledForegroundColor: Colors.grey,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16)),
                ),
                child: _selectedIndexes.length >= 3
                    ? Stack(
                        alignment: Alignment.center,
                        children: [
                          Text(
                            '선택 완료',
                            style: TextStyle(
                              fontSize: 16,
                              foreground: Paint()
                                ..style = PaintingStyle.stroke
                                ..strokeWidth = 2
                                ..color = const Color(0xFFFFE5B6),
                            ),
                          ),
                          const Text(
                            '선택 완료',
                            style: TextStyle(
                              fontSize: 16,
                              color: Color(0xFFE17951),
                            ),
                          ),
                        ],
                      )
                    : const Text('선택 완료', style: TextStyle(fontSize: 16)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
