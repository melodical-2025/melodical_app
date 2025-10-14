import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});
  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  bool _isLoading = false;

  // UI 컬러 (기존 스타일 유지)
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

  // ---------------------------
  // Supabase 이메일/비번 - 회원가입
  // ---------------------------
  Future<void> _signupWithSupabase() async {
    final email = _emailController.text.trim();
    final pwd   = _passwordController.text.trim();
    if (email.isEmpty || pwd.isEmpty) {
      return _showSnack('이메일과 비밀번호를 입력해주세요');
    }
    if (pwd.length < 8) {
      return _showSnack('비밀번호는 8자 이상이어야 합니다');
    }

    setState(() => _isLoading = true);
    try {
      final res = await Supabase.instance.client.auth.signUp(
        email: email,
        password: pwd,
      );

      // 이메일 확인이 필요한 프로젝트가 많습니다.
      if (res.user != null && (res.user!.emailConfirmedAt == null)) {
        _showSnack('회원가입 완료! 이메일 인증 링크를 확인해주세요');
      } else if (res.user != null) {
        _showSnack('회원가입 완료! 이제 로그인하세요');
      } else {
        _showSnack('회원가입 실패: 알 수 없는 오류');
      }
    } on AuthException catch (e) {
      _showSnack(_translateError(e.message));
    } catch (e) {
      _showSnack('오류가 발생했습니다: $e');
    } finally {
      setState(() => _isLoading = false);
    }
  }

  // ---------------------------
  // Supabase 이메일/비번 - 로그인
  // ---------------------------
  Future<void> _loginWithSupabase() async {
    final email = _emailController.text.trim();
    final pwd   = _passwordController.text.trim();
    if (email.isEmpty || pwd.isEmpty) {
      return _showSnack('이메일과 비밀번호를 입력해주세요');
    }

    setState(() => _isLoading = true);
    try {
      final res = await Supabase.instance.client.auth
          .signInWithPassword(email: email, password: pwd);

      if (res.session != null) {
        if (!mounted) return;
        Navigator.pushReplacementNamed(context, '/home');
      } else {
        _showSnack('로그인 실패: 세션 없음');
      }
    } on AuthException catch (e) {
      _showSnack(_translateError(e.message));
    } catch (e) {
      _showSnack('오류가 발생했습니다: $e');
    } finally {
      setState(() => _isLoading = false);
    }
  }

  // ---------------------------
  // Supabase 비밀번호 재설정 이메일 전송
  // ---------------------------
  Future<void> _sendResetWithSupabase() async {
    final email = _emailController.text.trim();
    if (email.isEmpty) return _showSnack('재설정 이메일을 받을 주소를 입력해주세요');

    setState(() => _isLoading = true);
    try {
      await Supabase.instance.client.auth.resetPasswordForEmail(email);
      _showSnack('비밀번호 재설정 이메일을 보냈습니다');
    } on AuthException catch (e) {
      _showSnack(_translateError(e.message));
    } catch (e) {
      _showSnack('오류가 발생했습니다: $e');
    } finally {
      setState(() => _isLoading = false);
    }
  }

  // 소셜 로그인 자리(지금은 비활성 안내)
  Future<void> _notReadyYet(String provider) async {
    _showSnack('$provider 로그인은 나중에 연결할게요 (현재 이메일/비번 지원)');
  }

  /// Supabase 에러 메시지 → 한글화
  String _translateError(String? msg) {
    if (msg == null || msg.isEmpty) return '알 수 없는 오류가 발생했습니다.';
    // 자주 나오는 문자열 위주로 매핑
    if (msg.contains('Invalid login credentials')) {
      return '이메일 또는 비밀번호가 올바르지 않습니다';
    }
    if (msg.contains('Email not confirmed')) {
      return '이메일 인증이 완료되지 않았습니다. 메일함을 확인해주세요.';
    }
    if (msg.contains('User already registered')) {
      return '이미 가입된 이메일입니다. 로그인하거나 비밀번호를 재설정하세요.';
    }
    if (msg.contains('Rate limit') || msg.contains('Too many requests')) {
      return '요청이 너무 많습니다. 잠시 후 다시 시도해주세요.';
    }
    if (msg.contains('invalid email') || msg.contains('Invalid email')) {
      return '유효한 이메일 주소를 입력해주세요.';
    }
    if (msg.contains('Password should be at least')) {
      return '비밀번호 길이가 너무 짧습니다.';
    }
    if (msg.contains('JWT expired') || msg.contains('Session not found')) {
      return '로그인이 만료되었습니다. 다시 로그인해주세요.';
    }
    // 그 외는 원문 포함
    return '오류가 발생했습니다: $msg';
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
                    keyboardType: TextInputType.emailAddress,
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

                  // Supabase 이메일/비번 로그인 & 회원가입
                  _buildButton('로그인', primaryColor, _loginWithSupabase),
                  const SizedBox(height: 12),
                  _buildButton('회원가입', secondaryColor, () async {
                    if (!mounted) return;
                    Navigator.pushNamed(context, '/signup');
                  }),
                  const SizedBox(height: 12),
                  TextButton(
                    onPressed: _isLoading ? null : _sendResetWithSupabase,
                    child: const Text('비밀번호 재설정'),
                  ),

                  const SizedBox(height: 24),
                  Row(
                    children: [
                      Expanded(child: Divider(color: secondaryColor)),
                      const Padding(
                        padding: EdgeInsets.symmetric(horizontal: 8),
                        child: Text('간편로그인', style: TextStyle(color: textColor)),
                      ),
                      Expanded(child: Divider(color: secondaryColor)),
                    ],
                  ),
                  const SizedBox(height: 16),

                  // 소셜 아이콘 (현재는 안내만)
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      _socialIcon('assets/google.png', () => _notReadyYet('Google')),
                      const SizedBox(width: 20),
                      _socialIcon('assets/naver.png', () => _notReadyYet('Naver')),
                      const SizedBox(width: 20),
                      _socialIcon('assets/kakao.png', () => _notReadyYet('Kakao')),
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

  Widget _buildButton(String text, Color bg, Future<void> Function() onTap) {
    return SizedBox(
      width: double.infinity,
      child: ElevatedButton(
        onPressed: _isLoading ? null : () async { await onTap(); },
        style: ElevatedButton.styleFrom(
          backgroundColor: bg,
          padding: const EdgeInsets.symmetric(vertical: 14),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          side: bg == Colors.white ? const BorderSide(color: primaryColor) : BorderSide.none,
        ),
        child: Text(
          text,
          style: TextStyle(
            fontSize: 16,
            color: bg == Colors.white ? primaryColor : textColor,
            fontWeight: FontWeight.w600,
          ),
        ),
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
