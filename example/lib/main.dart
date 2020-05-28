import 'package:flutter/material.dart';
import 'package:nativewidget/nativewidget.dart';
import 'dart:async';

import 'package:random_words/random_words.dart';
import 'package:rxdart/rxdart.dart';
import 'package:shared_preferences/shared_preferences.dart';

//Native to Dart
const String nativeItemTapped = "NATIVE_ITEM_TAPPED";
const String refreshWords = "REFRESH_WORDS";
const String getWords = "GET_WORDS";

//Dart to Native
const String flutterItemTapped = "FLUTTER_ITEM_TAPPED";
const String receiveWords = "RECEIVE_WORDS";
const String newWord = "NEW_WORD";
const String pressedWords = "PRESSED_WORDS";

const String sharedPreferenceWordsPressedKey = "SHARED_PREF_WORDS_PRESSED";
const String sharedPreferenceWordListKey = "SHARED_PREF_WORDS_LIST";

void main() async {
  NativeWidget();
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  StreamSubscription wordTappedListener;
  StreamSubscription wordsRefreshedListener;

  static PublishSubject<bool> wordTapped = PublishSubject<bool>();
  static PublishSubject<bool> wordsRefreshed = PublishSubject<bool>();

  static void handleItemTapped(dynamic args) async {
    print("Handle Item Tapped in Widget");
    String word = args as String;

    SharedPreferences preferences = await SharedPreferences.getInstance();

    List<String> pressedWords =
        preferences.getStringList(sharedPreferenceWordsPressedKey) ?? [];

    int wordIdx = pressedWords.indexOf(word);

    if (wordIdx < 0) {
      pressedWords.add(word);
    } else {
      pressedWords.removeAt(wordIdx);
    }

    await preferences.setStringList(
        sharedPreferenceWordsPressedKey, pressedWords);

    if (wordTapped == null || wordTapped.isClosed) {
      wordTapped = PublishSubject<bool>();
    }

    wordTapped.sink.add(true);
  }

  _handleWordTappedInFlutter(String word) async {
    print("Handle word tapped in Flutter");
    SharedPreferences preferences = await SharedPreferences.getInstance();

    setState(() {
      activeWords = preferences.getStringList(sharedPreferenceWordsPressedKey);
      int wordIdx = activeWords.indexOf(word);

      if (wordIdx >= 0) {
        activeWords.removeAt(wordIdx);
      } else {
        activeWords.add(word);
      }
    });

    await preferences.setStringList(
        sharedPreferenceWordsPressedKey, activeWords);
  
    _sendWordTapped(word);
  }

  void _sendWordTapped(String word) {
    print("Sending word tapped");
    NativeWidget.sendData(flutterItemTapped, word);
  }

  void _addWordToWordList() async {
    print("Adding new word to list");
    String newWord =
        generateNoun(maxSyllables: 2).take(1).first.asCapitalized.toString();

    SharedPreferences sharedPreferences = await SharedPreferences.getInstance();

    wordList =
        sharedPreferences.getStringList(sharedPreferenceWordListKey) ?? [];
    wordList.add(newWord);

    await sharedPreferences.setStringList(
        sharedPreferenceWordListKey, wordList);

    _sendNewWord(newWord);
  }

  _sendNewWord(String word) {
    print("Sending new word");
    NativeWidget.sendData(newWord, word);
  }

  static void handleGetWords(dynamic args) async {
    print("Handling Get Words Request");
    sendAllWordLists();
  }

  static void sendAllWordLists() async {
    print("Sending all words lists");

    SharedPreferences sharedPreferences = await SharedPreferences.getInstance();

    List<String> wordsToSend =
        sharedPreferences.getStringList(sharedPreferenceWordListKey);
    NativeWidget.sendData(receiveWords, wordsToSend);

    List<String> pressedWordsToSend =
        sharedPreferences.getStringList(sharedPreferenceWordsPressedKey);
    NativeWidget.sendData(pressedWords, pressedWordsToSend);
  }

  static void handleRefreshWords(dynamic args) async {
    print("Handling Refresh Words Request");
    SharedPreferences sharedPreferences = await SharedPreferences.getInstance();

    sharedPreferences.remove(sharedPreferenceWordListKey);
    sharedPreferences.remove(sharedPreferenceWordsPressedKey);

    List<String> wordList = generateNoun(maxSyllables: 2)
        .take(8)
        .map((WordNoun wordNoun) => wordNoun.asCapitalized.toString())
        .toList();

    await sharedPreferences.setStringList(
        sharedPreferenceWordListKey, wordList);

    await sharedPreferences.setStringList(sharedPreferenceWordsPressedKey, []);

    sendAllWordLists();

    wordsRefreshed.sink.add(true);
  }

  List<String> wordList = [];

  List<String> activeWords = [];

  @override
  void dispose() {
    print("Disposing...");
    wordTappedListener?.cancel();
    wordTapped?.close();
    wordsRefreshedListener?.cancel();
    wordsRefreshed?.close();
    super.dispose();
  }

  @override
  void initState() {
    print("Initializing State...");
    super.initState();

    initPlatformState();

    if (wordsRefreshedListener == null) {
      wordsRefreshedListener = wordsRefreshed.listen((bool refreshed) async {
        SharedPreferences preferences = await SharedPreferences.getInstance();

        setState(() {
          wordList = preferences.getStringList(sharedPreferenceWordListKey);
          activeWords =
              preferences.getStringList(sharedPreferenceWordsPressedKey);
        });
      });
    }

    if (wordTappedListener == null) {
      wordTappedListener = wordTapped.listen((bool tapped) async {
        SharedPreferences preferences = await SharedPreferences.getInstance();

        setState(() {
          activeWords =
              preferences.getStringList(sharedPreferenceWordsPressedKey);
        });
      });
    }

    wordTapped.sink.add(true);
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    print("Initializing Platform State...");
    // String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    // try {
    //   platformVersion = await NativeWidget.platformVersion;
    // } on PlatformException {
    //   platformVersion = 'Failed to get platform version.';
    // }

    print("Registering Callbacks");
    NativeWidget.registerActionCallbacks({
      getWords: _MyAppState.handleGetWords,
      refreshWords: _MyAppState.handleRefreshWords,
      nativeItemTapped: _MyAppState.handleItemTapped,
    });

    print("Initializing Native Widget");
    await NativeWidget.initialize();

    print("Preparing word lists");
    SharedPreferences preferences = await SharedPreferences.getInstance();
    activeWords = preferences.getStringList(sharedPreferenceWordsPressedKey);
    wordList = preferences.getStringList(sharedPreferenceWordListKey);

    if (activeWords == null) {
      activeWords = [];
      await preferences.setStringList(
          sharedPreferenceWordsPressedKey, activeWords);
    }

    if (wordList == null) {
      wordList = [];
      await preferences.setStringList(sharedPreferenceWordListKey, wordList);
    }

    // SharedPreferences preferences = await SharedPreferences.getInstance();
    // activeWords = preferences.getStringList(sharedPreferenceWordsKey);

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    // setState(() {
    //   _platformVersion = platformVersion;
    // });
  }

  @override
  Widget build(BuildContext context) {
    print("Building...");
    List<Widget> gridWidgets = wordList
        ?.map(
          (String word) => RaisedButton(
            child:
                Text(word, style: TextStyle(fontSize: 16, color: Colors.white)),
            color: activeWords?.contains(word) == true
                ? Colors.lightBlue
                : Colors.grey,
            onPressed: () {
              // setState(() {
              _handleWordTappedInFlutter(word);
              // int wordIdx = activeWords.indexOf(word);
              // if (wordIdx >= 0) {
              //   activeWords.removeAt(wordIdx);
              // } else {
              //   activeWords.add(word);
              // }
              // print("$word Pressed");

              // _sendWordTapped(word);
              // });
            },
          ),
        )
        ?.toList();

    if (gridWidgets == null || gridWidgets.isEmpty) {
      gridWidgets = [Container(child: Text("Press the FAB"))];
    }

    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Add AppWidget and Press Stuff'),
        ),
        body: GridView.count(
          padding: EdgeInsets.all(10),
          crossAxisSpacing: 10,
          mainAxisSpacing: 10,
          crossAxisCount: 4,
          children: gridWidgets,
        ),
        floatingActionButton: FloatingActionButton(
          child: Icon(Icons.add),
          onPressed: () {
            setState(() {
              _addWordToWordList();
            });
          },
        ),
      ),
    );
  }
}
