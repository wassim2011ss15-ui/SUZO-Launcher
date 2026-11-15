package com.suzo.launcher;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import android.text.*;
import java.text.*;
import java.util.*;
import com.suzo.launcher.themes.ThemeEngine;
import org.xmlpull.v1.XmlPullParser;

public class MainActivity extends Activity {
    LinearLayout root, content, appsGrid;
    EditText searchInput;
    TextView clock, date;
    Handler handler = new Handler();
    PackageManager pm;
    ThemeEngine.Theme theme;
    int fg, muted;
    int iconSize, gridColumns, clockSize, dockCount;
    boolean showDate, showLabels, clock12;
    int labelSize;
    int iconStyle;
    String settingsSnapshot;
    ArrayList<ResolveInfo> apps = new ArrayList<>();

    int dp(float n) {
        return (int)(n * getResources().getDisplayMetrics().density + .5f);
    }

    GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    TextView text(String value, float size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    void loadTheme() {
        theme = ThemeEngine.getTheme(this);
        fg = ThemeEngine.foreground(theme);
        muted = ThemeEngine.secondary(theme);
        android.content.SharedPreferences p =
            getSharedPreferences("suzo_settings",0);
        iconSize = p.getInt("icon_size",43);
        gridColumns = p.getInt("columns",4);
        clockSize = p.getInt("clock_size",69);
        dockCount = p.getInt("dock_count",4);
        showDate = p.getBoolean("show_date",true);
        showLabels = p.getBoolean("show_labels",true);
        clock12 = p.getBoolean("clock_12",false);
        labelSize = p.getInt("label_size",10);
        iconStyle = p.getInt("icon_style",2);
    }

    Runnable ticker = new Runnable() {
        public void run() {
            if (clock != null) {
                Date d = new Date();
                clock.setText(new SimpleDateFormat(clock12 ? "hh:mm a" : "HH:mm", Locale.getDefault()).format(d));
                date.setText(new SimpleDateFormat("EEEE  •  dd MMMM", Locale.getDefault()).format(d));
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        pm = getPackageManager();
        loadTheme();
        settingsSnapshot = getSharedPreferences("suzo_settings",0)
            .getAll().toString();
        getWindow().setStatusBarColor(Color.rgb(12,14,24));
        getWindow().setNavigationBarColor(Color.rgb(12,14,24));
        getWindow().getDecorView().setSystemUiVisibility(0);
        createUI();
        loadApps();
        showApps("");
        handler.post(ticker);
    }

    void createUI() {
        root = vertical();
        root.setPadding(dp(19), dp(12), dp(19), dp(10));

        GradientDrawable background = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{0xFF090D19,0xFF171C35,0xFF24203E,0xFF0A1020}
        );
        root.setBackground(background);
        String wallpaper = getSharedPreferences("suzo_settings",0)
            .getString("wallpaper_uri",null);
        if (wallpaper != null) {
            try {
                android.net.Uri uri = android.net.Uri.parse(wallpaper);
                android.graphics.Bitmap bitmap =
                    android.graphics.BitmapFactory.decodeStream(
                        getContentResolver().openInputStream(uri));
                if (bitmap != null) {
                    android.graphics.drawable.BitmapDrawable image =
                        new android.graphics.drawable.BitmapDrawable(
                            getResources(),bitmap);
                    image.setGravity(android.view.Gravity.FILL);
                    root.setBackground(image);
                }
            } catch(Exception ignored) {}
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);

        content = vertical();
        topBar();
        hero();
        search();
        section();
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1,0,1));

        dock();
        setContentView(root);

        content.setAlpha(0f);
        content.setTranslationY(dp(15));
        content.animate().alpha(1f).translationY(0).setDuration(550).start();
    }

    void topBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout brand = vertical();
        TextView logo = text("SUZO",22,Color.WHITE);
        logo.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        TextView caption = text("DESIGNED FOR YOUR WORLD",9,0xFF9DA9C7);
        brand.addView(logo);
        brand.addView(caption);
        bar.addView(brand,new LinearLayout.LayoutParams(0,-2,1));

        TextView settings = text("⚙",23,Color.WHITE);
        settings.setGravity(Gravity.CENTER);
        settings.setBackground(shape(0x33FFFFFF,19,0x44FFFFFF));
        bar.addView(settings,new LinearLayout.LayoutParams(dp(48),dp(48)));
        settings.setOnClickListener(v ->
            startActivity(new Intent(this,SettingsActivity.class)));

