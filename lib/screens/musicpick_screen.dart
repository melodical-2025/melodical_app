import 'package:flutter/material.dart';
import '../models/song.dart';
import '../services/api_service.dart';
import 'home_screen.dart';

class MusicpickScreen extends StatefulWidget {
  const MusicpickScreen({super.key});
  @override
  State<MusicpickScreen> createState() => _MusicpickScreenState();
}

class _MusicpickScreenState extends State<MusicpickScreen> {
  List<Song> _songs = [];
  final Set<int> _selected = {};
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _fetchSongs();
  }

  Future<void> _fetchSongs() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final songs = await ApiService.fetchTopMusic();
      setState(() {
        _songs = songs;
      });
    } catch (e) {
      _error = e.toString();
    } finally {
      setState(() {
        _loading = false;
      });
    }
  }

  Future<void> _submitSelection() async {
    final ratings = _selected
        .map((i) => {'songId': _songs[i].id, 'rating': 5.0})
        .toList();
    try {
      await ApiService.rateBatchMusic(ratings);
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (_) => const HomeScreen()),
      );
    } catch (e) {
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text('저장 실패: $e')));
    }
  }

  @override
  Widget build(BuildContext ctx) {
    if (_loading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    if (_error != null) {
      return Scaffold(
        body: Center(child: Text('에러: $_error')),
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
                            height: 1.4
                        ),
                        children: [
                          TextSpan(text: '좋아하는 ', style: TextStyle(color: Colors.black)),
                          TextSpan(text: '음악', style: TextStyle(color: Color(0xFFFFB224))),
                          TextSpan(text: '을\n3개 이상 선택하세요', style: TextStyle(color: Colors.black)),
                        ],
                      ),
                    ),
                  ),
                ),
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    child: GridView.builder(
                      itemCount: _songs.length,
                      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 3, crossAxisSpacing: 12, mainAxisSpacing: 4, childAspectRatio: 0.65
                      ),
                      padding: const EdgeInsets.only(bottom: 100),
                      itemBuilder: (_, idx) {
                        final song = _songs[idx];
                        final isSel = _selected.contains(idx);
                        return GestureDetector(
                          onTap: () {
                            setState(() {
                              isSel ? _selected.remove(idx) : _selected.add(idx);
                            });
                          },
                          child: Column(
                            children: [
                              Container(
                                width: 90,
                                height: 90,
                                decoration: BoxDecoration(
                                  shape: BoxShape.circle,
                                  border: Border.all(
                                    color: isSel ? const Color(0xFFE17951) : Colors.transparent,
                                    width: 3,
                                  ),
                                  image: DecorationImage(
                                    image: NetworkImage(song.artworkUrl),
                                    fit: BoxFit.cover,
                                  ),
                                ),
                              ),
                              const SizedBox(height: 8),
                              Text(
                                song.title,
                                textAlign: TextAlign.center,
                                style: const TextStyle(
                                    color: Colors.black, fontSize: 15, fontWeight: FontWeight.w500
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
              left: 16, right: 16, bottom: 24,
              child: ElevatedButton(
                onPressed: _selected.length >= 3 ? _submitSelection : null,
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFFFAD75),
                  foregroundColor: const Color(0xFFE17951),
                  disabledBackgroundColor: Colors.grey.shade300,
                  disabledForegroundColor: Colors.grey,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                ),
                child: const Text('선택 완료', style: TextStyle(fontSize: 16)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}