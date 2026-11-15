package com.suzo.launcher;

import android.inputmethodservice.InputMethodService;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Optional SUZO keyboard with a lightweight QWERTY layout and emoji panel. */
public class SuzoKeyboardService extends InputMethodService {
    private boolean upper = true;
    private boolean emojiMode = false;
    private LinearLayout root;

    private int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density + .5f);
    }

    @Override public View onCreateInputView() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(5),dp(6),dp(5),dp(6));
        root.setBackgroundColor(0xFF171923);
        showLetters();
        return root;
    }

    private GradientDrawable keyBackground(boolean special) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(special ? 0xFF34394C : 0xFF292D3B);
        d.setCornerRadius(dp(8));
        d.setStroke(dp(1),0xFF454B60);
        return d;
    }

    private Button key(String label, String value, boolean special, float weight) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setAllCaps(false);
        b.setPadding(0,0,0,0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(dp(43));
        b.setMinimumHeight(dp(43));
        b.setBackground(keyBackground(special));
        b.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic == null) return;
            if ("BACK".equals(value)) ic.deleteSurroundingText(1,0);
            else if ("ENTER".equals(value)) {
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER));
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_ENTER));
            } else if ("SHIFT".equals(value)) {
                upper = !upper;
                showLetters();
            } else if ("EMOJI".equals(value)) {
                emojiMode = true;
                showEmoji();
            } else if ("SPACE".equals(value)) ic.commitText(" ",1);
            else if ("HIDE".equals(value)) requestHideSelf(0);
            else ic.commitText(value,1);
        });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,dp(46),weight);
        p.setMargins(dp(2),dp(2),dp(2),dp(2));
        b.setLayoutParams(p);
        return b;
    }

    private void addRow(String[] labels, String[] values, boolean special) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i=0;i<labels.length;i++) row.addView(key(labels[i],values[i],special && i==0,1f));
        root.addView(row);
    }

    private void showLetters() {
        if (root == null) return;
        emojiMode = false;
        root.removeAllViews();
        addRow(new String[]{"1","2","3","4","5","6","7","8","9","0"},
            new String[]{"1","2","3","4","5","6","7","8","9","0"},false);
        String first = upper ? "QWERTYUIOP" : "qwertyuiop";
        String second = upper ? "ASDFGHJKL" : "asdfghjkl";
        String third = upper ? "ZXCVBNM" : "zxcvbnm";
        addRow(first.split(""),first.split(""),false);
        addRow(second.split(""),second.split(""),false);
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.addView(key("⇧","SHIFT",true,1.25f));
        bottom.addView(key("😊","EMOJI",true,1.1f));
        for (int i=0;i<third.length();i++) {
            String ch=third.substring(i,i+1);
            bottom.addView(key(ch,ch,false,1f));
        }
        bottom.addView(key("⌫","BACK",true,1.25f));
        root.addView(bottom);
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.addView(key("?123","EMOJI",true,1.2f));
        actions.addView(key(", ",",",false,.8f));
        actions.addView(key("English","SPACE",false,4.5f));
        actions.addView(key(".",".",false,.8f));
        actions.addView(key("↵","ENTER",true,1.2f));
        root.addView(actions);
    }

    private void showEmoji() {
        if (root == null) return;
        root.removeAllViews();
        TextView heading = new TextView(this);
        heading.setText("SUZO EMOJI");
        heading.setTextColor(Color.WHITE);
        heading.setTextSize(14);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setPadding(dp(8),0,0,0);
        root.addView(heading,new LinearLayout.LayoutParams(-1,dp(34)));
        String[] emojis={"😀","😁","😂","🤣","😊","😍","🥰","😎","😭","😴","🤔","🙌","👏","👍","❤️","🔥","✨","🎉","🌙","⭐","⚽","🎮","📱","💯"};
        for(int start=0;start<emojis.length;start+=6){
            LinearLayout row=new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for(int i=start;i<Math.min(start+6,emojis.length);i++){
                final String emoji=emojis[i];
                Button b=key(emoji,emoji,false,1f);
                row.addView(b);
            }
            root.addView(row);
        }
        LinearLayout footer=new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.addView(key("ABC","LETTERS",true,1f));
        footer.addView(key("⌫","BACK",true,1f));
        footer.addView(key("Hide","HIDE",true,1f));
        root.addView(footer);
        // The ABC key is handled explicitly to keep emoji mode simple.
        View abc=footer.getChildAt(0);
        abc.setOnClickListener(v -> showLetters());
    }
}
