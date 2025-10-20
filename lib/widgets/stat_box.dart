import 'package:flutter/material.dart';

class StatBox extends StatelessWidget {
  const StatBox({
    super.key,
    this.icon,
    this.assetPath,
    required this.label1,
    required this.label2,
    required this.count,
    this.textColor = Colors.black,
    this.backgroundColor = const Color(0xFFFFF2DB),
    this.width = 120,
    this.height = 65,
  }) : assert(icon != null || assetPath != null,
  'icon 또는 assetPath 중 하나는 제공해야 합니다.');

  final IconData? icon;
  final String? assetPath;
  final String label1;
  final String label2;
  final int count;
  final Color textColor;
  final Color backgroundColor;
  final double width;
  final double height;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: width,
      height: height,
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(10),
      ),
      child: Row(
        children: [
          if (assetPath != null)
            Image.asset(assetPath!, width: 25, height: 25, fit: BoxFit.contain)
          else
            Icon(icon, color: Colors.red, size: 25),
          const SizedBox(width: 8),
          Flexible(
            child: Text(
              '$label1\n$label2 $count개',
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.bold,
                color: textColor,
                height: 1.2,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
