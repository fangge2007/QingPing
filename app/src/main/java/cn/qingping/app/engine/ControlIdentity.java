package cn.qingping.app.engine;

/** Short-lived geometry only, used to avoid escalating a different control with a reused ID. */
public final class ControlIdentity {
    private final String sourcePackage;
    private final int windowId, left, top, right, bottom;
    public ControlIdentity(String sourcePackage, int windowId, int left, int top, int right, int bottom) {
        this.sourcePackage = sourcePackage;
        this.windowId = windowId;
        this.left = left; this.top = top; this.right = right; this.bottom = bottom;
    }
    public boolean matches(String pkg, int window, int l, int t, int r, int b) {
        return sourcePackage != null && sourcePackage.equals(pkg) && windowId == window
                && left == l && top == t && right == r && bottom == b && right > left && bottom > top;
    }
}
