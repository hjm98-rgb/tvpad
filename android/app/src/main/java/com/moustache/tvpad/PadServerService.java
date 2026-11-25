package com.moustache.tvpad;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PadServerService extends Service {

    public static final String TAG = "tvpad";
    public static final int UDP_DISCOVER_PORT = 47810;
    public static final int UDP_ANNOUNCE_PORT = 47812;
    public static final int TCP_PORT = 47811;

    public static volatile boolean running = false;
    public static volatile int clientCount = 0;
    public static volatile String lastEvent = "—";
    public static volatile String localIp = "";
    public static volatile boolean rootAvailable = false;

    private volatile boolean stop = true;
    private ServerSocket serverSocket;
    private DatagramSocket discoverSocket;
    private DatagramSocket announceSocket;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Set<Socket> clients = Collections.synchronizedSet(new HashSet<Socket>());
    private final Map<Integer, Runnable> holds = new HashMap<Integer, Runnable>();
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundCompat();
        running = true;
        stop = false;
        localIp = detectIp();
        pool.execute(new Runnable() {
            public void run() {
                udpDiscoverLoop();
            }
        });
        pool.execute(new Runnable() {
            public void run() {
                announceLoop();
            }
        });
        pool.execute(new Runnable() {
            public void run() {
                tcpLoop();
            }
        });
        pool.execute(new Runnable() {
            public void run() {
                rootAvailable = Inj.hasRoot();
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stop = true;
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Throwable ignored) {
        }
        try {
            if (discoverSocket != null) discoverSocket.close();
        } catch (Throwable ignored) {
        }
        try {
            if (announceSocket != null) announceSocket.close();
        } catch (Throwable ignored) {
        }
        synchronized (clients) {
            for (Socket s : clients) {
                try {
                    s.close();
                } catch (Throwable ignored) {
                }
            }
            clients.clear();
        }
        clientCount = 0;
        pool.shutdownNow();
        super.onDestroy();
    }

    private void startForegroundCompat() {
        String ch = "tvpad_service";
        Notification n;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel c = new NotificationChannel(ch, "TV 手柄服务", NotificationManager.IMPORTANCE_LOW);
            c.setShowBadge(false);
            nm.createNotificationChannel(c);
            n = new Notification.Builder(this, ch)
                    .setContentTitle("TV 手柄正在等待手机识别")
                    .setContentText("端口 " + TCP_PORT + " · 保持电视与手机同一 WiFi")
                    .setSmallIcon(android.R.drawable.ic_menu_manage)
                    .setOngoing(true)
                    .build();
        } else {
            n = new Notification.Builder(this)
                    .setContentTitle("TV 手柄正在等待手机识别")
                    .setContentText("端口 " + TCP_PORT)
                    .setSmallIcon(android.R.drawable.ic_menu_manage)
                    .setOngoing(true)
                    .build();
        }
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1001, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(1001, n);
        }
    }

    private String infoJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("t", "tvpad");
            o.put("name", tvName());
            o.put("model", Build.MODEL);
            o.put("brand", Build.BRAND);
            o.put("ip", localIp);
            o.put("port", TCP_PORT);
            o.put("ver", 1);
            o.put("sw", Inj.w());
            o.put("sh", Inj.h());
            o.put("a11y", PadAccessibilityService.ready());
            o.put("root", rootAvailable);
            return o.toString();
        } catch (Throwable t) {
            return "{\"t\":\"tvpad\"}";
        }
    }

    private String tvName() {
        String m = Build.MODEL == null ? "TV" : Build.MODEL.trim();
        return "电视 · " + m;
    }

    private void udpDiscoverLoop() {
        try {
            DatagramSocket s = new DatagramSocket(null);
            s.setReuseAddress(true);
            s.setBroadcast(true);
            s.bind(new InetSocketAddress(UDP_DISCOVER_PORT));
            discoverSocket = s;
            byte[] buf = new byte[1024];
            while (!stop) {
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                s.receive(pkt);
                String msg = new String(pkt.getData(), 0, pkt.getLength(), StandardCharsets.UTF_8);
                if (msg.contains("tvpad_discover")) {
                    byte[] out = infoJson().getBytes(StandardCharsets.UTF_8);
                    s.send(new DatagramPacket(out, out.length, pkt.getAddress(), pkt.getPort()));
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "discover loop ended: " + t);
        }
    }

    private void announceLoop() {
        try {
            DatagramSocket s = new DatagramSocket(null);
            s.setReuseAddress(true);
            s.setBroadcast(true);
            s.bind(new InetSocketAddress(0));
            announceSocket = s;
            byte[] out = infoJson().getBytes(StandardCharsets.UTF_8);
            while (!stop) {
                for (InetAddress addr : broadcastAddresses()) {
                    try {
                        s.send(new DatagramPacket(out, out.length, addr, UDP_ANNOUNCE_PORT));
                    } catch (Throwable ignored) {
                    }
                }
                Thread.sleep(2000);
            }
        } catch (Throwable t) {
            Log.w(TAG, "announce loop ended: " + t);
        }
    }

    private List<InetAddress> broadcastAddresses() {
        List<InetAddress> list = new ArrayList<InetAddress>();
        try {
            list.add(InetAddress.getByName("255.255.255.255"));
        } catch (Throwable ignored) {
        }
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    InetAddress b = ia.getBroadcast();
                    if (b != null) list.add(b);
                }
            }
        } catch (Throwable ignored) {
        }
        return list;
    }

    static String detectIp() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                String n = ni.getName() == null ? "" : ni.getName().toLowerCase();
                if (n.startsWith("rmnet") || n.startsWith("dummy")) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address && a.isSiteLocalAddress()) {
                        return a.getHostAddress();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return "0.0.0.0";
    }

    private void tcpLoop() {
        try {
            ServerSocket ss = new ServerSocket();
            ss.setReuseAddress(true);
            ss.bind(new InetSocketAddress(TCP_PORT));
            serverSocket = ss;
            while (!stop) {
                final Socket s = ss.accept();
                s.setTcpNoDelay(true);
                clients.add(s);
                clientCount = clients.size();
                pool.execute(new Runnable() {
                    public void run() {
                        handleClient(s);
                    }
                });
            }
        } catch (Throwable t) {
            Log.w(TAG, "tcp loop ended: " + t);
        }
    }

    private void handleClient(Socket s) {
        try {
            s.setSoTimeout(0);
            OutputStream out = s.getOutputStream();
            out.write((infoJson() + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while (!stop && (line = in.readLine()) != null) {
                String msg = line.trim();
                if (msg.isEmpty()) continue;
                try {
                    handleEvent(new JSONObject(msg));
                } catch (Throwable t) {
                    Log.w(TAG, "bad event: " + msg);
                }
            }
        } catch (Throwable t) {
            Log.i(TAG, "client disconnected: " + t);
        }
        clients.remove(s);
        clientCount = clients.size();
        try {
            s.close();
        } catch (Throwable ignored) {
        }
    }

    private int px(JSONObject o, String key) {
        return (int) Math.round(o.optDouble(key, 0.5) * Inj.w());
    }

    private int py(JSONObject o, String key) {
        return (int) Math.round(o.optDouble(key, 0.5) * Inj.h());
    }

    private void handleEvent(JSONObject o) {
        String t = o.optString("t", "");
        lastEvent = t + " · " + android.text.format.DateFormat.format("HH:mm:ss", System.currentTimeMillis());
        if ("tap".equals(t)) {
            Inj.tap(px(o, "x"), py(o, "y"), o.optInt("ms", 70));
        } else if ("press".equals(t)) {
            int id = o.optInt("id", 0);
            int x = px(o, "x");
            int y = py(o, "y");
            if (o.optBoolean("hold", false)) {
                startHold(id, x, y);
            } else {
                Inj.tap(x, y, 80);
            }
        } else if ("release".equals(t)) {
            stopHold(o.optInt("id", 0));
        } else if ("drag".equals(t)) {
            Inj.swipe(px(o, "x1"), py(o, "y1"), px(o, "x2"), py(o, "y2"), o.optInt("ms", 140));
        } else if ("key".equals(t)) {
            Inj.key(o.optInt("n", 0));
        }
    }

    private void startHold(final int id, final int x, final int y) {
        stopHold(id);
        Runnable r = new Runnable() {
            public void run() {
                Inj.tap(x, y, 200);
                ui.postDelayed(this, 240);
            }
        };
        holds.put(id, r);
        ui.post(r);
    }

    private void stopHold(int id) {
        Runnable r = holds.remove(id);
        if (r != null) ui.removeCallbacks(r);
    }
}
