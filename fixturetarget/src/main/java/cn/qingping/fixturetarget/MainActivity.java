package cn.qingping.fixturetarget;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(32, 64, 32, 64);
        root.setBackgroundColor(Color.rgb(253, 239, 224));
        TextView title = new TextView(this);
        title.setText("已打开测试目标 APP");
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView description = new TextView(this);
        description.setText("如果清屏的指定跳转返回规则满足条件，本页会在打开后被返回。\n\n这证明的是跳转后的返回，不是阻止本 APP 启动。\n\n如果是正常内容页的主动打开，本页应保留。");
        description.setTextSize(17);
        description.setPadding(20, 40, 20, 40);
        root.addView(description);
        Button close = new Button(this);
        close.setText("手动返回");
        close.setOnClickListener(v -> finish());
        root.addView(close);
        setContentView(root);
    }
}
