import 'package:flutter/material.dart';

class BottomNavBar extends StatelessWidget {
  final int currentIndex;

  const BottomNavBar({
    Key? key,
    required this.currentIndex,
  }) : super(key: key);

  void _onTabTapped(BuildContext context, int index) {
    String route;
    switch (index) {
      case 0:
        route = '/home';
        break;
      case 1:
        route = '/search';
        break;
      case 2:
        route = '/ratemusical';
        break;
      case 3:
        route = '/account';
        break;
      default:
        return;
    }

    if (ModalRoute.of(context)?.settings.name != route) {
      Navigator.pushReplacementNamed(context, route);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 79,
      decoration: BoxDecoration(
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
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          // 홈 버튼
          GestureDetector(
            onTap: () => _onTabTapped(context, 0),
            child: Icon(
              Icons.home,
              size: 32,
              color: currentIndex == 0 ? Color(0xFFE17951) : Colors.black,
            ),
          ),
          // 검색 버튼
          GestureDetector(
            onTap: () => _onTabTapped(context, 1),
            child: Icon(
              Icons.search,
              size: 32,
              color: currentIndex == 1 ? Color(0xFFE17951) : Colors.black,
            ),
          ),
          // 평가 버튼
          GestureDetector(
            onTap: () => _onTabTapped(context, 2),
            child: Icon(
              Icons.star,
              size: 32,
              color: currentIndex == 2 ? Color(0xFFE17951) : Colors.black,
            ),
          ),
          // 마이페이지 버튼
          GestureDetector(
            onTap: () => _onTabTapped(context, 3),
            child: Icon(
              Icons.person_outline_rounded,
              size: 32,
              color: currentIndex == 3 ? Color(0xFFE17951) : Colors.black,
            ),
          ),
        ],
      ),
    );
  }
}
