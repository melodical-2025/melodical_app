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
                    borderRadius: BorderRadius.circular(8),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderSide: const BorderSide(color: borderColor, width: 2),
                    borderRadius: BorderRadius.circular(8),
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
                    borderRadius: BorderRadius.circular(8),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderSide: const BorderSide(color: borderColor, width: 2),
                    borderRadius: BorderRadius.circular(8),
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
                    foregroundColor: Color(0xFFE17951),
                    padding: EdgeInsets.symmetric(vertical: 12),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16), // 더 둥글게
                    ),
                  ),
                  onPressed: () {
                    Navigator.pushNamed(context, '/musicalpick');
                  }, // 눌렀을때 동작 정의안됨 아직
                  child: Stack(
                    children: [
                      // Stroke 텍스트 (아래쪽)
                      Text(
                        '로그인',
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
                        '로그인',
                        style: TextStyle(
                          fontSize: 16,
                          color: Color(0xFFE17951),
                        ),
                      ),
                    ],
                  ),

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


// 소셜 로그인 버튼 (구글, 네이버, 카카오 이미지)
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  GestureDetector(
                    onTap: () {
                      // 구글 로그인 기능
                    },
                    child: CircleAvatar(
                      radius: 20,
                      backgroundImage: AssetImage('assets/google.png'),
                    ),
                  ),
                  const SizedBox(width: 20),
                  GestureDetector(
                    onTap: () {
                      // 네이버 로그인 기능
                    },
                    child: CircleAvatar(
                      radius: 20,
                      backgroundImage: AssetImage('assets/naver.png'),
                    ),
                  ),
                  const SizedBox(width: 20),
                  GestureDetector(
                    onTap: () {
                      // 카카오 로그인 기능
                    },
                    child: CircleAvatar(
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
