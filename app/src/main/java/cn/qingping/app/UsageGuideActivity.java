package cn.qingping.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Offline, read-only help. Opening a tutorial never enables protection or changes a rule. */
public final class UsageGuideActivity extends Activity {
    public static final String EXTRA_TOPIC = "guide_topic";
    private static final int GREEN = Color.rgb(36, 118, 88), INK = Color.rgb(25, 46, 37);
    private static final int MUTED = Color.rgb(94, 111, 102);
    private final Set<String> expanded = new HashSet<>();
    private AppStore store;
    private ScrollView scroll;
    private LinearLayout topics;
    private EditText search;
    private TextView status, matches;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new AppStore(this);
        String requested = getIntent().getStringExtra(EXTRA_TOPIC);
        if (savedInstanceState != null) {
            ArrayList<String> ids = savedInstanceState.getStringArrayList("expanded");
            if (ids != null) expanded.addAll(ids);
        } else expanded.add(requested == null ? "start" : requested);

        LinearLayout root = column();
        root.setBackgroundColor(Color.rgb(244, 247, 244));
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page = column(); page.setPadding(dp(24), dp(16), dp(24), dp(28));
        scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
        setContentView(root);

        add(page, button("返回清屏", this::finish), 0);
        TextView title = text("使用教程与问题处理", 28, INK);
        title.setTypeface(null, Typeface.BOLD); add(page, title, 18);
        add(page, text("适用于不同 APP 与支持 Android APK 的手机。按步骤启用，按现象查找应对；系统菜单名称以本机为准。教程全部保存在本机，可离线查看。", 15, MUTED), 10);

