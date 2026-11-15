package com.suzo.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.suzo.launcher.themes.ThemeEngine;

/**
 * SUZO in-launcher quick panel.
 * Android protects direct control of radios and system settings, so each tile
 * opens the corresponding system page rather than silently changing settings.
 */
public final class QuickPanel {
    private QuickPanel() {}

    private static int dp(Activity a, float value) {
        return (int)(value * a.getResources().getDisplayMetrics().density + .5f);
    }

    private static GradientDrawable panel(Activity a, int color, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(a, 20));
        d.setStroke(dp(a, 1), stroke);
        return d;
    }

    private static TextView text(Activity a, String value, int size, int color) {
        TextView t = new TextView(a);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    public static void show(Activity activity) {
        ThemeEngine.Theme theme = ThemeEngine.getTheme(activity);
        boolean light = theme == ThemeEngine.Theme.CLASSIC;
        int fg = ThemeEngine.foreground(theme);
        int muted = ThemeEngine.secondary(theme);
        int surface = light ? 0xFFF9F9FC : theme == ThemeEngine.Theme.DARK ? 0xFF25252C : 0xFF26304D;
        int stroke = light ? 0xFFE0E0E8 : 0x55FFFFFF;

        LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(activity, 20), dp(activity, 18), dp(activity, 20), dp(activity, 18));
        page.setBackgroundColor(ThemeEngine.background(theme));

        TextView title = text(activity, "SUZO CONTROL CENTER", 18, fg);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(title);

        TextView subtitle = text(activity, "Quick access to your phone settings", 12, muted);
        subtitle.setGravity(Gravity.CENTER_VERTICAL);
        subtitle.setPadding(0, dp(activity, 5), 0, dp(activity, 18));
        page.addView(subtitle);

        Intent battery = activity.registerReceiver(null,
            new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        String batteryText = "Battery information unavailable";
        if (battery != null) {
            int level = battery.getIntExtra("level", -1);
            int scale = battery.getIntExtra("scale", 100);
            int percent = (level >= 0 && scale > 0) ? Math.round(level * 100f / scale) : -1;
            if (percent >= 0) batteryText = "Battery  •  " + percent + "%";
        }
        TextView status = text(activity, batteryText, 15, fg);
        status.setBackground(panel(activity, surface, stroke));
        status.setPadding(dp(activity, 12), dp(activity, 16), dp(activity, 12), dp(activity, 16));
        page.addView(status);

        String[][] tiles = {
            {"Wi-Fi & Internet", Settings.ACTION_WIFI_SETTINGS},
            {"Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS},
            {"Display", Settings.ACTION_DISPLAY_SETTINGS},
            {"Sound", Settings.ACTION_SOUND_SETTINGS},
            {"Battery", Settings.ACTION_BATTERY_SAVER_SETTINGS},
            {"All Settings", Settings.ACTION_SETTINGS}
        };

        LinearLayout grid = new LinearLayout(activity);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(0, dp(activity, 14), 0, 0);
        for (int i = 0; i < tiles.length; i += 2) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int j = i; j < Math.min(i + 2, tiles.length); j++) {
                final String label = tiles[j][0];
                final String action = tiles[j][1];
                TextView tile = text(activity, label, 14, fg);
                tile.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                tile.setBackground(panel(activity, surface, stroke));
                tile.setPadding(dp(activity, 8), dp(activity, 18), dp(activity, 8), dp(activity, 18));
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(activity, 88), 1);
                p.setMargins(0, 0, dp(activity, 8), dp(activity, 8));
                row.addView(tile, p);
                tile.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(action);
                        if (intent.resolveActivity(activity.getPackageManager()) != null) {
                            activity.startActivity(intent);
                        } else {
                            activity.startActivity(new Intent(Settings.ACTION_SETTINGS));
                        }
                    } catch (Exception e) {
                        Toast.makeText(activity, "This setting is not available on this device", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            grid.addView(row);
        }
        page.addView(grid);

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(page);
        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setView(scroll)
            .setNegativeButton("CLOSE", null)
            .create();
        dialog.show();
    }
}
