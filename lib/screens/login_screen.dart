import 'package:flutter/material.dart';

class LoginScreen extends StatelessWidget {
  const LoginScreen({super.key});

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
                decoration: InputDecoration(
                  filled: true,
                  fillColor: Colors.white,
                  labelText: '아이디',
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
                    backgroundColor: Color(0xFFFFAD75),
                    foregroundColor: Color(0xFFE17951), // 텍스트 색 수정
                  ),
                  onPressed: () {},
                  child: const Text('로그인'),
                ),
              ),
              const SizedBox(height: 12),

              // 회원가입 버튼
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: () {
                    Navigator.pushNamed(context, '/signin');
                  },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Color(0xFFFFD9A3),
                    foregroundColor: Color(0xFFE17951), // 텍스트 색 수정
                  ),
                  child: const Text('회원가입'),
                ),
              ),



              // 간편 로그인 텍스트
              const SizedBox(height: 24), // 로그인/회원가입과 간격
              Row(
                children: [
                  Expanded(
                    child: Divider(
                      thickness: 1,
                      color: Color(0xFFFFD9A3),
                      endIndent: 10,
                    ),
                  ),
                  Text(
                    '간편로그인',
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


              // 간편 로그인 아이콘 버튼 3개
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  // 구글
                  CircleAvatar(
                    backgroundColor: Colors.white,
                    radius: 24,
                    child: IconButton(
                      onPressed: () {},
                      icon: Image.asset('assets/google.png'),
                      iconSize: 24,
                    ),
                  ),
                  // 네이버
                  CircleAvatar(
                    backgroundColor: Colors.white,
                    radius: 24,
                    child: IconButton(
                      onPressed: () {},
                      icon: Image.asset('assets/naver.png'),
                      iconSize: 24,
                    ),
                  ),
                  // 카카오
                  CircleAvatar(
                    backgroundColor: Colors.white,
                    radius: 24,
                    child: IconButton(
                      onPressed: () {},
                      icon: Image.asset('assets/kakao.png'),
                      iconSize: 24,
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
