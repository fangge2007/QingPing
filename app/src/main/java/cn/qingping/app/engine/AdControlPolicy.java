package cn.qingping.app.engine;

import java.util.Locale;
import java.util.List;

/** Pure checks shared by the Android adapter and regression tests. No screen text is retained. */
public final class AdControlPolicy {
    private AdControlPolicy() { }
    private static final java.util.regex.Pattern POSITIVE_COUNT = java.util.regex.Pattern.compile("[1-9][0-9]?");

    /**
     * Candidate identifiers verified in MIT ganlinte/GKD-subscription@4746d192b4c57a4b62ed92a7f5deaf79e64fa811.
     * These NEVER supply advertising context by themselves. The adapter must separately require
     * readable ad evidence, a selected source app, enabled visibility and bounded geometry.
     * See docs/SDK_RESEARCH_0.3.md for original per-app conditions and limitations.
     */
    public static boolean isConditionalSdkDismissId(CharSequence viewId, String activity) {
        String id = resourceName(viewId);
        if (id.equals("tt_reward_full_count_down_after_close"))
            return "com.bytedance.sdk.openadsdk.stub.activity.Stub_Standard_Portrait_Activity".equals(activity);
        if (id.equals("ms_activity_sdk_interstitial_cacel"))
            return "com.meishu.sdk.activity.SdkInterstitialActivity".equals(activity);
        if (id.equals("anythink_myoffer_btn_close_id"))
            return "com.anythink.basead.ui.ATPortraitTranslucentActivity".equals(activity);
        return id.equals("ksad_auto_close_btn") || id.equals("jad_feed_close");
    }

    public static boolean hasDismissEvidence(boolean explicit, boolean adSpecificLabel,
                                              boolean adContext, boolean requiresContext) {
        return (!requiresContext || adContext) && (explicit || adSpecificLabel || adContext);
    }

    public static boolean isWithinCandidateScope(boolean explicit, boolean adSpecificLabel, long elapsed) {
        // A normal “Skip” sharing a page with a banner must not become all-day automation.
        return elapsed >= 0 && (explicit || adSpecificLabel || elapsed <= AdDecisionEngine.LAUNCH_WINDOW_MS);
    }

    /** Some SDKs expose “跳” and “过” as sibling leaf labels in a small clickable container. */
    public static String combineDismissLabel(List<String> fragments) {
        if (fragments == null || fragments.size() < 2 || fragments.size() > 5) return "";
        StringBuilder combined = new StringBuilder();
        for (String fragment : fragments) {
            if (fragment == null || fragment.trim().isEmpty() || fragment.length() > 10
                    || isUnsafeAction(fragment, null, null)) return "";
            combined.append(fragment.trim());
            if (combined.length() > 24) return "";
        }
        String label = combined.toString();
        return AdDecisionEngine.isSkipLabel(label) || AdDecisionEngine.isAdCloseLabel(label)
                || isExplicitAdSkipLabel(label) ? label : "";
    }

    /** Waiting wording is not a promise that ACTION_CLICK already dismisses the ad. */
    public static boolean isLockedDismissLabel(CharSequence label) {
        if (label == null || label.length() > 100) return false;
        String value = label.toString().trim().toLowerCase(Locale.ROOT);
        if (!POSITIVE_COUNT.matcher(value).find()) return false;
        return value.contains("后") && (value.contains("关闭") || value.contains("跳过"))
                || value.startsWith("关闭") || value.startsWith("close")
                || value.contains("skip in") || value.contains("close in");
    }

    public static boolean isMotionAdLabel(CharSequence label) {
        if (label == null || label.length() > 100) return false;
        String value = label.toString();
        return value.contains("摇一摇") || value.contains("扭一扭");
    }

    public static boolean isExplicitAdSkipLabel(CharSequence label) {
        if (label == null || label.length() > 48) return false;
        String value = label.toString().trim().toLowerCase(Locale.ROOT);
        return value.equals("跳过视频广告") || value.equals("skip video ad")
                || AdDecisionEngine.isSkipLabel(label) && (value.contains("广告") || value.contains("skip ad"));
    }

    /** Deny acquisition/payment/creative actions even when a custom rule points at them. */
    public static boolean isUnsafeAction(CharSequence text, CharSequence description, CharSequence viewId) {
        return unsafeLabel(text) || unsafeLabel(description) || unsafeId(resourceName(viewId));
    }

    private static boolean unsafeLabel(CharSequence label) {
        if (label == null) return false;
        if (label.length() > 160) return true;
        String value = label.toString().toLowerCase(Locale.ROOT);
        for (String word : new String[]{"下载", "安装", "支付", "购买", "下单", "订阅", "开通", "领取", "继续观看", "打开", "了解更多",
                "download", "install", "purchase", "payment", "buy now", "subscribe", "claim reward", "watch ad", "learn more"}) {
            if (value.contains(word)) return true;
        }
        return false;
    }

    private static boolean unsafeId(String id) {
        String value = id.toLowerCase(Locale.ROOT);
        for (String word : new String[]{"download", "install", "purchase", "payment", "landing", "creative", "ad_click"}) {
            if (value.contains(word)) return true;
        }
        return value.equals("pay") || value.endsWith("_pay") || value.endsWith("_cta");
    }

    private static String resourceName(CharSequence viewId) {
        if (viewId == null || viewId.length() > 260) return "";
        String value = viewId.toString();
        int marker = value.indexOf(":id/");
        return marker > 0 && marker + 4 < value.length() ? value.substring(marker + 4) : "";
    }
}
