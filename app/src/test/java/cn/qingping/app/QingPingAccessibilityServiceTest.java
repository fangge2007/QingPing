package cn.qingping.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.KeyguardManager;
import android.graphics.Rect;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import cn.qingping.app.engine.AdDecisionEngine;
import cn.qingping.app.engine.ControlIdentity;
import java.util.Arrays;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.LooperMode;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.shadows.ShadowAccessibilityService;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;
import static org.junit.Assert.*;

/** Exercises the actual Android adapter; shadows replace only the system IPC boundary. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 35}, qualifiers = "w1080dp-h1920dp", shadows = QingPingAccessibilityServiceTest.Connection.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class QingPingAccessibilityServiceTest {
    private static final String SOURCE = "example.reader", TARGET = "example.shop";
    private ServiceController<QingPingAccessibilityService> controller;
    private QingPingAccessibilityService service;
    private AppStore store;
    private ShadowAccessibilityService shadow;

    @Implements(AccessibilityService.class)
    public static class Connection extends ShadowAccessibilityService {
        private AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        @Implementation protected AccessibilityServiceInfo getServiceInfo() { return info; }
        @Implementation protected void setServiceInfo(AccessibilityServiceInfo value) { info = value; }
    }

    @Before public void connect() {
        controller = Robolectric.buildService(QingPingAccessibilityService.class).create();
        service = controller.get();
        store = new AppStore(service);
        store.preferences().edit().clear().commit();
        store.clearStats();
        store.setConsent(true); store.setProtectionEnabled(true); store.setAppEnabled(SOURCE, true);
        Shadows.shadowOf(service.getSystemService(PowerManager.class)).setIsInteractive(true);
        Shadows.shadowOf(service.getSystemService(KeyguardManager.class)).setKeyguardLocked(false);
        shadow = Shadows.shadowOf((AccessibilityService) service);
        service.onServiceConnected();
    }

    @After public void disconnect() { controller.destroy(); QingPingAccessibilityService.connected = false; }

    private AccessibilityNodeInfo node(String pkg, String label) {
        AccessibilityNodeInfo node = AccessibilityNodeInfo.obtain();
        node.setPackageName(pkg); node.setText(label); node.setVisibleToUser(true);
        node.setEnabled(true); node.setClickable(true); node.setBoundsInScreen(new Rect(10, 10, 110, 45));
        Shadows.shadowOf(node).setRefreshReturnValue(true);
        Shadows.shadowOf(node).setOnPerformActionListener((action, args) -> true);
        return node;
    }

    private boolean click(AccessibilityNodeInfo node, String pkg) {
        return ReflectionHelpers.callInstanceMethod(service, "clickNode",
                ClassParameter.from(AccessibilityNodeInfo.class, node), ClassParameter.from(String.class, pkg));
    }

    private void scan() { ReflectionHelpers.callInstanceMethod(service, "scanActiveWindow"); }
    private void settingsDelivered() { Shadows.shadowOf(Looper.getMainLooper()).idle(); }
    private void foregroundWithChild(AccessibilityNodeInfo child) {
        AccessibilityNodeInfo root = node(SOURCE, "广告");
        root.setClickable(false); root.setBoundsInScreen(new Rect(0, 0, 1080, 1920));
        Shadows.shadowOf(root).addChild(child); shadow.setRootInActiveWindow(root);
    }

    @Test public void systemFilterTracksSourcesTargetsAndRevocation() {
        assertArrayEquals(new String[]{SOURCE}, service.getServiceInfo().packageNames);
        store.setRule(SOURCE, true, TARGET, ""); settingsDelivered();
        assertEquals(Arrays.asList(SOURCE, TARGET), Arrays.asList(service.getServiceInfo().packageNames));
        store.setAppEnabled(SOURCE, false); settingsDelivered();
        assertArrayEquals(new String[]{service.getPackageName()}, service.getServiceInfo().packageNames);
        store.setAppEnabled(SOURCE, true); store.setConsent(false); settingsDelivered();
        assertArrayEquals(new String[]{service.getPackageName()}, service.getServiceInfo().packageNames);
    }

    @Test public void actualClickRequiresConsentProtectionSelectionAndUnlockedScreen() {
        AccessibilityNodeInfo node = node(SOURCE, "跳过广告"); shadow.setRootInActiveWindow(node);
        assertTrue(click(node, SOURCE));
        store.setProtectionEnabled(false); assertFalse(click(node, SOURCE));
        store.setProtectionEnabled(true); store.setConsent(false); assertFalse(click(node, SOURCE));
        store.setConsent(true); store.setAppEnabled(SOURCE, false); assertFalse(click(node, SOURCE));
        store.setAppEnabled(SOURCE, true);
        Shadows.shadowOf(service.getSystemService(KeyguardManager.class)).setKeyguardLocked(true);
        assertFalse(click(node, SOURCE));
        assertEquals(Collections.singletonList(AccessibilityNodeInfo.ACTION_CLICK), Shadows.shadowOf(node).getPerformedActions());
    }

    @Test public void destructiveChildAndForeignForegroundNeverReceiveClicks() {
        AccessibilityNodeInfo parent = node(SOURCE, "跳过广告");
        AccessibilityNodeInfo child = node(SOURCE, "删除账户");
        Shadows.shadowOf(parent).addChild(child); shadow.setRootInActiveWindow(parent);
        assertFalse(click(parent, SOURCE));
        assertTrue(Shadows.shadowOf(parent).getPerformedActions().isEmpty());
        AccessibilityNodeInfo safe = node(SOURCE, "跳过广告");
        shadow.setRootInActiveWindow(node(TARGET, "跳过广告"));
        assertFalse(click(safe, SOURCE));
        assertTrue(Shadows.shadowOf(safe).getPerformedActions().isEmpty());
    }

    @Test public void disabledAndStaleControlsNeverReceiveClicks() {
        AccessibilityNodeInfo node = node(SOURCE, "跳过广告"); shadow.setRootInActiveWindow(node);
        node.setEnabled(false); assertFalse(click(node, SOURCE)); node.setEnabled(true);
        Shadows.shadowOf(node).setRefreshReturnValue(false); assertFalse(click(node, SOURCE));
        assertTrue(Shadows.shadowOf(node).getPerformedActions().isEmpty());
    }

    @Test public void fullScanFindsExplicitAdSkipWithoutCountingSubmissionAsSuccess() {
        AccessibilityNodeInfo node = node(SOURCE, "跳过广告"); foregroundWithChild(node);
        scan();
        assertEquals(Collections.singletonList(AccessibilityNodeInfo.ACTION_CLICK), Shadows.shadowOf(node).getPerformedActions());
        assertEquals(0, store.total(0));
    }

    @Test public void fullScanRejectsOrdinaryBusinessCloseAndEditableContent() {
        AccessibilityNodeInfo close = node(SOURCE, "关闭"); foregroundWithChild(close); scan();
        assertTrue(Shadows.shadowOf(close).getPerformedActions().isEmpty());
        AccessibilityNodeInfo input = node(SOURCE, "跳过广告"); input.setEditable(true);
        foregroundWithChild(input); scan();
        assertTrue(Shadows.shadowOf(input).getPerformedActions().isEmpty());
    }

    @Test public void legacyCriticalSelectionCannotReachAnAction() {
        store.preferences().edit().putBoolean("enabled.com.android.phone", true).commit();
        AccessibilityNodeInfo phone = node("com.android.phone", "跳过广告"); shadow.setRootInActiveWindow(phone);
        assertFalse(store.isAppEnabled("com.android.phone"));
        assertFalse(click(phone, "com.android.phone")); scan();
        assertTrue(Shadows.shadowOf(phone).getPerformedActions().isEmpty());
        assertTrue(shadow.getGlobalActionsPerformed().isEmpty());
    }

    private AdDecisionEngine.Decision decision() {
        AdDecisionEngine engine = ReflectionHelpers.getField(service, "engine");
        long now = SystemClock.elapsedRealtime(); engine.onForeground(SOURCE, now, false);
        return engine.onScreen(SOURCE, new AdDecisionEngine.Rule(true, "", TARGET),
                new AdDecisionEngine.Screen(true, true,
                        Collections.singletonList(new AdDecisionEngine.Candidate("skip")), Collections.singleton("skip")), now).decision;
    }

    private boolean gesture(AccessibilityNodeInfo node, AdDecisionEngine.Decision decision) {
        return ReflectionHelpers.callInstanceMethod(service, "gestureClick",
                ClassParameter.from(AccessibilityNodeInfo.class, node),
                ClassParameter.from(AdDecisionEngine.Decision.class, decision),
                ClassParameter.from(ControlIdentity.class, null));
    }

    @Test public void gestureRequiresOptInAndRevalidatesDangerousLabels() {
        AccessibilityNodeInfo node = node(SOURCE, "跳过广告"); shadow.setRootInActiveWindow(node);
        AdDecisionEngine.Decision decision = decision(); assertNotNull(decision);
        assertFalse(gesture(node, decision)); assertTrue(shadow.getGesturesDispatched().isEmpty());
        store.setGestureAllowed(SOURCE, true); shadow.setCanDispatchGestures(true);
        assertTrue(gesture(node, decision)); assertEquals(1, shadow.getGesturesDispatched().size());
        node.setText("转账"); assertFalse(gesture(node, decision));
        assertEquals(1, shadow.getGesturesDispatched().size());
    }

    @Test public void cancelledGestureDoesNotCountAndReleasesInFlightState() {
        AccessibilityNodeInfo node = node(SOURCE, "跳过广告"); shadow.setRootInActiveWindow(node);
        store.setGestureAllowed(SOURCE, true); shadow.setCanDispatchGestures(true);
        AdDecisionEngine.Decision decision = decision(); assertTrue(gesture(node, decision));
        ShadowAccessibilityService.GestureDispatch dispatch = shadow.getGesturesDispatched().get(0);
        dispatch.callback().onCancelled(dispatch.description());
        assertFalse((boolean) ReflectionHelpers.getField(service, "gestureInFlight"));
        AdDecisionEngine engine = ReflectionHelpers.getField(service, "engine");
        assertFalse(engine.hasPendingAction(SystemClock.elapsedRealtime()));
        assertEquals(0, store.total(0));
    }

    @Test public void completionAfterConsentRevocationCannotReviveTheAction() {
        AccessibilityNodeInfo node = node(SOURCE, "跳过广告"); shadow.setRootInActiveWindow(node);
        store.setGestureAllowed(SOURCE, true); shadow.setCanDispatchGestures(true);
        AdDecisionEngine.Decision decision = decision(); assertTrue(gesture(node, decision));
        ShadowAccessibilityService.GestureDispatch dispatch = shadow.getGesturesDispatched().get(0);
        store.setConsent(false); settingsDelivered();
        dispatch.callback().onCompleted(dispatch.description());
        AdDecisionEngine engine = ReflectionHelpers.getField(service, "engine");
        assertFalse(engine.hasPendingAction(SystemClock.elapsedRealtime()));
        assertFalse((boolean) ReflectionHelpers.getField(service, "gestureInFlight"));
        assertEquals(0, store.total(0));
        assertArrayEquals(new String[]{service.getPackageName()}, service.getServiceInfo().packageNames);
    }

    @Test public void configuredRecentAdJumpInvokesRealBackBoundary() {
        store.setRule(SOURCE, true, TARGET, ""); settingsDelivered();
        AdDecisionEngine engine = ReflectionHelpers.getField(service, "engine");
        long now = SystemClock.elapsedRealtime(); engine.onForeground(SOURCE, now, false);
        engine.onScreen(SOURCE, new AdDecisionEngine.Rule(true, "", TARGET),
                new AdDecisionEngine.Screen(true, true, Collections.emptyList(), Collections.emptySet()), now);
        shadow.setRootInActiveWindow(node(TARGET, "商品")); scan();
        assertEquals(Collections.singletonList(AccessibilityService.GLOBAL_ACTION_BACK), shadow.getGlobalActionsPerformed());
        assertEquals(0, store.total(0)); // Submission is not recorded as confirmed return.
    }

    @Test public void unrelatedAppAndPausedServiceNeverInvokeBack() {
        store.setRule(SOURCE, true, TARGET, ""); settingsDelivered();
        decision(); shadow.setRootInActiveWindow(node("other.app", "商品")); scan();
        assertTrue(shadow.getGlobalActionsPerformed().isEmpty());
        store.setProtectionEnabled(false); shadow.setRootInActiveWindow(node(TARGET, "商品")); scan();
        assertTrue(shadow.getGlobalActionsPerformed().isEmpty());
    }
}
