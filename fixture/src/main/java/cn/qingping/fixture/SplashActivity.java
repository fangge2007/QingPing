package cn.qingping.fixture;

import android.app.Activity;
import android.app.Dialog;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class SplashActivity extends Activity implements SensorEventListener {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private FrameLayout frame;
    private String mode;
    private SensorManager sensorManager;
    private boolean shakeOpened;
    private Dialog testDialog;
    private int businessClicks;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        String requested = getIntent().getStringExtra("mode");
        mode = requested == null ? "skip" : requested;
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        frame = new FrameLayout(this);
        frame.setBackgroundColor(Color.rgb(226, 240, 233));
        frame.setPadding(0, dp(28), 0, dp(28));
        setContentView(frame);
        if (mode.equals("delayed") || mode.equals("late") || mode.equals("toolate")) {
            long delay = mode.equals("toolate") ? 18_000 : mode.equals("late") ? 10_000 : 3_000;
            waitForDisplay("普通内容，等待测试页面", (int) (delay / 1_000), () -> show("skip"));
        } else if (mode.equals("popup35")) {
            waitForDisplay("普通内容，35 秒后出现应用内弹窗", 35, this::showAdDialog);
        } else if (mode.equals("video")) {
            showVideo();
        } else if (mode.equals("chain") || mode.equals("repeat")) {
            showStage(1);
        } else if (mode.equals("dialog")) {
            showPlainContent("普通内容：将显示独立窗口测试弹窗");
            handler.postDelayed(this::showAdDialog, 500);
        } else if (mode.equals("business")) {
            showBusiness();
        } else if (mode.equals("split") || mode.equals("splitbusiness")) {
            showSplitControl(mode.equals("splitbusiness"));
        } else if (mode.equals("aggregate")) {
            showAggregateParent(getIntent().getBooleanExtra("longDescription", false));
        } else if (mode.equals("dangerousparent")) {
            showDangerousParent();
        } else if (mode.equals("notimportant")) {
            showUnimportantSkip();
        } else if (mode.equals("noopclick")) {
            showNoopClick();
        } else if (mode.equals("dangerousgesture")) {
            showDangerousGesture();
        } else if (mode.equals("businessdialog")) {
            show("skip");
            // Show synchronously so no event can expose an actionable background before the dialog.
            showBusinessDialog();
        } else {
            show(mode);
        }
    }

    private void waitForDisplay(String title, int seconds, Runnable action) {
        frame.removeAllViews();
        LinearLayout content = verticalContent();
        content.addView(text(title, 23));
        TextView remaining = text("剩余 " + seconds + " 秒，请保持此页前台", 18);
        content.addView(remaining);
        addMenu(content);
        countDown(remaining, seconds, action);
    }

    private void countDown(TextView remaining, int seconds, Runnable action) {
        if (seconds <= 0) {
            action.run();
            return;
        }
        remaining.setText("剩余 " + seconds + " 秒，请保持此页前台");
        handler.postDelayed(() -> countDown(remaining, seconds - 1, action), 1_000);
    }

    private void showVideo() {
        frame.removeAllViews();
        LinearLayout content = verticalContent();
        content.addView(text("广告", 30));
        content.addView(text("视频广告占位界面\n只模拟关闭时机，不播放网络视频。\n按钮前 8 秒 disabled，清屏不能提前操作。", 18));
        addMenu(content);
        Button close = edgeButton(R.id.video_close, "跳过 8", Gravity.TOP | Gravity.END);
        close.setContentDescription("关闭广告");
        close.setEnabled(false);
        close.setOnClickListener(v -> showPlainContent("视频测试结束：关闭按钮被点击"));
        updateVideoCountdown(close, 8);
    }

    private void updateVideoCountdown(Button close, int seconds) {
        if (seconds <= 0) {
            close.setText("关闭广告");
            close.setEnabled(true);
            return;
        }
        close.setText("跳过 " + seconds);
        handler.postDelayed(() -> updateVideoCountdown(close, seconds - 1), 1_000);
    }

    private void showStage(int stage) {
        frame.removeAllViews();
        LinearLayout content = verticalContent();
        content.addView(text("广告", 30));
        content.addView(text((mode.equals("chain") ? "连续两阶段" : "关闭后再次出现")
                + " · 第 " + stage + " 阶段\n全程不更换包名或 Activity。", 20));
        addMenu(content);
        int id = mode.equals("repeat") ? R.id.skip_button
                : stage == 1 ? R.id.stage_one_skip : R.id.stage_two_close;
        Button close = edgeButton(id, stage == 1 ? "跳过广告" : "关闭广告", Gravity.TOP | Gravity.END);
        close.setOnClickListener(v -> {
            if (stage == 2) {
                showPlainContent("测试结束：两个广告阶段都已关闭");
            } else if (mode.equals("chain")) {
                // No intervening clean content: do not confirm stage one as a completed dismissal.
                showStage(2);
            } else {
                // A real clean interval in the same Activity, with the same skip ID reused later.
                waitForDisplay("普通内容：第一段已关闭，稍后再展示第二段", 4, () -> showStage(2));
            }
        });
    }

    private void showPlainContent(String title) {
        frame.removeAllViews();
        frame.setContentDescription(null);
        LinearLayout content = verticalContent();
        content.addView(text(title, 24));
        content.addView(text("普通内容页面，可到清屏核对统计。", 18));
        addMenu(content);
    }

    private void showAdDialog() {
        if (isFinishing() || isDestroyed()) return;
        showPlainContent("普通内容：独立窗口测试进行中");
        testDialog = new Dialog(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(20), dp(20), dp(20));
        content.setBackgroundColor(Color.WHITE);
        content.addView(text("广告", 28));
        content.addView(text("应用内弹窗测试\n窗口属于同一测试 APP，关闭后应回到下层内容。", 18));
        Button close = new Button(this);
        close.setId(R.id.dialog_close);
        close.setText("关闭广告");
        close.setAllCaps(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(144), dp(52));
        params.gravity = Gravity.CENTER_HORIZONTAL;
        content.addView(close, params);
        close.setOnClickListener(v -> {
            testDialog.dismiss();
            showPlainContent("弹窗测试结束：独立窗口已关闭");
        });
        testDialog.setContentView(content);
        testDialog.show();
        if (testDialog.getWindow() != null) {
            testDialog.getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels - dp(32), dp(360)), -2);
        }
    }

    private void showBusiness() {
        frame.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(20), dp(20), dp(20));
        scroll.addView(content);
        frame.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        content.addView(text("普通业务控件误触测试", 24));
        TextView result = text("业务按钮被点击次数：0\n清屏应保留所有按钮且统计不变。", 18);
        // Place baits in an edge region too: safe handling must rely on semantics and context.
        addBusinessButton(content, result, R.id.business_close, "关闭", null);
        addBusinessButton(content, result, R.id.business_skip, "跳过", null);
        addBusinessButton(content, result, R.id.business_countdown_skip, "跳过 5", null);
        addBusinessButton(content, result, R.id.business_english_skip, "Skip 3", null);
        addBusinessButton(content, result, R.id.business_fake_skip, "跳过广告并下载", null);
        addBusinessButton(content, result, R.id.business_download_skip, "跳过广告", "立即下载并安装");
        content.addView(result);
        addMenu(content);
    }

    private void showSplitControl(boolean business) {
        frame.removeAllViews();
        LinearLayout content = verticalContent();
        // Independent evidence prevents the negative case passing only through absent context.
        content.addView(text("广告", 30));
        content.addView(text(business ? "拆分业务按钮误触测试" : "拆分文字关闭控件测试", 23));
        content.addView(text("右上小容器本身没有文字或描述；文字是直接、不可点击的子节点。", 18));
        TextView result = text(business
                ? "业务按钮被点击次数：0\n清屏应保留右上控件，统计不变。"
                : "关闭后应留在同一 Activity 的普通内容页，跳过统计只增加一次。", 18);
        content.addView(result);
        addMenu(content);

        LinearLayout control = new LinearLayout(this);
        control.setId(business ? R.id.compound_business_control : R.id.compound_control);
        control.setOrientation(LinearLayout.HORIZONTAL);
        control.setGravity(Gravity.CENTER);
        control.setBackgroundColor(Color.WHITE);
        control.setContentDescription(null);
        control.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        String[] fragments = business ? new String[]{"跳过", "下载"} : new String[]{"跳", "过", "广告"};
        for (String fragment : fragments) {
            TextView leaf = new TextView(this);
            leaf.setText(fragment);
            leaf.setTextSize(16);
            leaf.setTextColor(Color.rgb(24, 57, 44));
            leaf.setGravity(Gravity.CENTER);
            leaf.setClickable(false);
            leaf.setFocusable(false);
            leaf.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            control.addView(leaf, new LinearLayout.LayoutParams(-2, -1));
        }
        control.setOnClickListener(v -> {
            if (business) {
                result.setText("业务按钮被点击次数：" + (++businessClicks)
                        + "\n若非手动点击，本用例失败；这里只计数，不下载或跳转。");
            } else {
                showPlainContent("拆分控件测试结束：已进入普通内容页");
            }
        });
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(132), dp(52), Gravity.TOP | Gravity.END);
        params.topMargin = dp(12);
        params.rightMargin = dp(16);
        frame.addView(control, params);
    }

    private void showAggregateParent(boolean longDescription) {
        frame.removeAllViews();
        frame.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        if (longDescription) {
            // More than 160 characters, without acquisition words: isolate length filtering.
            StringBuilder description = new StringBuilder();
            for (int i = 0; i < 24; i++) description.append("测试父容器聚合描述。");
            frame.setContentDescription(description);
        } else {
            frame.setContentDescription("测试父容器的聚合说明：立即下载。该容器本身不可点击。");
        }
        LinearLayout content = verticalContent();
        content.addView(text("广告", 30));
        content.addView(text(longDescription ? "超长父描述与独立关闭子控件" : "危险聚合父描述与独立关闭子控件", 23));
        content.addView(text("右上是有独立监听器的安全子按钮。不能因父容器的描述就跳过整棵子树；也不能点击父容器。", 18));
        addMenu(content);
        Button close = edgeButton(R.id.aggregate_skip, "跳过广告", Gravity.TOP | Gravity.END);
        close.setOnClickListener(v -> showPlainContent("聚合描述测试结束：独立跳过子按钮被点击"));
    }

    private void showDangerousParent() {
        frame.removeAllViews();
        LinearLayout content = verticalContent();
        content.addView(text("广告", 30));
        content.addView(text("危险父容器误触测试", 23));
        TextView result = text("危险父容器被点击次数：0\n右上子文字没有点击监听器，不能借用危险父容器执行动作。", 18);
        content.addView(result);
        addMenu(content);

        FrameLayout control = new FrameLayout(this);
        control.setId(R.id.dangerous_parent);
        control.setBackgroundColor(Color.WHITE);
        control.setContentDescription("立即下载并安装");
        control.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        TextView leaf = new TextView(this);
        leaf.setId(R.id.dangerous_parent_skip_leaf);
        leaf.setText("跳过广告");
        leaf.setTextSize(16);
        leaf.setTextColor(Color.rgb(24, 57, 44));
        leaf.setGravity(Gravity.CENTER);
        leaf.setClickable(false);
        leaf.setFocusable(false);
        leaf.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        control.addView(leaf, new FrameLayout.LayoutParams(-1, -1));
        control.setOnClickListener(v -> result.setText("危险父容器被点击次数：" + (++businessClicks)
                + "\n若非手动点击，本用例失败。仅计数，不下载、安装或跳转。"));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(132), dp(52), Gravity.TOP | Gravity.END);
        params.topMargin = dp(12);
        params.rightMargin = dp(16);
        frame.addView(control, params);
    }

    private void showUnimportantSkip() {
        frame.removeAllViews();
        LinearLayout content = verticalContent();
        content.addView(text("广告", 30));
        content.addView(text("非重要原生节点测试", 23));
        content.addView(text("右上原生按钮声明 importantForAccessibility=NO。此页仅用于设备检查服务 flag 是否使节点可读取；系统不暴露时不能视为通过。", 18));
        addMenu(content);
        Button close = edgeButton(R.id.unimportant_skip, "跳过广告", Gravity.TOP | Gravity.END);
        close.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        close.setOnClickListener(v -> showPlainContent("非重要节点测试结束：原生跳过按钮被点击"));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showNoopClick() {
        frame.removeAllViews();
        LinearLayout content = verticalContent();
        content.addView(text("广告", 30));
        content.addView(text("语义点击无效果测试", 23));
        content.addView(text("右上按钮的 ACTION_CLICK 只更新计数，不关闭页面；只有真实触摸 ACTION_UP 才关闭。请勿手动点击，以免混淆自动手势结果。", 18));
        TextView result = text("语义点击请求次数：0\n未关闭前不应增加跳过统计。", 18);
        content.addView(result);
        addMenu(content);
        int[] semanticClicks = {0};
        Button close = edgeButton(R.id.image_close, "×", Gravity.TOP | Gravity.END);
        close.setContentDescription("关闭广告");
        close.setOnClickListener(v -> result.setText("语义点击请求次数：" + (++semanticClicks[0])
                + "\n按钮仍然存在，不能把请求接受当成关闭成功。"));
        close.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                showPlainContent("触摸升级测试结束：按钮收到 ACTION_UP；此前语义点击请求次数：" + semanticClicks[0]);
            }
            return true;
        });
    }

    private void showDangerousGesture() {
        frame.removeAllViews();
        FrameLayout creative = new FrameLayout(this);
        creative.setBackgroundColor(Color.rgb(244, 235, 225));
        creative.setContentDescription("立即下载并安装，测试全屏可点击父容器");
        creative.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        frame.addView(creative, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(24), dp(70), dp(24), dp(24));
        creative.addView(content, new FrameLayout.LayoutParams(-1, -1));
        content.addView(text("广告", 30));
        content.addView(text("大型危险祖先的手势误触测试", 23));
        TextView result = text("危险父容器被点击次数：0\n左上图标不可点击且不处理触摸，触摸会交给全屏危险父容器。\n即使规则与 APP 均允许手势，也必须拒绝。", 18);
        content.addView(result);
        addMenu(content);
        creative.setOnClickListener(v -> result.setText("危险父容器被点击次数：" + (++businessClicks)
                + "\n若非手动点击，本用例失败。仅计数，不下载、安装或跳转。"));

        ImageView bait = new ImageView(this);
        bait.setId(R.id.image_close);
        bait.setImageResource(R.drawable.ic_close);
        bait.setContentDescription("关闭广告");
        bait.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        bait.setFocusable(true);
        bait.setClickable(false);
        bait.setPadding(dp(14), dp(14), dp(14), dp(14));
        bait.setBackgroundColor(Color.WHITE);
        // No touch listener: a dispatched gesture would be handled by the dangerous ancestor.
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(52), dp(52), Gravity.TOP | Gravity.START);
        params.topMargin = dp(12);
        params.leftMargin = dp(16);
        creative.addView(bait, params);
    }

    private void addBusinessButton(LinearLayout content, TextView result, int id, String label, String description) {
        Button button = new Button(this);
        button.setId(id);
        button.setText(label);
        button.setAllCaps(false);
        if (description != null) button.setContentDescription(description);
        button.setOnClickListener(v -> result.setText("业务按钮被点击次数：" + (++businessClicks)
                + "\n最近按钮：" + label + "\n若非手动点击，本用例失败。"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(160), dp(52));
        params.gravity = Gravity.END;
        content.addView(button, params);
    }

    private void showBusinessDialog() {
        testDialog = new Dialog(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(20), dp(20), dp(20));
        content.addView(text("普通业务弹窗", 25));
        TextView result = text("应保留本弹窗及底层页面。业务按钮被点击次数：0", 18);
        content.addView(result);
        Button close = new Button(this);
        close.setId(R.id.business_dialog_close);
        close.setText("关闭");
        close.setOnClickListener(v -> result.setText("业务按钮被点击次数：" + (++businessClicks)
                + "\n若非手动点击，本用例失败。"));
        content.addView(close, new LinearLayout.LayoutParams(-1, dp(56)));
        testDialog.setContentView(content);
        testDialog.show();
        if (testDialog.getWindow() != null) {
            testDialog.getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels - dp(32), dp(320)), -2);
        }
    }

    private LinearLayout verticalContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(24), dp(70), dp(24), dp(24));
        frame.addView(content, new FrameLayout.LayoutParams(-1, -1));
        return content;
    }

    private Button edgeButton(int id, String label, int gravity) {
        Button button = new Button(this);
        button.setId(id);
        button.setText(label);
        button.setAllCaps(false);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(132), dp(52), gravity);
        params.topMargin = dp(12);
        params.rightMargin = dp(16);
        frame.addView(button, params);
        return button;
    }

    private void addMenu(LinearLayout content) {
        Button menu = new Button(this);
        menu.setText("打开测试菜单");
        menu.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        content.addView(menu, new LinearLayout.LayoutParams(-1, dp(56)));
    }

    private void show(String mode) {
        frame.removeAllViews();
        boolean normal = mode.equals("normal") || mode.equals("normaljump");
        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        center.setPadding(dp(28), dp(70), dp(28), dp(32));
        frame.addView(center, new FrameLayout.LayoutParams(-1, -1));
        center.addView(text(normal ? "普通内容页面" : "广告", 30));
        String description = mode.equals("shake") || mode.equals("imageclose")
                ? "测试开屏广告\n摇一摇\n这是测试界面，不含商业推广。"
                : mode.equals("jump") ? "测试开屏广告\n摇一摇触发区，可点详情模拟跳转。\n这是测试界面，不含商业推广。"
                : "测试开屏广告\n这不是商业广告，点击可测试误触跳转返回。";
        center.addView(text(normal ? "这是正常业务页面，清屏应保留当前操作。" : description, 18));

        if (!mode.equals("normal")) {
            Button jump = new Button(this);
            jump.setText(normal ? "主动打开目标 APP" : mode.equals("shake") ? "模拟摇一摇跳转" : "点击查看广告详情");
            jump.setAllCaps(false);
            jump.setOnClickListener(v -> openTarget());
            center.addView(jump, new LinearLayout.LayoutParams(-1, dp(64)));
        }

        if (!mode.equals("jump") && !mode.equals("normaljump") && !mode.equals("imageclose")) {
            Button skip = new Button(this);
            skip.setId(R.id.skip_button);
            skip.setText(normal ? "跳过" : "跳过广告 5");
            skip.setAllCaps(false);
            skip.setOnClickListener(v -> openContent());
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(120), dp(52), Gravity.TOP | Gravity.END);
            params.topMargin = dp(12);
            params.rightMargin = dp(16);
            frame.addView(skip, params);
        }

        if (mode.equals("imageclose")) addTouchOnlyClose();

        Button menu = new Button(this);
        menu.setText("打开测试菜单");
        menu.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        center.addView(menu, new LinearLayout.LayoutParams(-1, dp(56)));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addTouchOnlyClose() {
        // Deliberately expose no ACTION_CLICK: this fixture verifies an explicitly allowed gesture fallback.
        ImageView close = new ImageView(this);
        close.setId(R.id.image_close);
        close.setImageResource(R.drawable.ic_close);
        close.setContentDescription("关闭广告");
        close.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        close.setFocusable(true);
        close.setClickable(false);
        close.setPadding(dp(14), dp(14), dp(14), dp(14));
        close.setBackgroundColor(Color.WHITE);
        close.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_UP) openContent();
            return true;
        });
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(52), dp(52), Gravity.TOP | Gravity.START);
        params.topMargin = dp(12);
        params.leftMargin = dp(16);
        frame.addView(close, params);
    }

    private void openContent() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override public void onResume() {
        super.onResume();
        if (mode.equals("shake") && !shakeOpened && sensorManager != null) {
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            } else {
                Toast.makeText(this, "设备无加速度计，可使用模拟摇一摇按钮", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override public void onPause() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (shakeOpened || !mode.equals("shake")) return;
        float x = event.values[0], y = event.values[1], z = event.values[2];
        double force = Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;
        if (force >= 2.5d) {
            shakeOpened = true;
            sensorManager.unregisterListener(this);
            openTarget();
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    private void openTarget() {
        try {
            Intent intent = new Intent().setComponent(new ComponentName("cn.qingping.fixturetarget", "cn.qingping.fixturetarget.MainActivity"));
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException error) {
            Toast.makeText(this, "请先安装清屏测试目标 APK", Toast.LENGTH_LONG).show();
        }
    }

    private TextView text(String value, int size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(Color.rgb(24, 57, 44));
        view.setGravity(Gravity.CENTER);
        view.setPadding(0, dp(12), 0, dp(12));
        return view;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (testDialog != null && testDialog.isShowing()) testDialog.dismiss();
        super.onDestroy();
    }
}
