package cn.qingping.app.engine;

import org.junit.Test;
import static org.junit.Assert.*;

public class AdControlPolicyTest {
    private static final String BYTE_ACTIVITY = "com.bytedance.sdk.openadsdk.stub.activity.Stub_Standard_Portrait_Activity";

    @Test public void splitCaptionsRequireExactDismissalAndRejectMixedBusinessActions() {
        assertEquals("跳过", AdControlPolicy.combineDismissLabel(java.util.Arrays.asList("跳", "过")));
        assertEquals("跳过广告5", AdControlPolicy.combineDismissLabel(java.util.Arrays.asList("跳", "过", "广告", "5")));
        assertEquals("关闭广告", AdControlPolicy.combineDismissLabel(java.util.Arrays.asList("关闭", "广告")));
        assertEquals("", AdControlPolicy.combineDismissLabel(java.util.Arrays.asList("跳过", "下载")));
        assertEquals("", AdControlPolicy.combineDismissLabel(java.util.Arrays.asList("跳过", "教程")));
        assertEquals("", AdControlPolicy.combineDismissLabel(java.util.Arrays.asList("关闭", "账户")));
        assertEquals("", AdControlPolicy.combineDismissLabel(java.util.Arrays.asList("跳过", "", "广告")));
        assertEquals("", AdControlPolicy.combineDismissLabel(java.util.Arrays.asList("1", "2", "3", "4", "跳", "过")));
        // Bare composed Skip remains launch-only and still requires independent advertising evidence.
        assertFalse(AdControlPolicy.isExplicitAdSkipLabel("跳过"));
    }

    @Test public void videoCloseRequiresVerifiedActivityAndIndependentEvidence() {
        String id = "com.byted.pangle:id/tt_reward_full_count_down_after_close";
        assertTrue(AdControlPolicy.isConditionalSdkDismissId(id, BYTE_ACTIVITY));
        assertFalse(AdControlPolicy.isConditionalSdkDismissId(id, "cn.example.MainActivity"));
        assertFalse(AdControlPolicy.hasDismissEvidence(true, false, false, true));
        assertTrue(AdControlPolicy.hasDismissEvidence(true, false, true, true));
        assertFalse(AdControlPolicy.isConditionalSdkDismissId("sdk.vendor:id/tt_reward_full_count_down_after", BYTE_ACTIVITY));
    }

    @Test public void otherFamiliesMatchExactIdsAndRequiredActivities() {
        assertTrue(AdControlPolicy.isConditionalSdkDismissId("sdk.vendor:id/ksad_auto_close_btn", "cn.example.MainActivity"));
        assertTrue(AdControlPolicy.isConditionalSdkDismissId("sdk.vendor:id/jad_feed_close", "cn.example.MainActivity"));
        assertTrue(AdControlPolicy.isConditionalSdkDismissId("sdk.vendor:id/ms_activity_sdk_interstitial_cacel", "com.meishu.sdk.activity.SdkInterstitialActivity"));
        assertFalse(AdControlPolicy.isConditionalSdkDismissId("sdk.vendor:id/ms_activity_sdk_interstitial_cacel", BYTE_ACTIVITY));
        assertTrue(AdControlPolicy.isConditionalSdkDismissId("sdk.vendor:id/anythink_myoffer_btn_close_id", "com.anythink.basead.ui.ATPortraitTranslucentActivity"));
        assertFalse(AdControlPolicy.isConditionalSdkDismissId("sdk.vendor:id/anythink_myoffer_btn_close_id", "cn.example.MainActivity"));
        for (String id : new String[]{"close", "iv_close", "tt_item_tv", "download", "jad_feed_close_extra", "ksad_auto_close_btn"})
            assertFalse(AdControlPolicy.isConditionalSdkDismissId(id, BYTE_ACTIVITY));
    }

    @Test public void explicitAdvertisingWordingHasNoBusinessCountdownShortcut() {
        for (String label : new String[]{"跳过广告", "跳过视频广告", "Skip ad", "skip video ad", "跳过广告 5s"})
            assertTrue(label, AdControlPolicy.isExplicitAdSkipLabel(label));
        for (String label : new String[]{"跳过", "Skip 3", "跳过 5", "跳过广告并下载", "跳过教程", "×"})
            assertFalse(label, AdControlPolicy.isExplicitAdSkipLabel(label));
        assertFalse(AdControlPolicy.hasDismissEvidence(false, false, false, false));
        assertTrue(AdControlPolicy.hasDismissEvidence(false, true, false, false));
    }

    @Test public void dangerousLabelsAndIdsVetoEvenExactRules() {
        for (String label : new String[]{"跳过广告并下载", "立即安装", "支付", "购买会员", "领取奖励", "继续观看", "Download now", "Learn more"}) {
            assertTrue(label, AdControlPolicy.isUnsafeAction(label, null, null));
            assertTrue(label, AdControlPolicy.isUnsafeAction("跳过广告", label, null));
        }
        for (String id : new String[]{"btn_download", "ad_install", "payment", "ad_creative", "jump_landing", "ad_click"})
            assertTrue(AdControlPolicy.isUnsafeAction(null, null, "sdk.vendor:id/" + id));
        assertFalse(AdControlPolicy.isUnsafeAction("关闭广告", "关闭广告", "com.bytedance.sdk.openadsdk:id/tt_reward_full_count_down_after_close"));
    }

    @Test public void businessSkipNextToBannerHasNoPersistentScope() {
        assertTrue(AdControlPolicy.isWithinCandidateScope(false, false, 15_000));
        assertFalse(AdControlPolicy.isWithinCandidateScope(false, false, 15_001));
        assertTrue(AdControlPolicy.isWithinCandidateScope(true, false, 120_000));
        assertTrue(AdControlPolicy.isWithinCandidateScope(false, true, 120_000));
        assertFalse(AdControlPolicy.isWithinCandidateScope(true, true, -1));
    }

    @Test public void countdownWaitsForUnlockButImmediateSkipCountdownRemainsEligible() {
        for (String label : new String[]{"5秒后可关闭", "关闭广告（3）", "Skip in 10 seconds", "2s后跳过广告", "Close in 5"})
            assertTrue(label, AdControlPolicy.isLockedDismissLabel(label));
        for (String label : new String[]{"跳过 5s", "关闭广告", "关闭(0)", "Skip ad", "0秒后关闭"})
            assertFalse(label, AdControlPolicy.isLockedDismissLabel(label));
    }
}
