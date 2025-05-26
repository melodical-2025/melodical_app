import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:flutter_rating_bar/flutter_rating_bar.dart';
import '../models/user_provider.dart';

class RateMusicalTab extends StatelessWidget {
  const RateMusicalTab({super.key});

  @override
  Widget build(BuildContext context) {
    final provider = Provider.of<UserProvider>(context);

    final List<Map<String, dynamic>> musicalList = [
      {'title': '도리안 그레이', 'image': 'assets/poster.png'},
      {'title': '모리스', 'image': 'assets/poster.png'},
      {'title': '알라딘', 'image': 'assets/poster.png'},
      {'title': '매디슨 카운티의 다리', 'image': 'assets/poster.png'},
    ];

    return ListView.builder(
      padding: const EdgeInsets.all(18),
      itemCount: musicalList.length,
      itemBuilder: (context, index) {
        final musical = musicalList[index];
        final rating = provider.musicalRatings[musical['title']] ?? 0.0;

        return Padding(
          padding: const EdgeInsets.symmetric(vertical: 16.0),
          child: Row(
            children: [
              ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: Image.asset(
                  musical['image'],
                  width: 80,
                  height: 110,
                  fit: BoxFit.cover,
                ),
              ),
              const SizedBox(width: 18),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '<${musical['title']}>',
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
                        provider.rateMusical(musical['title'], newRating);
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
