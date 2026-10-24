package cn.qingping.app.engine;

import java.util.Locale;

/** Shared floor for configuration, imports, event filters and action-time checks. */
public final class PackagePolicy {
    private PackagePolicy() { }

    public static boolean isCritical(String pkg) {
        if (pkg == null || pkg.isEmpty()) return true;
        String lower = pkg.toLowerCase(Locale.ROOT);
        return lower.equals("android") || lower.equals("cn.qingping.app")
                || lower.equals("com.android.settings") || lower.equals("com.android.systemui")
                || lower.equals("com.android.phone") || lower.equals("com.android.server.telecom")
                || lower.contains("emergency") || lower.contains("dialer")
                || lower.contains("permissioncontroller") || lower.contains("packageinstaller")
                || lower.contains("launcher") || lower.contains("inputmethod");
    }
}
