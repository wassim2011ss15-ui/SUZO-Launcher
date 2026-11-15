package com.suzo.launcher;

import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import com.suzo.launcher.themes.ThemeEngine;

public class SettingsActivity extends Activity {

    LinearLayout root;
    SharedPreferences prefs;
    int white = Color.WHITE;
    int muted = 0xFFADB8D0;
    ThemeEngine.Theme activeTheme = ThemeEngine.Theme.GLASS;

    int dp(float n) {
        return (int)(n * getResources().getDisplayMetrics().density + .5f);
    }

    TextView label(String s, int size, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    GradientDrawable panel() {
        GradientDrawable d = new GradientDrawable();
        if (activeTheme == ThemeEngine.Theme.CLASSIC) {
            d.setColor(0xFFF9F9FC);
            d.setStroke(dp(1),0xFFE0E0E8);
        } else if (activeTheme == ThemeEngine.Theme.DARK) {
            d.setColor(0xFF25252C);
            d.setStroke(dp(1),0xFF44444C);
        } else {
            d.setColor(0xFF202941);
            d.setStroke(dp(1),0x44FFFFFF);
        }
        d.setCornerRadius(dp(22));
        return d;
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("suzo_settings",0);
        activeTheme = ThemeEngine.getTheme(this);
        white = ThemeEngine.foreground(activeTheme);
        muted = ThemeEngine.secondary(activeTheme);

        int pageBackground = ThemeEngine.background(activeTheme);
        getWindow().setStatusBarColor(pageBackground);
        getWindow().setNavigationBarColor(pageBackground);
        getWindow().getDecorView().setSystemUiVisibility(
            activeTheme == ThemeEngine.Theme.CLASSIC ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20),dp(20),dp(20),dp(20));
        root.setBackgroundColor(pageBackground);

        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);

        TextView title = label("SUZO Settings",29,white);
        title.setTypeface(null,Typeface.BOLD);
        page.addView(title);

        TextView subtitle = label("Make your space yours",13,muted);
        subtitle.setPadding(0,dp(5),0,dp(25));
        page.addView(subtitle);

        heading(page,"APPEARANCE");
        option(page,"Interface Theme","Choose your visual style",() -> chooseTheme());

        heading(page,"HOME SCREEN");
        slider(page,"Icon Size","icon_size",35,65,43," px");
        option(page,"Icon Shape","Choose icon corners",() -> chooseIcons());
        option(page,"Icon Color","Original • Dark • White monochrome",() -> chooseIconAppearance());
        option(page,"Icon Pack","Dark • Glass • iOS 27",() -> chooseIconPack());
        slider(page,"Grid Columns","columns",3,5,4,"");
        toggle(page,"Show App Names","show_labels",true);
        slider(page,"Dock Icons","dock_count",2,5,4,"");

        heading(page,"WALLPAPER");
        option(page,"Choose Wallpaper","Use an image from your phone",() -> chooseWallpaper());

        heading(page,"CLOCK");
        slider(page,"Clock Size","clock_size",42,82,69,"");
        option(page,"Clock Format","Choose 12 or 24 hour format",() -> chooseClock());
        slider(page,"App Name Size","label_size",9,16,10,"");
        toggle(page,"Show Date","show_date",true);

