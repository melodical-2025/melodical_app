import 'package:flutter/material.dart';
import '../models/musical.dart';

class LikedMusicalListScreen extends StatefulWidget {
  const LikedMusicalListScreen({super.key});

  @override
  State<LikedMusicalListScreen> createState() => _LikedMusicalListScreenState();
}

class _LikedMusicalListScreenState extends State<LikedMusicalListScreen> {
  List<Musical> likedMusicals = [];

  @override
  void initState() {
    super.initState();
    fetchLikedMusicals();
  }

  void fetchLikedMusicals() async {
    // 샘플 데이터 1개를 여러 번 반복해서 20개 생성
    final sample = Musical(
      id: 1,
      title: '엘리자벳',
      cast: '',
      startDate: '',
      endDate: '',
      runtime: '',
      theater: '',
      posterUrl: 'https://picsum.photos/200/300',
    );

    likedMusicals = List.generate(20, (_) => sample);

    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white, // 배경 흰색
      appBar: AppBar(
        title: const Text('관심있는 뮤지컬'),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black,
        elevation: 1,
      ),
      body: likedMusicals.isEmpty
          ? const Center(child: Text('관심있는 뮤지컬이 없습니다.'))
          : Padding(
        padding: const EdgeInsets.all(12.0),
        child: GridView.builder(
          itemCount: likedMusicals.length,
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 3,
            mainAxisSpacing: 12,
            crossAxisSpacing: 12,
            childAspectRatio: 0.6, // 이미지+제목 비율 조정
          ),
          itemBuilder: (context, index) {
            final m = likedMusicals[index];
            return Column(
              children: [
                ClipRRect(
                  borderRadius: BorderRadius.circular(8),
                  child: Image.network(
                    m.posterUrl,
                    height: 170,
                    width: double.infinity,
                    fit: BoxFit.cover,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  m.title,
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
