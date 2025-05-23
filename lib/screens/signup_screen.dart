import 'package:flutter/material.dart';

class SignupScreen extends StatefulWidget {
  const SignupScreen({super.key});

  @override
  State<SignupScreen> createState() => _SignupScreenState();
}

class _SignupScreenState extends State<SignupScreen> {
  final _formKey = GlobalKey<FormState>();

  final TextEditingController _nicknameController = TextEditingController();
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final TextEditingController _confirmPasswordController = TextEditingController();

  void _signUp() {
    if (_formKey.currentState!.validate()) {
      print('회원가입 성공');
    }
  }

  InputDecoration customInputDecoration(String label) {
    return InputDecoration(
      labelText: label,
      filled: true,
      fillColor: Colors.white,
      labelStyle: const TextStyle(color: Color(0xFFFFAD75)),
      enabledBorder: OutlineInputBorder(
        borderSide: const BorderSide(color: Color(0xFFFFAD75)),
        borderRadius: BorderRadius.circular(8),
      ),
      focusedBorder: OutlineInputBorder(
        borderSide: const BorderSide(color: Color(0xFFFFAD75), width: 2),
        borderRadius: BorderRadius.circular(8),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFFF2DB),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // 뒤로가기 버튼
                IconButton(
                  icon: const Icon(Icons.arrow_back),
                  color: Color(0xFFD65E2E),
                  onPressed: () {
                    Navigator.pop(context);
                  },
                ),
                const SizedBox(height: 50),

                // 제목
                const Text(
                  "회원가입",
                  style: TextStyle(
                    fontSize: 28,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFFD65E2E),
                  ),
                ),
                const SizedBox(height: 30),

                // 닉네임
                TextFormField(
                  controller: _nicknameController,
                  decoration: customInputDecoration('닉네임'),
                  style: const TextStyle(color: Color(0xFFFFAD75)),
                  validator: (value) {
                    if (value == null || value.isEmpty) return '닉네임을 입력해주세요.';
                    return null;
                  },
                ),
                const SizedBox(height: 20),

                // 이메일
                TextFormField(
                  controller: _emailController,
                  decoration: customInputDecoration('이메일'),
                  keyboardType: TextInputType.emailAddress,
                  style: const TextStyle(color: Color(0xFFFFAD75)),
                  validator: (value) {
                    if (value == null || value.isEmpty) return '이메일을 입력해주세요.';
                    if (!value.contains('@')) return '올바른 이메일 형식이 아닙니다.';
                    return null;
                  },
                ),
                const SizedBox(height: 20),

                // 비밀번호
                TextFormField(
                  controller: _passwordController,
                  decoration: customInputDecoration('비밀번호'),
                  obscureText: true,
                  style: const TextStyle(color: Color(0xFFFFAD75)),
                  validator: (value) {
                    if (value == null || value.length < 6) return '비밀번호는 6자 이상이어야 합니다.';
                    return null;
                  },
                ),
                const SizedBox(height: 20),

                // 비밀번호 확인
                TextFormField(
                  controller: _confirmPasswordController,
                  decoration: customInputDecoration('비밀번호 확인'),
                  obscureText: true,
                  style: const TextStyle(color: Color(0xFFFFAD75)),
                  validator: (value) {
                    if (value != _passwordController.text) return '비밀번호가 일치하지 않습니다.';
                    return null;
                  },
                ),
                const SizedBox(height: 30),

                // 회원가입 버튼
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: () {
                      //회원가입로직
                    },
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Color(0xFFFFD9A3),
                      foregroundColor: Color(0xFFE17951),
                      padding: EdgeInsets.symmetric(vertical: 12),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16), // 더 둥글게
                      ),
                    ),
                    child: Stack(
                      children: [
                        // Stroke 텍스트 (아래쪽)
                        Text(
                          '회원가입',
                          style: TextStyle(
                            fontSize: 16,
                            foreground: Paint()
                              ..style = PaintingStyle.stroke
                              ..strokeWidth = 1.5
                              ..color = Color(0xFFFFE5B6),
                          ),
                        ),
                        // 기본 텍스트 (위쪽)
                        Text(
                          '회원가입',
                          style: TextStyle(
                            fontSize: 16,
                            color: Color(0xFFE17951),
                          ),
                        ),
                      ],
                    ),

                  ),
                ),

                // 간편 회원가입 텍스트
                const SizedBox(height: 24), // 로그인/회원가입과 간격
                Row(
                  children: const [
                    Expanded(
                      child: Divider(
                        thickness: 1,
                        color: Color(0xFFFFD9A3),
                        endIndent: 10,
                      ),
                    ),
                    Text(
                      '간편 회원가입',
                      style: TextStyle(
                        color: Color(0xFFE17951),
                        fontSize: 14,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                    Expanded(
                      child: Divider(
                        thickness: 1,
                        color: Color(0xFFFFD9A3),
                        indent: 10,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),

                // 소셜 아이콘
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    GestureDetector(
                      onTap: () {},
                      child: const CircleAvatar(
                        radius: 20,
                        backgroundImage: AssetImage('assets/google.png'),
                      ),
                    ),
                    const SizedBox(width: 20),
                    GestureDetector(
                      onTap: () {},
                      child: const CircleAvatar(
                        radius: 20,
                        backgroundImage: AssetImage('assets/naver.png'),
                      ),
                    ),
                    const SizedBox(width: 20),
                    GestureDetector(
                      onTap: () {},
                      child: const CircleAvatar(
                        radius: 20,
                        backgroundImage: AssetImage('assets/kakao.png'),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