        heading(page,"KEYBOARD & EMOJI");
        option(page,"Enable SUZO Keyboard","Open Android keyboard settings",() -> {
            try { startActivity(new Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS)); }
            catch(Exception e) { startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS)); }
        });
        option(page,"Choose Active Keyboard","Select SUZO Keyboard after enabling it",() -> {
            android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showInputMethodPicker();
        });

        heading(page,"ABOUT");
        TextView about = label("SUZO Launcher  •  Version 1.5",12,muted);
        about.setPadding(dp(5),dp(15),0,dp(20));
        page.addView(about);

        scroll.addView(page);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        TextView done = label("DONE  ✓",15,white);
        done.setGravity(Gravity.CENTER);
        done.setTypeface(null,Typeface.BOLD);
        done.setBackground(panel());
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1,dp(55));
        dp.setMargins(0,dp(12),0,0);
        root.addView(done,dp);
        done.setOnClickListener(v -> finish());

        setContentView(root);
    }

    void heading(LinearLayout parent,String value) {
        TextView t = label(value,11,activeTheme == ThemeEngine.Theme.CLASSIC ? 0xFF6654B5 : 0xFFB8A9FF);
        t.setTypeface(null,Typeface.BOLD);
        t.setPadding(dp(5),dp(22),0,dp(10));
        parent.addView(t);
    }

    void option(LinearLayout parent,String title,String desc,Runnable action) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16),dp(15),dp(16),dp(15));
        box.setBackground(panel());

        TextView a = label(title,15,white);
        TextView b = label(desc,11,muted);
        b.setPadding(0,dp(5),0,0);
        box.addView(a);
        box.addView(b);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0,0,0,dp(10));
        parent.addView(box,p);
        box.setOnClickListener(v -> action.run());
    }

    void slider(LinearLayout parent,String title,String key,
                int min,int max,int def,String suffix) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(15),dp(13),dp(15),dp(12));
        box.setBackground(panel());

        TextView name = label(title,14,white);
        TextView value = label("",12,activeTheme == ThemeEngine.Theme.CLASSIC ? 0xFF6654B5 : 0xFFB8A9FF);
        SeekBar seek = new SeekBar(this);
        seek.setMax(max-min);
        seek.setProgress(prefs.getInt(key,def)-min);

        value.setText((seek.getProgress()+min)+suffix);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s,int progress,boolean user) {
                int v = progress+min;
                value.setText(v+suffix);
                if (user) prefs.edit().putInt(key,v).apply();
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });

        box.addView(name);
        box.addView(value);
        box.addView(seek);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0,0,0,dp(10));
        parent.addView(box,p);
    }

    void toggle(LinearLayout parent,String title,String key,boolean def) {
        LinearLayout box = new LinearLayout(this);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(15),dp(12),dp(12),dp(12));
        box.setBackground(panel());

        TextView name = label(title,14,white);
        box.addView(name,new LinearLayout.LayoutParams(0,dp(45),1));

        Switch sw = new Switch(this);
        sw.setChecked(prefs.getBoolean(key,def));
        box.addView(sw);
        sw.setOnCheckedChangeListener((button,checked) ->
            prefs.edit().putBoolean(key,checked).apply());

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0,0,0,dp(10));
        parent.addView(box,p);
    }



    void chooseWallpaper() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent,501);
    }

    void chooseClock() {
        String[] choices = {"24-hour","12-hour"};
        new AlertDialog.Builder(this)
            .setTitle("Clock Format")
            .setSingleChoiceItems(choices,
                prefs.getBoolean("clock_12",false) ? 1 : 0,
                (dialog,which) -> {
                    prefs.edit().putBoolean("clock_12",which==1).apply();
                    dialog.dismiss();
                }).show();
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode==501 && resultCode==RESULT_OK && data!=null) {
            Uri uri = data.getData();
            if (uri!=null) {
                try {
                    getContentResolver().takePersistableUriPermission(
                        uri,data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    prefs.edit().putString("wallpaper_uri",uri.toString()).apply();
                    Toast.makeText(this,"Wallpaper saved",Toast.LENGTH_SHORT).show();
                } catch(Exception e) {
                    Toast.makeText(this,"Could not save wallpaper",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    void chooseIconAppearance() {
        String[] names = {"Original App Colors","Dark Monochrome","White Monochrome"};
        int current = prefs.getInt("icon_appearance",0);
        new AlertDialog.Builder(this)
            .setTitle("Icon Color")
            .setSingleChoiceItems(names,current,(dialog,which) -> {
                prefs.edit().putInt("icon_appearance",which).apply();
                dialog.dismiss();
                Toast.makeText(this,"Icon color saved",Toast.LENGTH_SHORT).show();
            }).setNegativeButton("Cancel",null).show();
    }

    void chooseIconPack() {
        String[] names = {
            "System Icons",
            "iLiquid Dark",
            "iDark OS",
            "Liquid Glass",
            "iOS 27 Liquid Glass"
        };

        String[] packages = {
            "none",
            "com.eatos.iliquid.dark",
            "com.eatos.idarkos",
            "com.eatos.iliquidos.glass",
            "com.launcher.ios27.liquid.glass"
        };

        String current = prefs.getString("icon_pack","none");
        int selected = 0;
        for (int i=0; i<packages.length; i++) {
            if (packages[i].equals(current)) selected = i;
        }

        new AlertDialog.Builder(this)
            .setTitle("Choose Icon Pack")
            .setSingleChoiceItems(names,selected,(dialog,which) -> {
                if (which > 0) {
                    try {
                        getPackageManager().getPackageInfo(packages[which],0);
                    } catch (Exception notInstalled) {
                        Toast.makeText(this,"Install this icon pack first, then select it again",Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                prefs.edit().putString("icon_pack",packages[which]).apply();
                dialog.dismiss();
                Toast.makeText(this,"Icon pack saved",Toast.LENGTH_SHORT).show();
            }).setNegativeButton("Cancel",null).show();
    }

    void chooseIcons() {
        String[] names = {"Rounded Square","Circle","iOS Rounded"};
        new AlertDialog.Builder(this)
            .setTitle("Icon Shape")
            .setSingleChoiceItems(names,prefs.getInt("icon_style",2),
                (dialog,which) -> {
                    prefs.edit().putInt("icon_style",which).apply();
                    dialog.dismiss();
                    Toast.makeText(this,"Icon style saved",Toast.LENGTH_SHORT).show();
                }).show();
    }

    void chooseTheme() {
        String[] names = {
            "Liquid Dark",
            "Liquid Glass",
            "Classic"
        };
        ThemeEngine.Theme[] themes = {
            ThemeEngine.Theme.DARK,
            ThemeEngine.Theme.GLASS,
            ThemeEngine.Theme.CLASSIC
        };

        new AlertDialog.Builder(this)
            .setTitle("Choose Theme")
            .setItems(names,(dialog,which) -> {
                ThemeEngine.setTheme(this,themes[which]);
                Toast.makeText(this,"Theme saved",Toast.LENGTH_SHORT).show();
                recreate();
            }).show();
    }
}
