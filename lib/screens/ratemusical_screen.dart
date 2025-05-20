import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../widgets/navigationbar.dart';
import '../models/user_provider.dart';

class RateMusicalScreen extends StatefulWidget {
  const RateMusicalScreen({super.key});

  @override
  State<RateMusicalScreen> createState() => _RateMusicalScreenState();
}

class _RateMusicalScreenState extends State<RateMusicalScreen> {
  int _currentIndex = 2;

  final List<Map<String, dynamic>> musicalList = [
    {'title': '도리안 그레이', 'image': 'assets/default.png'},
    {'title': '모리스', 'image': 'assets/default.png'},
    {'title': '알라딘', 'image': 'assets/default.png'},
    {'title': '매디슨 카운티의 다리', 'image': 'assets/default.png'},
  ];

  void _onTabTapped(int index) {
    if (index == 0) Navigator.pushNamed(context, '/home');
    if (index == 1) Navigator.pushNamed(context, '/search');
    if (index == 2) return;
    if (index == 3) Navigator.pushNamed(context, '/account');
  }

  @override
  Widget build(BuildContext context) {
    final provider = Provider.of<UserProvider>(context);

    return Scaffold(
      backgroundColor: const Color(0xFFFFF2DB),
      appBar: AppBar(
        backgroundColor: const Color(0xFFFFF2DB),
        elevation: 0,
        centerTitle: true,
        title: const Text('평가', style: TextStyle(color: Color(0xFFE17951))),
        iconTheme: const IconThemeData(color: Color(0xFFE17951)),
      ),
      body: Column(
        children: [
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              TextButton(
                onPressed: () {},
                child: const Text(
                  '뮤지컬',
                  style: TextStyle(
                    color: Color(0xFFE17951),
                    fontWeight: FontWeight.bold,
                    fontSize: 16,
                  ),
                ),
              ),
              const SizedBox(width: 16),
              TextButton(
                onPressed: () {
                  Navigator.pushReplacementNamed(context, '/ratemusic');
                },
                child: const Text(
                  '음악',
                  style: TextStyle(
                    color: Colors.black54,
                    fontSize: 16,
                  ),
                ),
              ),
            ],
          ),
          const Divider(color: Color(0xFFE17951), height: 1, thickness: 1),
          const SizedBox(height: 8),
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: musicalList.length,
              itemBuilder: (context, index) {
                final musical = musicalList[index];
                final rating = provider.musicalRatings[musical['title']] ?? 0;

                return Padding(
                  padding: const EdgeInsets.symmetric(vertical: 8.0),
                  child: Row(
                    children: [
                      ClipRRect(
                        borderRadius: BorderRadius.circular(8),
                        child: Image.asset(
                          musical['image'],
                          width: 60,
                          height: 80,
                          fit: BoxFit.cover,
                          errorBuilder: (context, error, stackTrace) => Image.asset(
                            'assets/default.png',
                            width: 60,
                            height: 80,
                            fit: BoxFit.cover,
                          ),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '<${musical['title']}>',
                              style: const TextStyle(fontWeight: FontWeight.bold),
                            ),
                            const SizedBox(height: 4),
                            Row(
                              children: List.generate(5, (star) {
                                return IconButton(
                                  iconSize: 20,
                                  padding: EdgeInsets.zero,
                                  onPressed: () {
                                    provider.rateMusical(musical['title'], star + 1);
                                  },
                                  icon: Icon(
                                    star < rating ? Icons.star : Icons.star_border,
                                    color: Colors.orange,
                                  ),
                                );
                              }),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                );
              },
            ),
          ),
        ],
      ),
      bottomNavigationBar: BottomNavBar(
        currentIndex: _currentIndex,
        onTabTapped: _onTabTapped,
      ),
    );
  }
}
