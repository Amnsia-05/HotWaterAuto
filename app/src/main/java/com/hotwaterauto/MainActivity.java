package com.hotwaterauto;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_OVERLAY_PERMISSION = 1001;
    private Button btnStart;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = findViewById(R.id.btn_start);
        tvStatus = findViewById(R.id.tv_status);

        btnStart.setOnClickListener(v -> startAutomation());
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissionsAndUpdateUI();
    }

    private void checkPermissionsAndUpdateUI() {
        boolean accessibilityEnabled = isAccessibilityServiceEnabled();
        boolean overlayGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);

        if (!accessibilityEnabled) {
            tvStatus.setText("状态：❌ 未开启无障碍服务\n请先前往设置中开启");
            btnStart.setEnabled(false);
            promptEnableAccessibility();
        } else if (!overlayGranted) {
            tvStatus.setText("状态：❌ 未开启悬浮窗权限");
            btnStart.setEnabled(false);
            requestOverlayPermission();
        } else {
            tvStatus.setText("状态：✅ 就绪，点击下方按钮启动");
            btnStart.setEnabled(true);
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        String service = getPackageName() + "/.HotWaterAutomationService";
        try {
            String enabledServices = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            return enabledServices != null && enabledServices.contains(service);
        } catch (Exception e) {
            return false;
        }
    }

    private void promptEnableAccessibility() {
        new AlertDialog.Builder(this)
                .setTitle("需要无障碍服务")
                .setMessage("请前往设置 → 无障碍 → 已安装的服务 → 开启「热水自动启动」服务")
                .setPositiveButton("前往设置", (dialog, which) -> {
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        }
    }

    private void startAutomation() {
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_SHORT).show();
            promptEnableAccessibility();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请先开启悬浮窗权限", Toast.LENGTH_SHORT).show();
            requestOverlayPermission();
            return;
        }

        tvStatus.setText("状态：🔄 正在启动趣智校园...");
        btnStart.setEnabled(false);

        Intent serviceIntent = new Intent(this, HotWaterAutomationService.class);
        serviceIntent.setAction(HotWaterAutomationService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        Intent intent = new Intent();
        intent.setClassName("com.klcxkj.zqxy", "com.klcxkj.zqxy.SplashActivity");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
            Toast.makeText(this, "已启动趣智校园，自动化即将开始", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            try {
                intent.setClassName("com.klcxkj.zqxy", "com.klcxkj.zqxy.MainActivity");
                startActivity(intent);
            } catch (Exception e2) {
                Toast.makeText(this, "未找到趣智校园，请确认已安装", Toast.LENGTH_LONG).show();
                tvStatus.setText("状态：❌ 未找到趣智校园 App");
                btnStart.setEnabled(true);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            checkPermissionsAndUpdateUI();
        }
    }
}
