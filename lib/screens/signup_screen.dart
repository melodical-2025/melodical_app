import 'package:flutter/material.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

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

    // 기본 유효성
    if (nick.isEmpty || email.isEmpty || pwd.isEmpty || confirm.isEmpty) {
      _showSnack('모든 항목을 입력해주세요');
      return;
    }
    if (!_looksLikeEmail(email)) {
      _showSnack('유효한 이메일 주소를 입력해주세요');
      return;
    }
    if (pwd.length < 8) {
      _showSnack('비밀번호는 8자 이상이어야 합니다');
      return;
    }
    if (pwd != confirm) {
      _showSnack('비밀번호가 일치하지 않습니다');
      return;
    }

    setState(() => _isLoading = true);
    try {
      final supa = Supabase.instance.client;
      final auth = supa.auth;

      // 1) 회원가입 (닉네임은 user metadata에 저장)
      final res = await auth.signUp(
        email: email,
        password: pwd,
        data: {'nickname': nick},
      );

      // 2) profiles 테이블에도 동기화 (id = auth.users.id)
      final uid = res.user?.id ?? auth.currentUser?.id;
      if (uid != null) {
        await supa.from('profiles').upsert({'id': uid, 'nickname': nick});
      }

      // 3) 안내 및 이동
      if (res.user != null && (res.user!.emailConfirmedAt == null)) {
        _showSnack('회원가입 완료! 이메일 인증 링크를 확인해주세요.');
      } else {
        _showSnack('회원가입 완료! 로그인해주세요.');
      }

      if (!mounted) return;
      Navigator.pushReplacementNamed(context, '/login');
    } on AuthException catch (e) {
      _showSnack(_translateError(e.message));
    } catch (e) {
      _showSnack('회원가입 오류: $e');
    } finally {
      setState(() => _isLoading = false);
    }
  }

  // 소셜은 추후 연동
  void _notReady(String provider) {
    _showSnack('$provider 회원가입/로그인은 나중에 연결할게요 (지금은 이메일/비번만)');
  }

  // ========= 헬퍼 =========

  bool _looksLikeEmail(String v) {
    // 아주 가벼운 형태 체크 (정교할 필요 X)
    return RegExp(r'^[^@]+@[^@]+\.[^@]+').hasMatch(v);
  }

  String _translateError(String? msg) {
    if (msg == null || msg.isEmpty) return '알 수 없는 오류가 발생했습니다.';
    if (msg.contains('User already registered')) {
      return '이미 가입된 이메일입니다. 로그인하거나 비밀번호를 재설정하세요.';
    }
    if (msg.contains('Email not confirmed')) {
      return '이메일 인증이 완료되지 않았습니다. 메일함을 확인해주세요.';
    }
    if (msg.contains('Invalid email') || msg.contains('invalid email')) {
      return '유효한 이메일 주소를 입력해주세요.';
    }
    if (msg.contains('Password should be at least')) {
      return '비밀번호 길이가 너무 짧습니다.';
    }
    if (msg.contains('Rate limit') || msg.contains('Too many requests')) {
      return '요청이 너무 많습니다. 잠시 후 다시 시도해주세요.';
    }
    return '오류가 발생했습니다: $msg';
  }

  void _showSnack(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
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
      child: CircleAvatar(radius: 20, backgroundImage: AssetImage(assetPath)),
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
                decoration: _inputDecoration('비밀번호 (8자 이상)'),
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
                      const Text('회원가입', style: TextStyle(fontSize: 16, color: textColor)),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 24),

              Row(
                children: const [
                  Expanded(child: Divider(thickness: 1, color: secondaryColor, endIndent: 10)),
                  Text('간편 회원가입', style: TextStyle(color: textColor, fontSize: 14, fontWeight: FontWeight.w500)),
                  Expanded(child: Divider(thickness: 1, color: secondaryColor, indent: 10)),
                ],
              ),
              const SizedBox(height: 16),

              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  _socialIcon('assets/google.png', () => _notReady('Google')),
                  const SizedBox(width: 20),
                  _socialIcon('assets/naver.png', () => _notReady('Naver')),
                  const SizedBox(width: 20),
                  _socialIcon('assets/kakao.png', () => _notReady('Kakao')),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
