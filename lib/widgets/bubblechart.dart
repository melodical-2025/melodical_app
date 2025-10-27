import 'package:flutter/material.dart';

class GenreBubbleChart extends StatelessWidget {
  final Map<String, double> genrePreferences;

  const GenreBubbleChart({super.key, required this.genrePreferences});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 180,
      child: Center(
        child: Wrap(
          spacing: 8,
          runSpacing: 8,
          alignment: WrapAlignment.center,
          children: genrePreferences.entries.map((entry) {
            final genre = entry.key;
            final value = entry.value;
            final size = 50 + (value * 70); // 50~120 크기

            return Container(
              width: size,
              height: size,
              decoration: BoxDecoration(
                color: const Color(0xFFFFCA82),
                shape: BoxShape.circle,
              ),
              alignment: Alignment.center,
              child: Padding(
                padding: const EdgeInsets.all(6.0),
                child: FittedBox(
                  fit: BoxFit.scaleDown,
                  child: Text(
                    genre,
                    textAlign: TextAlign.center,
                    style: const TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.bold,
                      fontSize: 18,
                    ),
                  ),
                ),
              ),
            );
          }).toList(),
        ),
      ),
    );
  }
}
