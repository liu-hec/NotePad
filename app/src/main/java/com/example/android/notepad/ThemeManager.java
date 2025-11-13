/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.notepad;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;

/**
 * 主题管理类，用于统一管理应用的主题
 */
public class ThemeManager {
    // 主题常量
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    
    /**
     * 应用主题到Activity
     * @param activity 要应用主题的Activity
     */
    public static void applyTheme(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        int theme = prefs.getInt("theme", THEME_LIGHT); // 默认使用浅色主题

        switch (theme) {
            case THEME_DARK:
                activity.setTheme(R.style.NotePadTheme_Dark);
                break;
            case THEME_LIGHT:
            default:
                activity.setTheme(R.style.NotePadTheme_Light);
                break;
        }
    }
    
    /**
     * 设置当前主题
     * @param activity Activity上下文
     * @param theme 要设置的主题
     */
    public static void setTheme(Activity activity, int theme) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        prefs.edit().putInt("theme", theme).apply();
    }
    
    /**
     * 获取当前主题
     * @param activity Activity上下文
     * @return 当前主题
     */
    public static int getCurrentTheme(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return prefs.getInt("theme", THEME_LIGHT);
    }
}