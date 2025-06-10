import 'package:flutter/material.dart';
import 'package:flutter_rating_bar/flutter_rating_bar.dart';
import 'package:provider/provider.dart';
import '../models/song.dart';
import '../models/user_provider.dart';
import '../services/api_service.dart';

class RateMusicTab extends StatefulWidget {
  const RateMusicTab({super.key});

  @override
  State<RateMusicTab> createState() => _RateMusicTabState();
}

class _RateMusicTabState extends State<RateMusicTab> {
  List<Song> _songs = [];
  bool _loading = true;
  String? _error;
  final Map<String, double> _local = {};

  @override
  void initState() {
    super.initState();
    _loadTop();
  }

  Future<void> _loadTop() async {
    setState(() { _loading = true; _error = null; });
    try {
      final songs = await ApiService.fetchTopMusic();
      setState(() { _songs = songs; });
    } catch (e) {
      _error = e.toString();
    } finally {
      setState(() { _loading = false; });
    }
  }

  Future<void> _submitRatings() async {
    final ratings = _local.entries
        .map((e) => {'songId': e.key, 'rating': e.value})
        .toList();
    try {
      await ApiService.rateBatchMusic(ratings);
      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text('저장 완료')));
    } catch (e) {
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text('저장 실패: $e')));
    }
  }

  @override
  Widget build(BuildContext ctx) {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null) {
      return Center(child: Text('에러: $_error'));
    }

    return Stack(
      children: [
        ListView.builder(
          padding: const EdgeInsets.only(bottom: 80),
          itemCount: _songs.length,
          itemBuilder: (_, i) {
            final s = _songs[i];
            final cur = _local[s.id] ?? s.userRating ?? 0.0;
            return ListTile(
              leading: Image.network(s.artworkUrl, width: 50),
              title: Text(s.title),
              subtitle: Slider(
                value: cur,
                min: 0,
                max: 5,
                divisions: 10,
                label: cur.toStringAsFixed(1),
                onChanged: (v) => setState(() => _local[s.id] = v),
              ),
            );
          },
        ),
        Positioned(
          bottom: 16, left: 16, right: 16,
          child: ElevatedButton(
            onPressed: _local.isNotEmpty ? _submitRatings : null,
            style: ElevatedButton.styleFrom(
              padding: const EdgeInsets.symmetric(vertical: 14),
              backgroundColor: const Color(0xFFFFAD75),
              foregroundColor: const Color(0xFFE17951),
              disabledBackgroundColor: Colors.grey.shade300,
            ),
            child: const Text('저장하기'),
          ),
        ),
      ],
    );
  }
}
