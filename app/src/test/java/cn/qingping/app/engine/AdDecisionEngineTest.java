package cn.qingping.app.engine;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class AdDecisionEngineTest {
    private final AdDecisionEngine.Rule rule = new AdDecisionEngine.Rule(true, "", "shop.app");
    private AdDecisionEngine.Screen screen(boolean ad, String key) {
        return new AdDecisionEngine.Screen(ad, true,
                key == null ? Collections.emptyList() : Collections.singletonList(new AdDecisionEngine.Candidate(key)),
                key == null ? Collections.emptySet() : Collections.singleton(key));
    }
    private AdDecisionEngine started() {
        AdDecisionEngine e = new AdDecisionEngine();
        e.onForeground("news.app", 100, false);
        return e;
    }
    @Test public void exactLabelsRejectOnboardingAndOrdinaryWords() {
        for (String s : new String[]{"跳过", "跳过 5", "5s 跳过", "跳过广告", "Skip ad", "Skip 5s"})
            assertTrue(s, AdDecisionEngine.isSkipLabel(s));
        for (String s : new String[]{"跳过登录", "不再提示", "关闭", "下一步", "跳过这一步", "Skip tutorial", "点击跳过广告领取奖励"})
            assertFalse(s, AdDecisionEngine.isSkipLabel(s));
    }
    @Test public void separatorCountdownsMatchWithoutAcceptingOtherBusinessWords() {
        for (String s : new String[]{"跳过 | 5", "跳过｜5s", "跳过·3秒", "5 • 跳过", "Skip | 5s"})
            assertTrue(s, AdDecisionEngine.isSkipLabel(s));
        for (String s : new String[]{"跳过 | 登录", "跳过·片头", "跳过 | 下载", "Skip | tutorial"})
            assertFalse(s, AdDecisionEngine.isSkipLabel(s));
    }
    @Test public void acceptedNoOpMayEscalateOnlyAfterTimeoutAndKeepsConfirmationSeparate() {
        AdDecisionEngine e = started();
        assertFalse(e.shouldEscalateGesture("button", 200));
        AdDecisionEngine.Decision first = e.onScreen("news.app", rule, screen(true, "button"), 200).decision;
        e.onActionResult(first, true, 200);
        assertFalse(e.shouldEscalateGesture("button", 2_500));
        assertNull(e.onScreen("news.app", rule, screen(true, "button"), 2_500).confirmation);
        AdDecisionEngine.Decision retry = e.onScreen("news.app", rule, screen(true, "button"), 2_701).decision;
        assertNotNull(retry);
        assertTrue(e.shouldEscalateGesture("button", 2_701));
        assertFalse(e.shouldEscalateGesture("another-button", 2_701));
        e.onActionResult(retry, true, 2_701);
        assertNull(e.onScreen("news.app", rule, screen(false, null), 3_000).confirmation);
        assertNotNull(e.onScreen("news.app", rule, screen(false, null), 3_200).confirmation);
        e.reset();
        assertFalse(e.shouldEscalateGesture("button", 10_000));
    }
    @Test public void skipNeedsConfirmationAndDoesNotDoubleCount() {
        AdDecisionEngine e = started();
        AdDecisionEngine.Decision d = e.onScreen("news.app", rule, screen(true, "button"), 200).decision;
        assertNotNull(d);
        e.onActionResult(d, true, 200);
        assertNull(e.onScreen("news.app", rule, screen(true, "button"), 500).confirmation);
        assertNull(e.onScreen("news.app", rule, screen(false, null), 600).confirmation);
        assertEquals("skip", e.onScreen("news.app", rule, screen(false, null), 800).confirmation.kind);
        assertNull(e.onScreen("news.app", rule, screen(false, null), 1_000).confirmation);
        assertNull(e.onScreen("news.app", rule, screen(true, "button"), 1_100).decision);
    }
    @Test public void disappearanceBeforeAcceptedClickDoesNotCount() {
        AdDecisionEngine e = started();
        AdDecisionEngine.Decision d = e.onScreen("news.app", rule, screen(true, "button"), 200).decision;
        e.onActionResult(d, false, 200);
        assertNull(e.onScreen("news.app", rule, screen(false, null), 600).confirmation);
        assertNull(e.onScreen("news.app", rule, screen(false, null), 900).confirmation);
    }
    @Test public void transientOrIncompleteTreeDoesNotConfirm() {
        AdDecisionEngine e = started();
        AdDecisionEngine.Decision d = e.onScreen("news.app", rule, screen(true, "button"), 200).decision;
        e.onActionResult(d, true, 200);
        AdDecisionEngine.Screen partial = new AdDecisionEngine.Screen(false, false, Collections.emptyList(), Collections.emptySet());
        assertNull(e.onScreen("news.app", rule, partial, 500).confirmation);
        assertNull(e.onScreen("news.app", rule, partial, 900).confirmation);
        assertNull(e.onScreen("news.app", rule, screen(false, null), 1_100).confirmation);
        assertNull(e.onScreen("news.app", rule, screen(true, "button"), 1_200).confirmation);
        assertNull(e.onScreen("news.app", rule, screen(false, null), 1_400).confirmation);
    }
    @Test public void timeoutOrLeavingSourceNeverConfirmsSkip() {
        AdDecisionEngine e = started();
        AdDecisionEngine.Decision d = e.onScreen("news.app", rule, screen(true, "button"), 200).decision;
        e.onActionResult(d, true, 200);
        assertNull(e.onScreen("news.app", rule, screen(false, null), 3_000).confirmation);
        e = started();
        d = e.onScreen("news.app", rule, screen(true, "button"), 200).decision;
        e.onActionResult(d, true, 200);
        assertNull(e.onForeground("other.app", 300, false).confirmation);
    }
    @Test public void buttonGoneWhileAdvertisementRemainsDoesNotCount() {
        AdDecisionEngine e = started();
        AdDecisionEngine.Decision d = e.onScreen("news.app", rule, screen(true, "button"), 200).decision;
        e.onActionResult(d, true, 200);
        assertNull(e.onScreen("news.app", rule, screen(true, null), 500).confirmation);
        assertNull(e.onScreen("news.app", rule, screen(true, null), 900).confirmation);
        assertNull(e.onScreen("news.app", rule, screen(false, null), 1_100).confirmation);
        assertEquals("skip", e.onScreen("news.app", rule, screen(false, null), 1_400).confirmation.kind);
    }
    @Test public void guardIsExactShortLivedAndConfirmedBySourceRestore() {
        AdDecisionEngine e = started();
        e.onScreen("news.app", rule, screen(true, null), 200);
        AdDecisionEngine.Decision d = e.onForeground("shop.app", 300, true).decision;
        assertEquals(AdDecisionEngine.Action.BACK, d.action);
        e.onActionResult(d, true, 300);
        assertNull(e.onForeground("shop.app", 350, true).confirmation);
        AdDecisionEngine.Result restored = e.onForeground("news.app", 500, true);
        assertEquals("news.app", restored.confirmation.sourcePackage);
        assertEquals("return", restored.confirmation.kind);
        e.onScreen("news.app", rule, screen(true, null), 550);
        assertEquals("", e.armedSource());
        assertNull(e.onForeground("shop.app", 600, true).decision);
    }
    @Test public void ordinaryNavigationAndPauseAndWrongTargetsAreNotBlocked() {
        AdDecisionEngine e = started();
        e.onScreen("news.app", rule, screen(false, null), 200);
        assertNull(e.onForeground("shop.app", 300, true).decision);
        e = started(); e.onScreen("news.app", rule, screen(true, null), 200);
        assertNull(e.onForeground("shop.app", 300, false).decision);
        e = started(); e.onScreen("news.app", rule, screen(true, null), 200);
        assertNull(e.onForeground("browser.app", 300, true).decision);
        e = started(); e.onScreen("news.app", rule, screen(true, null), 200);
        assertNull(e.onForeground("shop.app", 2_201, true).decision);
    }
    @Test public void monitoringContinuesAfterLaunchWindowAndResetRevokesPendingAction() {
        AdDecisionEngine e = started();
        assertTrue(e.shouldInspect(100));
        assertTrue(e.shouldInspect(15_101));
        assertNotNull(e.onScreen("news.app", rule, screen(true, "button"), 15_101).decision);
        e = started();
        AdDecisionEngine.Decision d = e.onScreen("news.app", rule, screen(true, "button"), 200).decision;
        e.onActionResult(d, true, 200);
        e.reset();
        assertFalse(e.shouldInspect(600));
        assertNull(e.onScreen("news.app", rule, screen(false, null), 600).confirmation);
    }
    @Test public void guardRequestWithoutActualReturnDoesNotCount() {
        AdDecisionEngine e = started();
        e.onScreen("news.app", rule, screen(true, null), 200);
        AdDecisionEngine.Decision d = e.onForeground("shop.app", 300, true).decision;
        e.onActionResult(d, true, 300);
        assertNull(e.onForeground("news.app", 3_000, true).confirmation);
    }
    @Test public void safeGeometryAndAdEvidenceAreConservative() {
        assertTrue(AdDecisionEngine.safeTopRight(800, 60, 1000, 150, 1080, 1920));
        assertFalse(AdDecisionEngine.safeTopRight(0, 0, 1080, 1920, 1080, 1920));
        assertFalse(AdDecisionEngine.safeTopRight(800, 1500, 1000, 1600, 1080, 1920));
        assertTrue(AdDecisionEngine.isAdContext("广告"));
        assertTrue(AdDecisionEngine.isAdContext("摇一摇了解详情"));
        assertFalse(AdDecisionEngine.isAdContext("了解更多"));
        assertFalse(AdDecisionEngine.isAdContext("点击进入"));
    }
    @Test public void delayedAdsRemainEligibleAfterMinutesInSameApp() {
        AdDecisionEngine e = started();
        assertEquals(15_000, rule.windowMs);
        assertTrue(e.shouldInspect(10_100));
        assertEquals(10_000, e.elapsedInSession(10_100));
        assertNotNull(e.onScreen("news.app", rule, screen(true, "button"), 10_100).decision);
        assertNotNull(e.onScreen("news.app", rule, screen(true, "button"), 15_100).decision);
        assertNotNull(e.onScreen("news.app", rule, screen(true, "button"), 15_101).decision);
        assertTrue(e.shouldInspect(300_100));
        assertNotNull(e.onScreen("news.app", rule, screen(true, "button"), 300_100).decision);
    }
    @Test public void customWindowsAreBoundedButAdapterControlsCandidateScope() {
        AdDecisionEngine e = started();
        AdDecisionEngine.Rule custom = new AdDecisionEngine.Rule(true, "", "shop.app", 30);
        assertEquals(30_000, custom.windowMs);
        assertTrue(e.shouldInspect(20_100));
        assertTrue(e.shouldInspect(20_100, custom.windowMs));
        assertNotNull(e.onScreen("news.app", custom, screen(true, "button"), 20_100).decision);
        assertNotNull(e.onScreen("news.app", custom, screen(true, "button"), 30_101).decision);
        assertEquals(3_000, new AdDecisionEngine.Rule(true, "", "", -1).windowMs);
        assertEquals(30_000, new AdDecisionEngine.Rule(true, "", "", 99).windowMs);
    }
    @Test public void lateAdvertisingEpisodeCanArmOnlyConfiguredRecentJumpGuard() {
        AdDecisionEngine.Rule custom = new AdDecisionEngine.Rule(true, "", "shop.app", 30);
        AdDecisionEngine e = started();
        AdDecisionEngine.Result late = e.onScreen("news.app", custom, screen(true, "button"), 20_100);
        assertNotNull(late.decision); // The explicit 30-second selector remains usable.
        assertEquals(AdDecisionEngine.Action.CLICK_SKIP, late.decision.action);
        assertEquals("news.app", e.armedSource());
        assertNotNull(e.onForeground("shop.app", 20_200, true).decision);

        e = started();
        e.onScreen("news.app", custom, screen(true, null), 15_000);
        assertEquals("news.app", e.armedSource());
        // A complete ordinary page revokes an earlier arm before its TTL elapses.
        e.onScreen("news.app", custom, screen(false, null), 15_101);
        assertEquals("", e.armedSource());
        assertNull(e.onForeground("shop.app", 15_200, true).decision);
    }
    @Test public void staticBannerEvidenceCannotArmOrPreserveCrossAppReturn() {
        AdDecisionEngine e = started();
        AdDecisionEngine.Screen banner = new AdDecisionEngine.Screen(true, true,
                Collections.emptyList(), Collections.emptySet(), false);
        e.onScreen("news.app", rule, banner, 300_000);
        assertEquals("", e.armedSource());
        assertNull(e.onForeground("shop.app", 300_100, true).decision);
        e.onForeground("news.app", 400_000, false);
        e.onScreen("news.app", rule, screen(true, null), 400_100);
        assertEquals("news.app", e.armedSource());
        e.onScreen("news.app", rule, banner, 400_200);
        assertEquals("", e.armedSource());
        assertNull(e.onForeground("shop.app", 400_300, true).decision);
    }
    @Test public void explicitAdCloseLabelsExcludeGenericCloseAndRewardSentences() {
        for (String label : new String[]{"关闭广告", "关闭此广告", " close ad ",
                "CLOSE ADVERTISEMENT", "Dismiss Ad", "关闭此条广告", "关闭视频广告",
                "关闭本广告", "close this ad", "dismiss this ad"}) {
            assertTrue(label, AdDecisionEngine.isAdCloseLabel(label));
            assertTrue(label, AdDecisionEngine.isAdContext(label));
        }
        for (String label : new String[]{"关闭", "close", "关闭账户", "关闭广告领取奖励",
                "看完奖励广告后关闭广告", "close ad now", "dismiss", ""})
            assertFalse(label, AdDecisionEngine.isAdCloseLabel(label));
        assertFalse(AdDecisionEngine.isAdCloseLabel(null));
    }
    @Test public void ineffectiveClickHasOnlyOneQuickRetry() {
        AdDecisionEngine e = started();
        AdDecisionEngine.Decision first = e.onScreen("news.app", rule, screen(true, "button"), 200).decision;
        e.onActionResult(first, true, 200);
        e.onActionResult(first, true, 200); // Duplicate callbacks must not consume another attempt.
        assertNull(e.onScreen("news.app", rule, screen(true, "button"), 2_500).decision);
        AdDecisionEngine.Decision second = e.onScreen("news.app", rule, screen(true, "button"), 2_701).decision;
        assertNotNull(second);
        e.onActionResult(second, true, 2_701);
        assertFalse(e.isSessionComplete());
        assertNull(e.onScreen("news.app", rule, screen(true, "button"), 5_202).decision);
        assertFalse(e.isSessionComplete()); // Keep watching for a later advertising episode.
        assertNull(e.onScreen("news.app", rule, screen(true, "button"), 8_000).decision);
    }
    @Test public void successSuppressesRepeatedActionsUntilStableClearScreen() {
        AdDecisionEngine e = started();
        AdDecisionEngine.Decision d = e.onScreen("news.app", rule, screen(true, "button"), 200).decision;
        e.onActionResult(d, true, 200);
        e.onScreen("news.app", rule, screen(false, null), 500);
        assertNotNull(e.onScreen("news.app", rule, screen(false, null), 800).confirmation);
        assertTrue(e.shouldInspect(5_000));
        assertFalse(e.isSessionComplete());
        assertNull(e.onScreen("news.app", rule, screen(true, "button"), 5_000).decision);
        assertEquals("", e.armedSource());
    }
    @Test public void sameControlCanHandleSeparateAdsWithoutLeavingApp() {
        AdDecisionEngine e = started();
        AdDecisionEngine.Decision first = e.onScreen("news.app", rule, screen(true, "button"), 200).decision;
        e.onActionResult(first, true, 200);
        e.onScreen("news.app", rule, screen(false, null), 500);
        assertNotNull(e.onScreen("news.app", rule, screen(false, null), 700).confirmation);
        // Confirming is not itself permission to click during a short phase transition.
        assertNull(e.onScreen("news.app", rule, screen(true, "button"), 800).decision);
        e.onScreen("news.app", rule, screen(false, null), 1_000);
        e.onScreen("news.app", rule, screen(false, null), 1_500);
        AdDecisionEngine.Decision second = e.onScreen("news.app", rule, screen(true, "button"), 60_000).decision;
        assertNotNull(second);
        e.onActionResult(second, true, 60_000);
        assertNull(e.onScreen("news.app", rule, screen(false, null), 60_300).confirmation);
        assertNotNull(e.onScreen("news.app", rule, screen(false, null), 60_500).confirmation);
        assertNull(e.onScreen("news.app", rule, screen(false, null), 60_800).confirmation);
    }
    @Test public void multipleAdPhasesHaveSharedBudgetAndSingleFinalConfirmation() {
        AdDecisionEngine e = started();
        String[] keys = {"video-skip", "video-skip", "end-card-close", "end-card-close"};
        for (int i = 0; i < keys.length; i++) {
            long time = 200 + i * 2_600L;
            AdDecisionEngine.Result result = e.onScreen("news.app", rule, screen(true, keys[i]), time);
            assertNull(result.confirmation);
            assertNotNull(result.decision);
            e.onActionResult(result.decision, true, time);
        }
        assertNull(e.onScreen("news.app", rule, screen(true, "another-close"), 8_100).decision);
        assertNull(e.onScreen("news.app", rule, screen(false, null), 8_300).confirmation);
        assertEquals("skip", e.onScreen("news.app", rule, screen(false, null), 8_500).confirmation.kind);
        assertNull(e.onScreen("news.app", rule, screen(false, null), 8_700).confirmation);
    }
    @Test public void exhaustedFirstControlDoesNotHideEligibleSecondControl() {
        AdDecisionEngine e = started();
        for (long time : new long[]{200, 2_800}) {
            AdDecisionEngine.Decision d = e.onScreen("news.app", rule, screen(true, "skip"), time).decision;
            e.onActionResult(d, true, time);
        }
        AdDecisionEngine.Screen two = new AdDecisionEngine.Screen(true, true,
                Arrays.asList(new AdDecisionEngine.Candidate("skip"), new AdDecisionEngine.Candidate("close")),
                Collections.singleton("close"));
        assertEquals("close", e.onScreen("news.app", rule, two, 5_400).decision.candidateKey);
    }
    @Test public void failedRequestsAreDebouncedAndNeverCounted() {
        AdDecisionEngine e = started();
        AdDecisionEngine.Decision d = e.onScreen("news.app", rule, screen(true, "button"), 200).decision;
        e.onActionResult(d, false, 200);
        assertNull(e.onScreen("news.app", rule, screen(true, "button"), 949).decision);
        assertNotNull(e.onScreen("news.app", rule, screen(true, "button"), 950).decision);
        assertNull(e.onScreen("news.app", rule, screen(false, null), 1_200).confirmation);
        assertNull(e.onScreen("news.app", rule, screen(false, null), 1_800).confirmation);
    }
    @Test public void noCandidateDuringCountdownConsumesNoBudget() {
        AdDecisionEngine e = started();
        AdDecisionEngine.Screen locked = new AdDecisionEngine.Screen(true, true, Collections.emptyList(),
                Collections.singleton("same-button"));
        for (long time = 200; time < 30_000; time += 500)
            assertNull(e.onScreen("news.app", rule, locked, time).decision);
        assertNotNull(e.onScreen("news.app", rule, screen(true, "same-button"), 30_200).decision);
    }
    @Test public void repeatedRejectedRequestsStopUntilStableClearAndNeverCount() {
        AdDecisionEngine e = started();
        for (long time : new long[]{200, 950, 1_700}) {
            AdDecisionEngine.Decision d = e.onScreen("news.app", rule, screen(true, "broken"), time).decision;
            assertNotNull(d);
            e.onActionResult(d, false, time);
        }
        assertNull(e.onScreen("news.app", rule, screen(true, "broken"), 10_000).decision);
        assertNull(e.onScreen("news.app", rule, screen(false, null), 10_100).confirmation);
        assertNull(e.onScreen("news.app", rule, screen(false, null), 10_600).confirmation);
        assertNotNull(e.onScreen("news.app", rule, screen(true, "broken"), 10_700).decision);
    }
    @Test public void changingRejectedControlsCannotBypassSharedRequestBudget() {
        AdDecisionEngine e = started();
        for (int i = 0; i < 8; i++) {
            long time = 200 + i * 800L;
            AdDecisionEngine.Decision d = e.onScreen("news.app", rule, screen(true, "broken-" + i), time).decision;
            assertNotNull(d);
            e.onActionResult(d, false, time);
        }
        assertNull(e.onScreen("news.app", rule, screen(true, "new-control"), 30_000).decision);
        assertNull(e.onScreen("news.app", rule, screen(false, null), 30_100).confirmation);
    }
    @Test public void acceptedNoOpDuringLongCountdownAllowsOneDelayedFinalRetry() {
        AdDecisionEngine e = started();
        for (long time : new long[]{200, 2_800}) {
            AdDecisionEngine.Decision d = e.onScreen("news.app", rule, screen(true, "skip"), time).decision;
            assertNotNull(d);
            e.onActionResult(d, true, time);
        }
        assertNull(e.onScreen("news.app", rule, screen(true, "skip"), 12_799).decision);
        AdDecisionEngine.Decision finalTry = e.onScreen("news.app", rule, screen(true, "skip"), 12_800).decision;
        assertNotNull(finalTry);
        e.onActionResult(finalTry, true, 12_800);
        // Even repeated candidates and enough time never grant a fourth attempt.
        assertNull(e.onScreen("news.app", rule, screen(true, "skip"), 30_000).decision);
        AdDecisionEngine.Decision close = e.onScreen("news.app", rule, screen(true, "end-card"), 30_100).decision;
        assertNotNull(close);
        e.onActionResult(close, true, 30_100);
        assertNull(e.onScreen("news.app", rule, screen(true, "last-phase"), 32_601).decision);
        assertNull(e.onScreen("news.app", rule, screen(false, null), 32_700).confirmation);
    }
    @Test public void anotherVisibleAdControlPreventsPhaseTransitionFromCountingAsSuccess() {
        AdDecisionEngine e = started();
        AdDecisionEngine.Decision d = e.onScreen("news.app", rule, screen(true, "skip"), 200).decision;
        e.onActionResult(d, true, 200);
        AdDecisionEngine.Screen replacement = new AdDecisionEngine.Screen(false, true, Collections.emptyList(),
                Collections.singleton("end-card-close"));
        assertNull(e.onScreen("news.app", rule, replacement, 500).confirmation);
        assertNull(e.onScreen("news.app", rule, replacement, 1_000).confirmation);
        assertNull(e.onScreen("news.app", rule, screen(false, null), 1_500).confirmation);
        assertNotNull(e.onScreen("news.app", rule, screen(false, null), 1_700).confirmation);
    }
    @Test public void incompleteOrBriefClearScreensNeverResetAttemptBudget() {
        AdDecisionEngine e = started();
        for (long time : new long[]{200, 2_800}) {
            AdDecisionEngine.Decision d = e.onScreen("news.app", rule, screen(true, "skip"), time).decision;
            e.onActionResult(d, true, time);
        }
        AdDecisionEngine.Screen partial = new AdDecisionEngine.Screen(false, false, Collections.emptyList(), Collections.emptySet());
        e.onScreen("news.app", rule, partial, 5_400);
        e.onScreen("news.app", rule, partial, 6_000);
        assertNull(e.onScreen("news.app", rule, screen(true, "skip"), 6_100).decision);
        e.onScreen("news.app", rule, screen(false, null), 6_200);
        e.onScreen("news.app", rule, screen(false, null), 6_699);
        assertNull(e.onScreen("news.app", rule, screen(true, "skip"), 6_700).decision);
        e.onScreen("news.app", rule, screen(false, null), 7_000);
        e.onScreen("news.app", rule, screen(false, null), 7_500);
        assertNotNull(e.onScreen("news.app", rule, screen(true, "skip"), 7_600).decision);
    }
    @Test public void returnSuppressesSameAdButLaterEpisodeCanReturnAgain() {
        AdDecisionEngine e = started();
        e.onScreen("news.app", rule, screen(true, null), 30_000);
        AdDecisionEngine.Decision d = e.onForeground("shop.app", 30_100, true).decision;
        e.onActionResult(d, true, 30_100);
        assertNotNull(e.onForeground("news.app", 30_300, true).confirmation);
        assertNull(e.onScreen("news.app", rule, screen(true, "button"), 30_500).decision);
        assertEquals("", e.armedSource());
        e.onScreen("news.app", rule, screen(false, null), 30_600);
        e.onScreen("news.app", rule, screen(false, null), 31_100);
        e.onScreen("news.app", rule, screen(true, null), 60_000);
        assertNotNull(e.onForeground("shop.app", 60_100, true).decision);
    }
    @Test public void staleActionCallbacksCannotCrossResetsForegroundOrEpisodeBoundary() {
        AdDecisionEngine e = started();
        AdDecisionEngine.Decision old = e.onScreen("news.app", rule, screen(true, "skip"), 200).decision;
        e.onForeground("other.app", 300, false);
        e.onForeground("news.app", 400, false);
        e.onActionResult(old, true, 500);
        assertFalse(e.hasPendingAction(500));
        old = e.onScreen("news.app", rule, screen(true, "skip"), 600).decision;
        e.onScreen("news.app", rule, screen(false, null), 700);
        e.onScreen("news.app", rule, screen(false, null), 1_200);
        e.onActionResult(old, true, 1_300);
        assertFalse(e.hasPendingAction(1_300));
        old = e.onScreen("news.app", rule, screen(true, "skip"), 1_400).decision;
        e.reset();
        e.onForeground("news.app", 1_500, false);
        e.onActionResult(old, true, 1_600);
        assertFalse(e.hasPendingAction(1_600));
    }
    @Test public void delayedOrSupersededCallbacksCannotStartConfirmation() {
        AdDecisionEngine e = started();
        AdDecisionEngine.Decision first = e.onScreen("news.app", rule, screen(true, "one"), 200).decision;
        AdDecisionEngine.Decision newer = e.onScreen("news.app", rule, screen(true, "two"), 300).decision;
        e.onActionResult(first, true, 400);
        assertFalse(e.hasPendingAction(400));
        e.onActionResult(newer, true, 2_801);
        assertFalse(e.hasPendingAction(2_801));
    }
    @Test public void knownSdkIdsMatchExactResourceNamesOnly() {
        for (String id : new String[]{"com.byted.pangle:id/tt_splash_skip_btn",
                "gov.pianzong.androidnga:id/ksad_splash_circle_skip_view", "app:id/ksad_skip_view_skip"})
            assertTrue(id, AdDecisionEngine.isKnownAdSkipId(id));
        for (String id : new String[]{"app:id/close", "app:id/skip_video", "app:id/my_tt_splash_skip_btn",
                "app:id/tt_splash_skip_btn_extra", "unknown_sdk_skip", "Skip ad"})
            assertFalse(id, AdDecisionEngine.isKnownAdSkipId(id));
        assertFalse(AdDecisionEngine.isKnownAdSkipId(null));
    }
    @Test public void countdownEvidenceRejectsEmbeddedOrUnrelatedDigits() {
        for (String label : new String[]{"跳过 5", "5s 跳过", "Skip 5s", "跳过广告(3秒)"})
            assertTrue(label, AdDecisionEngine.isCountdownSkipLabel(label));
        for (String label : new String[]{"跳过", "Skip ad", "跳过登录5", "关闭5秒", "点击跳过5领取奖励", "Skip tutorial 5"})
            assertFalse(label, AdDecisionEngine.isCountdownSkipLabel(label));
        assertFalse(AdDecisionEngine.isCountdownSkipLabel(null));
    }
    @Test public void edgeGeometryAllowsBottomControlsButRejectsWholePageAndCenter() {
        assertTrue(AdDecisionEngine.safeEdgeControl(40, 60, 220, 150, 1080, 1920));
        assertTrue(AdDecisionEngine.safeEdgeControl(800, 1600, 1000, 1720, 1080, 1920));
        assertFalse(AdDecisionEngine.safeEdgeControl(0, 0, 1080, 1920, 1080, 1920));
        assertFalse(AdDecisionEngine.safeEdgeControl(400, 850, 600, 950, 1080, 1920));
        assertFalse(AdDecisionEngine.safeEdgeControl(-10, 50, 100, 100, 1080, 1920));
        assertFalse(AdDecisionEngine.safeEdgeControl(0, 0, 0, 0, 1080, 1920));
    }
}
