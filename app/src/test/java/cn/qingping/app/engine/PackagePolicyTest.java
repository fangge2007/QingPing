package cn.qingping.app.engine;

import cn.qingping.app.rules.ExactRule;
import org.junit.Test;
import static org.junit.Assert.*;

public class PackagePolicyTest {
    @Test public void importsAndRuntimeUseTheSameCriticalPackagePolicy() {
        for (String pkg : new String[]{null, "", "android", "cn.qingping.app", "com.android.phone",
                "com.android.server.telecom", "com.android.emergency", "com.google.android.dialer",
                "vendor.emergency.service", "com.android.settings", "com.android.systemui",
                "vendor.launcher", "vendor.inputmethod", "vendor.packageinstaller", "vendor.permissioncontroller"}) {
            assertTrue(String.valueOf(pkg), PackagePolicy.isCritical(pkg));
            assertTrue(String.valueOf(pkg), ExactRule.isCriticalPackage(pkg));
        }
        assertFalse(PackagePolicy.isCritical("com.example.reader"));
    }

    @Test public void destructiveAndTelephonyActionsAreVetoed() {
        for (String label : new String[]{"删除", "清空记录", "拨号", "转账", "卸载", "呼叫", "恢复出厂设置",
                "Delete", "Clear all", "Dial", "Call now", "Transfer money", "Uninstall", "Reset device"}) {
            assertTrue(label, AdControlPolicy.isUnsafeAction(label, null, null));
            assertTrue(label, AdControlPolicy.isUnsafeAction("关闭广告", label, null));
        }
        for (String id : new String[]{"btn_delete", "clear_data", "dial", "btn_call", "transfer", "uninstall"})
            assertTrue(id, AdControlPolicy.isUnsafeAction(null, null, "example.app:id/" + id));
        assertFalse(AdControlPolicy.isUnsafeAction("Close dialog", null, "example.app:id/dialog_close"));
    }
}
