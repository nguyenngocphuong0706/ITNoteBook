package com.it_notebook_app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

/**
 * Tiện ích quản lý ngôn ngữ ứng dụng (Localization i18n).
 * Hỗ trợ chuyển đổi mượt mà giữa Tiếng Việt ("vi") và English ("en").
 */
public class LocaleHelper {

    public static final String PREF_NAME = "app_settings";
    public static final String KEY_LANGUAGE = "app_language";

    public static final String LANG_VI = "vi";
    public static final String LANG_EN = "en";

    /**
     * Áp dụng ngôn ngữ đã lưu khi khởi động App.
     */
    public static void applySavedLocale(Context context) {
        String lang = getSavedLanguage(context);
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang));
    }

    /**
     * Đổi ngôn ngữ và lưu vào SharedPreferences.
     */
    public static void setLocale(Context context, String languageCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode));
    }

    /**
     * Lấy mã ngôn ngữ hiện tại ("vi" hoặc "en").
     */
    public static String getSavedLanguage(Context context) {
        LocaleListCompat current = AppCompatDelegate.getApplicationLocales();
        if (!current.isEmpty()) {
            String tag = current.get(0).getLanguage();
            if (LANG_EN.equalsIgnoreCase(tag)) return LANG_EN;
            if (LANG_VI.equalsIgnoreCase(tag)) return LANG_VI;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, LANG_VI);
    }

    /**
     * Trả về tên hiển thị của ngôn ngữ hiện tại.
     */
    public static String getCurrentLanguageDisplayName(Context context) {
        String lang = getSavedLanguage(context);
        if (LANG_EN.equalsIgnoreCase(lang)) {
            return "English";
        }
        return "Tiếng Việt";
    }
}
