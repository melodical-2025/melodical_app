import 'package:flutter/material.dart';

class Searchbar extends StatefulWidget {
  final TextEditingController controller;

  const Searchbar({super.key, required this.controller});

  @override
  State<Searchbar> createState() => _SearchbarState();
}

class _SearchbarState extends State<Searchbar> {
  late FocusNode _focusNode;

  @override
  void initState() {
    super.initState();
    _focusNode = FocusNode();
    _focusNode.addListener(() {
      setState(() {}); // 포커스 상태 바뀌면 rebuild
    });
  }

  @override
  void dispose() {
    _focusNode.dispose();
    super.dispose();
  }

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
            controller: widget.controller,
            focusNode: _focusNode, // <- 추가
            textAlign: TextAlign.center,
            style: TextStyle(
              fontSize: 14,
              color: Colors.black.withOpacity(0.5),
              fontWeight: FontWeight.w100,
            ),
            decoration: InputDecoration(
              border: InputBorder.none,
              hintText: _focusNode.hasFocus ? '' : '뮤지컬 제목을 입력하세요', // <- 클릭하면 사라짐
              hintStyle: const TextStyle(
                color: Color(0xFF6F5858),
                fontSize: 12,
                fontWeight: FontWeight.w100,
              ),
              contentPadding: const EdgeInsets.symmetric(horizontal: 0),
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
