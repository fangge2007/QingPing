package cn.qingping.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

/** Does not start or bind the accessibility service; only Android can bind it. */
public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;
        AppStore store = new AppStore(context);
        if (!store.hasConsent() || !store.isProtectionEnabled() || !store.bootReminderEnabled()) return;
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("recovery", "开机恢复检查", NotificationManager.IMPORTANCE_DEFAULT));
        PendingIntent open = PendingIntent.getActivity(context, 1, new Intent(context, MainActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        manager.notify(1, new Notification.Builder(context, "recovery").setSmallIcon(R.drawable.ic_qingping)
            .setContentTitle("清屏 · 开机恢复检查").setContentText("点击查看无障碍服务状态。系统可能稍后连接服务。")
            .setStyle(new Notification.BigTextStyle().bigText("无障碍服务由系统恢复。若厂商清理或强行停止使服务失效，请打开清屏查看设置。"))
            .setContentIntent(open).setAutoCancel(true).build());
    }
}
