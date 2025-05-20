import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/user_provider.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final TextEditingController idController = TextEditingController();
  final TextEditingController passwordController = TextEditingController();

  @override
  Widget build(BuildContext context) {
    const borderColor = Color(0xFFFFAD75);

    return Scaffold(
      backgroundColor: const Color(0xFFFFF2DB),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // 로고 이미지
              Image.asset(
                'assets/logo.png',
                height: 150,
              ),
              const SizedBox(height: 0),

              // 아이디 입력
              TextField(
                controller: idController,
                decoration: InputDecoration(
                  filled: true,
                  fillColor: Colors.white,
                  labelText: '이메일 주소',
                  labelStyle: const TextStyle(color: borderColor),
                  enabledBorder: OutlineInputBorder(
                    borderSide: const BorderSide(color: borderColor),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderSide: const BorderSide(color: borderColor, width: 2),
                  ),
                ),
              ),
              const SizedBox(height: 16),

              // 비밀번호 입력
              TextField(
                controller: passwordController,
                obscureText: true,
                decoration: InputDecoration(
                  filled: true,
                  fillColor: Colors.white,
                  labelText: '비밀번호',
                  labelStyle: const TextStyle(color: borderColor),
                  enabledBorder: OutlineInputBorder(
                    borderSide: const BorderSide(color: borderColor),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderSide: const BorderSide(color: borderColor, width: 2),
                  ),
                ),
              ),
              const SizedBox(height: 24),

              // 로그인 버튼
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFFFAD75),
                    foregroundColor: const Color(0xFFE17951),
                  ),
                  onPressed: () {
                    final id = idController.text.trim();
                    final password = passwordController.text.trim();

                    if (id.isNotEmpty && password.isNotEmpty) {
                      // ✅ 전역 상태에 사용자 정보 저장
                      Provider.of<UserProvider>(context, listen: false).setUserInfo(
                        nickname: '멜로디 유저', // 또는 서버 응답 값으로 대체
                        email: id,
                      );

                      Navigator.pushNamed(context, '/home');
                    } else {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('아이디와 비밀번호를 입력해주세요')),
                      );
                    }
                  },
                  child: const Text('로그인'),
                ),
              ),
              const SizedBox(height: 12),

              // 회원가입 버튼
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: () {
                    Navigator.pushNamed(context, '/signup');
                  },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFFFD9A3),
                    foregroundColor: const Color(0xFFE17951),
                  ),
                  child: const Text('회원가입'),
                ),
              ),

              const SizedBox(height: 24),

              // 간편 로그인 구분선
              Row(
                children: [
                  const Expanded(
                    child: Divider(
                      thickness: 1,
                      color: Color(0xFFFFD9A3),
                      endIndent: 10,
                    ),
                  ),
                  const Text(
                    '간편로그인',
                    style: TextStyle(
                      color: Color(0xFFE17951),
                      fontSize: 14,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  const Expanded(
                    child: Divider(
                      thickness: 1,
                      color: Color(0xFFFFD9A3),
                      indent: 10,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),

              // 소셜 로그인 버튼들
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  GestureDetector(
                    onTap: () {
                      // 구글 로그인 기능 추가 예정
                    },
                    child: const CircleAvatar(
                      radius: 20,
                      backgroundImage: AssetImage('assets/google.png'),
                    ),
                  ),
                  const SizedBox(width: 20),
                  GestureDetector(
                    onTap: () {
                      // 네이버 로그인 기능 추가 예정
                    },
                    child: const CircleAvatar(
                      radius: 20,
                      backgroundImage: AssetImage('assets/naver.png'),
                    ),
                  ),
                  const SizedBox(width: 20),
                  GestureDetector(
                    onTap: () {
                      // 카카오 로그인 기능 추가 예정
                    },
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
    );
  }
}
