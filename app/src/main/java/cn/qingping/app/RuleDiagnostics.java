package cn.qingping.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.os.Handler;
import android.os.Looper;

/** Explicit, short-lived local diagnostics. Never captures a full tree or a screenshot. */
public final class RuleDiagnostics {
    private RuleDiagnostics() { }
    private static String armedPackage = "";
    private static long armedUntil;
    private static Report report;
    private static final Handler expiry = new Handler(Looper.getMainLooper());
    private static final Runnable erase = RuleDiagnostics::clear;
    public static synchronized void arm(String pkg, long now) {
        expiry.removeCallbacks(erase);
        armedPackage = pkg; armedUntil = now + 30_000; report = null;
    }
    public static synchronized boolean armed(String pkg, long now) {
        return pkg.equals(armedPackage) && now <= armedUntil;
    }
    public static synchronized void publish(String pkg, String activity, int nodes, boolean complete,
                                            boolean ad, String status, List<Control> controls, long now) {
        if (!armed(pkg, now)) return;
        if (report != null && report.pkg.equals(pkg) && !report.controls.isEmpty() && controls.isEmpty()) return;
        report = new Report(pkg, activity, nodes, complete, ad, status, controls, now);
        expiry.removeCallbacks(erase);
        expiry.postDelayed(erase, 120_000);
    }
    public static synchronized Report report(String pkg, long now) {
        if (report != null && now - report.time > 120_000) clear();
        if (report == null || !pkg.equals(report.pkg)) return null;
        return report;
    }
    public static synchronized void clear() { armedPackage = ""; armedUntil = 0; report = null; expiry.removeCallbacks(erase); }
    public static final class Control {
        public final String viewId, label, description;
        public final boolean clickable;
        public Control(String id, String label, String description, boolean clickable) {
            this.viewId = id == null ? "" : id; this.label = label; this.description = description; this.clickable = clickable;
        }
    }
    public static final class Report {
        public final String pkg, activity, status;
        public final int nodes;
        public final boolean complete, ad;
        public final List<Control> controls;
        public final long time;
        Report(String pkg, String activity, int nodes, boolean complete, boolean ad, String status, List<Control> controls, long time) {
            this.pkg = pkg; this.activity = activity; this.nodes = nodes; this.complete = complete;
            this.ad = ad; this.status = status; this.controls = Collections.unmodifiableList(new ArrayList<>(controls)); this.time = time;
        }
    }
}
