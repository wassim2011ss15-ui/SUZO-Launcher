package com.suzo.launcher.themes;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

public final class ThemeEngine {

    public enum Theme {
        DARK, GLASS, CLASSIC
    }

    public static Theme getTheme(Activity activity) {
        SharedPreferences prefs =
            activity.getSharedPreferences("suzo_settings", 0);

        try {
            return Theme.valueOf(
                prefs.getString("theme", "GLASS")
            );
        } catch (Exception e) {
            return Theme.GLASS;
        }
    }

    public static void setTheme(Activity activity, Theme theme) {
        activity.getSharedPreferences("suzo_settings", 0)
            .edit()
            .putString("theme", theme.name())
            .apply();
    }

    public static int background(Theme theme) {
        switch (theme) {
            case DARK: return 0xFF101014;
            case CLASSIC: return 0xFFF2F2F7;
            default: return 0xFF252B49;
        }
    }

    public static int foreground(Theme theme) {
        return theme == Theme.CLASSIC
            ? Color.BLACK : Color.WHITE;
    }

    public static int secondary(Theme theme) {
        return theme == Theme.CLASSIC
            ? 0xFF777777 : 0xCCFFFFFF;
    }

    public static GradientDrawable panel(Theme theme) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(32);

        switch (theme) {
            case DARK:
                d.setColor(0xFF25252C);
                d.setStroke(1, 0xFF44444C);
                break;
            case CLASSIC:
                d.setColor(0xFFF9F9FC);
                d.setStroke(1, 0xFFE0E0E8);
                break;
            default:
                d.setColor(0x553FFFFF);
                d.setStroke(1, 0x66FFFFFF);
                break;
        }
        return d;
    }

    private ThemeEngine() {}
}
