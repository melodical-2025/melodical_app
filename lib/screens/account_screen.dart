import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/user_provider.dart';
import '../widgets/navigationbar.dart';

class AccountScreen extends StatelessWidget {
  const AccountScreen({super.key});

  static const backgroundColor = Colors.white;
  static const primaryColor = Color(0xFFFFAD75);
  static const textColor = Color(0xFFE17951);
  static const dividerColor = Color(0xFFFFD9A3);

  @override
  Widget build(BuildContext context) {
    final user = Provider.of<UserProvider>(context);

    return Scaffold(
      backgroundColor: backgroundColor,

      appBar: PreferredSize(
        preferredSize: const Size.fromHeight(110),
        child: Container(
          height: 110,
          decoration: const BoxDecoration(
            color: Colors.white,
            boxShadow: [
              BoxShadow(
                color: Color(0xFFE17951),
                blurRadius: 4,
                offset: Offset(5, 0),
                spreadRadius: 0,
              ),
            ],
          ),
          child: SafeArea(
            bottom: false,
            child: Padding(
              padding: const EdgeInsets.only(bottom: 16, left: 8, right: 8),
              child: Stack(
                alignment: Alignment.bottomCenter,
                children: [
                  const Positioned(
                    bottom: 0,
                    left: 0,
                    right: 0,
                    child: Center(
                      child: Text(
                        '계정 설정',
                        style: TextStyle(
                          color: textColor,
                          fontSize: 24,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ),
                  Positioned(
                    left: 0,
                    bottom: 0,
                    child: IconButton(
                      icon: const Icon(Icons.arrow_back_ios_new, color: textColor),
                      tooltip: '뒤로',
                      onPressed: () {
                        // 명시적으로 마이페이지로 이동 (스택 정리 원하면 pushReplacement 사용)
                        Navigator.pushReplacementNamed(context, '/accountmypage');
                      },
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),

      body: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            const SizedBox(height: 32),
            const CircleAvatar(
              radius: 40,
              backgroundImage: AssetImage('assets/logo.png'),
            ),
            const SizedBox(height: 12),
            Text(
              user.nickname,
              style: const TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
                color: Colors.black,
              ),
            ),
            Text(
              user.email,
              style: const TextStyle(
                fontSize: 14,
                color: Colors.black54,
              ),
            ),
            const SizedBox(height: 24),

            _flatSettingItem(
              context,
              '닉네임 변경',
              onTap: () async {
                final result = await Navigator.pushNamed(context, '/nickname-setting');
                // 닉네임 변경 후 돌아오면 화면 새로고침
                if (result == true && context.mounted) {
                  // UserProvider 새로고침하여 닉네임 업데이트
                  Provider.of<UserProvider>(context, listen: false).loadUserInfo();
                }
              },
            ),
            _flatSettingItem(
              context,
              '회원정보 수정',
              onTap: () => Navigator.pushNamed(context, '/accountedit'),
            ),
            _flatSettingItem(
              context,
              '로그아웃',
              onTap: () => Navigator.pushReplacementNamed(context, '/login'),
            ),
            _flatSettingItem(
              context,
              '탈퇴하기',
              onTap: () {
                // TODO: 탈퇴 처리 연결
              },
            ),
          ],
        ),
      ),

      // 필요 시 유지 (다른 화면과 톤 통일)
      bottomNavigationBar: const BottomNavBar(currentIndex: 3),
    );
  }

  Widget _flatSettingItem(BuildContext context, String label, {VoidCallback? onTap}) {
    return Column(
      children: [
        InkWell(
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 16.0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(label, style: const TextStyle(fontSize: 16, color: Colors.black)),
                const Icon(Icons.chevron_right, color: textColor),
              ],
            ),
          ),
        ),
        const Divider(color: dividerColor, thickness: 1, height: 1),
      ],
    );
  }
}
