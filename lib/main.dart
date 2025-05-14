import 'package:flutter/material.dart';
import 'screens/splash_screen.dart';
import 'screens/login_screen.dart';
import 'screens/signup_screen.dart';
import 'screens/ratemusical_screen.dart';
import 'screens/ratemusic_screen.dart';
import 'screens/home_screen.dart';
import 'screens/search_screen.dart';
import 'screens/detail_screen.dart';
import 'screens/ratemusical_screen.dart';
import 'screens/ratemusic_screen.dart';
import 'screens/account_screen.dart';
import 'screens/accountedit_screen.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Melodical',
      initialRoute: '/',
      routes: {
        '/': (context) => const SplashScreen(),
        '/login': (context) => const LoginScreen(),
        //'/signup': (context) => const SignupScreen(),
        //'/musicalpick': (context) => const MusicalpickScreen(),
        //'/musicpick': (context) => const MusicpickScreen(),
        //'/home': (context) => const HomeScreen(),
        //'/search': (context) => const SearchScreen(),
        //'/detail': (context) => const DetailScreen(),
        //'/ratemusical': (context) => const RatemusicalScreen(),
        //'/ratemusic': (context) => const RatemusicScreen(),
        //'/account': (context) => const AccountPage(),
        //'/accountedit': (context) => const AccounteditPage(),
      },

      //home: const SearchScreen(),

    );
  }
}
