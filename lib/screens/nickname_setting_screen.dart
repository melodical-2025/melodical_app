import 'package:flutter/material.dart';
import '../services/api_service.dart';

class NicknameSettingScreen extends StatefulWidget {
  const NicknameSettingScreen({super.key});

  @override
  State<NicknameSettingScreen> createState() => _NicknameSettingScreenState();
}

class _NicknameSettingScreenState extends State<NicknameSettingScreen> {
  final TextEditingController _nicknameController = TextEditingController();
  bool _isLoading = false;
  String _currentNickname = '';

  @override
  void initState() {
    super.initState();
    _loadCurrentNickname();
  }

  @override
  void dispose() {
    _nicknameController.dispose();
    super.dispose();
  }

  Future<void> _loadCurrentNickname() async {
    try {
      final userData = await ApiService.getCurrentUser();
      setState(() {
        _currentNickname = userData['nickname'] ?? userData['name'] ?? '';
        _nicknameController.text = _currentNickname;
      });
    } catch (e) {
      print('❌ Failed to load current nickname: $e');
    }
  }

  Future<void> _updateNickname() async {
    final newNickname = _nicknameController.text.trim();
    
    if (newNickname.isEmpty) {
      _showError('닉네임을 입력해주세요');
      return;
    }

    if (newNickname == _currentNickname) {
      _showError('현재 닉네임과 동일합니다');
      return;
    }

    setState(() {
      _isLoading = true;
    });

    try {
      await ApiService.updateNickname(newNickname);
      
      if (!mounted) return;
      
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('닉네임이 변경되었습니다'),
          backgroundColor: Color(0xFFE17951),
        ),
      );
      
      Navigator.pop(context, true); // true를 반환하여 새로고침 트리거
    } catch (e) {
      if (!mounted) return;
      _showError('닉네임 변경에 실패했습니다: $e');
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.red,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: const Text('닉네임 설정'),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black,
        elevation: 1,
      ),
      body: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '닉네임',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: Color(0xFFE17951),
              ),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _nicknameController,
              decoration: InputDecoration(
                hintText: '닉네임을 입력하세요',
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(8),
                  borderSide: const BorderSide(
                    color: Color(0xFFE17951),
                    width: 2,
                  ),
                ),
              ),
              maxLength: 20,
            ),
            const SizedBox(height: 20),
            SizedBox(
              width: double.infinity,
              height: 48,
              child: ElevatedButton(
                onPressed: _isLoading ? null : _updateNickname,
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFE17951),
                  disabledBackgroundColor: Colors.grey,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                ),
                child: _isLoading
                    ? const SizedBox(
                        width: 24,
                        height: 24,
                        child: CircularProgressIndicator(
                          color: Colors.white,
                          strokeWidth: 2,
                        ),
                      )
                    : const Text(
                        '변경',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                        ),
                      ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
