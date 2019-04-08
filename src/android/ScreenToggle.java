package cn.lizz.cordova.plugin;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;

import java.util.HashSet;

import org.apache.cordova.CallbackContext;

import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import android.app.Activity;

import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class echoes a string called from JavaScript.
 */
public class ScreenToggle extends CordovaPlugin {

    final String tag = "lizz.ScreenToggle";
    DevicePolicyManager policyManager;
    ComponentName adminReceiver;
    KeyguardManager keyguardManager;
    PowerManager.WakeLock wakeLock;
    PowerManager powerManager;
    KeyguardManager.KeyguardLock keyLock;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        // 获取GetContext
        Activity context = cordova.getActivity();

        powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        keyLock = keyguardManager.newKeyguardLock("unlock");
        wakeLock = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.FULL_WAKE_LOCK,
                "lizz:bright");

        // 获取DevicePolicyManager
        policyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        // 获取ComponentName
        adminReceiver = new ComponentName(context, ScreenOffAdminReceiver.class);
        // 判断是否激活设备管理器
        if (!policyManager.isAdminActive(adminReceiver)) {
            Toast.makeText(context, "初始化检查：请求授权设备管理", Toast.LENGTH_LONG).show();
            requestDeviceAdmin();
        } else {
            Toast.makeText(context, "初始化检查：设备已被激活", Toast.LENGTH_LONG).show();
            // todo 开启服务
            StartService();
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("open")) {
            this.open(callbackContext);
            return true;
        } else if (action.equals("close")) {
            this.close(callbackContext);
            return true;
        } else if (action.equals("enable")) {
            String message = args.getString(0);
            this.enable(callbackContext);
            return true;
        } else if (action.equals("disable")) {
            String message = args.getString(0);
            this.disable(callbackContext);
            return true;
        } else if (action.equals("config")) {
            JSONArray opens = args.getJSONArray(0);
            JSONArray closes = args.getJSONArray(1);
            this.config(opens, closes, callbackContext);
            return true;
        }
        return false;
    }

    private void open(CallbackContext callbackContext) {
        try {
            turnOnScreen();
            boolean isScreenOn = powerManager.isScreenOn();
            callbackContext.success(isScreenOn + "");
        } catch (Exception e) {
            callbackContext.error(e.getMessage());
        }
    }

    private void close(CallbackContext callbackContext) {
        try {
            turnOffScreen();
            boolean isScreenOn = powerManager.isScreenOn();
            callbackContext.success(isScreenOn + "");
        } catch (Exception e) {
            callbackContext.error(e.getMessage());
        }
    }

    private void enable(CallbackContext callbackContext) {
        StartService();
    }

    private void disable(CallbackContext callbackContext) {
        StopService();
    }

    private void config(JSONArray opens, JSONArray closes, CallbackContext callbackContext) {

        HashSet<String> openSet = new HashSet<String>();
        HashSet<String> closeSet = new HashSet<String>();

        for (int i = 0; i < opens.length(); i++) {
            try {
                String open = opens.getString(i);
                Log.d(tag + "-open", open);
                openSet.add(open);
            } catch (Exception e) {
                callbackContext.error(e.getMessage());
            }
        }

        for (int i = 0; i < closes.length(); i++) {
            try {
                String close = closes.getString(i);
                Log.d(tag + "-close", close);
                closeSet.add(close);
            } catch (Exception e) {
                callbackContext.error(e.getMessage());
            }
        }

        Activity context = cordova.getActivity();

        SharedPreferences preferences = context.getSharedPreferences("Config", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putStringSet("open", openSet);
        editor.putStringSet("close", closeSet);

        editor.commit();

        callbackContext.success("" + true);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Activity context = cordova.getActivity();
        if (policyManager.isAdminActive(adminReceiver)) {// 判断超级管理员是否激活
            Toast.makeText(context, "设备已被激活", Toast.LENGTH_LONG).show();

            // todo 开启服务
            StartService();
        } else {
            Toast.makeText(context, "设备没有被激活", Toast.LENGTH_LONG).show();
        }
    }

    public void requestDeviceAdmin() {
        Intent policyIntent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        policyIntent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminReceiver);
        policyIntent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "开启后就可以使用锁屏功能了...");
        this.cordova.startActivityForResult((CordovaPlugin) this, policyIntent, 0);
    }

    public void StartService() {
        Activity context = cordova.getActivity();

        Intent intent = new Intent(context, BackGroundService.class);

        context.startService(intent);

        Log.v(tag, "start Service");
    }

    public void StopService() {
        Activity context = cordova.getActivity();

        Intent intent = new Intent(context, BackGroundService.class);

        context.stopService(intent);

        Log.v(tag, "stop Service");
    }

    public void turnOnScreen() {
        Log.v(tag, "ON!");

        keyLock.disableKeyguard();

        wakeLock.acquire();
        wakeLock.release();
    }

    public void turnOffScreen() {
        Log.v(tag, "Off!");
        policyManager.lockNow();
    }
}
