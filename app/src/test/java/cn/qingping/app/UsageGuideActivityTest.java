package cn.qingping.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 35})
@LooperMode(LooperMode.Mode.PAUSED)
public class UsageGuideActivityTest {
    private AppStore store;
    private ActivityController<UsageGuideActivity> controller;
    private UsageGuideActivity activity;
    @Before public void reset() {
        store = new AppStore(RuntimeEnvironment.getApplication()); store.preferences().edit().clear().commit();
        store.clearStats();
        QingPingAccessibilityService.connected = false;
    }
    private void launch(String topic) {
        Intent intent = new Intent(RuntimeEnvironment.getApplication(), UsageGuideActivity.class);
        if (topic != null) intent.putExtra(UsageGuideActivity.EXTRA_TOPIC, topic);
        controller = Robolectric.buildActivity(UsageGuideActivity.class, intent).setup(); activity = controller.get();
    }
    @After public void destroy() { if (controller != null) controller.pause().stop().destroy(); QingPingAccessibilityService.connected = false; }
    private UsageGuide.Topic topic(String id) {
        for (UsageGuide.Topic topic : UsageGuide.topics()) if (topic.id.equals(id)) return topic;
        throw new AssertionError("Unknown topic");
    }
    private TextView body(String id) { return UiViews.text(activity, topic(id).body); }
    private EditText search() { return UiViews.require(UiViews.root(activity), EditText.class, view -> true); }
    private Button toggle(String id) { return UiViews.require(UiViews.root(activity).findViewWithTag(id), Button.class, view -> true); }

    @Test public void guideBeforeConsentIsReadOnlyAndShowsMissingConditions() {
        launch(null); assertTrue(body("start").isShown());
        assertTrue(UiViews.require(UiViews.root(activity), TextView.class,
                view -> view.getText().toString().contains("用途同意：尚未完成")).isShown());
        assertFalse(store.hasConsent()); assertFalse(store.isProtectionEnabled()); assertTrue(store.selectedPackages().isEmpty());
    }
    @Test public void requestedTopicCanExpandAndCollapseWithoutGrantingActions() {
        launch("button"); assertTrue(body("button").isShown());
        toggle("button").performClick(); assertEquals(View.GONE, body("button").getVisibility());
        toggle("button").performClick(); assertTrue(body("button").isShown());
        assertFalse(store.hasConsent()); assertFalse(store.isProtectionEnabled());
    }
    @Test public void searchShowsMatchingBodiesAndHasARealEmptyState() {
        launch(null); search().setText("30 秒按钮诊断");
        assertTrue(body("diagnose").isShown()); assertNull(UiViews.root(activity).findViewWithTag("start"));
        search().setText("this-topic-does-not-exist-927");
        assertTrue(UiViews.text(activity, "找到 0 个相关主题").isShown());
        UiViews.click(activity, "清除搜索并收起全部"); assertEquals("", search().getText().toString());
        for (UsageGuide.Topic topic : UsageGuide.topics()) assertEquals(View.GONE, body(topic.id).getVisibility());
    }
    @Test public void expansionAndSearchSurviveRecreation() {
        launch("rules"); search().setText("30 秒按钮诊断"); Bundle state = new Bundle();
        controller.saveInstanceState(state).pause().stop().destroy();
        controller = Robolectric.buildActivity(UsageGuideActivity.class).create(state).start().resume().visible(); activity = controller.get();
        assertEquals("30 秒按钮诊断", search().getText().toString()); assertTrue(body("diagnose").isShown());
        search().setText(""); assertTrue(body("rules").isShown());
    }
    @Test public void returnToAppSettingsSelectsTheRequestedTabWithoutEnablingAnything() {
        launch(null); UiViews.click(activity, "前往应用配置");
        Intent next = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(MainActivity.class.getName(), next.getComponent().getClassName());
        assertEquals(1, next.getIntExtra(MainActivity.EXTRA_OPEN_TAB, -1)); assertTrue(activity.isFinishing());
        assertFalse(store.hasConsent()); assertFalse(store.isProtectionEnabled());
    }
    @Test public void resumedStatusReflectsSettingsWithoutChangingThem() {
        launch(null); store.setConsent(true); store.setProtectionEnabled(true); store.setAppEnabled("example.reader", true);
        QingPingAccessibilityService.connected = true; controller.pause().resume();
        assertTrue(UiViews.require(UiViews.root(activity), TextView.class,
                view -> view.getText().toString().contains("已选择 APP：1 个")
                        && view.getText().toString().contains("无障碍服务：已连接")).isShown());
        assertTrue(store.hasConsent()); assertTrue(store.isProtectionEnabled());
        UiViews.click(activity, "返回清屏"); assertTrue(activity.isFinishing());
    }
}
