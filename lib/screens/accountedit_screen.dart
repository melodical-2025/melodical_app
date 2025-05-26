import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/user_provider.dart';
import '../widgets/navigationbar.dart';

class AccounteditScreen extends StatefulWidget {
  const AccounteditScreen({super.key});

  @override
  State<AccounteditScreen> createState() => _AccounteditScreenState();
}

class _AccounteditScreenState extends State<AccounteditScreen> {
  late TextEditingController _nicknameController;
  late TextEditingController _emailController;
  final TextEditingController _currentPasswordController = TextEditingController();
  final TextEditingController _newPasswordController = TextEditingController();

  static const primaryColor = Color(0xFFFFAD75);
  static const textColor = Color(0xFFE17951);

  @override
  void initState() {
    super.initState();
    final user = Provider.of<UserProvider>(context, listen: false);
    _nicknameController = TextEditingController(text: user.nickname);
    _emailController = TextEditingController(text: user.email);
  }

  @override
  void dispose() {
    _nicknameController.dispose();
    _emailController.dispose();
    _currentPasswordController.dispose();
    _newPasswordController.dispose();
    super.dispose();
  }

  void _saveChanges() {
    final nickname = _nicknameController.text.trim();
    final email = _emailController.text.trim();

    if (nickname.isEmpty || email.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('닉네임과 이메일을 모두 입력해주세요')),
      );
      return;
    }

    // 비밀번호 검증은 생략 (필요하면 추가 가능)
    final user = Provider.of<UserProvider>(context, listen: false);
    user.setUserInfo(nickname: nickname, email: email);

    Navigator.pop(context);
  }

  @override
  Widget build(BuildContext context) {
    final user = Provider.of<UserProvider>(context);

    return Scaffold(
      backgroundColor: Colors.white, // 배경색 흰색
      appBar: AppBar(
        backgroundColor: Colors.white, // 앱바 배경색 흰색
        elevation: 0,
        centerTitle: true,
        title: const Text('회원정보 수정', style: TextStyle(color: textColor)),
        iconTheme: const IconThemeData(color: textColor),
      ),
      body: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24),
        child: SingleChildScrollView(
          child: Column(
            children: [
              const SizedBox(height: 16),
              const CircleAvatar(
                radius: 40,
                backgroundImage: AssetImage('assets/logo.png'),
              ),
              const SizedBox(height: 12),
              Text(
                user.nickname,
                style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              Text(
                user.email,
                style: const TextStyle(color: Colors.black54),
              ),
              const SizedBox(height: 32),

              _buildInputField(_nicknameController, '닉네임'),
              const SizedBox(height: 12),
              _buildInputField(_emailController, '이메일 주소'),
              const SizedBox(height: 12),
              _buildInputField(_currentPasswordController, '기존 비밀번호', obscureText: true),
              const SizedBox(height: 12),
              _buildInputField(_newPasswordController, '수정할 비밀번호', obscureText: true),
              const SizedBox(height: 32),

              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: _saveChanges,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: primaryColor,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  child: const Text('저장', style: TextStyle(fontSize: 16)),
                ),
              ),
              const SizedBox(height: 40),
            ],
          ),
        ),
      ),
      bottomNavigationBar: BottomNavBar(currentIndex: 3),
    );
  }

  Widget _buildInputField(TextEditingController controller, String hint, {bool obscureText = false}) {
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
}