        content.addView(bar);
    }

    void hero() {
        LinearLayout card = vertical();
        card.setPadding(dp(22),dp(22),dp(22),dp(20));

        GradientDrawable glass = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{0x443E64A4,0x222B386C,0x333B2D68}
        );
        glass.setCornerRadius(dp(30));
        glass.setStroke(dp(1),0x55FFFFFF);
        card.setBackground(glass);

        TextView eyebrow = text("GOOD TO SEE YOU",10,0xFFB9C7E9);
        eyebrow.setLetterSpacing(.15f);
        clock = text("--:--",clockSize,Color.WHITE);
        clock.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
        date = text("",13,0xFFD1D9EF);
        date.setVisibility(showDate ? View.VISIBLE : View.GONE);

        TextView line = text("✦  YOUR SPACE. YOUR MOMENT.",10,0xFFB9A8FF);
        line.setPadding(0,dp(17),0,0);

        card.addView(eyebrow);
        card.addView(clock);
        card.addView(date);
        card.addView(line);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0,dp(25),0,dp(22));
        content.addView(card,p);
    }

    void search() {
        LinearLayout box = new LinearLayout(this);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(15),0,dp(14),0);
        box.setBackground(shape(0x22FFFFFF,20,0x33FFFFFF));

        TextView icon = text("⌕",27,0xFFB8C3DD);
        box.addView(icon);

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextSize(14);
        searchInput.setTextColor(Color.WHITE);
        searchInput.setHintTextColor(0xFF9CA8C3);
        searchInput.setHint("Search applications");
        searchInput.setBackgroundColor(Color.TRANSPARENT);
        searchInput.setPadding(dp(10),dp(15),0,dp(15));
        box.addView(searchInput,new LinearLayout.LayoutParams(0,-2,1));

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0,0,0,dp(25));
        content.addView(box,p);

        searchInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int a,int c,int f) {}
            public void onTextChanged(CharSequence s,int a,int before,int count) {
                showApps(s.toString());
            }
            public void afterTextChanged(Editable e) {}
        });
    }

    void section() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = text("Your Applications",19,Color.WHITE);
        title.setTypeface(null,Typeface.BOLD);
        TextView all = text("APP LIBRARY",9,0xFFB7A8FF);
        header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        header.addView(all);
        content.addView(header);

        appsGrid = vertical();
        appsGrid.setPadding(0,dp(8),0,dp(15));
        content.addView(appsGrid);
    }

    void loadApps() {
        apps.clear();
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(i,0);
        Collections.sort(list,(a,b) ->
            a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString()));
        apps.addAll(list);
    }


    android.graphics.drawable.Drawable loadPackIcon(ResolveInfo app) {
        String pack = getSharedPreferences("suzo_settings",0)
            .getString("icon_pack","none");

        if (pack.equals("none")) return app.loadIcon(pm);

        try {
            Context pc = createPackageContext(pack,Context.CONTEXT_IGNORE_SECURITY);
            android.content.res.Resources r = pc.getResources();
            int xml = r.getIdentifier("appfilter","xml",pack);
            if (xml == 0) return app.loadIcon(pm);

            org.xmlpull.v1.XmlPullParser parser = r.getXml(xml);
            String wanted = app.activityInfo.packageName + "/" + app.activityInfo.name;
            String shortWanted = app.activityInfo.packageName + "/" +
                app.activityInfo.name.replace(app.activityInfo.packageName,"");

            int event;
            while ((event=parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG &&
                    "item".equals(parser.getName())) {

                    String component = parser.getAttributeValue(null,"component");
                    String drawable = parser.getAttributeValue(null,"drawable");

                    if (component == null || drawable == null) continue;

                    component = component.replace("ComponentInfo{","").replace("}","").trim();

                    if (component.equals(wanted) ||
                        component.equals(shortWanted) ||
                        component.contains(wanted)) {

                        int id = r.getIdentifier(drawable,"drawable",pack);
                        if (id == 0) id = r.getIdentifier(drawable,"mipmap",pack);

                        if (id != 0) {
                            parser = null;
                            return pc.getResources().getDrawable(id,pc.getTheme());
                        }
                    }
                }
            }
            if (parser instanceof android.content.res.XmlResourceParser)
                ((android.content.res.XmlResourceParser)parser).close();

        } catch(Exception ignored) {}

        return app.loadIcon(pm);
    }

    android.graphics.drawable.Drawable loadPackIconForPackage(String pkg) {
        for (ResolveInfo app : apps) {
            if (app.activityInfo.packageName.equals(pkg))
                return loadPackIcon(app);
        }
        try { return pm.getApplicationIcon(pkg); }
        catch(Exception e) { return null; }
    }

    void showApps(String filter) {
        if (appsGrid == null) return;
        appsGrid.removeAllViews();

        ArrayList<ResolveInfo> visible = new ArrayList<>();
        for (ResolveInfo a : apps) {
            String name = a.loadLabel(pm).toString();
            if (name.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT)))
                visible.add(a);
        }

        int width = getResources().getDisplayMetrics().widthPixels;
        int widthDp = (int)(width/getResources().getDisplayMetrics().density);
        int columns = gridColumns;
        LinearLayout row = null;

        for (int i=0;i<visible.size();i++) {
            if (i%columns==0) {
                row = new LinearLayout(this);
                row.setGravity(Gravity.CENTER);
                appsGrid.addView(row,new LinearLayout.LayoutParams(-1,-2));
            }

            ResolveInfo app = visible.get(i);
            LinearLayout item = vertical();
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(2),dp(5),dp(2),dp(5));

            FrameLayout iconBox = new FrameLayout(this);
            int radius = iconStyle == 0 ? 8 :
                         iconStyle == 1 ? 50 : 16;
            iconBox.setBackgroundColor(Color.TRANSPARENT);
            ImageView icon = new ImageView(this);
            try { icon.setImageDrawable(loadPackIcon(app)); } catch(Exception ignored) { icon.setImageDrawable(app.loadIcon(pm)); }
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            icon.setBackgroundColor(Color.TRANSPARENT);
            FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(dp(iconSize),dp(iconSize),Gravity.CENTER);
            iconBox.addView(icon,ip);
            item.addView(iconBox,new LinearLayout.LayoutParams(dp(iconSize),dp(iconSize)));

            TextView label = text(app.loadLabel(pm).toString(),labelSize,0xFFE7EAF4);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(1);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
            lp.setMargins(0,dp(7),0,0);
            if (showLabels) item.addView(label,lp);

            item.setOnClickListener(v -> {
                try {
                    Intent launch = pm.getLaunchIntentForPackage(app.activityInfo.packageName);
                    if (launch != null) startActivity(launch);
                } catch(Exception ignored) {}
            });

            item.setOnTouchListener((v,e) -> {
                if (e.getAction()==MotionEvent.ACTION_DOWN)
                    v.animate().scaleX(.92f).scaleY(.92f).setDuration(90).start();
                else if (e.getAction()==MotionEvent.ACTION_UP ||
                         e.getAction()==MotionEvent.ACTION_CANCEL)
                    v.animate().scaleX(1f).scaleY(1f).setDuration(130).start();
                return false;
            });

            row.addView(item,new LinearLayout.LayoutParams(0,-2,1));
        }

        if (visible.isEmpty()) {
            TextView empty = text("No applications found",13,muted);
            empty.setGravity(Gravity.CENTER);
            appsGrid.addView(empty);
        }
    }

    void dock() {
        LinearLayout dock = new LinearLayout(this);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(10),dp(10),dp(10),dp(10));
        dock.setBackground(shape(0x553D4564,27,0x55FFFFFF));

        String[][] preferred = {
            {"com.google.android.dialer","com.android.dialer"},
            {"com.google.android.apps.messaging","com.android.messaging"},
            {"com.android.chrome"},
            {"com.android.camera","com.google.android.GoogleCamera"}
        };

        ArrayList<String> chosen = new ArrayList<>();
        HashSet<String> used = new HashSet<>();

        for (String[] candidates : preferred) {
            for (String pkg : candidates) {
                if (pm.getLaunchIntentForPackage(pkg)!=null) {
                    chosen.add(pkg);
                    used.add(pkg);
                    break;
                }
            }
        }

        for (ResolveInfo a : apps) {
            String pkg = a.activityInfo.packageName;
            if (chosen.size()>=dockCount) break;
            if (!used.contains(pkg)) {
                chosen.add(pkg);
                used.add(pkg);
            }
        }

        for (String pkg : chosen) {
            FrameLayout cell = new FrameLayout(this);
            ImageView image = new ImageView(this);
            try { image.setImageDrawable(loadPackIconForPackage(pkg)); }
            catch(Exception ignored) {}
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(dp(42),dp(42),Gravity.CENTER);
            cell.addView(image,ip);
            cell.setOnClickListener(v -> {
                Intent launch = pm.getLaunchIntentForPackage(pkg);
                if (launch!=null) startActivity(launch);
            });
            dock.addView(cell,new LinearLayout.LayoutParams(0,dp(50),1));
        }

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0,dp(12),0,dp(4));
        root.addView(dock,p);
    }

    @Override protected void onResume() {
        super.onResume();
        if (theme == null) return;

        String current = getSharedPreferences("suzo_settings",0)
            .getAll().toString();

        if (settingsSnapshot != null && !settingsSnapshot.equals(current)) {
            recreate();
        }
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(ticker);
        super.onDestroy();
    }
}
