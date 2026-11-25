package com.moustache.tvpad;

import android.util.DisplayMetrics;
import android.util.Log;

import java.io.OutputStream;

/** 注入工具：手势 + 可选 root 按键。 */
final class Inj {

    private Inj() {
    }

    static int w() {
        DisplayMetrics dm = android.content.res.Resources.getSystem().getDisplayMetrics();
        return dm.widthPixels;
    }

    static int h() {
        DisplayMetrics dm = android.content.res.Resources.getSystem().getDisplayMetrics();
        return dm.heightPixels;
    }

    static void tap(int x, int y, int ms) {
        PadAccessibilityService s = PadAccessibilityService.get();
        if (s != null) s.tap(x, y, ms);
    }

    static void swipe(int x1, int y1, int x2, int y2, int ms) {
        PadAccessibilityService s = PadAccessibilityService.get();
        if (s != null) s.swipe(x1, y1, x2, y2, ms);
    }

    /** 需要 root，仅作彩蛋：真正发硬件按键，适合模拟器 / Java 版启动器。 */
    static void key(int keycode) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "input keyevent " + keycode});
            p.waitFor();
        } catch (Throwable t) {
            Log.w("tvpad", "root key failed: " + t);
        }
    }

    static boolean hasRoot() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            java.io.BufferedReader r = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()));
            String line = r.readLine();
            p.waitFor();
            return line != null && line.contains("uid=0");
        } catch (Throwable t) {
            return false;
        }
    }
}
