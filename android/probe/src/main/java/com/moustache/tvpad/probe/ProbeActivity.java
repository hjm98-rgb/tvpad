package com.moustache.tvpad.probe;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 目的只有一个：绕过被裁掉的设置菜单，用 Intent 直接打到系统里可能还存在的
 * 开发者选项页 / 无障碍页。哪个能打开，就在那一页里手动把开关打开。
 */
public class ProbeActivity extends Activity {

    private TextView log;

    private final String[][] TARGETS = {
            {"无障碍设置（标准）", "action:" + Settings.ACTION_ACCESSIBILITY_SETTINGS},
            {"开发者选项（标准）", "action:" + Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS},
            {"realm: AccessibilitySettings", "comp:com.android.settings/.accessibility.AccessibilitySettings"},
            {"comp: Settings$AccessibilitySettingsActivity", "comp:com.android.settings/.Settings$AccessibilitySettingsActivity"},
            {"comp: DevelopmentSettings", "comp:com.android.settings/.DevelopmentSettings"},
            {"comp: Settings$DevelopmentSettingsActivity", "comp:com.android.settings/.Settings$DevelopmentSettingsActivity"},
            {"comp: tv settings / accessibility", "comp:com.android.tv.settings/.accessibility.AccessibilityActivity"},
            {"comp: HmSettings 无障碍", "comp:com.hisense.settings/.accessibility.AccessibilitySettingsActivity"},
            {"comp: 海信 settings 高级", "comp:com.hisense.settings/.AdvancedSettingsActivity"},
            {"应用详情（本 App）", "action:" + Settings.ACTION_APPLICATION_DETAILS_SETTINGS},
            {"系统设置首页", "action:" + Settings.ACTION_SETTINGS}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0B0F14"));
        int pad = dp(36);
        root.setPadding(pad, dp(28), pad, dp(28));

        TextView title = new TextView(this);
        title.setText("系统页探测 · 逐个点，看哪个能跳进去");
        title.setTextColor(Color.parseColor("#3BE38E"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("点一个按钮，如果跳到了系统页面，就在那一页里开「开发者选项 / USB调试」或「无障碍服务」。\n每个都点一遍，下面会记录结果。");
        hint.setTextColor(Color.parseColor("#AEBDCB"));
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        hint.setPadding(0, dp(8), 0, dp(16));
        root.addView(hint);

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < TARGETS.length; i++) {
            final String label = TARGETS[i][0];
            final String spec = TARGETS[i][1];
            Button b = new Button(this);
            b.setText(label);
            b.setAllCaps(false);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
            lp.bottomMargin = dp(6);
            b.setLayoutParams(lp);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    go(label, spec);
                }
            });
            grid.addView(b);
        }

        ScrollView sv = new ScrollView(this);
        sv.addView(grid);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(sv, slp);

        log = new TextView(this);
        log.setText("结果：—");
        log.setTextColor(Color.parseColor("#7C8A99"));
        log.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        log.setPadding(0, dp(10), 0, 0);
        root.addView(log);

        setContentView(root);
        log("提示：如果点了没反应，说明这个系统把对应页面删干净了，只能走 adb 或换设备。");

        // 顺手把当前 App 的包名/版本显示出来，方便核对
        log("包名：" + getPackageName());
    }

    private void go(String label, String spec) {
        Intent intent;
        try {
            if (spec.startsWith("action:")) {
                intent = new Intent(spec.substring(7));
            } else {
                String s = spec.substring(5);
                int slash = s.indexOf('/');
                String pkg = s.substring(0, slash);
                String cls = s.substring(slash + 1);
                if (cls.startsWith(".")) cls = pkg + cls;
                intent = new Intent();
                intent.setComponent(new ComponentName(pkg, cls));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            if (Settings.ACTION_APPLICATION_DETAILS_SETTINGS.equals(spec.substring(7))) {
                intent.setData(Uri.parse("package:" + getPackageName()));
            }
            startActivity(intent);
            log("✔ 已发起：" + label);
            Toast.makeText(this, "已打开：" + label, Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            log("✘ 打不开：" + label + " → " + t.getClass().getSimpleName());
            Toast.makeText(this, "打不开：" + label, Toast.LENGTH_SHORT).show();
        }
    }

    private void log(String s) {
        log.setText(s + "\n" + log.getText());
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
