import 'package:flutter/material.dart';
import 'package:kakao_flutter_sdk_auth/kakao_flutter_sdk_auth.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';
import 'models/user_provider.dart';
import 'screens/splash_screen.dart';
import 'screens/login_screen.dart';
import 'screens/signup_screen.dart';
import 'screens/home_screen.dart';
import 'screens/search_screen.dart';
import 'screens/rate_screen.dart';
import 'screens/account_screen.dart';
import 'screens/accountedit_screen.dart';
import 'screens/accountmypage_screen.dart';
import 'screens/musicpick_screen.dart';
import 'screens/musicalpick_screen.dart';
import 'screens/detail_screen.dart';
// import 'package:firebase_core/firebase_core.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  //await Firebase.initializeApp();
  await Supabase.initialize(
    url: 'https://nqzplkpbcxasadrcqdaz.supabase.co',
    anonKey: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5xenBsa3BiY3hhc2FkcmNxZGF6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA0NzU1MDksImV4cCI6MjA3NjA1MTUwOX0.3pAoEcBnbspwERErcYeqcT6ve33VkwUajSKjLRSkmpQ',
  );

  KakaoSdk.init(nativeAppKey: 'f44e738db2fe6cbe4d9e6ec86bd0b8d2');

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
        textTheme: GoogleFonts.nanumGothicTextTheme(
          Theme.of(context).textTheme,
        ),
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
        '/accountmypage': (context) => const AccountMyPageScreen(),
        '/musicalpick': (context) => const MusicalpickScreen(),
        '/musicpick': (context) => const MusicpickScreen(),
        '/detail': (context) => const DetailScreen(),
      },
    );
  }
}
