package cn.qingping.app;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.net.Uri;
import android.provider.Settings;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Switch;
import cn.qingping.app.rules.ExactRule;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
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
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowToast;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 35})
@LooperMode(LooperMode.Mode.PAUSED)
public class MainActivityTest {
    private AppStore store;
    private ActivityController<MainActivity> controller;
    private MainActivity activity;

    @Before public void reset() {
        store = new AppStore(RuntimeEnvironment.getApplication());
        store.preferences().edit().clear().commit();
        store.clearStats();
        QingPingAccessibilityService.connected = false;
    }
    private void launch(boolean consent) {
        store.setConsent(consent);
        controller = Robolectric.buildActivity(MainActivity.class).setup(); activity = controller.get();
    }
    @After public void destroy() {
        if (controller != null) controller.pause().stop().destroy();
        QingPingAccessibilityService.connected = false;
    }

    @Test public void uncheckedDisclosureCannotEnableAnything() {
        launch(false); UiViews.click(activity, "同意并进入清屏");
        assertFalse(store.hasConsent()); assertFalse(store.isProtectionEnabled());
        assertEquals("请先阅读并勾选用途说明", ShadowToast.getTextOfLatestToast());
        assertTrue(UiViews.text(activity, "启用前，请了解无障碍访问").isShown());
    }
    @Test public void checkedConsentEntersHomeWithoutEnablingProtection() {
        launch(false);
        UiViews.require(UiViews.root(activity), CheckBox.class, view -> true).performClick();
        UiViews.click(activity, "同意并进入清屏");
        assertTrue(store.hasConsent()); assertFalse(store.isProtectionEnabled());
        assertTrue(UiViews.text(activity, "○  保护已暂停").isShown());
    }
    @Test public void disclosureCanBeDeclinedOrReadWithoutAuthorization() {
        launch(false); UiViews.click(activity, "先阅读使用教程与问题处理");
        Intent guide = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(UsageGuideActivity.class.getName(), guide.getComponent().getClassName());
        assertFalse(store.hasConsent()); assertFalse(store.isProtectionEnabled());
        UiViews.click(activity, "暂不使用"); assertTrue(activity.isFinishing());
    }
    @Test public void protectionTogglePersistsAndConnectionStatusIsHonest() {
        launch(true);
        UiViews.require(UiViews.root(activity), Switch.class, v -> "启用保护".contentEquals(v.getText())).performClick();
        assertTrue(store.isProtectionEnabled());
        assertTrue(UiViews.text(activity, "●  等待无障碍连接").isShown());
        controller.pause().resume();
        assertTrue(UiViews.require(UiViews.root(activity), Switch.class, v -> "启用保护".contentEquals(v.getText())).isChecked());
        QingPingAccessibilityService.connected = true; controller.pause().resume();
        assertTrue(UiViews.text(activity, "●  保护运行中").isShown());
        UiViews.require(UiViews.root(activity), Switch.class, v -> "启用保护".contentEquals(v.getText())).performClick();
        assertFalse(store.isProtectionEnabled());
    }
    @Test public void openingAccessibilityIsAnIntentNotAnAutomaticGrant() {
        launch(true); UiViews.click(activity, "前往开启无障碍");
        assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, Shadows.shadowOf(activity).getNextStartedActivity().getAction());
        assertFalse(QingPingAccessibilityService.isConnected()); assertFalse(store.isProtectionEnabled());
    }
    @Test public void manualSourceRejectsCriticalPackagesAndAcceptsAnOrdinarySource() {
        launch(true); UiViews.click(activity, "应用");
        UiViews.click(activity, "手动添加包名");
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        EditText input = UiViews.require(dialog.getWindow().getDecorView(), EditText.class, view -> true);
        input.setText("com.android.phone"); UiViews.clickDialog(dialog, DialogInterface.BUTTON_POSITIVE);
        assertNotNull(input.getError()); assertFalse(store.isAppEnabled("com.android.phone")); assertTrue(dialog.isShowing());
        input.setText("example.reader"); UiViews.clickDialog(dialog, DialogInterface.BUTTON_POSITIVE);
        assertTrue(store.isAppEnabled("example.reader")); assertFalse(dialog.isShowing());
    }
    @Test public void statisticsClearRequiresConfirmationAndKeepsAppSelection() {
        launch(true); store.setAppEnabled("example.reader", true); store.recordConfirmed("example.reader", "skip");
        UiViews.click(activity, "统计"); assertEquals(1, store.total(0));
        UiViews.click(activity, "设置"); UiViews.click(activity, "清空统计");
        UiViews.clickDialog(ShadowAlertDialog.getLatestAlertDialog(), DialogInterface.BUTTON_NEGATIVE);
        assertEquals(1, store.total(0)); UiViews.click(activity, "清空统计");
        UiViews.clickDialog(ShadowAlertDialog.getLatestAlertDialog(), DialogInterface.BUTTON_POSITIVE);
        assertEquals(0, store.total(0)); assertTrue(store.isAppEnabled("example.reader"));
    }
    @Test public void revocationRequiresConfirmationAndPausesProtection() {
        launch(true); store.setProtectionEnabled(true); UiViews.click(activity, "设置");
        UiViews.click(activity, "撤销同意并暂停保护");
        UiViews.clickDialog(ShadowAlertDialog.getLatestAlertDialog(), DialogInterface.BUTTON_NEGATIVE);
        assertTrue(store.hasConsent()); assertTrue(store.isProtectionEnabled());
        UiViews.click(activity, "撤销同意并暂停保护");
        UiViews.clickDialog(ShadowAlertDialog.getLatestAlertDialog(), DialogInterface.BUTTON_POSITIVE);
        assertFalse(store.hasConsent()); assertFalse(store.isProtectionEnabled());
        assertTrue(UiViews.text(activity, "启用前，请了解无障碍访问").isShown());
    }
    @Test public void tabSurvivesActivityRecreationWithoutChangingConsent() {
        launch(true); UiViews.click(activity, "统计"); Bundle state = new Bundle();
        controller.saveInstanceState(state).pause().stop().destroy();
        controller = Robolectric.buildActivity(MainActivity.class).create(state).start().resume().visible(); activity = controller.get();
        assertTrue(UiViews.text(activity, "暂时没有处理记录").isShown()); assertTrue(store.hasConsent());
    }
    @Test public void settingsTutorialAndRuleExportUseExplicitNavigation() {
        launch(true); UiViews.click(activity, "设置"); UiViews.click(activity, "打开完整教程");
        assertEquals(UsageGuideActivity.class.getName(), Shadows.shadowOf(activity).getNextStartedActivity().getComponent().getClassName());
        UiViews.click(activity, "导出规则 JSON");
        Intent export = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(Intent.ACTION_CREATE_DOCUMENT, export.getAction()); assertEquals("application/json", export.getType());
        assertTrue(store.exactRules().isEmpty()); assertFalse(store.isProtectionEnabled());
    }
    @Test public void appSearchAndSelectionDoNotEnableGlobalProtection() {
        store.setAppEnabled("example.reader", true); launch(true); UiViews.click(activity, "应用");
        EditText search = UiViews.require(UiViews.root(activity), EditText.class, v -> true);
        search.setText("no-matching-app-927");
        assertTrue(UiViews.text(activity, "没有匹配的应用。").isShown());
        search.setText("example.reader");
        UiViews.require(UiViews.root(activity), Switch.class, v -> "example.reader".contentEquals(v.getText())).performClick();
        assertFalse(store.isAppEnabled("example.reader")); assertFalse(store.isProtectionEnabled());
    }
    @Test public void ruleDialogValidatesTargetAndIdBeforeSavingExplicitChoices() {
        store.setAppEnabled("example.reader", true); launch(true); UiViews.click(activity, "应用");
        UiViews.click(activity, "配置规则"); AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        android.view.View root = dialog.getWindow().getDecorView();
        EditText target = UiViews.require(root, EditText.class, v -> v.getHint().toString().contains("目标包名"));
        EditText id = UiViews.require(root, EditText.class, v -> v.getHint().toString().contains(":id/"));
        target.setText("com.android.phone"); UiViews.clickDialog(dialog, DialogInterface.BUTTON_POSITIVE);
        assertNotNull(target.getError()); assertEquals("", store.getRule("example.reader").guardTarget);
        target.setText("example.shop"); id.setText("not-a-full-id"); UiViews.clickDialog(dialog, DialogInterface.BUTTON_POSITIVE);
        assertNotNull(id.getError()); assertTrue(dialog.isShowing());
        id.setText("example.reader:id/ad_skip");
        UiViews.require(root, CheckBox.class, v -> v.getText().toString().contains("模拟点击")).performClick();
        UiViews.clickDialog(dialog, DialogInterface.BUTTON_POSITIVE);
        assertEquals("example.shop", store.getRule("example.reader").guardTarget);
        assertEquals("example.reader:id/ad_skip", store.getRule("example.reader").skipViewId);
        assertTrue(store.isGestureAllowed("example.reader")); assertFalse(store.isProtectionEnabled());
    }
    @Test public void bootReminderIsAnIndependentOptionalSetting() {
        launch(true); UiViews.click(activity, "设置");
        Switch reminder = UiViews.require(UiViews.root(activity), Switch.class,
                v -> v.getText().toString().startsWith("开机后提醒"));
        assertFalse(reminder.isChecked()); reminder.performClick(); assertTrue(store.bootReminderEnabled());
        reminder.performClick(); assertFalse(store.bootReminderEnabled()); assertFalse(store.isProtectionEnabled());
    }
    @Test public void importedRulesRequirePreviewConfirmationAndCannotSelectAppsOrAuthorizeGestures() {
        launch(true); UiViews.click(activity, "设置"); UiViews.click(activity, "导入规则 JSON");
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, Shadows.shadowOf(activity).getNextStartedActivity().getAction());
        ExactRule rule = new ExactRule("import-test", "导入测试", "example.reader", "", "example.reader:id/close_ad",
                "关闭广告", "", true, 15);
        byte[] bytes = ExactRule.serializeDocument(Collections.singletonList(rule)).getBytes(StandardCharsets.UTF_8);
        Uri uri = Uri.parse("content://test.rules/local.json");
        Shadows.shadowOf(activity.getContentResolver()).registerInputStreamSupplier(uri, () -> new ByteArrayInputStream(bytes));
        Intent result = new Intent().setData(uri);
        activity.onActivityResult(13, Activity.RESULT_OK, result); UiViews.idle();
        assertTrue(store.exactRules().isEmpty());
        UiViews.clickDialog(ShadowAlertDialog.getLatestAlertDialog(), DialogInterface.BUTTON_NEGATIVE);
        assertTrue(store.exactRules().isEmpty());
        activity.onActivityResult(13, Activity.RESULT_OK, result); UiViews.idle();
        UiViews.clickDialog(ShadowAlertDialog.getLatestAlertDialog(), DialogInterface.BUTTON_POSITIVE);
        assertEquals(1, store.exactRules().size()); assertTrue(store.exactRules().get(0).allowGesture);
        assertFalse(store.isAppEnabled("example.reader")); assertFalse(store.isGestureAllowed("example.reader"));
        assertFalse(store.isProtectionEnabled());
    }
}
