import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/user_provider.dart';

class SignupScreen extends StatefulWidget {
  const SignupScreen({super.key});

  @override
  State<SignupScreen> createState() => _SignupScreenState();
}

class _SignupScreenState extends State<SignupScreen> {
  final TextEditingController _nicknameController = TextEditingController();
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final TextEditingController _confirmPasswordController = TextEditingController();

  static const backgroundColor = Color(0xFFFFF2DB);
  static const primaryColor = Color(0xFFFFAD75);
  static const textColor = Color(0xFFE17951);
  static const secondaryColor = Color(0xFFFFD9A3);

  void _signUp() {
    final nickname = _nicknameController.text.trim();
    final email = _emailController.text.trim();
    final password = _passwordController.text.trim();
    final confirm = _confirmPasswordController.text.trim();

    if (nickname.isEmpty || email.isEmpty || password.isEmpty || confirm.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('모든 항목을 입력해주세요')),
      );
      return;
    }

    if (password != confirm) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('비밀번호가 일치하지 않습니다')),
      );
      return;
    }

    Provider.of<UserProvider>(context, listen: false).setUserInfo(
      nickname: nickname,
      email: email,
    );

    Navigator.pushReplacementNamed(context, '/musicalpick');
  }

  InputDecoration _inputDecoration(String label) {
    return InputDecoration(
      labelText: label,
      filled: true,
      fillColor: Colors.white,
      labelStyle: const TextStyle(color: primaryColor),
      enabledBorder: OutlineInputBorder(
        borderSide: const BorderSide(color: primaryColor),
        borderRadius: BorderRadius.circular(8),
      ),
      focusedBorder: OutlineInputBorder(
        borderSide: const BorderSide(color: primaryColor, width: 2),
        borderRadius: BorderRadius.circular(8),
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
              // 뒤로가기 버튼
              IconButton(
                icon: const Icon(Icons.arrow_back),
                color: textColor,
                onPressed: () => Navigator.pop(context),
              ),
              const SizedBox(height: 40),

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

              // ✅ 로그인 버튼 (stroke 포함)
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: _signUp,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: secondaryColor,
                    foregroundColor: textColor,
                    padding: const EdgeInsets.symmetric(vertical: 12),
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
                  _socialIcon('assets/google.png'),
                  const SizedBox(width: 20),
                  _socialIcon('assets/naver.png'),
                  const SizedBox(width: 20),
                  _socialIcon('assets/kakao.png'),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _socialIcon(String assetPath) {
    return GestureDetector(
      onTap: () {
        // 소셜 로그인 연동 예정
      },
      child: CircleAvatar(
        radius: 20,
        backgroundImage: AssetImage(assetPath),
      ),
    );
  }
}
