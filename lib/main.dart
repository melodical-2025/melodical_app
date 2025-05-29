import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:firebase_core/firebase_core.dart'; // 🔸 추가
import 'models/user_provider.dart';
import 'screens/splash_screen.dart';
import 'screens/login_screen.dart';
import 'screens/signup_screen.dart';
import 'screens/home_screen.dart';
import 'screens/search_screen.dart';
import 'screens/rate_screen.dart';
import 'screens/account_screen.dart';
import 'screens/accountedit_screen.dart';
import 'screens/musicpick_screen.dart';
import 'screens/musicalpick_screen.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();            // 🔸 필수
  await Firebase.initializeApp();                      // 🔸 Firebase 초기화

  runApp(
    ChangeNotifierProvider(
      create: (_) => UserProvider(),
      child: const MyApp(),
    ),
  );
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Melodical',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        primarySwatch: Colors.orange,
        scaffoldBackgroundColor: const Color(0xFFFFF2DB),
      ),
      initialRoute: '/splash',
      routes: {
        '/splash': (context) => const SplashScreen(),
        '/login': (context) => const LoginScreen(),
        '/signup': (context) => const SignupScreen(),
        '/home': (context) => const HomeScreen(),
        '/search': (context) => const SearchScreen(),
        '/ratemusical': (context) => const RateScreen(),
        '/account': (context) => const AccountScreen(),
        '/accountedit': (context) => const AccounteditScreen(),
        '/musicalpick': (context) => const MusicalpickScreen(),
        '/musicpick': (context) => const MusicpickScreen(),
      },
    );
  }
}
