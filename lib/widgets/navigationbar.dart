import 'package:flutter/material.dart';

class BottomNavBar extends StatelessWidget {
  final int currentIndex;
  final Function(int) onTabTapped;

  const BottomNavBar({
    Key? key,
    required this.currentIndex,
    required this.onTabTapped,
  }) : super(key: key);

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
            onTap: () => onTabTapped(0),
            child: Icon(
              Icons.home,
              color: currentIndex == 0 ? Color(0xFFE17951) : Colors.black,
            ),
          ),
          // 검색 버튼
          GestureDetector(
            onTap: () => onTabTapped(1),
            child: Icon(
              Icons.search,
              color: currentIndex == 1 ? Color(0xFFE17951) : Colors.black,
            ),
          ),
          // 평가 버튼
          GestureDetector(
            onTap: () => onTabTapped(2),
            child: Icon(
              Icons.star,
              color: currentIndex == 2 ? Color(0xFFE17951) : Colors.black,
            ),
          ),
          // 마이페이지 버튼
          GestureDetector(
            onTap: () => onTabTapped(3),
            child: Icon(
              Icons.account_circle,
              color: currentIndex == 3 ? Color(0xFFE17951) : Colors.black,
            ),
          ),
        ],
      ),
    );
  }
}
