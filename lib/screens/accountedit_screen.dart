import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:firebase_auth/firebase_auth.dart';
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

  static const backgroundColor = Colors.white;
  static const primaryColor = Color(0xFFFFAD75);
  static const textColor = Color(0xFFE17951);
  static const secondaryColor = Color(0xFFFFD9A3);

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

  Future<void> _saveChanges() async {
    final nickname = _nicknameController.text.trim();
    final currentPassword = _currentPasswordController.text.trim();
    final newPassword = _newPasswordController.text.trim();

    if (nickname.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('닉네임을 입력해주세요')),
      );
      return;
    }

    try {
      final user = FirebaseAuth.instance.currentUser!;
      final email = user.email!;

      final cred = EmailAuthProvider.credential(email: email, password: currentPassword);
      await user.reauthenticateWithCredential(cred);

      await user.updateDisplayName(nickname);

      if (newPassword.isNotEmpty) {
        await user.updatePassword(newPassword);
      }

      Provider.of<UserProvider>(context, listen: false).setUserInfo(
        nickname: nickname,
        email: email,
      );

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('정보가 수정되었습니다')),
      );
      Navigator.pop(context);
    } on FirebaseAuthException catch (e) {
      String message = '정보 수정 실패';
      if (e.code == 'wrong-password') {
        message = '현재 비밀번호가 일치하지 않습니다';
      }
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
    }
  }

  InputDecoration _inputDecoration(String label, {bool readOnly = false}) {
    return InputDecoration(
      labelText: label,
      filled: true,
      fillColor: readOnly ? const Color(0xFFEFEFEF) : Colors.white,
      labelStyle: TextStyle(color: readOnly ? Colors.grey : primaryColor),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: BorderSide(color: readOnly ? Colors.grey : primaryColor),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: BorderSide(color: readOnly ? Colors.grey : primaryColor, width: 2),
      ),
    );
  }

  Widget _textField(TextEditingController controller, String label,
      {bool obscure = false, bool readOnly = false}) {
    return TextField(
      controller: controller,
      obscureText: obscure,
      readOnly: readOnly,
      decoration: _inputDecoration(label, readOnly: readOnly),
      style: TextStyle(color: readOnly ? Colors.grey : textColor),
    );
  }

  @override
  Widget build(BuildContext context) {
    final user = Provider.of<UserProvider>(context);

    return Scaffold(
      backgroundColor: backgroundColor,
      appBar: AppBar(
        backgroundColor: backgroundColor,
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
              const SizedBox(height: 36),
              const CircleAvatar(
                radius: 40,
                backgroundImage: AssetImage('assets/logo.png'),
              ),
              const SizedBox(height: 12),
              Text(user.nickname, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              Text(user.email, style: const TextStyle(color: Colors.black54)),
              const SizedBox(height: 32),

              _textField(_nicknameController, '닉네임'),
              const SizedBox(height: 12),
              _textField(_emailController, '이메일 주소', readOnly: true),
              const SizedBox(height: 12),
              _textField(_currentPasswordController, '현재 비밀번호', obscure: true),
              const SizedBox(height: 12),
              _textField(_newPasswordController, '새 비밀번호 (선택)', obscure: true),
              const SizedBox(height: 30),

              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: _saveChanges,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: primaryColor, // ← LoginScreen과 동일
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16), // ← 둥근 테두리 통일
                    ),
                  ),
                  child: Stack(
                    children: [
                      Text(
                        '저장',
                        style: TextStyle(
                          fontSize: 16,
                          foreground: Paint()
                            ..style = PaintingStyle.stroke
                            ..strokeWidth = 1.5
                            ..color = const Color(0xFFFFE5B6), // stroke 효과
                        ),
                      ),
                      const Text(
                        '저장',
                        style: TextStyle(
                          fontSize: 16,
                          color: textColor,
                        ),
                      ),
                    ],
                  ),
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
}
