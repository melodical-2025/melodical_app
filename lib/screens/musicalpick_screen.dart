import 'package:flutter/material.dart';
import '../models/musical.dart';
import '../services/api_service.dart';
import 'home_screen.dart';
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
      final list = await ApiService.fetchAllMusicals();
      setState(() {
        _musicals = list;
      });
    } catch (e) {
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
    final payload = _selectedIndexes
        .map((i) => {'musicalId': _musicals[i].id, 'rating': 5.0})
        .toList();
    try {
      await ApiService.rateBatchMusical(payload);
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (_) => const HomeScreen()),
      );
    } catch (e) {
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text('저장 실패: $e')));
    }
  }
  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    if (_error != null) {
      return Scaffold(body: Center(child: Text('에러: $_error')));
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
                              text: '좋아하는 ',
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
                                  image: DecorationImage(
                                    image: NetworkImage(m.posterUrl),
                                    fit: BoxFit.cover,
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
            // 그라데이션
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
            // 선택 완료 버튼
            Positioned(
              left: 16,
              right: 16,
              bottom: 24,
              child: ElevatedButton(
                onPressed: _selectedIndexes.length >= 3
                    ? _submitSelection
                    : null,
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
