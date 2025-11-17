import 'package:flutter/material.dart';

class CustomHeader extends StatelessWidget implements PreferredSizeWidget {
  final String title;
  final bool showBackButton;
  final bool showSettingsButton;
  final VoidCallback? onBackPressed;
  final VoidCallback? onSettingsPressed;

  const CustomHeader({
    super.key,
    required this.title,
    this.showBackButton = false,
    this.showSettingsButton = false,
    this.onBackPressed,
    this.onSettingsPressed,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      height: preferredSize.height,
      decoration: const BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Color(0xFFE17951),
            blurRadius: 4,
            offset: Offset(5, 0),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.only(bottom: 16, left: 16, right: 16),
        child: Align(
          alignment: Alignment.bottomCenter,
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              // ← 뒤로가기 버튼
              if (showBackButton)
                IconButton(
                  icon: const Icon(Icons.arrow_back, color: Color(0xFFE17951)),
                  onPressed: onBackPressed ?? () => Navigator.pop(context),
                )
              else
                const SizedBox(width: 48),

              // 제목
              Expanded(
                child: Text(
                  title,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    color: Color(0xFFE17951),
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),

              // ⚙️ 설정 버튼
              if (showSettingsButton)
                IconButton(
                  icon: const Icon(Icons.settings, color: Color(0xFFE17951)),
                  onPressed: onSettingsPressed,
                )
              else
                const SizedBox(width: 48),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Size get preferredSize => const Size.fromHeight(110);
}