        LinearLayout check = card(); add(page, check, 18);
        check.addView(text("先看运行条件", 19, INK));
        status = text("", 15, MUTED); add(check, status, 10);
        add(check, text("已连接仅说明服务可运行，不代表每条广告都已识别或关闭。", 13, MUTED), 6);
        add(check, button("打开系统无障碍设置", () -> openSettings(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))), 12);
        add(check, button("前往应用配置", () -> openTab(1)), 6);
        add(check, button("查看运行与后台设置", () -> openTab(3)), 6);

        search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(16);
        search.setHint("搜索问题，例如：点不动、倒计时、后台");
        search.setContentDescription("搜索使用教程和应对策略");
        search.setPadding(dp(12), dp(12), dp(12), dp(12));
        search.setBackground(round(0xFFEAF0EA, 12));
        add(page, search, 22);
        matches = text("", 13, MUTED); add(page, matches, 8);
        add(page, button("清除搜索并收起全部", () -> { expanded.clear(); search.setText(""); renderTopics(); }), 8);
        topics = column(); add(page, topics, 4);
        add(page, text("阅读教程不会自动授权、修改规则或开启模拟点击。需要调整时，请通过对应设置入口主动操作。", 13, MUTED), 20);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderTopics(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        if (savedInstanceState != null) search.setText(savedInstanceState.getString("query", ""));
        renderTopics();
        if (savedInstanceState != null) {
            int position = savedInstanceState.getInt("scroll", 0);
            scroll.post(() -> scroll.scrollTo(0, position));
        } else if (requested != null) {
            scroll.post(() -> {
                for (int i = 0; i < topics.getChildCount(); i++) {
                    android.view.View child = topics.getChildAt(i);
                    if (requested.equals(child.getTag())) {
                        scroll.smoothScrollTo(0, topics.getTop() + child.getTop());
                        break;
                    }
                }
            });
        }
    }

    @Override protected void onResume() { super.onResume(); refreshStatus(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putStringArrayList("expanded", new ArrayList<>(expanded));
        state.putString("query", search.getText().toString());
        state.putInt("scroll", scroll.getScrollY());
        super.onSaveInstanceState(state);
    }

    private void refreshStatus() {
        if (status == null) return;
        String value = String.format(Locale.ROOT, "用途同意：%s\n无障碍服务：%s\n保护总开关：%s\n已选择 APP：%d 个",
                store.hasConsent() ? "已完成" : "尚未完成，请返回清屏阅读同意",
                QingPingAccessibilityService.isConnected() ? "已连接" : "未连接或正在等待系统连接",
                store.isProtectionEnabled() ? "已开启" : "已暂停", store.selectedPackages().size());
        status.setText(value);
    }

    private void renderTopics() {
        if (topics == null) return;
        topics.removeAllViews();
        String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        int count = 0;
        for (UsageGuide.Topic topic : UsageGuide.topics()) {
            String content = (topic.title + "\n" + topic.summary + "\n" + topic.body).toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !content.contains(query)) continue;
            count++;
            boolean isOpen = !query.isEmpty() || expanded.contains(topic.id);
            LinearLayout card = card(); card.setTag(topic.id);
            TextView body = text(topic.body, 16, INK);
            body.setTextIsSelectable(true);
            body.setVisibility(isOpen ? android.view.View.VISIBLE : android.view.View.GONE);
            if (query.isEmpty()) {
                String label = String.format(Locale.ROOT, "%s · %s", topic.title, isOpen ? "收起" : "展开");
                Button toggle = button(label, () -> { });
                toggle.setOnClickListener(view -> {
                    boolean opening = !expanded.contains(topic.id);
                    if (opening) expanded.add(topic.id); else expanded.remove(topic.id);
                    body.setVisibility(opening ? android.view.View.VISIBLE : android.view.View.GONE);
                    toggle.setText(String.format(Locale.ROOT, "%s · %s", topic.title, opening ? "收起" : "展开"));
                });
                card.addView(toggle);
            } else {
                TextView heading = text(topic.title, 19, INK);
                heading.setTypeface(null, Typeface.BOLD);
                card.addView(heading);
            }
            add(card, text(topic.summary, 14, MUTED), 9);
            add(card, body, 13);
            add(topics, card, 12);
        }
        String result = String.format(Locale.ROOT, query.isEmpty() ? "共 %d 个主题，点标题展开" : "找到 %d 个相关主题", count);
        matches.setText(result);
        if (count == 0) add(topics, text("没有找到对应内容。可换用“开屏”“点击”“规则”“摇一摇”“统计”或“后台”，也可清除搜索查看完整目录。", 15, MUTED), 16);
    }

    private void openTab(int tab) {
        startActivity(new Intent(this, MainActivity.class).putExtra(MainActivity.EXTRA_OPEN_TAB, tab)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }
    private void openSettings(Intent intent) {
        try { startActivity(intent); }
        catch (android.content.ActivityNotFoundException | SecurityException ex) {
            Toast.makeText(this, "本机未提供此快捷入口，请手动打开系统设置查找", Toast.LENGTH_LONG).show();
        }
    }
    private LinearLayout column() { LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.VERTICAL); return view; }
    private LinearLayout card() { LinearLayout view = column(); view.setPadding(dp(18), dp(18), dp(18), dp(18)); view.setBackground(round(Color.WHITE, 20)); return view; }
    private TextView text(String value, int sp, int color) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(sp); view.setTextColor(color);
        view.setLineSpacing(dp(3), 1.05f); return view;
    }
    private Button button(String label, Runnable action) {
        Button view = new Button(this); view.setText(label); view.setTextSize(14); view.setTextColor(GREEN);
        view.setAllCaps(false); view.setMinHeight(dp(48)); view.setMinimumHeight(dp(48));
        view.setBackground(round(0xFFEDF4ED, 12)); view.setOnClickListener(v -> action.run()); return view;
    }
    private void add(LinearLayout parent, android.view.View child, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.topMargin = dp(top); parent.addView(child, params);
    }
    private GradientDrawable round(int color, int radius) { GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(dp(radius)); return drawable; }
    private int dp(float value) { return Math.round(getResources().getDisplayMetrics().density * value); }
}
