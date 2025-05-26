import 'package:flutter/material.dart';

class UserProvider extends ChangeNotifier {
  String _nickname = '';
  String _email = '';
  List<String> _selectedMusicals = [];
  List<String> _selectedMusics = [];

  // ⭐ NEW: 별점 상태 저장용 Map
  final Map<String, int> _musicalRatings = {};
  final Map<String, int> _musicRatings = {};

  String get nickname => _nickname;
  String get email => _email;
  List<String> get selectedMusicals => _selectedMusicals;
  List<String> get selectedMusics => _selectedMusics;

  // ⭐ NEW: 별점 getter
  Map<String, int> get musicalRatings => _musicalRatings;
  Map<String, int> get musicRatings => _musicRatings;

  void setUserInfo({
    required String nickname,
    required String email,
    List<String>? musicals,
    List<String>? musics,
  }) {
    _nickname = nickname;
    _email = email;
    if (musicals != null) _selectedMusicals = musicals;
    if (musics != null) _selectedMusics = musics;
    notifyListeners();
  }

  void updateUser({required String nickname, required String email}) {
    _nickname = nickname;
    _email = email;
    notifyListeners();
  }

  void updateNickname(String newNickname) {
    _nickname = newNickname;
    notifyListeners();
  }

  void updateEmail(String newEmail) {
    _email = newEmail;
    notifyListeners();
  }

  void updateMusicals(List<String> musicals) {
    _selectedMusicals = musicals;
    notifyListeners();
  }

  void updateMusics(List<String> musics) {
    _selectedMusics = musics;
    notifyListeners();
  }

  // ⭐ NEW: 별점 평가 저장 함수
  void rateMusical(String title, int rating) {
    _musicalRatings[title] = rating;
    notifyListeners();
  }

  void rateMusic(String title, int rating) {
    _musicRatings[title] = rating;
    notifyListeners();
  }
}