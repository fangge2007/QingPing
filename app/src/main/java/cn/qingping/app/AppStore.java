package cn.qingping.app;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.SystemClock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Collections;
import cn.qingping.app.rules.ExactRule;

/** Local settings and aggregate counts only. Never stores accessibility text. */
public final class AppStore {
    private final SharedPreferences prefs;
    private final CountsDb db;
    public AppStore(Context context) {
        Context app = context.getApplicationContext();
        prefs = app.getSharedPreferences("qingping", Context.MODE_PRIVATE);
        db = CountsDb.get(app);
    }
    public SharedPreferences preferences() { return prefs; }
    public boolean hasConsent() { return prefs.getBoolean("consent", false) && prefs.getInt("consent_version", 0) >= 4; }
    public boolean isProtectionEnabled() { return prefs.getBoolean("protection", false); }
    public void setConsent(boolean value) {
        SharedPreferences.Editor edit = prefs.edit().putBoolean("consent", value).putInt("consent_version", value ? 4 : 0);
        if (!value) edit.putBoolean("protection", false);
        edit.apply();
    }
    public void setProtectionEnabled(boolean value) { prefs.edit().putBoolean("protection", value).apply(); }
    public boolean isAppEnabled(String pkg) { return !ExactRule.isCriticalPackage(pkg) && prefs.getBoolean("enabled." + pkg, false); }
    public void setAppEnabled(String pkg, boolean value) {
        if (value && ExactRule.isCriticalPackage(pkg)) throw new IllegalArgumentException("系统关键应用不能作为来源");
        prefs.edit().putBoolean("enabled." + pkg, value).apply();
    }
    public Rule getRule(String pkg) {
        String target = prefs.getString("target." + pkg, "");
        if (ExactRule.isCriticalPackage(target)) target = "";
        return new Rule(prefs.getBoolean("skip." + pkg, true), target, prefs.getString("id." + pkg, ""));
    }
    public void setRule(String pkg, boolean skip, String target, String id) {
        if (ExactRule.isCriticalPackage(pkg) || !target.isEmpty() && ExactRule.isCriticalPackage(target))
            throw new IllegalArgumentException("系统关键应用不能配置自动动作");
        prefs.edit().putBoolean("skip." + pkg, skip).putString("target." + pkg, target)
            .putString("id." + pkg, id).apply();
    }
    public boolean isGestureAllowed(String pkg) { return prefs.getBoolean("gesture." + pkg, false); }
    public void setGestureAllowed(String pkg, boolean value) { prefs.edit().putBoolean("gesture." + pkg, value).apply(); }
    private String cachedDocument;
    private List<ExactRule> cachedRules = Collections.emptyList();
    public List<ExactRule> exactRules() {
        String document = prefs.getString("exact_rules", "");
        if (!document.equals(cachedDocument)) {
            try { cachedRules = document.isEmpty() ? Collections.emptyList() : ExactRule.parseDocument(document); }
            catch (IllegalArgumentException ex) { cachedRules = Collections.emptyList(); }
            cachedDocument = document;
        }
        return new ArrayList<>(cachedRules);
    }
    public List<ExactRule> exactRules(String pkg) {
        List<ExactRule> result = new ArrayList<>();
        for (ExactRule r : exactRules()) if (r.packageName.equals(pkg)) result.add(r);
        return result;
    }
    public void setExactRules(List<ExactRule> rules) {
        prefs.edit().putString("exact_rules", ExactRule.serializeDocument(rules)).apply();
    }
    public int scanWindowSeconds(String pkg) {
        int seconds = 15;
        for (ExactRule r : exactRules(pkg)) seconds = Math.max(seconds, r.windowSeconds);
        return seconds;
    }
    public List<String> selectedPackages() {
        TreeSet<String> selected = new TreeSet<>();
        for (Map.Entry<String, ?> e : prefs.getAll().entrySet())
            if (e.getKey().startsWith("enabled.") && Boolean.TRUE.equals(e.getValue())
                    && !ExactRule.isCriticalPackage(e.getKey().substring(8))) selected.add(e.getKey().substring(8));
        return new ArrayList<>(selected);
    }
    public void setServiceConnected(boolean value) { prefs.edit().putBoolean("last_connected", value).apply(); }
    public void setLastServiceEvent(long time) { prefs.edit().putLong("last_event", time).apply(); }
    public long getLastServiceEvent() { return prefs.getLong("last_event", 0); }
    public long getAllowUntilElapsed() {
        // The service and UI share a process; elapsed time is never persisted across reboot.
        return allowUntilElapsed;
    }
    private static volatile long allowUntilElapsed;
    public void allowJumpsForTenMinutes() { allowUntilElapsed = SystemClock.elapsedRealtime() + 600_000; }
    public void endJumpAllowance() { allowUntilElapsed = 0; }
    public boolean bootReminderEnabled() { return prefs.getBoolean("boot_reminder", false); }
    public void setBootReminderEnabled(boolean value) { prefs.edit().putBoolean("boot_reminder", value).apply(); }
    public void recordConfirmed(String pkg, String kind) {
        if (!"skip".equals(kind) && !"return".equals(kind)) return;
        SQLiteDatabase sql = db.getWritableDatabase();
        String day = LocalDate.now().toString();
        sql.beginTransaction();
        try {
            ContentValues row = new ContentValues();
            row.put("pkg", pkg); row.put("kind", kind); row.put("day", day); row.put("n", 0);
            sql.insertWithOnConflict("counts", null, row, SQLiteDatabase.CONFLICT_IGNORE);
            sql.execSQL("UPDATE counts SET n = n + 1 WHERE pkg = ? AND kind = ? AND day = ?", new Object[]{pkg, kind, day});
            sql.setTransactionSuccessful();
        } finally { sql.endTransaction(); }
    }
    public List<Stat> stats(int days) {
        List<Stat> rows = new ArrayList<>();
        String selection = days > 0 ? " WHERE day >= ?" : "";
        String[] args = days > 0 ? new String[]{LocalDate.now().minusDays(days - 1L).toString()} : null;
        try (Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT pkg, SUM(CASE WHEN kind = 'skip' THEN n ELSE 0 END), SUM(CASE WHEN kind = 'return' THEN n ELSE 0 END) FROM counts" + selection + " GROUP BY pkg ORDER BY SUM(n) DESC, pkg", args)) {
            while (c.moveToNext()) rows.add(new Stat(c.getString(0), c.getLong(1), c.getLong(2)));
        }
        return rows;
    }
    public long total(int days) { long total = 0; for (Stat s : stats(days)) total += s.skip + s.returned; return total; }
    public void clearStats() { db.getWritableDatabase().delete("counts", null, null); }
    public String csv() {
        StringBuilder out = new StringBuilder("\uFEFF日期,APP包名,类型,确认次数\r\n");
        try (Cursor c = db.getReadableDatabase().rawQuery("SELECT day,pkg,kind,n FROM counts ORDER BY day DESC,pkg,kind", null)) {
            while (c.moveToNext()) out.append(c.getString(0)).append(',').append(csvCell(c.getString(1))).append(',')
                .append("skip".equals(c.getString(2)) ? "广告跳过" : "跳转返回").append(',').append(c.getLong(3)).append("\r\n");
        }
        return out.toString();
    }
    private static String csvCell(String s) { return "\"" + s.replace("\"", "\"\"") + "\""; }
    public static final class Rule {
        public final boolean skipEnabled;
        public final String guardTarget, skipViewId;
        public Rule(boolean skip, String target, String id) { skipEnabled = skip; guardTarget = target; skipViewId = id; }
    }
    public static final class Stat {
        public final String pkg;
        public final long skip, returned;
        public Stat(String p, long s, long r) { pkg = p; skip = s; returned = r; }
    }
    private static final class CountsDb extends SQLiteOpenHelper {
        private static CountsDb instance;
        static synchronized CountsDb get(Context c) { if (instance == null) instance = new CountsDb(c); return instance; }
        private CountsDb(Context c) { super(c, "statistics.db", null, 1); }
        @Override public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE counts(pkg TEXT NOT NULL, kind TEXT NOT NULL, day TEXT NOT NULL, n INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(pkg,kind,day))");
        }
        @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { /* v1 */ }
    }
}
