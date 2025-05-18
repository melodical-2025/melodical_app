import 'package:flutter/material.dart';
import '../widgets/navigationbar.dart';

class AccountScreen extends StatelessWidget {
  const AccountScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,

      // 하단 내비게이션 바
      bottomNavigationBar: BottomNavBar(currentIndex: 3),
    );
  }
}
