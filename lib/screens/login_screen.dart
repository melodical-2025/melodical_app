import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_naver_login/interface/types/naver_login_status.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';
import 'package:kakao_flutter_sdk_auth/kakao_flutter_sdk_auth.dart';
import 'package:flutter_naver_login/flutter_naver_login.dart';

import '../services/api_service.dart';
import '../services/token_storage.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});
  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  bool _isLoading = false;

  static const backgroundColor = Color(0xFFFFF2DB);
  static const primaryColor = Color(0xFFFFAD75);
  static const textColor = Color(0xFFE17951);
  static const secondaryColor = Color(0xFFFFD9A3);

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    final email = _emailController.text.trim();
    final pwd   = _passwordController.text.trim();
    if (email.isEmpty || pwd.isEmpty) return _showSnack('이메일과 비밀번호를 입력해주세요');

    setState(() => _isLoading = true);
    try {
      final resp = await ApiService.login(email, pwd);
      if (resp.statusCode == 200) {
        final data = jsonDecode(resp.body);
        await TokenStorage().save(data['token']);
        Navigator.pushReplacementNamed(context, '/home');
      } else {
        _showSnack('로그인 실패 (${resp.statusCode})');
      }
    } catch (e) {
      _showSnack('오류: $e');
    } finally {
      setState(() => _isLoading = false);
    }
  }
  void _goToSignup() {
    Navigator.pushNamed(context, '/signup');
  }

  Future<void> _loginWithGoogle() async {
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
        Navigator.pushReplacementNamed(context, '/home');
      } else {
        throw 'Google 로그인 실패 (${resp.statusCode})';
      }
    } catch (e) {
      _showSnack(e.toString());
    }
    setState(() => _isLoading = false);
  }

  Future<void> _loginWithKakao() async {
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
        Navigator.pushReplacementNamed(context, '/home');
      } else {
        throw '카카오 로그인 실패 (${resp.statusCode})';
      }
    } catch (e) {
      _showSnack(e.toString());
    }
    setState(() => _isLoading = false);
  }

  Future<void> _loginWithNaver() async {
    setState(() => _isLoading = true);
    try {
      final res = await FlutterNaverLogin.logIn();
      if (res.status != NaverLoginStatus.loggedIn) {
        throw '네이버 로그인 취소/실패';
      }
      final resp = await ApiService.post('/auth/login/naver', {
        'token': res.accessToken,
      });
      if (resp.statusCode == 200) {
        final data = jsonDecode(resp.body);
        await TokenStorage().save(data['token']);
        Navigator.pushReplacementNamed(context, '/home');
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

  @override
  Widget build(BuildContext ctx) {
    return Scaffold(
      backgroundColor: backgroundColor,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24),
          child: Center(
            child: SingleChildScrollView(
              child: Column(
                children: [
                  Image.asset('assets/logo.png', height: 150),
                  const SizedBox(height: 24),
                  TextField(
                    controller: _emailController,
                    decoration: _inputDeco('이메일'),
                    style: const TextStyle(color: textColor),
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller: _passwordController,
                    obscureText: true,
                    decoration: _inputDeco('비밀번호'),
                    style: const TextStyle(color: textColor),
                  ),
                  const SizedBox(height: 24),
                  _buildButton('로그인', primaryColor, _login),
                  const SizedBox(height: 12),
                  _buildButton('회원가입', secondaryColor, _goToSignup),
                  const SizedBox(height: 24),
                  Row(
                      children: [
                        Expanded(child: Divider(color: secondaryColor)),
                        const Padding(
                      padding: EdgeInsets.symmetric(horizontal: 8),
                      child: Text('간편로그인', style: TextStyle(color: textColor)),
                    ),
                    Expanded(child: Divider(color: secondaryColor)),
                  ]),
                  const SizedBox(height: 16),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      _socialIcon('assets/google.png', _loginWithGoogle),
                      const SizedBox(width: 20),
                      _socialIcon('assets/naver.png', _loginWithNaver),
                      const SizedBox(width: 20),
                      _socialIcon('assets/kakao.png', _loginWithKakao),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  InputDecoration _inputDeco(String label) => InputDecoration(
    filled: true,
    fillColor: Colors.white,
    labelText: label,
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

  Widget _buildButton(String text, Color bg, VoidCallback onTap) {
    return SizedBox(
      width: double.infinity,
      child: ElevatedButton(
        onPressed: _isLoading ? null : onTap,
        style: ElevatedButton.styleFrom(
          backgroundColor: bg,
          padding: const EdgeInsets.symmetric(vertical: 14),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        ),
        child: Text(text, style: const TextStyle(fontSize: 16, color: textColor)),
      ),
    );
  }

  Widget _socialIcon(String asset, VoidCallback onTap) {
    return GestureDetector(
      onTap: _isLoading ? null : onTap,
      child: CircleAvatar(radius: 20, backgroundImage: AssetImage(asset)),
    );
  }
}
