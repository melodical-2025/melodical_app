import 'package:flutter/material.dart';
import 'package:flutter_rating_bar/flutter_rating_bar.dart';
import 'package:provider/provider.dart';
import '../models/user_provider.dart';

class RateMusicTab extends StatelessWidget {
  const RateMusicTab({super.key});

  @override
  Widget build(BuildContext context) {
    final provider = Provider.of<UserProvider>(context);

    final List<Map<String, dynamic>> musicList = [
      {'title': 'Whiplash - aespa', 'image': 'assets/music.png'},
      {'title': 'Drowning - WOODZ', 'image': 'assets/music.png'},
      {'title': 'APT. - 로제, Bruno Mars', 'image': 'assets/music.png'},
      {'title': 'Not Like Us - Kendrick Lamar', 'image': 'assets/music.png'},
      {'title': '너와의 모든 지금 - 재쓰비', 'image': 'assets/music.png'},
    ];

    return ListView.builder(
      padding: const EdgeInsets.all(18),
      itemCount: musicList.length,
      itemBuilder: (context, index) {
        final music = musicList[index];
        final rating = provider.musicRatings[music['title']] ?? 0.0;

        return Padding(
          padding: const EdgeInsets.symmetric(vertical: 16.0),
          child: Row(
            children: [
              ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: Image.asset(
                  music['image'],
                  width: 80,
                  height: 80,
                  fit: BoxFit.cover,
                ),
              ),
              const SizedBox(width: 18),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      music['title'],
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 18,
                      ),
                    ),
                    const SizedBox(height: 4),
                    RatingBar.builder(
                      initialRating: rating,
                      minRating: 0,
                      allowHalfRating: true,
                      itemCount: 5,
                      itemSize: 32,
                      itemPadding: const EdgeInsets.symmetric(horizontal: 2),
                      unratedColor: Colors.grey.shade300,
                      itemBuilder: (context, _) => const Icon(
                        Icons.star,
                        color: Colors.orange,
                      ),
                      onRatingUpdate: (newRating) {
                        provider.rateMusic(music['title'], newRating);
                      },
                    ),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}
