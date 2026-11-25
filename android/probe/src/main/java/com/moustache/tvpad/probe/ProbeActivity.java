package com.moustache.tvpad.probe;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * v2：不再猜类名，改成"全盘搜查"。
 * 1) 向系统问：谁声明了能打开无障碍设置（queryIntentActivities）
 * 2) 遍历所有已安装应用的 Activity，按关键词（accessib / easy / senior / child / assist）筛出可疑入口
 * 3) 每个结果一个按钮，点了就试；成功/失败都记在下方
 */
public class ProbeActivity extends Activity {

    private TextView log;
    private LinearLayout list;
    private final LinkedHashSet<String> found = new LinkedHashSet<>();
    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0B0F14"));
        int pad = dp(36);
        root.setPadding(pad, dp(24), pad, dp(20));

        TextView title = new TextView(this);
        title.setText("全盘搜查 · 无障碍入口 v2");
        title.setTextColor(Color.parseColor("#3BE38E"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("下面每个按钮都是从系统里真实查出来的候选页面，不是猜的。逐个点，能跳进去的那一页就是入口。");
        hint.setTextColor(Color.parseColor("#AEBDCB"));
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        hint.setPadding(0, dp(6), 0, dp(10));
        root.addView(hint);

        Button rescan = new Button(this);
        rescan.setText("重新扫描");
        rescan.setAllCaps(false);
        rescan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                found.clear();
                count = 0;
                list.removeAllViews();
                log.setText("结果：—");
                scan();
            }
        });
        root.addView(rescan, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(44)));

        ScrollView sv = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list);
        root.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        log = new TextView(this);
        log.setText("结果：—");
        log.setTextColor(Color.parseColor("#7C8A99"));
        log.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        log.setPadding(0, dp(8), 0, 0);
        root.addView(log);

        setContentView(root);
        log("包名：" + getPackageName());
        scan();
    }

    private void scan() {
        PackageManager pm = getPackageManager();

        // 1) 谁声明能处理"打开无障碍设置"
        try {
            Intent i = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            List<ResolveInfo> rs = pm.queryIntentActivities(i, 0);
            log("声明无障碍入口的页面数：" + rs.size());
            for (ResolveInfo ri : rs) {
                if (ri.activityInfo == null) continue;
                add(ri.activityInfo.packageName, ri.activityInfo.name, "intent→无障碍");
            }
        } catch (Throwable t) {
            log("查询 intent 失败：" + t.getClass().getSimpleName());
        }

        // 2) 遍历所有包，找无障碍 + 简易/长辈/儿童 类入口
        List<PackageInfo> pkgs;
        try {
            pkgs = pm.getInstalledPackages(PackageManager.GET_ACTIVITIES);
        } catch (Throwable t) {
            log("枚举包失败：" + t.getClass().getSimpleName());
            pkgs = new ArrayList<>();
        }
        for (PackageInfo pi : pkgs) {
            if (pi.activities == null) continue;
            String pkg = pi.packageName.toLowerCase();
            boolean sysish = pkg.startsWith("com.android.settings")
                    || pkg.startsWith("com.android.tv.settings")
                    || pkg.contains("settings")
                    || pkg.startsWith("com.hisense")
                    || pkg.startsWith("com.juhaokan")
                    || pkg.startsWith("com.vision");
            for (ActivityInfo ai : pi.activities) {
                if (ai.name == null) continue;
                String cls = ai.name.toLowerCase();
                if (cls.contains("accessib")) {
                    add(ai.packageName, ai.name, "accessib");
                } else if (sysish && (cls.contains("easy") || cls.contains("senior")
                        || cls.contains("child") || cls.contains("simple")
                        || cls.contains("assist") || cls.contains("aid")
                        || cls.contains("elder") || cls.contains("guide"))) {
                    add(ai.packageName, ai.name, "简易/长辈");
                }
            }
        }

        if (count == 0) {
            log("没扫到任何无障碍相关页面（说明这一类 Activity 真的被删了）");
        }
    }

    private void add(String pkg, String cls, String why) {
        String key = pkg + "/" + cls;
        if (!found.add(key)) return;
        if (count >= 60) return;

        String shortName = cls.startsWith(pkg) ? cls.substring(pkg.length()) : cls;
        if (shortName.startsWith(".")) shortName = shortName.substring(1);

        Button b = new Button(this);
        b.setText("[" + why + "] " + pkg + "\n" + shortName);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        lp.bottomMargin = dp(5);
        b.setLayoutParams(lp);
        final String fPkg = pkg, fCls = cls;
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                launch(fPkg, fCls);
            }
        });
        list.addView(b);
        count++;
    }

    private void launch(String pkg, String cls) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(pkg, cls));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            log("✔ 已打开：" + pkg + "/" + cls);
            Toast.makeText(this, "打开成功", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            log("✘ 打不开：" + pkg + "/" + cls + " → " + t.getClass().getSimpleName());
            Toast.makeText(this, "打不开", Toast.LENGTH_SHORT).show();
        }
    }

    private void log(String s) {
        log.setText(s + "\n" + log.getText());
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
