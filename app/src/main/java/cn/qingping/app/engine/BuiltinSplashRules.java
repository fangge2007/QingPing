package cn.qingping.app.engine;

import java.util.ArrayList;
import java.util.List;

/** Source-isolated facts from MIT ganlinte/GKD-subscription@4746d192b4c57a4b62ed92a7f5deaf79e64fa811. */
public final class BuiltinSplashRules {
    private BuiltinSplashRules() { }
    private static final String[][] IDS = {
            {"tv.danmaku.bili", "tv.danmaku.bili:id/count_down"},
            {"tv.danmaku.bili", "tv.danmaku.bili:id/skip"},
            {"tv.danmaku.bilibilihd", "tv.danmaku.bilibilihd:id/count_down"},
            {"com.zhihu.android", "com.zhihu.android:id/btn_skip"},
            {"com.ct.client", "com.ct.client:id/btSkip"},
            {"com.dubox.drive", "com.dubox.drive:id/tv_skip"},
            {"com.huawei.himovie", "com.huawei.hms.ads.uiengine:id/hiad_skip_text"},
            {"cn.damai", "cn.damai:id/homepage_advert_pb"},
            {"com.UCMobile", "com.byted.pangle:id/tt_splash_skip_btn"}
    };

    /** Only these exact IDs are eligible for early, bounded direct queries. */
    public static List<String> queryIds(String sourcePackage) {
        List<String> result = new ArrayList<>();
        for (String[] row : IDS) if (row[0].equals(sourcePackage)) result.add(row[1]);
        return result;
    }

    public static boolean matches(String sourcePackage, String viewId, CharSequence text,
                                  CharSequence description, long elapsed) {
        if (elapsed < 0 || AdControlPolicy.isUnsafeAction(text, description, viewId)) return false;
        boolean exactId = false;
        for (String[] row : IDS) if (row[0].equals(sourcePackage) && row[1].equals(viewId)) { exactId = true; break; }
        if (!exactId) return false;
        // Bilibili may mount a splash after returning from a widget/video within the same app.
        // Other catalog entries retain a short entry scope. Geometry and input checks live in the adapter.
        boolean lateBili = "tv.danmaku.bili".equals(sourcePackage) || "tv.danmaku.bilibilihd".equals(sourcePackage);
        // A rendered caption can be absent from accessibility text. This narrow ID-only
        // fallback is limited to Bilibili's verified IDs during entry; never global id/skip.
        boolean emptyBiliEntry = lateBili && elapsed <= AdDecisionEngine.LAUNCH_WINDOW_MS
                && (text == null || text.toString().trim().isEmpty())
                && (description == null || description.toString().trim().isEmpty());
        return (elapsed <= 30_000 || lateBili)
                && (AdDecisionEngine.isSkipLabel(text) || AdDecisionEngine.isSkipLabel(description) || emptyBiliEntry);
    }
}
