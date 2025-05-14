import 'package:flutter/material.dart';

class SplashScreen extends StatelessWidget {
  const SplashScreen({super.key});

  @override
  Widget build(BuildContext context) {
    Future.delayed(const Duration(seconds: 5), () {
      Navigator.pushReplacementNamed(context, '/login');
    });

    return const Scaffold(
      backgroundColor: Color(0xFFFFF2DB), // 배경색 FFF2DB
      body: Center(
        child: Image(
          image: AssetImage('assets/logo.png'),
          width: 300, // 로고 크기 조절 가능
        ),
      ),
    );
  }
}