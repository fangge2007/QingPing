package cn.qingping.app;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import cn.qingping.app.rules.ExactRule;

/** Native, dependency-free UI. All automation is opt-in and locally configured. */
public final class MainActivity extends Activity {
    public static final String EXTRA_OPEN_TAB = "open_tab";
    private static final int GREEN = Color.rgb(36,118,88), INK = Color.rgb(25,46,37);
    private static final int MUTED = Color.rgb(94,111,102), BG = Color.rgb(244,247,244);
    private AppStore store;
    private LinearLayout page;
    private int tab, statsDays;
    private final Map<String, String> labels = new HashMap<>();
    private List<AppEntry> apps;
    private final android.os.Handler uiHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new AppStore(this);
        if (savedInstanceState != null) tab = savedInstanceState.getInt("tab", 0);
        else readRequestedTab(getIntent());
        render();
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent); readRequestedTab(intent); render();
    }
    private void readRequestedTab(Intent intent) {
        if (intent != null && intent.hasExtra(EXTRA_OPEN_TAB)) {
            tab = Math.max(0, Math.min(3, intent.getIntExtra(EXTRA_OPEN_TAB, 0)));
            intent.removeExtra(EXTRA_OPEN_TAB);
        }
    }
    @Override protected void onResume() { super.onResume(); apps = null; if (store != null) render(); }
    @Override protected void onSaveInstanceState(Bundle out) { out.putInt("tab", tab); super.onSaveInstanceState(out); }
    @Override protected void onDestroy() { uiHandler.removeCallbacksAndMessages(null); super.onDestroy(); }

    private void render() {
        LinearLayout root = column();
        root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        page = column(); page.setPadding(dp(24), dp(20), dp(24), dp(24));
        scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout nav = new LinearLayout(this);
        nav.setPadding(dp(8), dp(5), dp(8), dp(5)); nav.setBackgroundColor(Color.WHITE);
        String[] tabs = {"保护", "应用", "统计", "设置"};
        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            Button b = button(tabs[i], () -> { tab = index; render(); });
            b.setTextColor(i == tab ? GREEN : MUTED);
            b.setBackground(round(i == tab ? 0xFFE7F1E9 : Color.WHITE, 16));
            nav.addView(b, new LinearLayout.LayoutParams(0, dp(52), 1));
        }
        root.addView(nav);
        setContentView(root);
        if (!store.hasConsent()) { disclosure(); return; }
        if (tab == 0) home(); else if (tab == 1) applicationList(); else if (tab == 2) statistics(); else settings();
    }

    private void header(String title, String subtitle) {
        TextView brand = text("清 屏  /  QINGPING", 12, GREEN); brand.setLetterSpacing(.08f); page.addView(brand);
        TextView h = text(title, 32, INK); h.setTypeface(null, Typeface.BOLD); add(page, h, 14);
        add(page, text(subtitle, 14, MUTED), 8);
    }
    private void disclosure() {
        header("让屏幕，清净一点。", "广告辅助关闭 · 按应用管理 · 统计留在本机");
        LinearLayout card = card();
        card.addView(text("启用前，请了解无障碍访问", 20, INK));
        add(card, text("清屏会接收前台应用变化，并在屏幕解锁、您选定的 APP 位于前台时持续检查界面结构和按钮文字，包括系统允许读取的未标记为重要的视图；按已核实的来源控件或明确广告语义辅助关闭广告。进入时密集检测，之后低频重扫；成功关闭后仍可处理再次出现的广告。您单独允许后，可对明确匹配的控件模拟点击，包括普通点击受理但未关闭时的超时后备。对指定来源与目标 APP 的广告跳转尝试执行系统返回。", 15, INK), 14);
        add(card, text("界面数据可能包含敏感信息。清屏不检查输入框、不截图、不联网。仅在您主动开启30秒诊断时，在内存短暂保留少量按钮标签、控件 ID 与页面类名；您确认保存后才成为规则。不会保存整页文字，统计只含包名、日期和次数。它无法关闭其他 APP 的运动传感器或在系统层阻止跳转发生。", 15, MUTED), 12);
        add(card, text("无障碍需要您在系统设置中手动开启。您可以随时暂停保护、关闭单个 APP 或撤销授权。首次同意不会自动开启保护。", 15, MUTED), 12);
        CheckBox agree = new CheckBox(this); agree.setText("我已阅读，允许按上述用途使用界面数据"); agree.setTextSize(14); add(card, agree, 18);
        Button go = button("同意并进入清屏", () -> {
            if (!agree.isChecked()) { toast("请先阅读并勾选用途说明"); return; }
            store.setConsent(true); render();
        }); add(card, go, 12);
        add(card, button("暂不使用", this::finish), 4);
        add(card, button("先阅读使用教程与问题处理", () -> openGuide("start")), 8);
    }
    private void home() {
        header("少一点打扰。", "让每一次打开，都更接近您想看的内容。");
        boolean connected = QingPingAccessibilityService.isConnected();
        boolean enabled = store.isProtectionEnabled();
        LinearLayout state = card();
        TextView badge = text(enabled && connected ? "●  保护运行中" : enabled ? "●  等待无障碍连接" : "○  保护已暂停", 21, GREEN);
        badge.setTypeface(null, Typeface.BOLD); state.addView(badge);
        add(state, text(enabled && connected ? "已为 " + store.selectedPackages().size() + " 个 APP 启用规则。" : "先选择应用，再打开保护与系统无障碍服务。", 14, MUTED), 10);
        Switch toggle = new Switch(this); toggle.setText("启用保护"); toggle.setTextSize(17); toggle.setChecked(enabled);
        toggle.setOnCheckedChangeListener((b, checked) -> { store.setProtectionEnabled(checked); render(); }); add(state, toggle, 18);
        add(state, button(connected ? "查看无障碍设置" : "前往开启无障碍", this::openAccessibility), 14);
        LinearLayout counts = card();
        counts.addView(text("今日已确认处理", 14, MUTED));
        TextView value = text(Long.toString(store.total(1)), 46, INK); value.setTypeface(null, Typeface.BOLD); add(counts, value, 4);
        add(counts, text("累计 " + store.total(0) + " 次   ·   点击“统计”按 APP 查看", 14, MUTED), 4);
        LinearLayout guide = card(); guide.addView(text("三步开始", 18, INK));
        add(guide, text("01  在“应用”选择需要处理的 APP\n02  手动开启清屏的系统无障碍服务\n03  打开保护，重新进入目标 APP", 15, MUTED), 12);
        add(guide, button("选择要保护的应用", () -> { tab = 1; render(); }), 14);
        add(guide, button("完整使用教程与问题处理", () -> openGuide("start")), 8);
        LinearLayout sensor = card(); sensor.addView(text("华为 / vivo：先检查系统防摇一摇", 18, INK));
        add(sensor, text("部分系统提供“应用获取设备方向”或“访问设备动作与方向”控制，可优先限制广告摇一摇；具体入口随版本变化。清屏不能替您修改其他 APP 的权限。", 14, MUTED), 10);
        add(sensor, button("查看系统防摇一摇设置指南", this::sensorGuide), 10);
        LinearLayout bounds = card();
        bounds.addView(text("能力有边界，统计有依据", 18, INK));
        add(bounds, text("所选 APP 位于前台且屏幕解锁时，持续响应界面变化并低频检测后续广告。倒计时期间等待可用关闭控件，不加速视频。旧精确规则仍受进入后的时间窗口限制，新规则可单独启用持续匹配。只有观察到关闭或来源恢复，才计入次数。", 14, MUTED), 10);
        add(bounds, text("强行停止、厂商一键清理和无障碍被关闭都可能让保护失效。请查看“设置”的后台运行指南。", 14, MUTED), 10);
    }
    private void applicationList() {
        header("只管您选的。", "默认不处理任何 APP。点击规则可配置跳转目标。");
        add(page, button("使用教程：规则、点击与诊断", () -> openGuide("button")), 16);
        EditText search = input("搜索应用名或包名", ""); add(page, search, 20);
        LinearLayout list = column(); add(page, list, 12);
        List<AppEntry> entries = loadApps();
        Runnable update = () -> {
            list.removeAllViews(); String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
            for (AppEntry app : entries) {
                if (!q.isEmpty() && !(app.label + app.pkg).toLowerCase(Locale.ROOT).contains(q)) continue;
                LinearLayout row = bareCard();
                Switch on = new Switch(this); on.setText(app.label); on.setTextSize(17); on.setTextColor(INK);
                on.setChecked(store.isAppEnabled(app.pkg)); on.setOnCheckedChangeListener((v, checked) -> store.setAppEnabled(app.pkg, checked)); row.addView(on);
                add(row, text(app.pkg, 11, MUTED), 4);
                AppStore.Rule rule = store.getRule(app.pkg);
                add(row, text((rule.skipEnabled ? "广告辅助跳过" : "跳过关闭") + (rule.guardTarget.isEmpty() ? " · 跳转返回关闭" : " · 返回规则已配置"), 12, GREEN), 10);
                add(row, button("配置规则", () -> configure(app)), 6);
                add(row, button("精确关闭规则与诊断（" + store.exactRules(app.pkg).size() + "）", () -> showExactRules(app)), 4);
                add(list, row, 10);
            }
            if (list.getChildCount() == 0) list.addView(text("没有匹配的应用。", 15, MUTED));
        };
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { update.run(); }
            @Override public void afterTextChanged(Editable s) { }
        }); update.run();
        add(page, button("手动添加包名", this::manualPackage), 16);
        add(page, button("导入清屏规则 JSON", this::importRules), 8);
        add(page, text("应用列表仅查询桌面入口；不会申请读取所有安装包。没有桌面入口的 APP 可以手动添加。", 12, MUTED), 10);
    }
    private void manualPackage() {
        EditText pkg = input("例如 com.example.app", "");
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("添加来源 APP").setView(pkg)
            .setPositiveButton("添加", null).setNegativeButton("取消", null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            String p = pkg.getText().toString().trim();
            if (!validPackage(p) || criticalPackage(p)) { pkg.setError("请输入可处理的 APP 包名"); return; }
            store.setAppEnabled(p, true); apps = null; dialog.dismiss(); render();
        })); dialog.show();
    }
    private void configure(AppEntry app) {
        AppStore.Rule rule = store.getRule(app.pkg);
        LinearLayout form = column(); form.setPadding(dp(22), dp(8), dp(22), dp(4));
        CheckBox skip = new CheckBox(this); skip.setText("识别并点击广告跳过 / 关闭控件"); skip.setChecked(rule.skipEnabled); form.addView(skip);
        if (!cn.qingping.app.engine.BuiltinSplashRules.queryIds(app.pkg).isEmpty())
            add(form, text("已内置此 APP 的开屏按钮适配，无需手动填写控件 ID。仍点不动时，可按需打开下方模拟点击。", 13, GREEN), 10);
        add(form, text("跳转返回：填写一个目标 APP 的精确包名。仅在短时广告场景从当前 APP 进入该目标后返回。留空即关闭。正常广告链接也可能被返回，可在设置中临时放行。", 13, MUTED), 12);
        EditText target = input("目标包名（可选）", rule.guardTarget); add(form, target, 8);
        add(form, button("从应用列表选择跳转目标", () -> {
            List<AppEntry> choices = new ArrayList<>();
            for (AppEntry item : loadApps()) if (!item.pkg.equals(app.pkg)) choices.add(item);
            String[] names = new String[choices.size()];
            for (int i = 0; i < choices.size(); i++) names[i] = choices.get(i).label + "\n" + choices.get(i).pkg;
            new AlertDialog.Builder(this).setTitle("选择广告跳转目标")
                    .setItems(names, (d,w) -> target.setText(choices.get(w).pkg)).setNegativeButton("取消", null).show();
        }), 8);
        add(form, button("打开此 APP 的系统权限页", () -> open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + app.pkg)))), 6);
        add(form, text("精确跳过控件 ID（高级选项）：仅填写已核实的广告跳过控件，填错可能点击普通按钮。留空使用保守的文案与广告场景规则。", 13, MUTED), 12);
        EditText id = input("来源包名:id/skip（可选）", rule.skipViewId); add(form, id, 8);
        CheckBox gesture = new CheckBox(this); gesture.setText(R.string.allow_app_gesture); gesture.setChecked(store.isGestureAllowed(app.pkg)); add(form, gesture, 12);
        add(form, text("普通点击失败，或受理后超过 2.5 秒仍未关闭时，可对重新匹配的精确控件模拟中心点击；不对通用“关闭”盲点屏幕。布局变化仍可能造成误点，可随时关闭。", 12, MUTED), 6);
        ScrollView scroll = new ScrollView(this); scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(app.label + " · 规则").setView(scroll)
            .setPositiveButton("保存", null).setNegativeButton("取消", null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            String t = target.getText().toString().trim(), viewId = id.getText().toString().trim();
            if (!t.isEmpty() && (!validPackage(t) || criticalPackage(t) || t.equals(app.pkg))) { target.setError("请输入其他非系统关键 APP 的精确包名"); return; }
            if (!viewId.isEmpty() && !viewId.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+:id/[A-Za-z_][A-Za-z0-9_]*")) { id.setError("格式应为完整资源命名空间:id/控件名"); return; }
            store.setRule(app.pkg, skip.isChecked(), t, viewId); store.setGestureAllowed(app.pkg, gesture.isChecked()); dialog.dismiss(); render();
        })); dialog.show();
    }
    private void showExactRules(AppEntry app) {
        LinearLayout form = column(); form.setPadding(dp(20), dp(10), dp(20), dp(14));
        form.addView(text("规则仅适用于 " + app.pkg + "。先核实广告控件再保存；不会自动点击普通“关闭”。", 13, MUTED));
        add(form, button("手动添加精确关闭规则", () -> editExactRule(app, null, "")), 10);
        add(form, button("开始 30 秒按钮诊断", () -> {
            RuleDiagnostics.arm(app.pkg, android.os.SystemClock.elapsedRealtime());
            toast("30 秒内打开目标 APP，再回到此页查看诊断；需已开启该 APP 和保护");
        }), 8);
        add(form, button("刷新诊断结果", () -> showDiagnostic(app)), 8);
        for (ExactRule r : store.exactRules(app.pkg)) {
            add(form, text(r.name + "\n页面：" + (r.activity.isEmpty() ? "不限页面" : r.activity)
                    + "\nID：" + r.viewId + "\n文案：" + r.text + "\n描述：" + r.description
                    + "\n范围：" + (r.persistent ? "APP 前台持续匹配" : "进入后 " + r.windowSeconds + " 秒") + " · 模拟点击 " + (r.allowGesture ? "规则允许，另需 APP 开关" : "关闭"), 13, INK), 18);
            add(form, button("删除此规则", () -> new AlertDialog.Builder(this).setTitle("删除“" + r.name + "”？")
                    .setPositiveButton("删除", (d,w) -> {
                        List<ExactRule> all = store.exactRules(); all.removeIf(item -> item.key.equals(r.key));
                        store.setExactRules(all); toast("规则已删除");
                    }).setNegativeButton("取消", null).show()), 6);
        }
        ScrollView scroll = new ScrollView(this); scroll.addView(form);
        new AlertDialog.Builder(this).setTitle(app.label + " · 精确规则").setView(scroll).setPositiveButton("完成", null).show();
    }
    private void showDiagnostic(AppEntry app) {
        RuleDiagnostics.Report report = RuleDiagnostics.report(app.pkg, android.os.SystemClock.elapsedRealtime());
        if (report == null) { toast("暂无诊断。开始后请在 30 秒内打开目标 APP，再回来刷新"); return; }
        LinearLayout form = column(); form.setPadding(dp(20), dp(10), dp(20), dp(14));
        form.addView(text("页面：" + (report.activity.isEmpty() ? "未取得可靠页面类名" : report.activity)
                + "\n扫描节点：" + report.nodes + " · " + (report.complete ? "完整" : "达到扫描上限")
                + "\n广告特征：" + (report.ad ? "已检测" : "未检测") + "\n" + report.status, 13, INK));
        add(form, text("以下只是短时检测到的按钮候选，不能证明都是广告关闭按钮。请核实后再创建规则。结果只留在内存，2 分钟后失效。", 12, MUTED), 10);
        for (RuleDiagnostics.Control c : report.controls) {
            add(form, text("ID：" + c.viewId + "\n文案：" + c.label + "\n描述：" + c.description
                    + "\n控件声明可点击：" + (c.clickable ? "是" : "否"), 13, INK), 16);
            add(form, button("用此候选创建规则", () -> {
                if (RuleDiagnostics.report(app.pkg, android.os.SystemClock.elapsedRealtime()) != report) { toast("诊断已过期，请重新开始"); return; }
                editExactRule(app, c, report.activity);
            }), 6);
        }
        if (report.controls.isEmpty()) add(form, text("未发现可读关闭控件。纯画布、图片或被保护的节点仍可能无法处理。", 14, MUTED), 16);
        ScrollView scroll = new ScrollView(this); scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("本次按钮诊断").setView(scroll).setPositiveButton("关闭", null).create();
        Runnable expire = dialog::dismiss;
        dialog.setOnDismissListener(d -> { uiHandler.removeCallbacks(expire); form.removeAllViews(); });
        dialog.show();
        uiHandler.postDelayed(expire, Math.max(1, report.time + 120_000 - android.os.SystemClock.elapsedRealtime()));
    }
    private void editExactRule(AppEntry app, RuleDiagnostics.Control candidate, String activityName) {
        LinearLayout form = column(); form.setPadding(dp(20), dp(8), dp(20), dp(12));
        add(form, text("所有非空条件必须同时匹配。没有资源 ID 时必须指定完整页面类名。", 13, MUTED), 4);
        EditText name = input("规则名称", "广告关闭"); add(form, name, 10);
        EditText activity = input("完整 Activity 类名（可选）", activityName); add(form, activity, 10);
        EditText id = input("完整 namespace:id/name", candidate == null ? "" : candidate.viewId); add(form, id, 10);
        EditText label = input("精确按钮文案（可选）", candidate != null && candidate.viewId.isEmpty() ? candidate.label : ""); add(form, label, 10);
        EditText desc = input("精确无障碍描述（可选）", candidate == null ? "" : candidate.description); add(form, desc, 10);
        EditText seconds = input("检测窗口秒数，3–30", "15"); seconds.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); add(form, seconds, 10);
        CheckBox persistent = new CheckBox(this); persistent.setText("持续匹配（用于应用内弹窗 / 视频关闭）"); add(form, persistent, 10);
        add(form, text("持续匹配会在进入 APP 很久后仍检查此规则，请核实它只命中广告关闭控件。旧规则默认保留进入后的时间窗口。", 12, MUTED), 6);
        CheckBox gesture = new CheckBox(this); gesture.setText(R.string.allow_rule_gesture); add(form, gesture, 12);
        add(form, text("保存不会自动打开 APP 保护或模拟点击开关。控件变化时请关闭或删除规则。", 12, MUTED), 6);
        ScrollView scroll = new ScrollView(this); scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(app.label + " · 新增规则").setView(scroll)
                .setPositiveButton("保存", null).setNegativeButton("取消", null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            try {
                ExactRule rule = new ExactRule("local-" + Long.toHexString(System.nanoTime()), name.getText().toString().trim(),
                        app.pkg, activity.getText().toString().trim(), id.getText().toString().trim(),
                        label.getText().toString(), desc.getText().toString(), gesture.isChecked(), Integer.parseInt(seconds.getText().toString()), persistent.isChecked());
                List<ExactRule> all = store.exactRules(); all.add(rule); store.setExactRules(all);
                dialog.dismiss(); toast("精确规则已保存；请检查该 APP 的保护及模拟点击开关");
            } catch (IllegalArgumentException ex) { toast(ex.getMessage() == null ? "规则格式不正确" : ex.getMessage()); }
        })); dialog.show();
    }
    private void importRules() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
        try { startActivityForResult(intent, 13); } catch (android.content.ActivityNotFoundException ex) { toast("此系统未提供文件选择器"); }
    }
    private void exportRules() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE)
                .putExtra(Intent.EXTRA_TITLE, "清屏规则.json");
        try { startActivityForResult(intent, 14); } catch (android.content.ActivityNotFoundException ex) { toast("此系统未提供文件选择器"); }
    }
    private void previewImport(List<ExactRule> incoming) {
        StringBuilder preview = new StringBuilder("将导入 ").append(incoming.size()).append(" 条规则，按 key 合并。导入不会自动打开任何 APP 或模拟点击。\n\n");
        for (ExactRule r : incoming) preview.append(r.name).append("\n来源：").append(r.packageName)
                .append("\n页面：").append(r.activity).append("\nID：").append(r.viewId).append("\n文案：").append(r.text)
                .append("\n描述：").append(r.description).append("\n范围：").append(r.persistent ? "APP 前台持续匹配" : "进入后 " + r.windowSeconds + " 秒")
                .append("\n模拟点击：").append(r.allowGesture ? "允许，另需 APP 开关" : "关闭").append("\n\n");
        TextView text = text(preview.toString(), 13, INK); text.setPadding(dp(20),dp(12),dp(20),dp(12));
        ScrollView scroll = new ScrollView(this); scroll.addView(text);
        new AlertDialog.Builder(this).setTitle("核对导入规则").setView(scroll).setNegativeButton("取消", null)
                .setPositiveButton("确认合并", (d,w) -> {
                    try {
                        Map<String, ExactRule> merged = new java.util.LinkedHashMap<>();
                        for (ExactRule r : store.exactRules()) merged.put(r.key, r);
                        for (ExactRule r : incoming) merged.put(r.key, r);
                        store.setExactRules(new ArrayList<>(merged.values())); apps = null; render(); toast("规则已导入，请逐 APP 启用");
                    } catch (IllegalArgumentException ex) { toast(ex.getMessage()); }
                }).show();
    }
    private void statistics() {
        header("每次处理，都有迹可循。", "按来源 APP 统计 · 保存在本机 · 可导出 CSV");
        LinearLayout filter = new LinearLayout(this);
        int[] periods = {1, 7, 0}; String[] names = {"今天", "近 7 天", "全部"};
        for (int i = 0; i < periods.length; i++) {
            final int days = periods[i]; Button b = button(names[i], () -> { statsDays = days; render(); });
            b.setBackground(round(statsDays == days ? 0xFFE0EDE3 : Color.WHITE, 14));
            filter.addView(b, new LinearLayout.LayoutParams(0, dp(48), 1));
        } add(page, filter, 20);
        LinearLayout overview = card(); overview.addView(text("已确认处理  " + store.total(statsDays) + " 次", 24, INK));
        add(overview, text("广告跳过与跳转返回分别计数。请求失败、超时及仅发现广告不计数。", 13, MUTED), 10);
        List<AppStore.Stat> rows = store.stats(statsDays);
        long max = 1; for (AppStore.Stat s : rows) max = Math.max(max, s.skip + s.returned);
        for (AppStore.Stat s : rows) {
            LinearLayout row = card(); row.addView(text(label(s.pkg), 18, INK));
            add(row, text(s.pkg, 11, MUTED), 4);
            add(row, text("跳过 " + s.skip + "    ·    返回 " + s.returned, 15, GREEN), 12);
            LinearLayout track = new LinearLayout(this); track.setBackground(round(0xFFE9EFEA, 3));
            View bar = new View(this); bar.setBackground(round(GREEN, 3));
            track.addView(bar, new LinearLayout.LayoutParams(0, dp(6), Math.max(.01f, (float)(s.skip + s.returned) / max)));
            View blank = new View(this); track.addView(blank, new LinearLayout.LayoutParams(0, dp(6), Math.max(.001f, 1f - (float)(s.skip + s.returned) / max))); add(row, track, 14);
        }
        if (rows.isEmpty()) { LinearLayout empty = card(); empty.addView(text("暂时没有处理记录", 18, INK)); add(empty, text("启用规则后重新进入目标 APP。没有可确认的界面变化时，次数会保持为零。", 14, MUTED), 10); }
        add(page, button("导出按日统计 CSV", this::export), 16);
    }
    private void settings() {
        header("安心运行，明白授权。", "随时可停用，所有规则与统计都留在设备内。");
        LinearLayout tutorial = card(); tutorial.addView(text("使用教程与问题处理", 19, INK));
        add(tutorial, text("从首次启用到不同广告、后台恢复与规则诊断，按现象查找通用应对步骤。", 14, MUTED), 10);
        add(tutorial, button("打开完整教程", () -> openGuide("start")), 10);
        LinearLayout status = card(); status.addView(text("运行检查", 19, INK));
        add(status, text("系统无障碍开关：" + (accessibilityEnabled() ? "已开启" : "未开启") + "\n服务实际连接：" + (QingPingAccessibilityService.isConnected() ? "已连接" : "未连接") + "\n保护总开关：" + (store.isProtectionEnabled() ? "开启" : "关闭"), 15, MUTED), 12);
        add(status, button("刷新运行状态", this::render), 10);
        add(status, button("打开无障碍设置", this::openAccessibility), 4);
        add(status, button("系统防摇一摇设置指南", this::sensorGuide), 6);
        LinearLayout background = card(); background.addView(text("开机与后台运行", 19, INK));
        add(background, text("已启用的无障碍服务由 Android 绑定与恢复；开机解锁后通常会重新连接。清屏不会假冒前台服务保活，也无法从代码重新开启被关闭的无障碍。", 14, MUTED), 12);
        PowerManager pm = getSystemService(PowerManager.class);
        add(background, text("电池优化：" + (pm.isIgnoringBatteryOptimizations(getPackageName()) ? "当前已豁免" : "当前未豁免") + "\n建议在系统中允许后台活动，并按品牌设置允许自启动、锁定最近任务。厂商菜单位置不同。", 14, MUTED), 12);
        add(background, button("打开电池优化列表", () -> open(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))), 10);
        add(background, button("打开清屏应用详情", this::appDetails), 4);
        add(background, text("划掉最近任务通常只移除界面，服务仍可能继续；厂商一键清理可能直接结束服务。设置中的“强行停止”后需要您重新打开 APP，并视情况重开无障碍；无法保证自动恢复。", 14, MUTED), 12);
        Switch reminder = new Switch(this); reminder.setText("开机后提醒检查状态（可选）"); reminder.setChecked(store.bootReminderEnabled());
        reminder.setOnCheckedChangeListener((b, checked) -> {
            store.setBootReminderEnabled(checked);
            if (checked && Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 11);
        }); add(background, reminder, 16);
        add(background, text("通知仅用于开机/更新后的检查提醒，不影响广告识别。拒绝通知或关闭通知渠道将无法看到提醒。", 12, MUTED), 8);
        LinearLayout jump = card(); jump.addView(text("临时允许广告跳转", 19, INK));
        boolean allowing = store.getAllowUntilElapsed() > android.os.SystemClock.elapsedRealtime();
        add(jump, text(allowing ? "当前已临时放行，最长 10 分钟。跳过规则仍有效。" : "需要主动打开广告链接时，可临时暂停所有跳转返回规则。", 14, MUTED), 10);
        add(jump, button(allowing ? "结束放行" : "允许跳转 10 分钟", () -> { if (allowing) store.endJumpAllowance(); else store.allowJumpsForTenMinutes(); render(); }), 10);
        LinearLayout privacy = card(); privacy.addView(text("数据与权限", 19, INK));
        add(privacy, text("本软件不申请联网、悬浮窗、读取使用记录、读取全部安装包或存储权限。导出通过系统文件选择器完成。统计仅含包名、日期、类型与次数，卸载会删除本机数据。", 14, MUTED), 12);
        add(privacy, button("导出统计 CSV", this::export), 10);
        add(privacy, button("清空统计", () -> new AlertDialog.Builder(this).setTitle("清空全部统计？").setMessage("此操作不可恢复，应用规则会保留。")
            .setPositiveButton("清空", (d,w) -> { store.clearStats(); render(); }).setNegativeButton("取消", null).show()), 4);
        add(privacy, button("撤销同意并暂停保护", () -> new AlertDialog.Builder(this).setTitle("撤销数据用途同意？")
            .setMessage("清屏将立即停止处理。您还可以在下一步的系统设置中关闭无障碍服务。")
            .setPositiveButton("撤销", (d,w) -> { store.setConsent(false); render(); openAccessibility(); }).setNegativeButton("取消", null).show()), 4);
        LinearLayout rules = card(); rules.addView(text("本地广告关闭规则", 19, INK));
        add(rules, text("当前 " + store.exactRules().size() + " 条精确规则。只支持清屏格式，不直接执行 GKD 订阅或脚本。导入会先展示内容，确认后按 key 合并；仍需逐 APP 开启。", 14, MUTED), 10);
        add(rules, button("导入规则 JSON", this::importRules), 8);
        add(rules, button("导出规则 JSON", this::exportRules), 6);
        add(page, text("清屏 0.4.2  ·  Android 8.0+\n无 Root 版本：辅助关闭与跳转返回，不能禁用其他 APP 的摇一摇传感器。", 12, MUTED), 22);
    }
    private boolean accessibilityEnabled() {
        AccessibilityManager manager = getSystemService(AccessibilityManager.class);
        ComponentName expected = new ComponentName(this, QingPingAccessibilityService.class);
        for (AccessibilityServiceInfo info : manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            if (info.getResolveInfo() != null && info.getResolveInfo().serviceInfo != null) {
                android.content.pm.ServiceInfo service = info.getResolveInfo().serviceInfo;
                if (expected.equals(new ComponentName(service.packageName, service.name))) return true;
            }
        } return false;
    }
    private void sensorGuide() {
        LinearLayout form = column(); form.setPadding(dp(20),dp(10),dp(20),dp(14));
        form.addView(text("系统权限比跳转后返回更直接。以下为已核对的入口，具体机型可能没有该项。请在目标 APP 的权限中手动调整，清屏无法代为修改。", 14, INK));
        add(form, text("华为 HarmonyOS 4.2 / 4.3\n设置 → 应用和服务 → 权限管理 → 应用获取设备方向 → 关闭对应 APP。\n华为官方另列出 HarmonyOS 6.0.0.120+ 的入口：设置 → 隐私和安全 → 应用获取设备方向。安卓 APK 是否能安装需另看系统兼容性。", 14, MUTED), 14);
        add(form, button("查看华为官方说明", () -> open(new Intent(Intent.ACTION_VIEW, Uri.parse("https://consumer.huawei.com/cn/support/content/zh-cn16002151/")))), 8);
        add(form, text("vivo 部分 OriginOS 5 及后续适用版本\n设置 → 隐私 → 权限管理 → 权限 → 访问设备动作与方向。若提供“仅开屏时禁止”，可优先选择；没有时请按本机提供的选项调整。来源为 vivo 官网社区教程，不能代表全部机型。", 14, MUTED), 14);
        add(form, button("查看 vivo 官网社区教程", () -> open(new Intent(Intent.ACTION_VIEW, Uri.parse("https://bbs.vivo.com.cn/newbbs/thread/38170906")))), 8);
        add(form, text("禁止运动传感器可能影响游戏体感、方向感知等功能；优先选择仅限制开屏的选项。它限制摇一摇触发，不会删除广告画面。系统拦截量不纳入清屏的操作次数。", 13, MUTED), 14);
        add(form, button("打开系统设置", () -> open(new Intent(Settings.ACTION_SETTINGS))), 10);
        ScrollView scroll = new ScrollView(this); scroll.addView(form);
        new AlertDialog.Builder(this).setTitle("系统防摇一摇指南").setView(scroll).setPositiveButton("完成", null).show();
    }
    private void openAccessibility() { open(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
    private void openGuide(String topic) { startActivity(new Intent(this, UsageGuideActivity.class).putExtra(UsageGuideActivity.EXTRA_TOPIC, topic)); }
    private void appDetails() { open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))); }
    private void open(Intent intent) {
        try { startActivity(intent); } catch (android.content.ActivityNotFoundException | SecurityException ex) { toast("此系统没有对应设置入口，请在系统设置中手动打开"); }
    }
    private void export() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/csv").addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_TITLE, "清屏统计-" + LocalDate.now() + ".csv");
        try { startActivityForResult(intent, 12); } catch (android.content.ActivityNotFoundException ex) { toast("此系统未提供文件选择器"); }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        if (request == 13) {
            try (InputStream in = getContentResolver().openInputStream(data.getData()); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                if (in == null) throw new java.io.IOException("No stream");
                byte[] chunk = new byte[4096]; int n;
                while ((n = in.read(chunk)) != -1) { if (bytes.size() + n > ExactRule.MAX_DOCUMENT_BYTES) throw new IllegalArgumentException("规则文件超过 256KB"); bytes.write(chunk, 0, n); }
                previewImport(ExactRule.parseDocument(bytes.toString(StandardCharsets.UTF_8.name())));
            } catch (Exception ex) { toast("导入失败：" + (ex.getMessage() == null ? "无法读取文件" : ex.getMessage())); }
            return;
        }
        if (request != 12 && request != 14) return;
        try (OutputStream out = getContentResolver().openOutputStream(data.getData(), "wt")) {
            if (out == null) throw new java.io.IOException("No stream");
            out.write((request == 12 ? store.csv() : ExactRule.serializeDocument(store.exactRules())).getBytes(StandardCharsets.UTF_8)); toast("文件已导出");
        } catch (Exception ex) { toast("导出失败，请重新选择保存位置"); }
    }
    private List<AppEntry> loadApps() {
        if (apps != null) return apps;
        Map<String, AppEntry> found = new HashMap<>();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo info : getPackageManager().queryIntentActivities(main, 0)) {
            String pkg = info.activityInfo.packageName;
            if (!criticalPackage(pkg)) found.put(pkg, new AppEntry(pkg, info.loadLabel(getPackageManager()).toString()));
        }
        for (String pkg : store.selectedPackages()) if (!criticalPackage(pkg) && !found.containsKey(pkg)) found.put(pkg, new AppEntry(pkg, label(pkg)));
        for (ExactRule r : store.exactRules()) if (!criticalPackage(r.packageName) && !found.containsKey(r.packageName)) found.put(r.packageName, new AppEntry(r.packageName, label(r.packageName)));
        apps = new ArrayList<>(found.values());
        Collator collator = Collator.getInstance(Locale.CHINA);
        apps.sort((a,b) -> collator.compare(a.label, b.label));
        for (AppEntry e : apps) labels.put(e.pkg, e.label);
        return apps;
    }
    private String label(String pkg) {
        if (labels.containsKey(pkg)) return labels.get(pkg);
        try { return getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg, 0)).toString(); }
        catch (PackageManager.NameNotFoundException ex) { return pkg; }
    }
    private boolean criticalPackage(String p) {
        return cn.qingping.app.engine.PackagePolicy.isCritical(p) || p.equals(getPackageName());
    }
    private static boolean validPackage(String p) { return p.matches("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+"); }
    private static final class AppEntry {
        final String pkg, label;
        AppEntry(String pkg, String label) { this.pkg = pkg; this.label = label; }
    }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout bareCard() { LinearLayout l = column(); l.setPadding(dp(18),dp(18),dp(18),dp(18)); l.setBackground(round(Color.WHITE, 22)); return l; }
    private LinearLayout card() { LinearLayout l = bareCard(); add(page, l, 18); return l; }
    private TextView text(String value, int sp, int color) { TextView t = new TextView(this); t.setText(value); t.setTextSize(sp); t.setTextColor(color); t.setLineSpacing(dp(3), 1.05f); return t; }
    private Button button(String title, Runnable action) { Button b = new Button(this); b.setText(title); b.setTextSize(14); b.setTextColor(GREEN); b.setAllCaps(false); b.setMinHeight(dp(48)); b.setMinimumHeight(dp(48)); b.setBackground(round(0xFFEDF4ED, 13)); b.setOnClickListener(v -> action.run()); return b; }
    private EditText input(String hint, String value) { EditText e = new EditText(this); e.setSingleLine(true); e.setTextSize(14); e.setHint(hint); e.setText(value); e.setTextColor(INK); e.setPadding(dp(12),dp(12),dp(12),dp(12)); e.setBackground(round(0xFFEAF0EA, 12)); return e; }
    private void add(LinearLayout parent, View view, int top) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.topMargin = dp(top); parent.addView(view, p); }
    private GradientDrawable round(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private int dp(float value) { return (int)(getResources().getDisplayMetrics().density * value + .5f); }
    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_SHORT).show(); }
}
