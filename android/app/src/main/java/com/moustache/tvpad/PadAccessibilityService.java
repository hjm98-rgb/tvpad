package com.moustache.tvpad;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

/**
 * 唯一的注入后端：把手机发来的坐标变成系统手势。
 * Android 7.0+ 任意电视都能用，不需要 root。
 */
public class PadAccessibilityService extends AccessibilityService {

    private static volatile PadAccessibilityService instance;

    public static PadAccessibilityService get() {
        return instance;
    }

    public static boolean ready() {
        return instance != null;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.i("tvpad", "accessibility service connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    public boolean tap(int x, int y, int durationMs) {
        Path p = new Path();
        p.moveTo(x, y);
        return dispatch(p, durationMs);
    }

    public boolean swipe(int x1, int y1, int x2, int y2, int durationMs) {
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        return dispatch(p, durationMs);
    }

    private boolean dispatch(Path path, int durationMs) {
        try {
            GestureDescription.StrokeDescription stroke =
                    new GestureDescription.StrokeDescription(path, 0, Math.max(20, durationMs));
            GestureDescription gd = new GestureDescription.Builder().addStroke(stroke).build();
            return dispatchGesture(gd, null, null);
        } catch (Throwable t) {
            Log.w("tvpad", "dispatchGesture failed: " + t);
            return false;
        }
    }
}
