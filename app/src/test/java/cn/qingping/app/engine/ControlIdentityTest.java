package cn.qingping.app.engine;

import org.junit.Test;
import static org.junit.Assert.*;

public class ControlIdentityTest {
    @Test public void sameVisibleControlMayEscalateAfterItsSemanticRequestTimesOut() {
        assertTrue(new ControlIdentity("tv.danmaku.bili", 3, 800, 60, 1000, 140)
                .matches("tv.danmaku.bili", 3, 800, 60, 1000, 140));
    }
    @Test public void aReusedIdInAnotherWindowOrMovedLayoutCannotEscalate() {
        ControlIdentity old = new ControlIdentity("tv.danmaku.bili", 3, 800, 60, 1000, 140);
        assertFalse(old.matches("tv.danmaku.bili", 4, 800, 60, 1000, 140));
        assertFalse(old.matches("tv.danmaku.bili", 3, 800, 80, 1000, 160));
        assertFalse(old.matches("other.app", 3, 800, 60, 1000, 140));
    }
    @Test public void emptyOrInvertedGeometryNeverQualifies() {
        assertFalse(new ControlIdentity("app", 3, 10, 10, 10, 20).matches("app", 3, 10, 10, 10, 20));
        assertFalse(new ControlIdentity("app", 3, 10, 10, 20, 5).matches("app", 3, 10, 10, 20, 5));
    }
}
