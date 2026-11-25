package com.moustache.tvpad;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView mState;
    private TextView mClients;
    private TextView mInfo;
    private TextView mInjState;
    private TextView mLast;
    private Button mA11y;
    private final Handler h = new Handler(Looper.getMainLooper());

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            refresh();
            h.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0B0F14"));
        int pad = dp(56);
        root.setPadding(pad, dp(40), pad, dp(40));

        TextView title = new TextView(this);
        title.setText("TV 手柄");
        title.setTextColor(Color.parseColor("#3BE38E"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 46);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("电视接收端 · 虚拟手柄 · 局域网操控");
        sub.setTextColor(Color.parseColor("#7C8A99"));
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        sub.setPadding(0, dp(6), 0, dp(28));
        root.addView(sub);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout leftCol = new LinearLayout(this);
        leftCol.setOrientation(LinearLayout.VERTICAL);
        leftCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        mState = new TextView(this);
        mState.setText("● 正在等待识别");
        mState.setTextColor(Color.parseColor("#3BE38E"));
        mState.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34);
        mState.setTypeface(Typeface.DEFAULT_BOLD);
        leftCol.addView(mState);

        TextView hint = new TextView(this);
        hint.setText("请让手机和电视连同一个 WiFi，打开手机 App 点「识别设备」。");
        hint.setTextColor(Color.parseColor("#AEBDCB"));
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        hint.setPadding(0, dp(10), 0, dp(26));
        leftCol.addView(hint);

        mClients = new TextView(this);
        mClients.setText("已连接手机：0 台");
        mClients.setTextColor(Color.WHITE);
        mClients.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        leftCol.addView(mClients);

        mInfo = new TextView(this);
        mInfo.setText("");
        mInfo.setTextColor(Color.parseColor("#7C8A99"));
        mInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        mInfo.setPadding(0, dp(8), 0, 0);
        leftCol.addView(mInfo);

        mInjState = new TextView(this);
        mInjState.setText("");
        mInjState.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        mInjState.setPadding(0, dp(8), 0, 0);
        leftCol.addView(mInjState);

        mLast = new TextView(this);
        mLast.setText("");
        mLast.setTextColor(Color.parseColor("#5C6673"));
        mLast.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        mLast.setPadding(0, dp(8), 0, 0);
        leftCol.addView(mLast);

        row.addView(leftCol);

        LinearLayout rightCol = new LinearLayout(this);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        rightCol.setGravity(Gravity.CENTER_HORIZONTAL);

        mA11y = new Button(this);
        mA11y.setText("开启无障碍服务（必须）");
        mA11y.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        mA11y.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                } catch (Throwable t) {
                    startActivity(new Intent(Settings.ACTION_SETTINGS));
                }
            }
        });
        rightCol.addView(mA11y);

        Button restart = new Button(this);
        restart.setText("重启服务");
        restart.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        restart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopService(new Intent(MainActivity.this, PadServerService.class));
                h.postDelayed(new Runnable() {
                    public void run() {
                        startServer();
                    }
                }, 500);
            }
        });
        rightCol.addView(restart);

        row.addView(rightCol);
        root.addView(row);

        TextView foot = new TextView(this);
        foot.setText("电视端只负责接收和注入；按键布局、预设由手机端决定。");
        foot.setTextColor(Color.parseColor("#4C5763"));
        foot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        foot.setPadding(0, dp(40), 0, 0);
        root.addView(foot);

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startServer();
        h.removeCallbacks(tick);
        h.post(tick);
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacks(tick);
        super.onDestroy();
    }

    private void startServer() {
        if (PadServerService.running) return;
        Intent i = new Intent(this, PadServerService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(i);
        } else {
            startService(i);
        }
    }

    private void refresh() {
        boolean a11y = PadAccessibilityService.ready();
        mState.setText(PadServerService.clientCount > 0
                ? "● 手机已连接，可以开玩了"
                : (PadServerService.running ? "● 正在等待识别" : "● 服务未运行"));
        mState.setTextColor(Color.parseColor(PadServerService.clientCount > 0 ? "#3BE38E" : "#F2C94C"));
        mClients.setText("已连接手机：" + PadServerService.clientCount + " 台");

        String ips = PadServerService.localIp;
        mInfo.setText("设备名称：" + Build.MODEL + "   ·   IP：" + ips + "   ·   端口：" + PadServerService.TCP_PORT
                + "   ·   屏幕：" + Inj.w() + "×" + Inj.h());

        mInjState.setText("注入权限（无障碍）：" + (a11y ? "已开启" : "未开启，请点右上角按钮")
                + (PadServerService.rootAvailable ? "   ·   Root：可用" : ""));
        mInjState.setTextColor(Color.parseColor(a11y ? "#3BE38E" : "#FF6B6B"));

        mLast.setText(PadServerService.lastEvent.equals("—") ? "最近事件：—" : "最近事件：" + PadServerService.lastEvent);
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
