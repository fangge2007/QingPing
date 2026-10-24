package cn.qingping.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import java.util.function.Predicate;
import org.robolectric.Shadows;
import static org.junit.Assert.*;

/** Find public UI controls as a user sees them, without reaching into Activity fields. */
final class UiViews {
    private UiViews() { }
    static <T extends View> T find(View root, Class<T> kind, Predicate<T> predicate) {
        if (kind.isInstance(root) && predicate.test(kind.cast(root))) return kind.cast(root);
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                T found = find(group.getChildAt(i), kind, predicate);
                if (found != null) return found;
            }
        }
        return null;
    }
    static View root(Activity activity) { return activity.findViewById(android.R.id.content); }
    static <T extends View> T require(View root, Class<T> kind, Predicate<T> predicate) {
        T found = find(root, kind, predicate);
        assertNotNull("Missing UI control: " + kind.getSimpleName(), found);
        return found;
    }
    static TextView text(Activity activity, String label) {
        return require(root(activity), TextView.class, view -> label.contentEquals(view.getText()));
    }
    static void click(Activity activity, String label) {
        Button button = require(root(activity), Button.class, view -> label.contentEquals(view.getText()));
        assertTrue(button.isEnabled()); assertTrue(button.performClick());
        idle();
    }
    static void idle() { Shadows.shadowOf(Looper.getMainLooper()).idle(); }
    static void clickDialog(AlertDialog dialog, int which) {
        assertTrue(dialog.getButton(which).performClick()); idle();
    }
}
