import 'package:flutter/material.dart';
import '../models/song.dart';

class RatedSongListScreen extends StatefulWidget {
  const RatedSongListScreen({super.key});

  @override
  State<RatedSongListScreen> createState() => _RatedSongListScreenState();
}

class _RatedSongListScreenState extends State<RatedSongListScreen> {
  List<Song> ratedSongs = [];

  @override
  void initState() {
    super.initState();
    fetchRatedSongs();
  }

  void fetchRatedSongs() async {
    // 샘플 데이터 1개를 여러 번 반복해서 20개 생성
    final sample = Song(
      id: '1',
      title: 'Love Poem',
      artist: 'IU',
      artworkUrl: 'https://picsum.photos/200/300',
      genre: 'Ballad',
    );

    ratedSongs = List.generate(20, (_) => sample);

    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white, // 배경 흰색
      appBar: AppBar(
        title: const Text('평가한 음악'),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black,
        elevation: 1,
      ),
      body: ratedSongs.isEmpty
          ? const Center(child: Text('평가한 음악이 없습니다.'))
          : Padding(
        padding: const EdgeInsets.all(12.0),
        child: GridView.builder(
          itemCount: ratedSongs.length,
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 3,
            mainAxisSpacing: 12,
            crossAxisSpacing: 12,
            childAspectRatio: 0.6, // 이미지+제목 비율
          ),
          itemBuilder: (context, index) {
            final s = ratedSongs[index];
            return Column(
              children: [
                ClipRRect(
                  borderRadius: BorderRadius.circular(8),
                  child: Image.network(
                    s.artworkUrl,
                    height: 120,
                    width: double.infinity,
                    fit: BoxFit.cover,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  s.title,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                  ),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            );
          },
        ),
      ),
    );
  }
}
