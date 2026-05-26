package com.hotwaterauto;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class HotWaterAutomationService extends AccessibilityService {

    private static final String TAG = "HotWaterAuto";
    private static final String TARGET_PACKAGE = "com.klcxkj.zqxy";
    private static final long STEP_DELAY = 1500;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private static HotWaterAutomationService instance;
    private boolean isRunning = false;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 5;

    public static final String ACTION_START = "com.hotwaterauto.START";
    public static final String ACTION_STOP = "com.hotwaterauto.STOP";

    public static HotWaterAutomationService getInstance() {
        return instance;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isRunning) return;
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return;
        }

        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
        if (!packageName.equals(TARGET_PACKAGE)) return;

        Log.d(TAG, "趣智校园界面变化: " + event.getClassName());
        retryCount = 0;
        tryStartHotWater();
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "服务被中断");
        isRunning = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (ACTION_START.equals(intent.getAction())) {
                startAutomation();
            } else if (ACTION_STOP.equals(intent.getAction())) {
                stopAutomation();
            }
        }
        return START_STICKY;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.d(TAG, "无障碍服务已连接");

        AccessibilityServiceInfo info = getServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            info.flags |= AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        }
        setServiceInfo(info);
    }

    public void startAutomation() {
        Log.d(TAG, "开始自动化流程");
        isRunning = true;
        retryCount = 0;
    }

    public void stopAutomation() {
        Log.d(TAG, "停止自动化流程");
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
    }

    private void tryStartHotWater() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            retryCount++;
            if (retryCount < MAX_RETRIES) {
                handler.postDelayed(this::tryStartHotWater, STEP_DELAY);
            }
            return;
        }

        boolean clicked = false;

        clicked = clickButtonByText(root, "热水") || clicked;
        if (!clicked) clicked = clickButtonByText(root, "开始洗澡") || clicked;
        if (!clicked) clicked = clickButtonByText(root, "启动用水") || clicked;
        if (!clicked) clicked = clickButtonByText(root, "开始") || clicked;
        if (!clicked) clicked = clickButtonByText(root, "打开") || clicked;
        if (!clicked) clicked = clickButtonByText(root, "淋浴") || clicked;
        if (!clicked) clicked = clickButtonByText(root, "洗浴") || clicked;
        if (!clicked) clicked = clickButtonByDesc(root, "热水") || clicked;
        if (!clicked) clicked = clickButtonByDesc(root, "开始") || clicked;

        if (!clicked) {
            if (retryCount < MAX_RETRIES) {
                retryCount++;
                handler.postDelayed(this::tryStartHotWater, STEP_DELAY);
            }
        } else {
            isRunning = false;
        }

        root.recycle();
    }

    private boolean clickButtonByText(AccessibilityNodeInfo node, String text) {
        if (node == null) return false;

        List<AccessibilityNodeInfo> nodes = node.findAccessibilityNodeInfosByText(text);
        if (nodes != null) {
            for (AccessibilityNodeInfo n : nodes) {
                if (n.isClickable()) {
                    n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    Log.d(TAG, "点击了文字按钮: " + text);
                    return true;
                }
                AccessibilityNodeInfo parent = n.getParent();
                while (parent != null) {
                    if (parent.isClickable()) {
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.d(TAG, "点击了父级按钮(文字): " + text);
                        return true;
                    }
                    parent = parent.getParent();
                }
            }
        }

        if (node.getChildCount() == 0) return false;

        for (int i = 0; i < node.getChildCount(); i++) {
            if (clickButtonByText(node.getChild(i), text)) return true;
        }

        return false;
    }

    private boolean clickButtonByDesc(AccessibilityNodeInfo node, String desc) {
        if (node == null) return false;

        if (node.getContentDescription() != null
                && node.getContentDescription().toString().contains(desc)
                && node.isClickable()) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Log.d(TAG, "点击了描述按钮: " + desc);
            return true;
        }

        if (node.getChildCount() == 0) return false;

        for (int i = 0; i < node.getChildCount(); i++) {
            if (clickButtonByDesc(node.getChild(i), desc)) return true;
        }

        return false;
    }

    @Override
    public void onDestroy() {
        instance = null;
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
        return super.onUnbind(intent);
    }
}
