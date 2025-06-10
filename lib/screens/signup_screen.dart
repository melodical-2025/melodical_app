// lib/screens/signup_screen.dart

import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_naver_login/interface/types/naver_login_status.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';
import 'package:kakao_flutter_sdk_auth/kakao_flutter_sdk_auth.dart';
import 'package:flutter_naver_login/flutter_naver_login.dart';

import '../services/api_service.dart';
import '../services/token_storage.dart';

class SignupScreen extends StatefulWidget {
  const SignupScreen({super.key});

  @override
  State<SignupScreen> createState() => _SignupScreenState();
}

class _SignupScreenState extends State<SignupScreen> {
  final _nicknameController        = TextEditingController();
  final _emailController           = TextEditingController();
  final _passwordController        = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  bool _isLoading = false;

  static const backgroundColor = Color(0xFFFFF2DB);
  static const primaryColor    = Color(0xFFFFAD75);
  static const textColor       = Color(0xFFE17951);
  static const secondaryColor  = Color(0xFFFFD9A3);

  @override
  void dispose() {
    _nicknameController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  Future<void> _signUp() async {
    final nick    = _nicknameController.text.trim();
    final email   = _emailController.text.trim();
    final pwd     = _passwordController.text.trim();
    final confirm = _confirmPasswordController.text.trim();

    if (nick.isEmpty || email.isEmpty || pwd.isEmpty || confirm.isEmpty) {
      _showSnack('모든 항목을 입력해주세요');
      return;
    }
    if (pwd != confirm) {
      _showSnack('비밀번호가 일치하지 않습니다');
      return;
    }

    setState(() => _isLoading = true);
    final resp = await ApiService.signup(email, pwd, nick);

    if (resp.statusCode == 200 || resp.statusCode == 201) {
      // 백엔드가 바로 JWT를 리턴한다면
      // final data = jsonDecode(resp.body);
      // await TokenStorage().save(data['token']);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('회원가입 완료!')),
      );
      Navigator.pushReplacementNamed(context, '/musicalpick');
    } else {
      final error = jsonDecode(resp.body)['message'] ?? '회원가입 오류';
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(error)),
      );
    }
    setState(() => _isLoading = false);
  }

  Future<void> _signInWithGoogle() async {
    setState(() => _isLoading = true);
    try {
      final googleUser = await GoogleSignIn(scopes: ['email']).signIn();
      if (googleUser == null) throw '취소됨';
      final auth = await googleUser.authentication;
      final resp = await ApiService.post('/auth/login/google', {
        'token': auth.idToken,
      });
      if (resp.statusCode == 200) {
        final data = jsonDecode(resp.body);
        await TokenStorage().save(data['token']);
        Navigator.pushReplacementNamed(context, '/musicalpick');
      } else {
        throw 'Google 로그인 실패 (${resp.statusCode})';
      }
    } catch (e) {
      _showSnack(e.toString());
    }
    setState(() => _isLoading = false);
  }

  Future<void> _signInWithKakao() async {
    setState(() => _isLoading = true);
    try {
      OAuthToken token = await (await isKakaoTalkInstalled()
          ? UserApi.instance.loginWithKakaoTalk()
          : UserApi.instance.loginWithKakaoAccount());
      final resp = await ApiService.post('/auth/login/kakao', {
        'token': token.accessToken,
      });
      if (resp.statusCode == 200) {
        final data = jsonDecode(resp.body);
        await TokenStorage().save(data['token']);
        Navigator.pushReplacementNamed(context, '/musicalpick');
      } else {
        throw '카카오 로그인 실패 (${resp.statusCode})';
      }
    } catch (e) {
      _showSnack(e.toString());
    }
    setState(() => _isLoading = false);
  }

  Future<void> _signInWithNaver() async {
    setState(() => _isLoading = true);
    try {
      final res = await FlutterNaverLogin.logIn();
      if (res.status != NaverLoginStatus.loggedIn) throw '네이버 로그인 취소/실패';
      final resp = await ApiService.post('/auth/login/naver', {
        'token': res.accessToken,
      });
      if (resp.statusCode == 200) {
        final data = jsonDecode(resp.body);
        await TokenStorage().save(data['token']);
        Navigator.pushReplacementNamed(context, '/musicalpick');
      } else {
        throw '네이버 로그인 실패 (${resp.statusCode})';
      }
    } catch (e) {
      _showSnack(e.toString());
    }
    setState(() => _isLoading = false);
  }

  void _showSnack(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(msg)),
    );
  }

  InputDecoration _inputDecoration(String label) => InputDecoration(
    labelText: label,
    filled: true,
    fillColor: Colors.white,
    labelStyle: const TextStyle(color: primaryColor),
    enabledBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(8),
      borderSide: const BorderSide(color: primaryColor),
    ),
    focusedBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(8),
      borderSide: const BorderSide(color: primaryColor, width: 2),
    ),
  );

  Widget _socialIcon(String assetPath, VoidCallback onTap) {
    return GestureDetector(
      onTap: _isLoading ? null : onTap,
      child: CircleAvatar(
        radius: 20,
        backgroundImage: AssetImage(assetPath),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: backgroundColor,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              IconButton(
                icon: const Icon(Icons.arrow_back),
                color: textColor,
                onPressed: () => Navigator.pop(context),
              ),
              const SizedBox(height: 75),

              const Text(
                "회원가입",
                style: TextStyle(
                  fontSize: 28,
                  fontWeight: FontWeight.bold,
                  color: textColor,
                ),
              ),
              const SizedBox(height: 30),

              TextField(
                controller: _nicknameController,
                decoration: _inputDecoration('닉네임'),
                style: const TextStyle(color: primaryColor),
              ),
              const SizedBox(height: 20),

              TextField(
                controller: _emailController,
                decoration: _inputDecoration('이메일'),
                keyboardType: TextInputType.emailAddress,
                style: const TextStyle(color: primaryColor),
              ),
              const SizedBox(height: 20),

              TextField(
                controller: _passwordController,
                decoration: _inputDecoration('비밀번호'),
                obscureText: true,
                style: const TextStyle(color: primaryColor),
              ),
              const SizedBox(height: 20),

              TextField(
                controller: _confirmPasswordController,
                decoration: _inputDecoration('비밀번호 확인'),
                obscureText: true,
                style: const TextStyle(color: primaryColor),
              ),
              const SizedBox(height: 30),

              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: _isLoading ? null : _signUp,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: secondaryColor,
                    foregroundColor: textColor,
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                  ),
                  child: Stack(
                    children: [
                      Text(
                        '회원가입',
                        style: TextStyle(
                          fontSize: 16,
                          foreground: Paint()
                            ..style = PaintingStyle.stroke
                            ..strokeWidth = 1.5
                            ..color = const Color(0xFFFFE5B6),
                        ),
                      ),
                      const Text(
                        '회원가입',
                        style: TextStyle(
                          fontSize: 16,
                          color: textColor,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 24),

              Row(
                children: const [
                  Expanded(
                    child: Divider(
                      thickness: 1,
                      color: secondaryColor,
                      endIndent: 10,
                    ),
                  ),
                  Text(
                    '간편 회원가입',
                    style: TextStyle(
                      color: textColor,
                      fontSize: 14,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  Expanded(
                    child: Divider(
                      thickness: 1,
                      color: secondaryColor,
                      indent: 10,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),

              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  _socialIcon('assets/google.png', _signInWithGoogle),
                  const SizedBox(width: 20),
                  _socialIcon('assets/naver.png', _signInWithNaver),
                  const SizedBox(width: 20),
                  _socialIcon('assets/kakao.png', _signInWithKakao),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}