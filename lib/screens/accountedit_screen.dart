import 'package:flutter/material.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

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
  String _originalNick = '';

  static const primaryLine   = Color(0xFFFFE5B6);
  static const primaryShadow = Color(0xFFE17951);
  static const bgColor   = Colors.white;
  static const primary   = Color(0xFFFFAD75);
  static const textColor = Color(0xFFE17951);

  @override
  void initState() {
    super.initState();
    _loadProfile();
  }

  Future<void> _loadProfile() async {
    final user = Supabase.instance.client.auth.currentUser;
    if (user == null) {
      _showSnack('로그인이 필요합니다');
      if (!mounted) return;
      Navigator.pushReplacementNamed(context, '/login');
      return;
    }
    final nick = user.userMetadata?['nickname']?.toString() ?? '';
    _nickCtrl.text = nick;
    _originalNick = nick;
    setState(() {});
  }

  @override
  void dispose() {
    _nickCtrl.dispose();
    _currentCtrl.dispose();
    _newCtrl.dispose();
    super.dispose();
  }

  bool get _isChanged =>
      _nickCtrl.text.trim() != _originalNick.trim() ||
          _newCtrl.text.trim().isNotEmpty;

  Future<void> _saveChanges() async {
    final nick = _nickCtrl.text.trim();
    final curr = _currentCtrl.text.trim();
    final ne   = _newCtrl.text.trim();

    final auth = Supabase.instance.client.auth;
    final user = auth.currentUser;

    if (user == null) {
      _showSnack('로그인이 필요합니다');
      return;
    }
    if (!_isChanged) {
      _showSnack('변경된 내용이 없습니다');
      return;
    }
    if (curr.isEmpty) {
      _showSnack('현재 비밀번호를 입력해주세요');
      return;
    }
    if (ne.isNotEmpty && ne.length < 8) {
      _showSnack('새 비밀번호는 8자 이상이어야 합니다');
      return;
    }

    setState(() => _isLoading = true);
    try {
      final email = user.email;
      if (email == null) throw AuthException('이메일 정보를 찾을 수 없습니다');

      // 🔒 비밀번호 재검증
      final reauth = await auth.signInWithPassword(email: email, password: curr);
      if (reauth.session == null) {
        _showSnack('현재 비밀번호가 올바르지 않습니다');
        setState(() => _isLoading = false);
        return;
      }

      // ✅ 닉네임 변경
      if (nick != _originalNick) {
        await auth.updateUser(UserAttributes(data: {'nickname': nick}));
        await Supabase.instance.client
            .from('profiles')
            .upsert({'id': user.id, 'nickname': nick});
      }

      // ✅ 비밀번호 변경
      if (ne.isNotEmpty) {
        await auth.updateUser(UserAttributes(password: ne));
        await auth.signOut();
        if (!mounted) return;
        _showSnack('비밀번호가 변경되었습니다. 다시 로그인 해주세요.');
        Navigator.pushNamedAndRemoveUntil(context, '/login', (_) => false);
        return;
      }

      _originalNick = nick;
      _showSnack('정보가 수정되었습니다');
      if (!mounted) return;
      Navigator.pop(context);

    } on AuthException catch (e) {
      final msg = _translateError(e.message);
      _showSnack(msg);
    } catch (e) {
      _showSnack('오류가 발생했습니다: $e');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  /// 🪄 Supabase 오류 메시지 → 한글화 처리
  String _translateError(String? msg) {
    if (msg == null) return '알 수 없는 오류가 발생했습니다.';
    if (msg.contains('Invalid login credentials')) {
      return '현재 비밀번호가 올바르지 않습니다';
    } else if (msg.contains('Email not confirmed')) {
      return '이메일 인증이 완료되지 않았습니다.';
    } else if (msg.contains('JWT expired') || msg.contains('Session not found')) {
      return '로그인이 만료되었습니다. 다시 로그인해주세요.';
    } else if (msg.contains('New password should be different')) {
      return '새 비밀번호는 기존 비밀번호와 달라야 합니다.';
    } else {
      return '오류가 발생했습니다: $msg';
    }
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
  Widget build(BuildContext ctx) {
    final user = Supabase.instance.client.auth.currentUser;
    final email = user?.email ?? '';

    return Scaffold(
      backgroundColor: bgColor,
      appBar: PreferredSize(
        preferredSize: const Size.fromHeight(110),
        child: Container(
          height: 110,
          decoration: const BoxDecoration(
            color: Colors.white,
            boxShadow: [
              BoxShadow(
                color: primaryShadow,
                blurRadius: 4,
                offset: Offset(5, 0),
              ),
            ],
            border: Border(
              bottom: BorderSide(color: primaryLine, width: 1),
            ),
          ),
          child: SafeArea(
            bottom: false,
            child: Padding(
              padding: const EdgeInsets.only(bottom: 16, left: 8, right: 8),
              child: Stack(
                alignment: Alignment.bottomCenter,
                children: [
                  const Positioned(
                    bottom: 4,
                    left: 0,
                    right: 0,
                    child: Center(
                      child: Text(
                        '회원정보 수정',
                        style: TextStyle(
                          color: textColor,
                          fontWeight: FontWeight.bold,
                          fontSize: 24,
                          height: 1.1,
                        ),
                      ),
                    ),
                  ),
                  Positioned(
                    left: 0,
                    bottom: 0,
                    child: IconButton(
                      icon: const Icon(Icons.arrow_back_ios_new, color: textColor),
                      onPressed: () => Navigator.pop(context),
                      tooltip: '뒤로',
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
            if (email.isNotEmpty) ...[
              TextField(
                controller: TextEditingController(text: email),
                readOnly: true,
                decoration: _decor('이메일', readOnly: true),
                style: const TextStyle(color: Colors.grey),
              ),
              const SizedBox(height:12),
            ],
            TextField(
              controller: _nickCtrl,
              decoration: _decor('닉네임'),
              style: const TextStyle(color: textColor),
              onChanged: (_) => setState(() {}),
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
              onChanged: (_) => setState(() {}),
            ),
            const SizedBox(height:30),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: (!_isChanged || _isLoading)
                    ? null
                    : _saveChanges,
                style: ElevatedButton.styleFrom(
                  backgroundColor: primary,
                  disabledBackgroundColor: Colors.grey.shade300,
                  padding: const EdgeInsets.symmetric(vertical:14),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                  ),
                ),
                child: Text(
                  _isLoading ? '저장 중...' : '저장',
                  style: const TextStyle(fontSize:16),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
