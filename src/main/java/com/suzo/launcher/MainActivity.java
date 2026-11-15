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
    int iconAppearance;
    int appPage = 0;
    int appPageCount = 1;
    String currentFilter = "";
    TextView pageIndicator;
    LinearLayout categoryBar;
    int selectedCategory = -1;
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
        iconAppearance = p.getInt("icon_appearance",0);
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
        getWindow().setStatusBarColor(ThemeEngine.background(theme));
        getWindow().setNavigationBarColor(ThemeEngine.background(theme));
        getWindow().getDecorView().setSystemUiVisibility(
            theme == ThemeEngine.Theme.CLASSIC ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0);
        createUI();
        loadApps();
        showApps("");
        handler.post(ticker);
    }

    void createUI() {
        root = vertical();
        root.setPadding(dp(19), dp(12), dp(19), dp(10));

        GradientDrawable background;
        if (theme == ThemeEngine.Theme.DARK) {
            background = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF09090C,0xFF17171D,0xFF101014});
        } else if (theme == ThemeEngine.Theme.CLASSIC) {
            background = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFFF7F7FA,0xFFE9EAF0,0xFFF2F2F7});
        } else {
            background = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF090D19,0xFF171C35,0xFF24203E,0xFF0A1020});
        }
        root.setBackground(background);
        String wallpaper = getSharedPreferences("suzo_settings",0)
            .getString("wallpaper_uri",null);
        if (wallpaper != null) {
            try {
                android.net.Uri uri = android.net.Uri.parse(wallpaper);
                android.graphics.BitmapFactory.Options bounds = new android.graphics.BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                java.io.InputStream first = getContentResolver().openInputStream(uri);
                android.graphics.BitmapFactory.decodeStream(first,null,bounds);
                if (first != null) first.close();
                int targetW = getResources().getDisplayMetrics().widthPixels;
                int targetH = getResources().getDisplayMetrics().heightPixels;
                int sample = 1;
                while (bounds.outWidth / sample > targetW * 2 || bounds.outHeight / sample > targetH * 2) sample *= 2;
                android.graphics.BitmapFactory.Options decode = new android.graphics.BitmapFactory.Options();
                decode.inSampleSize = sample;
                java.io.InputStream second = getContentResolver().openInputStream(uri);
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(second,null,decode);
                if (second != null) second.close();
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
        TextView logo = text("SUZO",22,fg);
        logo.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        TextView caption = text("DESIGNED FOR YOUR WORLD",9,muted);
        brand.addView(logo);
        brand.addView(caption);
        bar.addView(brand,new LinearLayout.LayoutParams(0,-2,1));

        TextView control = text("☷",22,fg);
        control.setGravity(Gravity.CENTER);
        control.setBackground(shape(theme == ThemeEngine.Theme.CLASSIC ? 0xFFE5E5EC : 0x22FFFFFF,19,theme == ThemeEngine.Theme.CLASSIC ? 0xFFD0D0D8 : 0x44FFFFFF));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(dp(44),dp(44));
        cp.setMargins(0,0,dp(8),0);
        bar.addView(control,cp);
        control.setOnClickListener(v -> QuickPanel.show(this));

        TextView settings = text("⚙",23,fg);
        settings.setGravity(Gravity.CENTER);
        settings.setBackground(shape(theme == ThemeEngine.Theme.CLASSIC ? 0xFFE5E5EC : 0x22FFFFFF,19,theme == ThemeEngine.Theme.CLASSIC ? 0xFFD0D0D8 : 0x44FFFFFF));
        bar.addView(settings,new LinearLayout.LayoutParams(dp(48),dp(48)));
        settings.setOnClickListener(v ->
            startActivity(new Intent(this,SettingsActivity.class)));

        content.addView(bar);
    }

    void hero() {
        LinearLayout card = vertical();
        card.setPadding(dp(22),dp(22),dp(22),dp(20));

        if (theme == ThemeEngine.Theme.GLASS) {
            GradientDrawable glass = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0x443E64A4,0x222B386C,0x333B2D68});
            glass.setCornerRadius(dp(30));
            glass.setStroke(dp(1),0x55FFFFFF);
            card.setBackground(glass);
        } else {
            GradientDrawable panel = ThemeEngine.panel(theme);
            panel.setCornerRadius(dp(30));
            panel.setStroke(dp(1), theme == ThemeEngine.Theme.CLASSIC ? 0xFFE0E0E8 : 0xFF44444C);
            card.setBackground(panel);
        }

        TextView eyebrow = text("GOOD TO SEE YOU",10,muted);
        eyebrow.setLetterSpacing(.15f);
        clock = text("--:--",clockSize,fg);
        clock.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
        date = text("",13,muted);
        date.setVisibility(showDate ? View.VISIBLE : View.GONE);

        TextView line = text("✦  YOUR SPACE. YOUR MOMENT.",10,theme == ThemeEngine.Theme.CLASSIC ? 0xFF6654B5 : 0xFFB9A8FF);
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
        box.setBackground(shape(theme == ThemeEngine.Theme.CLASSIC ? 0xFFFFFFFF : 0x22FFFFFF,20,theme == ThemeEngine.Theme.CLASSIC ? 0xFFD9D9E2 : 0x33FFFFFF));

        TextView icon = text("⌕",27,0xFFB8C3DD);
        box.addView(icon);

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextSize(14);
        searchInput.setTextColor(fg);
        searchInput.setHintTextColor(muted);
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
                appPage = 0;
                showApps(s.toString());
            }
            public void afterTextChanged(Editable e) {}
        });
    }

    void section() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = text("Your Applications",19,fg);
        title.setTypeface(null,Typeface.BOLD);
        header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout pages = new LinearLayout(this);
        pages.setGravity(Gravity.CENTER);
        TextView previous = text("‹",24,fg);
        pageIndicator = text("1 / 1",10,muted);
        TextView next = text("›",24,fg);
        previous.setGravity(Gravity.CENTER);
        next.setGravity(Gravity.CENTER);
        previous.setPadding(dp(8),0,dp(8),0);
        next.setPadding(dp(8),0,dp(8),0);
        pages.addView(previous);
        pages.addView(pageIndicator);
        pages.addView(next);
        previous.setOnClickListener(v -> {
            if (appPage > 0) { appPage--; showApps(currentFilter); }
        });
        next.setOnClickListener(v -> {
            if (appPage + 1 < appPageCount) { appPage++; showApps(currentFilter); }
        });
        header.addView(pages);
        content.addView(header);

        HorizontalScrollView categoryScroll = new HorizontalScrollView(this);
        categoryScroll.setHorizontalScrollBarEnabled(false);
        categoryBar = new LinearLayout(this);
        categoryBar.setGravity(Gravity.CENTER_VERTICAL);
        categoryScroll.addView(categoryBar);
        LinearLayout.LayoutParams categoryParams = new LinearLayout.LayoutParams(-1,-2);
        categoryParams.setMargins(0,dp(10),0,dp(8));
        content.addView(categoryScroll,categoryParams);
        renderCategoryBar();

        appsGrid = vertical();
        appsGrid.setPadding(0,dp(8),0,dp(15));
        content.addView(appsGrid);
    }

    void renderCategoryBar() {
        if (categoryBar == null) return;
        categoryBar.removeAllViews();
        String[] names = {"All","Games","Social","Photos","Video","Music","Productivity"};
        int[] values = {-1,
            android.content.pm.ApplicationInfo.CATEGORY_GAME,
            android.content.pm.ApplicationInfo.CATEGORY_SOCIAL,
            android.content.pm.ApplicationInfo.CATEGORY_IMAGE,
            android.content.pm.ApplicationInfo.CATEGORY_VIDEO,
            android.content.pm.ApplicationInfo.CATEGORY_AUDIO,
            android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY
        };
        for (int i=0;i<names.length;i++) {
            final int value = values[i];
            boolean active = selectedCategory == value;
            TextView chip = text(names[i],12,active ? Color.WHITE : fg);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(15),dp(9),dp(15),dp(9));
            chip.setBackground(shape(active
                ? (theme == ThemeEngine.Theme.CLASSIC ? 0xFF6654B5 : 0xFF6654B5)
                : (theme == ThemeEngine.Theme.CLASSIC ? 0xFFE7E7EF : 0x22FFFFFF),
                18, active ? 0xFF6654B5 : (theme == ThemeEngine.Theme.CLASSIC ? 0xFFDADAE3 : 0x33FFFFFF)));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2,-2);
            p.setMargins(0,0,dp(8),0);
            categoryBar.addView(chip,p);
            chip.setOnClickListener(v -> {
                selectedCategory = value;
                renderCategoryBar();
                appPage = 0;
                showApps(currentFilter);
            });
        }
    }

    boolean categoryMatches(ResolveInfo app) {
        if (selectedCategory < 0) return true;
        if (android.os.Build.VERSION.SDK_INT < 26) return false;
        return app.activityInfo.applicationInfo.category == selectedCategory;
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
                            ((android.content.res.XmlResourceParser)parser).close();
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
        currentFilter = filter == null ? "" : filter;
        appsGrid.removeAllViews();

        ArrayList<ResolveInfo> visible = new ArrayList<>();
        for (ResolveInfo a : apps) {
            String name = a.loadLabel(pm).toString();
            if (name.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT))
                    && categoryMatches(a))
                visible.add(a);
        }

        int columns = Math.max(3, Math.min(5, gridColumns));
        int pageSize = columns * 4;
        appPageCount = Math.max(1, (int)Math.ceil(visible.size() / (double)pageSize));
        appPage = Math.max(0, Math.min(appPage, appPageCount - 1));
        if (pageIndicator != null)
            pageIndicator.setText((appPage + 1) + " / " + appPageCount);
        int start = appPage * pageSize;
        int end = Math.min(visible.size(), start + pageSize);
        LinearLayout row = null;

        for (int i=start;i<end;i++) {
            if ((i-start)%columns==0) {
                row = new LinearLayout(this);
                row.setGravity(Gravity.CENTER);
                appsGrid.addView(row,new LinearLayout.LayoutParams(-1,-2));
            }

            ResolveInfo app = visible.get(i);
            LinearLayout item = vertical();
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(2),dp(5),dp(2),dp(5));

            FrameLayout iconBox = new FrameLayout(this);
            iconBox.setBackgroundColor(Color.TRANSPARENT);
            ImageView icon = new ImageView(this);
            try { icon.setImageDrawable(loadPackIcon(app)); } catch(Exception ignored) { icon.setImageDrawable(app.loadIcon(pm)); }
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            icon.setBackgroundColor(Color.TRANSPARENT);
            applyIconAppearance(icon);
            icon.setClipToOutline(true);
            icon.setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View view, Outline outline) {
                    int w = view.getWidth();
                    int h = view.getHeight();
                    if (w <= 0 || h <= 0) return;
                    if (iconStyle == 1) outline.setOval(0,0,w,h);
                    else {
                        float r = dp(iconStyle == 0 ? 8 : 16);
                        outline.setRoundRect(0,0,w,h,Math.min(r,Math.min(w,h)/2f));
                    }
                }
            });
            FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(dp(iconSize),dp(iconSize),Gravity.CENTER);
            iconBox.addView(icon,ip);
            item.addView(iconBox,new LinearLayout.LayoutParams(dp(iconSize),dp(iconSize)));

            TextView label = text(app.loadLabel(pm).toString(),labelSize,fg);
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
            item.setOnLongClickListener(v -> {
                showAppOptions(app);
                return true;
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
            if (pageIndicator != null) pageIndicator.setText("1 / 1");
            TextView empty = text("No applications found",13,muted);
            empty.setGravity(Gravity.CENTER);
            appsGrid.addView(empty);
        }
    }

    void applyIconAppearance(ImageView image) {
        if (iconAppearance == 1) image.setColorFilter(Color.BLACK, android.graphics.PorterDuff.Mode.SRC_IN);
        else if (iconAppearance == 2) image.setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);
        else image.clearColorFilter();
    }

    void dock() {
        LinearLayout dock = new LinearLayout(this);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(10),dp(10),dp(10),dp(10));
        dock.setBackground(shape(theme == ThemeEngine.Theme.CLASSIC ? 0xFFE4E5EC : theme == ThemeEngine.Theme.DARK ? 0xFF24242B : 0x553D4564,27,theme == ThemeEngine.Theme.CLASSIC ? 0xFFD3D4DC : 0x55FFFFFF));

        String[][] preferred = {
            {"com.google.android.dialer","com.android.dialer"},
            {"com.google.android.apps.messaging","com.android.messaging"},
            {"com.android.chrome"},
            {"com.android.camera","com.google.android.GoogleCamera"}
        };

        ArrayList<String> chosen = new ArrayList<>();
        HashSet<String> used = new HashSet<>();

        android.content.SharedPreferences dockPrefs = getSharedPreferences("suzo_settings",0);
        HashSet<String> hiddenDock = new HashSet<>();
        String hidden = dockPrefs.getString("dock_hidden","");
        if (hidden.length() > 0) hiddenDock.addAll(Arrays.asList(hidden.split("\\|")));
        String savedDock = dockPrefs.getString("dock_apps","");
        if (savedDock.length() > 0) {
            for (String pkg : savedDock.split("\\|")) {
                if (chosen.size() >= dockCount) break;
                if (pkg.length() > 0 && !hiddenDock.contains(pkg)
                        && pm.getLaunchIntentForPackage(pkg) != null && !used.contains(pkg)) {
                    chosen.add(pkg);
                    used.add(pkg);
                }
            }
        }

        for (String[] candidates : preferred) {
            if (chosen.size() >= dockCount) break;
            for (String pkg : candidates) {
                if (!hiddenDock.contains(pkg) && pm.getLaunchIntentForPackage(pkg)!=null) {
                    chosen.add(pkg);
                    used.add(pkg);
                    break;
                }
            }
        }

        for (ResolveInfo a : apps) {
            String pkg = a.activityInfo.packageName;
            if (chosen.size()>=dockCount) break;
            if (!used.contains(pkg) && !hiddenDock.contains(pkg)) {
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
            applyIconAppearance(image);
            FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(dp(42),dp(42),Gravity.CENTER);
            cell.addView(image,ip);
            cell.setOnClickListener(v -> {
                Intent launch = pm.getLaunchIntentForPackage(pkg);
                if (launch!=null) startActivity(launch);
            });
            cell.setOnLongClickListener(v -> {
                removeFromDock(pkg);
                return true;
            });
            dock.addView(cell,new LinearLayout.LayoutParams(0,dp(50),1));
        }

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0,dp(12),0,dp(4));
        root.addView(dock,p);
    }

    void showAppOptions(ResolveInfo app) {
        String pkg = app.activityInfo.packageName;
        String name = app.loadLabel(pm).toString();
        String[] actions = {"Add to dock","App info"};
        new AlertDialog.Builder(this)
            .setTitle(name)
            .setItems(actions,(dialog,which) -> {
                if (which == 0) addToDock(pkg);
                else if (which == 1) {
                    try {
                        Intent info = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        info.setData(android.net.Uri.parse("package:" + pkg));
                        startActivity(info);
                    } catch(Exception ignored) {}
                }
            }).show();
    }

    void addToDock(String pkg) {
        android.content.SharedPreferences prefs = getSharedPreferences("suzo_settings",0);
        ArrayList<String> items = new ArrayList<>();
        String saved = prefs.getString("dock_apps","");
        if (saved.length() > 0) items.addAll(Arrays.asList(saved.split("\\|")));
        items.remove(pkg);
        items.add(0,pkg);
        ArrayList<String> hiddenItems = new ArrayList<>();
        String hidden = prefs.getString("dock_hidden","");
        if (hidden.length() > 0) hiddenItems.addAll(Arrays.asList(hidden.split("\\|")));
        hiddenItems.remove(pkg);
        prefs.edit().putString("dock_hidden",joinPackages(hiddenItems)).apply();
        while (items.size() > Math.max(1,dockCount)) items.remove(items.size()-1);
        StringBuilder value = new StringBuilder();
        for (String item : items) {
            if (value.length() > 0) value.append("|");
            value.append(item);
        }
        prefs.edit().putString("dock_apps",value.toString()).apply();
        recreate();
    }

    void removeFromDock(String pkg) {
        android.content.SharedPreferences prefs = getSharedPreferences("suzo_settings",0);
        ArrayList<String> items = new ArrayList<>();
        String saved = prefs.getString("dock_apps","");
        if (saved.length() > 0) items.addAll(Arrays.asList(saved.split("\\|")));
        items.remove(pkg);
        ArrayList<String> hiddenItems = new ArrayList<>();
        String hidden = prefs.getString("dock_hidden","");
        if (hidden.length() > 0) hiddenItems.addAll(Arrays.asList(hidden.split("\\|")));
        if (!hiddenItems.contains(pkg)) hiddenItems.add(pkg);
        prefs.edit().putString("dock_apps",joinPackages(items))
            .putString("dock_hidden",joinPackages(hiddenItems)).apply();
        recreate();
    }

    String joinPackages(ArrayList<String> packages) {
        StringBuilder value = new StringBuilder();
        for (String item : packages) {
            if (value.length() > 0) value.append("|");
            value.append(item);
        }
        return value.toString();
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
