import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/user_provider.dart';

class SignUpScreen extends StatefulWidget {
  const SignUpScreen({super.key});

  @override
  State<SignUpScreen> createState() => _SignUpScreenState();
}

class _SignUpScreenState extends State<SignUpScreen> {
  final TextEditingController nicknameController = TextEditingController();
  final TextEditingController emailController = TextEditingController();
  final TextEditingController passwordController = TextEditingController();
  final TextEditingController confirmPasswordController = TextEditingController();

  static const backgroundColor = Color(0xFFFFF2DB);
  static const primaryColor = Color(0xFFFFAD75);
  static const textColor = Color(0xFFE17951);
  static const secondaryColor = Color(0xFFFFD9A3);

  void _signup() {
    final nickname = nicknameController.text.trim();
    final email = emailController.text.trim();
    final password = passwordController.text.trim();
    final confirm = confirmPasswordController.text.trim();

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

    // ✅ Provider에 사용자 정보 저장
    Provider.of<UserProvider>(context, listen: false).setUserInfo(
      nickname: nickname,
      email: email,
    );

    // ✅ 다음 화면으로 이동
    Navigator.pushReplacementNamed(context, '/musicalpick');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: backgroundColor,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24.0),
          child: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                IconButton(
                  icon: const Icon(Icons.arrow_back, color: textColor),
                  onPressed: () {
                    Navigator.pop(context);
                  },
                ),
                const SizedBox(height: 10),
                const Text(
                  '회원가입',
                  style: TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                    color: textColor,
                  ),
                ),
                const SizedBox(height: 24),
                _buildTextField(nicknameController, '닉네임'),
                const SizedBox(height: 16),
                _buildTextField(emailController, '이메일 주소'),
                const SizedBox(height: 16),
                _buildTextField(passwordController, '비밀번호', obscureText: true),
                const SizedBox(height: 16),
                _buildTextField(confirmPasswordController, '비밀번호 확인', obscureText: true),
                const SizedBox(height: 24),
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: secondaryColor,
                      foregroundColor: textColor,
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                    onPressed: _signup,
                    child: const Text('회원가입', style: TextStyle(fontSize: 16)),
                  ),
                ),
                const SizedBox(height: 28),
                Row(
                  children: const [
                    Expanded(child: Divider(thickness: 1, color: secondaryColor, endIndent: 10)),
                    Text('간편 회원가입', style: TextStyle(color: textColor)),
                    Expanded(child: Divider(thickness: 1, color: secondaryColor, indent: 10)),
                  ],
                ),
                const SizedBox(height: 16),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    socialLoginIcon('assets/google.png'),
                    const SizedBox(width: 20),
                    socialLoginIcon('assets/naver.png'),
                    const SizedBox(width: 20),
                    socialLoginIcon('assets/kakao.png'),
                  ],
                ),
                const SizedBox(height: 40),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildTextField(TextEditingController controller, String hint, {bool obscureText = false}) {
    return TextField(
      controller: controller,
      obscureText: obscureText,
      decoration: InputDecoration(
        hintText: hint,
        hintStyle: const TextStyle(color: textColor),
        filled: true,
        fillColor: Colors.white,
        border: OutlineInputBorder(
          borderSide: const BorderSide(color: primaryColor),
          borderRadius: BorderRadius.circular(12),
        ),
        focusedBorder: OutlineInputBorder(
          borderSide: const BorderSide(color: textColor, width: 2),
          borderRadius: BorderRadius.circular(12),
        ),
      ),
    );
  }

  Widget socialLoginIcon(String assetPath) {
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
