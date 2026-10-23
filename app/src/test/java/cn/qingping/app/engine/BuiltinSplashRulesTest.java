package cn.qingping.app.engine;

import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

public class BuiltinSplashRulesTest {
    @Test public void bilibiliWithoutReadableAdBadgeMatchesVerifiedFullIds() {
        assertTrue(BuiltinSplashRules.matches("tv.danmaku.bili", "tv.danmaku.bili:id/count_down", "跳过 5", null, 200));
        assertTrue(BuiltinSplashRules.matches("tv.danmaku.bili", "tv.danmaku.bili:id/skip", "跳过", null, 200));
        assertTrue(BuiltinSplashRules.matches("tv.danmaku.bilibilihd", "tv.danmaku.bilibilihd:id/count_down", "跳过", null, 200));
    }
    @Test public void sourceAndCompleteIdAreBothRequired() {
        assertFalse(BuiltinSplashRules.matches("other.app", "tv.danmaku.bili:id/skip", "跳过", null, 200));
        assertFalse(BuiltinSplashRules.matches("tv.danmaku.bili", "other.app:id/skip", "跳过", null, 200));
        assertFalse(BuiltinSplashRules.matches("tv.danmaku.bili", "tv.danmaku.bili:id/skip_extra", "跳过", null, 200));
        assertFalse(BuiltinSplashRules.matches("tv.danmaku.bili", "skip", "跳过", null, 200));
    }
    @Test public void lateBiliSplashIsNotLostAfterWidgetOrVideoReturnsHome() {
        assertTrue(BuiltinSplashRules.matches("tv.danmaku.bili", "tv.danmaku.bili:id/count_down", "跳过", null, 120_000));
        assertFalse(BuiltinSplashRules.matches("com.zhihu.android", "com.zhihu.android:id/btn_skip", "跳过", null, 30_001));
        assertFalse(BuiltinSplashRules.matches("tv.danmaku.bili", "tv.danmaku.bili:id/skip", "跳过", null, -1));
    }
    @Test public void normalVideoAndDangerousBusinessCaptionsAreRejectedEvenWithKnownId() {
        for (String label : new String[]{"跳过片头", "跳过登录", "跳过教程", "跳过广告并下载", "点击跳过广告领取奖励", "3"})
            assertFalse(label, BuiltinSplashRules.matches("tv.danmaku.bili", "tv.danmaku.bili:id/skip", label, null, 200));
        assertFalse(BuiltinSplashRules.matches("tv.danmaku.bili", "tv.danmaku.bili:id/skip", "跳过", "立即下载", 200));
    }
    @Test public void catalogMatchesVerifiedOtherHostsAndSdkNamespaces() {
        String[][] entries = {{"com.zhihu.android", "com.zhihu.android:id/btn_skip"},
                {"com.ct.client", "com.ct.client:id/btSkip"}, {"com.dubox.drive", "com.dubox.drive:id/tv_skip"},
                {"com.huawei.himovie", "com.huawei.hms.ads.uiengine:id/hiad_skip_text"},
                {"cn.damai", "cn.damai:id/homepage_advert_pb"}, {"com.UCMobile", "com.byted.pangle:id/tt_splash_skip_btn"}};
        for (String[] row : entries) assertTrue(row[0], BuiltinSplashRules.matches(row[0], row[1], "跳过", null, 200));
        assertFalse(BuiltinSplashRules.matches("com.ct.client", "com.ct.client:id/tvSkip", "跳过", null, 200));
    }
    @Test public void descriptionCanIdentifySkipButLateEmptySemanticsCannot() {
        assertTrue(BuiltinSplashRules.matches("tv.danmaku.bili", "tv.danmaku.bili:id/skip", null, "跳过 5", 200));
        assertFalse(BuiltinSplashRules.matches("tv.danmaku.bili", "tv.danmaku.bili:id/skip", null, null, 15_001));
    }
    @Test public void emptyCaptionFallbackIsLimitedToVerifiedBiliIdsAndEntryWindow() {
        assertTrue(BuiltinSplashRules.matches("tv.danmaku.bili", "tv.danmaku.bili:id/count_down", null, null, 200));
        assertTrue(BuiltinSplashRules.matches("tv.danmaku.bilibilihd", "tv.danmaku.bilibilihd:id/count_down", "", "", 15_000));
        assertFalse(BuiltinSplashRules.matches("com.zhihu.android", "com.zhihu.android:id/btn_skip", null, null, 200));
        assertFalse(BuiltinSplashRules.matches("other.app", "tv.danmaku.bili:id/skip", null, null, 200));
        assertFalse(BuiltinSplashRules.matches("tv.danmaku.bili", "tv.danmaku.bili:id/skip", "跳过片头", null, 200));
    }
    @Test public void fastQueryCatalogIsIsolatedAndDoesNotExposeMutableState() {
        assertEquals(2, BuiltinSplashRules.queryIds("tv.danmaku.bili").size());
        assertTrue(BuiltinSplashRules.queryIds("other.app").isEmpty());
        BuiltinSplashRules.queryIds("tv.danmaku.bili").clear();
        assertEquals(2, BuiltinSplashRules.queryIds("tv.danmaku.bili").size());
    }
    @Test public void badgeFreeBuiltInEvidenceStillWaitsForRealDismissalBeforeStatistics() {
        String pkg = "tv.danmaku.bili", id = "tv.danmaku.bili:id/count_down";
        AdDecisionEngine engine = new AdDecisionEngine();
        engine.onForeground(pkg, 100, false);
        boolean recognized = BuiltinSplashRules.matches(pkg, id, "跳过", null, 200);
        AdDecisionEngine.Rule rule = new AdDecisionEngine.Rule(true, "", "");
        AdDecisionEngine.Screen ad = new AdDecisionEngine.Screen(recognized, true,
                Collections.singletonList(new AdDecisionEngine.Candidate(id)), Collections.singleton(id), false);
        AdDecisionEngine.Decision decision = engine.onScreen(pkg, rule, ad, 200).decision;
        assertNotNull(decision);
        engine.onActionResult(decision, true, 200);
        assertNull(engine.onScreen(pkg, rule, ad, 500).confirmation);
        AdDecisionEngine.Screen clear = new AdDecisionEngine.Screen(false, true, Collections.emptyList(), Collections.emptySet(), false);
        assertNull(engine.onScreen(pkg, rule, clear, 700).confirmation);
        assertEquals("skip", engine.onScreen(pkg, rule, clear, 900).confirmation.kind);
    }
}
