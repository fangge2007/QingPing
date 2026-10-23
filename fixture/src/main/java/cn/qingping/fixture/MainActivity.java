package cn.qingping.fixture;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Deliberately simple screens that expose stable accessibility nodes. */
public final class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(24);
        root.setPadding(padding, padding, padding, padding);
        root.setBackgroundColor(Color.rgb(241, 247, 244));
        scroll.addView(root);
        TextView title = new TextView(this);
        title.setText("清屏测试 · 内容页");
        title.setTextSize(26);
        root.addView(title);
        TextView info = new TextView(this);
        info.setText("这是测试 APP，不展示真实广告，也不会联网。\n菜单可直接启动每个场景；等待场景请保持前台，不要中途切换应用。普通按钮用于检查误触，不能当成关闭成功。\n\n连续广告场景全程保留在同一个 Activity：两阶段广告应在最后关闭时确认一次；广告再次出现应分别确认两次。每项操作前先记下清屏统计。\n");
        info.setTextSize(16);
        root.addView(info);
        add(root, "显示右上角跳过广告", "skip");
        add(root, "显示跳转广告（无跳过按钮）", "jump");
        add(root, "左上图标关闭（仅触摸可操作）", "imageclose");
        add(root, "真实运动传感器 + 手动模拟跳转", "shake");
        add(root, "延迟 3 秒显示广告", "delayed");
        add(root, "延迟 10 秒显示广告", "late");
        add(root, "延迟 18 秒显示广告", "toolate");
        add(root, "35 秒后显示应用内弹窗（保持前台）", "popup35");
        add(root, "视频倒计时：8 秒后关闭才可用", "video");
        add(root, "连续两阶段广告（最终仅确认一次）", "chain");
        add(root, "关闭后 4 秒再次展示广告（预期两次）", "repeat");
        add(root, "独立窗口：居中弹窗广告", "dialog");
        add(root, "拆分文字：跳 / 过 / 广告", "split");
        add(root, "拆分业务按钮（不能误触）", "splitbusiness");
        add(root, "父容器聚合“下载”描述：独立跳过应可用", "aggregate");
        add(root, "父容器超长描述：独立跳过应可用", "aggregate", true);
        add(root, "危险可点父容器包含跳过叶子（不能误触）", "dangerousparent");
        add(root, "非重要原生跳过节点（验证服务 flag）", "notimportant");
        add(root, "语义点击接受但不关闭（授权后升级手势）", "noopclick");
        add(root, "全屏危险父容器下的手势诱饵（不能误触）", "dangerousgesture");
        add(root, "普通业务关闭 / 倒计时 / 伪跳过 CTA", "business");
        add(root, "普通业务弹窗覆盖底层广告（不能误触）", "businessdialog");
        add(root, "正常页面：右上角跳过（无广告标签）", "normal");
        add(root, "正常页面：用户主动打开目标 APP", "normaljump");
        setContentView(scroll);
    }

    private void add(LinearLayout root, String label, String mode) {
        add(root, label, mode, false);
    }

    private void add(LinearLayout root, String label, String mode, boolean longDescription) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(v -> startActivity(new Intent(this, SplashActivity.class)
                .putExtra("mode", mode).putExtra("longDescription", longDescription)));
        root.addView(button, new LinearLayout.LayoutParams(-1, dp(60)));
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
