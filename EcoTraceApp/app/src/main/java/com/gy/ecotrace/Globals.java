package com.gy.ecotrace;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;

public class Globals extends Application {
    private static Globals instance;
    private final HashMap<String, Integer> variablesMapInt;
    private final HashMap<String, String> variablesMapStr;
    private final HashMap<String, Boolean> variablesMapBool;

    public Globals() {
        variablesMapInt = new HashMap<>();
        variablesMapStr = new HashMap<>();
        variablesMapBool = new HashMap<>();
        setDefaultValues();
    }
    public static synchronized Globals getInstance() {
        if (instance == null) {
            instance = new Globals();
        }
        return instance;
    }

    private void setDefaultValues() {
        setBool("profile-activity-is_registering", false);
    }
    public int getInt(String key) {
        if (variablesMapInt.containsKey(key)) {
            return variablesMapInt.get(key);
        } else {
            return 0;
        }
    }
    public void setInt(String key, Integer value){
        variablesMapInt.put(key, value);
    }

    public boolean getBool(String key) {
        if (variablesMapBool.containsKey(key)) {
            return variablesMapBool.get(key);
        } else {
            return false;
        }
    }
    public void setBool(String key, Boolean value){
        variablesMapBool.put(key, value);
    }

    public String getString(String key) {
        if (variablesMapStr.containsKey(key)) {
            return variablesMapStr.get(key);
        } else {
            return "0";
        }
    }
    public void setString(String key, String value){
        variablesMapStr.put(key, value);
    }


    @Override
    public void onTerminate() {

        super.onTerminate();
        new Thread(() -> {
            new FirebaseMethods().clearAllCache();
        }).start();
    }
}
