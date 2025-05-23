import 'package:flutter/material.dart';

class Searchbar extends StatelessWidget {
  final TextEditingController controller;

  const Searchbar({super.key, required this.controller});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 339,
      height: 56,
      decoration: ShapeDecoration(
        color: Colors.white,
        shape: RoundedRectangleBorder(
          side: const BorderSide(width: 1, color: Color(0xFFEE7A4D)),
          borderRadius: BorderRadius.circular(30),
        ),
      ),
      child: Stack(
        alignment: Alignment.center,
        children: [
          TextField(
            controller: controller,
            textAlign: TextAlign.center,
            style: TextStyle(
              fontSize: 14,
              color: Colors.black.withOpacity(0.5),
              fontWeight: FontWeight.w100,
            ),
            decoration: const InputDecoration(
              border: InputBorder.none,
              hintText: '뮤지컬 제목을 입력하세요',
              hintStyle: TextStyle(
                color: Color(0xFF6F5858),
                fontSize: 12,
                fontWeight: FontWeight.w100,
              ),
              contentPadding: EdgeInsets.symmetric(horizontal: 0),
            ),
          ),
          const Positioned(
            left: 16,
            child: Icon(Icons.search, color: Color(0xFFEE7A4D), size: 32),
          ),
        ],
      ),
    );
  }
}
