import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/user_provider.dart';
import '../widgets/navigationbar.dart';

class AccountScreen extends StatelessWidget {
  const AccountScreen({super.key});

  static const backgroundColor = Color(0xFFFFF2DB);
  static const primaryColor = Color(0xFFFFAD75);
  static const textColor = Color(0xFFE17951);
  static const dividerColor = Color(0xFFFFD9A3);

  @override
  Widget build(BuildContext context) {
    final user = Provider.of<UserProvider>(context);

    return Scaffold(
      backgroundColor: backgroundColor,
      appBar: AppBar(
        backgroundColor: backgroundColor,
        elevation: 0,
        centerTitle: true,
        title: const Text('마이페이지', style: TextStyle(color: textColor)),
        iconTheme: const IconThemeData(color: textColor),
      ),
      body: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            const SizedBox(height: 16),
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
            const SizedBox(height: 32),

            Align(
              alignment: Alignment.centerLeft,
              child: Text(
                '계정설정',
                style: TextStyle(
                  color: textColor,
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
            const SizedBox(height: 8),

            _flatSettingItem(
              context,
              '회원정보 수정',
              onTap: () {
                print('회원정보 수정 클릭됨');
                Navigator.pushNamed(context, '/accountedit');
              },
            ),
            _flatSettingItem(
              context,
              '로그아웃',
              onTap: () {
                Navigator.pushReplacementNamed(context, '/login');
              },
            ),
            _flatSettingItem(
              context,
              '탈퇴하기',
              onTap: () {
                // 탈퇴 처리 예정
              },
            ),
          ],
        ),
      ),
      bottomNavigationBar: BottomNavBar(
        currentIndex: 3,
        onTabTapped: (index) {
          if (index == 0) Navigator.pushNamed(context, '/home');
          if (index == 1) Navigator.pushNamed(context, '/search');
          if (index == 2) Navigator.pushNamed(context, '/ratemusical');
        },
      ),
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
