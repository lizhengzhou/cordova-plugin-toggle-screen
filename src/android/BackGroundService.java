package cn.lizz.cordova.plugin;

import android.app.KeyguardManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.regex.Pattern;

public class BackGroundService extends Service {

    final String tag = "lizz.BackGroundService";
    DevicePolicyManager policyManager;
    PowerManager powerManager;
    KeyguardManager keyguardManager;
    KeyguardManager.KeyguardLock keyLock;
    PowerManager.WakeLock wakeLock;
    PowerManager.WakeLock screenLock;
    boolean CanRun = false;
    Thread thread_t;
    int checkThreshold = 10;
    Date RemoteDate = new Date();
    Date LocalDate = new Date();

    public BackGroundService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(tag, "OnBind");
        return null;
    }

    @Override
    public void onCreate() {
        Log.i(tag, "onCreate");
        super.onCreate();

        powerManager = (PowerManager) this.getApplicationContext().getSystemService(Context.POWER_SERVICE);
        policyManager = (DevicePolicyManager) this.getApplicationContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
        keyguardManager = (KeyguardManager) this.getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
        keyLock = keyguardManager.newKeyguardLock("unlock");
        screenLock = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.FULL_WAKE_LOCK, "lizz:bright");

        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag + ":wakeLockTag");

        wakeLock.acquire();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(tag, "onStartCommand");

        if (!CanRun) {
            Log.i(tag, "new thread_t");
            CanRun = true;
            thread_t = new Thread() {
                @Override
                public void run() {
                    Date LastOpenStamp = new Date();
                    LastOpenStamp.setHours(0);
                    Date LastCloseStamp = new Date();
                    LastCloseStamp.setHours(0);
                    while (CanRun) {
                        try {

                            Log.i(tag, "DoSomething");

                            if (powerManager != null) {
                                boolean isScreenOn = powerManager.isScreenOn();
                                Log.i(tag, "isScreenOn：" + isScreenOn);
                                if (isScreenOn) {
                                    ArrayList<HourMinute> list = getList(false);
                                    if (CheckStamp(LastCloseStamp) && Match(list)) {
                                        turnOffScreen();
                                        LastCloseStamp = new Date();
                                    }
                                } else {
                                    ArrayList<HourMinute> list = getList(true);
                                    if (CheckStamp(LastOpenStamp) && Match(list)) {
                                        turnOnScreen();
                                        LastOpenStamp = new Date();
                                    }
                                }
                            }

                            Thread.sleep(30000);
                        } catch (InterruptedException e) {
                            Log.d(tag, e.getMessage());
                            e.printStackTrace();
                        } catch (Exception e) {
                            Log.d(tag, e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            };
            thread_t.setDaemon(true);
            thread_t.start();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        CanRun = false;
        wakeLock.release();

        super.onDestroy();
    }

    public void turnOnScreen() {
        Log.v(tag, "ON!");

        keyLock.disableKeyguard();

        screenLock.acquire();
        screenLock.release();

    }

    public void turnOffScreen() {
        Log.v(tag, "Off!");
        policyManager.lockNow();
    }

    public ArrayList<HourMinute> getList(boolean IsOpen) {
        ArrayList<HourMinute> list = new ArrayList<HourMinute>();

        SharedPreferences preferences = getSharedPreferences("Config", Context.MODE_PRIVATE);
        HashSet<String> hashSet = (HashSet<String>) preferences.getStringSet(IsOpen ? "open" : "close", new HashSet<String>());
        for (String str : hashSet) {
            Log.d(tag, str);

            if (Pattern.matches("^\\d{1,2}:\\d{1,2}:\\d{1,2}$", str)) {
                String[] splitStr = str.split(":");

                HourMinute hm = new HourMinute();
                hm.Hour = Integer.parseInt(splitStr[0]);
                hm.Minute = Integer.parseInt(splitStr[1]);
                list.add(hm);
            }
        }
        return list;
    }

    public boolean Match(ArrayList<HourMinute> list) {
        Date now = getNow();
        Log.i(tag, now.toString());
        SimpleDateFormat format = new SimpleDateFormat("HH");
        int Hour = Integer.parseInt(format.format(now));
        format = new SimpleDateFormat("mm");
        int Minute = Integer.parseInt(format.format(now));

        for (HourMinute item : list) {
            int NowMinutes = Hour * 60 + Minute;
            int CheckMinites = item.Hour * 60 + item.Minute;
            if (NowMinutes >= CheckMinites && (NowMinutes - CheckMinites) < checkThreshold) {
                return true;
            }
        }
        return false;
    }

    public Date getNow() {
        SharedPreferences preferences = getSharedPreferences("Config", Context.MODE_PRIVATE);
        String syncUrl = (String) preferences.getString("syncUrl", "");

        Date now = new Date();

        if (syncUrl != "") {
            now = VisitURL(syncUrl);
        }

        return now;
    }


    /**
     * 网址访问
     *
     * @param url 网址
     * @return urlDate 对象网址时间
     */
    public Date VisitURL(String url) {
        Date urlDate = GetFixedLocalDate();
        try {
            URL url1 = new URL(url);
            URLConnection conn = url1.openConnection();  //生成连接对象
            conn.connect();  //连接对象网页
            urlDate = new Date(conn.getDate());  //获取对象网址时间        
            Log.i(tag, urlDate.toString());
            RemoteDate = urlDate;
            LocalDate = new Date();
        } catch (Exception e) {
            Log.d(tag, e.getMessage());
            e.printStackTrace();
        }
        return urlDate;
    }


    boolean CheckStamp(Date LastStamp) {
        Date Now = new Date();

        long longStart = LastStamp.getTime(); //获取开始时间毫秒数
        long longEnd = Now.getTime();  //获取结束时间毫秒数
        long longExpend = longEnd - longStart;  //获取时间差

        long longMinutes = longExpend / (60 * 1000);   //根据时间差来计算分钟数

        if (longMinutes > checkThreshold) {
            return true;
        } else {
            return false;
        }
    }

    Date GetFixedLocalDate() {
        Date now = new Date();

        long longNow = now.getTime();
        long longRemoteDate = RemoteDate.getTime();
        long longLocalDate = LocalDate.getTime();

        long longFixedNow = longNow - longLocalDate + longRemoteDate;

        now = new Date(longFixedNow);

        return now;
    }
}
