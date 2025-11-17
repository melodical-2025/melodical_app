// lib/screens/accountedit_screen.dart

import 'dart:convert';
import 'package:flutter/material.dart';
import '../services/api_service.dart';
import '../services/token_storage.dart';

class AccounteditScreen extends StatefulWidget {
  const AccounteditScreen({super.key});
  @override
  State<AccounteditScreen> createState() => _AccounteditScreenState();
}

class _AccounteditScreenState extends State<AccounteditScreen> {
  final _nickCtrl    = TextEditingController();
  final _currentCtrl = TextEditingController();
  final _newCtrl     = TextEditingController();
  bool _isLoading = false;

  static const bgColor   = Colors.white;
  static const primary   = Color(0xFFFFAD75);
  static const textColor = Color(0xFFE17951);
  static const secondary = Color(0xFFFFD9A3);

  @override
  void dispose() {
    _nickCtrl.dispose();
    _currentCtrl.dispose();
    _newCtrl.dispose();
    super.dispose();
  }

  Future<void> _saveChanges() async {
    final nick = _nickCtrl.text.trim();
    final curr = _currentCtrl.text.trim();
    final ne   = _newCtrl.text.trim();

    if (nick.isEmpty) {
      _showSnack('닉네임을 입력해주세요');
      return;
    }

    setState(() => _isLoading = true);

    // 1) 닉네임 업데이트
    final resp1 = await ApiService.post('/api/users/update-nickname', {
      'nickname': nick,
    });
    if (resp1.statusCode != 200) {
      _showSnack('닉네임 변경 실패');
      setState(() => _isLoading = false);
      return;
    }

    // 2) 비밀번호 변경 (선택)
    if (curr.isNotEmpty && ne.isNotEmpty) {
      final resp2 = await ApiService.post('/api/users/change-password', {
        'currentPassword': curr,
        'newPassword': ne,
      });
      if (resp2.statusCode != 200) {
        _showSnack('비밀번호 변경 실패');
        setState(() => _isLoading = false);
        return;
      }
    }

    _showSnack('정보가 수정되었습니다');
    setState(() => _isLoading = false);
    if (!mounted) return;
    Navigator.pop(context);
  }

  void _showSnack(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  InputDecoration _decor(String label,{bool readOnly=false}) => InputDecoration(
    labelText: label,
    filled: true,
    fillColor: readOnly? Colors.grey[100] : Colors.white,
    labelStyle: TextStyle(color: readOnly? Colors.grey : primary),
    enabledBorder: OutlineInputBorder(
      borderSide: BorderSide(color: readOnly? Colors.grey : primary),
      borderRadius: BorderRadius.circular(8),
    ),
    focusedBorder: OutlineInputBorder(
      borderSide: BorderSide(color: primary, width: 2),
      borderRadius: BorderRadius.circular(8),
    ),
  );

  @override
  Widget build(BuildContext ctx) => Scaffold(
    backgroundColor: bgColor,

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
                      '회원정보 수정',
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
                      if (Navigator.of(context).canPop()) {
                        Navigator.of(context).pop();
                      } else {
                        // 스택이 없으면 계정설정 화면으로
                        Navigator.pushReplacementNamed(context, '/account');
                      }
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
      padding: const EdgeInsets.symmetric(horizontal:24, vertical:20),
      child: Column(
        children:[
          TextField(
            controller: _nickCtrl,
            decoration: _decor('닉네임'),
            style: const TextStyle(color: textColor),
          ),
          const SizedBox(height:12),
          TextField(
            controller: _currentCtrl,
            decoration: _decor('현재 비밀번호'),
            obscureText: true,
            style: const TextStyle(color: textColor),
          ),
          const SizedBox(height:12),
          TextField(
            controller: _newCtrl,
            decoration: _decor('새 비밀번호 (선택)'),
            obscureText: true,
            style: const TextStyle(color: textColor),
          ),
          const SizedBox(height:30),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: _isLoading ? null : _saveChanges,
              style: ElevatedButton.styleFrom(
                backgroundColor: primary,
                padding: const EdgeInsets.symmetric(vertical:14),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
              ),
              child: const Text('저장', style: TextStyle(fontSize:16)),
            ),
          ),
        ],
      ),
    ),
  );
}
