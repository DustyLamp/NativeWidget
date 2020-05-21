package net.realapps.nativewidget_example;

import java.util.HashMap;
import java.util.Map;

public class WordButton {
    String mWord;
    boolean mActive = false;

    WordButton(String word, boolean active){
        mWord = word;
        this.mActive = active;
    }

    WordButton(Map<String, Object> wordButtonMap){
        mWord = (String) wordButtonMap.get("word");
        mActive = (boolean) wordButtonMap.get("active");
    }

    void toggleActive(){
        mActive = !mActive;
    }

    public Map<String, Object> getMap(){
        Map<String, Object> wordMap = new HashMap<>();

        wordMap.put("word", mWord);
        wordMap.put("active", mActive);

        return wordMap;
    }
}
