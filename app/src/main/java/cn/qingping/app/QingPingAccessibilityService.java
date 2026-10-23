package cn.qingping.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.qingping.app.engine.AdDecisionEngine;
import cn.qingping.app.engine.AdControlPolicy;
import cn.qingping.app.engine.BuiltinSplashRules;
import cn.qingping.app.engine.ControlIdentity;
import cn.qingping.app.rules.ExactRule;

/** User-enabled, system-bound accessibility service. No process resurrection tricks. */
public final class QingPingAccessibilityService extends AccessibilityService {
    public static volatile boolean connected;
    private static final int MAX_NODES = 700;
    private static final int MAX_DEPTH = 24;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AdDecisionEngine engine = new AdDecisionEngine();
    private boolean scanScheduled;
    private long scanDueAt;
    private final Runnable scanTask = () -> { scanScheduled = false; scanActiveWindow(); };
    private AppStore store;
    private String launcherPackage = "";
    private long lastHeartbeat;
    private final Map<String, String> activities = new HashMap<>();
    private int missingRootRetries;
    private long generation;
    private boolean gestureInFlight;
    private String gestureSource = "";
    private final Map<String, ControlIdentity> semanticClicks = new HashMap<>();
    private String observedPackage = "";
    private boolean receiverRegistered;
    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                generation++;
                gestureInFlight = false;
                semanticClicks.clear();
                engine.reset();
                handler.removeCallbacks(scanTask);
                scanScheduled = false;
            } else if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
                schedule(0);
            }
        }
    };
    private final SharedPreferences.OnSharedPreferenceChangeListener settingsListener = (prefs, key) -> {
        if (key == null || key.equals("consent") || key.equals("consent_version") || key.equals("protection")
                || key.startsWith("enabled.") || key.startsWith("skip.")
                || key.startsWith("target.") || key.startsWith("id.")
                || key.startsWith("gesture.") || key.equals("exact_rules")) {
            generation++;
            gestureInFlight = false;
            semanticClicks.clear();
            engine.reset();
            if (!store.hasConsent()) RuleDiagnostics.clear();
            handler.removeCallbacks(scanTask);
            scanScheduled = false;
            if (connected) schedule(0);
        }
    };

    public static boolean isConnected() { return connected; }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        store = new AppStore(this);
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                    | AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
                    | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            setServiceInfo(info);
        }
        store.preferences().registerOnSharedPreferenceChangeListener(settingsListener);
        if (!receiverRegistered) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            else registerReceiver(screenReceiver, filter);
            receiverRegistered = true;
        }
        connected = true;
        store.setServiceConnected(true);
        store.setLastServiceEvent(System.currentTimeMillis());
        Intent homeIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo home = getPackageManager().resolveActivity(homeIntent, 0);
        if (home != null && home.activityInfo != null) launcherPackage = home.activityInfo.packageName;
        engine.reset();
        schedule(0);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!connected || store == null || event == null) return;
        int type = event.getEventType();
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.getPackageName() != null
                && event.getClassName() != null) {
            String pkg = event.getPackageName().toString(), cls = event.getClassName().toString();
            if (store.hasConsent() && store.isAppEnabled(pkg) && !isProtectedPackage(pkg)) {
                try {
                    // Dialog/widget class names must not replace the current Activity.
                    getPackageManager().getActivityInfo(new ComponentName(pkg, cls), 0);
                    if (activities.size() > 64) activities.clear();
                    activities.put(pkg, cls);
                } catch (android.content.pm.PackageManager.NameNotFoundException | SecurityException ignored) { }
            }
        }
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || type == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            schedule(0);
        } else if (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                || type == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            // Coalesce noisy UI updates while retaining a fixed upper work bound.
            scheduleSoon(90);
        }
    }

    private void schedule(long delay) {
        handler.removeCallbacks(scanTask);
        scanScheduled = true;
        scanDueAt = SystemClock.elapsedRealtime() + delay;
        handler.postDelayed(scanTask, delay);
    }

    private void scheduleSoon(long delay) {
        if (!scanScheduled || SystemClock.elapsedRealtime() + delay < scanDueAt) schedule(delay);
    }

    private boolean authorized() {
        return connected && store != null && store.hasConsent() && store.isProtectionEnabled();
    }

    private void scanActiveWindow() {
        if (!authorized() || !screenUsable()) {
            engine.reset();
            return;
        }
        AccessibilityNodeInfo root = null;
        try {
            root = getRootInActiveWindow();
            if (root == null || root.getPackageName() == null) {
                if (engine.hasPendingAction(SystemClock.elapsedRealtime()) || missingRootRetries++ < 6) schedule(150);
                return;
            }
            missingRootRetries = 0;
            String pkg = root.getPackageName().toString();
            if (!pkg.equals(observedPackage)) { semanticClicks.clear(); observedPackage = pkg; }
            if (gestureInFlight && !pkg.equals(gestureSource)) {
                generation++;
                gestureInFlight = false;
            }
            long now = SystemClock.elapsedRealtime();
            if (now - lastHeartbeat >= 30_000) {
                lastHeartbeat = now;
                store.setLastServiceEvent(System.currentTimeMillis());
            }

            String armedSource = engine.armedSource();
            boolean guardAllowed = !armedSource.isEmpty() && store.isAppEnabled(armedSource)
                    && store.getAllowUntilElapsed() <= now
                    && store.getRule(armedSource).guardTarget.equals(engine.armedTarget())
                    && !isProtectedPackage(pkg);
            AdDecisionEngine.Result foreground = engine.onForeground(pkg, now, guardAllowed);
            record(foreground.confirmation);
            if (foreground.decision != null) {
                // Revalidate immediately before issuing the only cross-app action.
                boolean accepted = authorized() && store.isAppEnabled(foreground.decision.sourcePackage)
                        && store.getAllowUntilElapsed() <= now
                        && store.getRule(foreground.decision.sourcePackage).guardTarget.equals(pkg)
                        && performGlobalAction(GLOBAL_ACTION_BACK);
                engine.onActionResult(foreground.decision, accepted, now);
                if (accepted) schedule(250);
                return;
            }
            if (isProtectedPackage(pkg) || !store.isAppEnabled(pkg) || gestureInFlight) return;

            AppStore.Rule stored = store.getRule(pkg);
            int windowSeconds = store.scanWindowSeconds(pkg);
            boolean diagnosing = RuleDiagnostics.armed(pkg, now);
            AdDecisionEngine.Rule rule = new AdDecisionEngine.Rule(stored.skipEnabled,
                    stored.skipViewId, stored.guardTarget, windowSeconds);
            Snapshot snapshot = inspect(root, pkg, rule, activities.getOrDefault(pkg, ""),
                    store.exactRules(pkg), engine.elapsedInSession(now), now);
            try {
                AdDecisionEngine.Result result = engine.onScreen(pkg, rule, snapshot.screen, now);
                record(result.confirmation);
                if (result.decision != null) {
                    ClickTarget target = snapshot.clickTargets.get(result.decision.candidateKey);
                    ControlIdentity previous = semanticClicks.get(result.decision.candidateKey);
                    if (target != null && target.allowGesture && store.isGestureAllowed(pkg)
                            && engine.shouldEscalateGesture(result.decision.candidateKey, now)
                            && previous != null && readyForAction(pkg) && gestureClick(target.node, result.decision, previous)) return;
                    boolean accepted = target != null && clickNode(target.node, pkg);
                    if (accepted) {
                        Rect bounds = new Rect(); target.node.getBoundsInScreen(bounds);
                        if (semanticClicks.size() >= 16) semanticClicks.clear();
                        semanticClicks.put(result.decision.candidateKey, new ControlIdentity(pkg, target.node.getWindowId(),
                                bounds.left, bounds.top, bounds.right, bounds.bottom));
                    }
                    if (!accepted && target != null && target.allowGesture && store.isGestureAllowed(pkg)
                            && readyForAction(pkg) && gestureClick(target.node, result.decision, null)) {
                        return; // The completion callback, rather than submission, records acceptance.
                    }
                    engine.onActionResult(result.decision, accepted, now);
                    schedule(accepted ? 230 : 350);
                } else if (engine.hasPendingAction(now)) {
                    // Two bounded observations are needed after a request. The engine expires
                    // pending requests at 2.5 seconds, so no idle page is polled indefinitely.
                    schedule(230);
                } else {
                    // Events accelerate this low-frequency safety net. Never poll another app,
                    // a locked screen, or a paused service; every tree remains bounded.
                    schedule(diagnosing || engine.elapsedInSession(now) <= rule.windowMs ? 250 : 1_500);
                }
            } finally {
                snapshot.recycle();
            }
        } catch (IllegalStateException | SecurityException ignored) {
            // A window may disappear between reading and clicking. Fail closed and do not count.
            engine.reset();
        } finally {
            recycle(root);
        }
    }

    private boolean readyForAction(String pkg) {
        return authorized() && store.isAppEnabled(pkg) && store.getRule(pkg).skipEnabled && screenUsable();
    }

    private boolean screenUsable() {
        KeyguardManager keyguard = getSystemService(KeyguardManager.class);
        PowerManager power = getSystemService(PowerManager.class);
        return power != null && keyguard != null && power.isInteractive() && !keyguard.isKeyguardLocked();
    }

    private boolean clickNode(AccessibilityNodeInfo node, String pkg) {
        if (!readyForAction(pkg)) return false;
        AccessibilityNodeInfo current = getRootInActiveWindow();
        try {
            if (current == null || current.getPackageName() == null || !pkg.contentEquals(current.getPackageName())
                    || current.getWindowId() != node.getWindowId() || !node.refresh()
                    || !node.isVisibleToUser() || !node.isEnabled() || !node.isClickable()
                    || !safeActionSubtree(node, pkg)) return false;
            Rect rect = new Rect(); node.getBoundsInScreen(rect);
            return bounded(rect, getResources().getDisplayMetrics().widthPixels,
                    getResources().getDisplayMetrics().heightPixels) && node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        } finally { recycle(current); }
    }

    private boolean gestureClick(AccessibilityNodeInfo node, AdDecisionEngine.Decision decision, ControlIdentity expected) {
        AccessibilityNodeInfo current = getRootInActiveWindow();
        try {
            if (current == null || current.getPackageName() == null
                    || !decision.sourcePackage.contentEquals(current.getPackageName())
                    || current.getWindowId() != node.getWindowId() || !node.refresh()
                    || !node.isVisibleToUser() || !node.isEnabled()
                    || !safeActionSubtree(node, decision.sourcePackage)) return false;
            Rect rect = new Rect(); node.getBoundsInScreen(rect);
            if (!bounded(rect, getResources().getDisplayMetrics().widthPixels,
                    getResources().getDisplayMetrics().heightPixels)) return false;
            if (expected != null && !expected.matches(decision.sourcePackage, node.getWindowId(),
                    rect.left, rect.top, rect.right, rect.bottom)) return false;
            if (!safeGesturePath(node, current, decision.sourcePackage)) return false;
            Path path = new Path(); path.moveTo(rect.exactCenterX(), rect.exactCenterY());
            long token = generation;
            gestureSource = decision.sourcePackage;
            gestureInFlight = true;
            boolean submitted = dispatchGesture(new GestureDescription.Builder().addStroke(
                    new GestureDescription.StrokeDescription(path, 0, 50)).build(), new GestureResultCallback() {
                @Override public void onCompleted(GestureDescription gesture) { finishGesture(true); }
                @Override public void onCancelled(GestureDescription gesture) { finishGesture(false); }
                private void finishGesture(boolean completed) {
                    if (generation != token) return;
                    gestureInFlight = false;
                    engine.onActionResult(decision, completed && readyForAction(decision.sourcePackage), SystemClock.elapsedRealtime());
                    schedule(230);
                }
            }, handler);
            if (!submitted) gestureInFlight = false;
            return submitted;
        } catch (IllegalStateException | SecurityException ignored) {
            // A ROM can reject gesture dispatch even after the service advertised the capability.
            // Never leave the source stuck in an in-flight state waiting for a missing callback.
            generation++;
            gestureInFlight = false;
            return false;
        } finally { recycle(current); }
    }

    private boolean isProtectedPackage(String pkg) {
        String lower = pkg.toLowerCase(java.util.Locale.ROOT);
        String inputId = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        ComponentName input = inputId == null ? null : ComponentName.unflattenFromString(inputId);
        return pkg.equals(getPackageName()) || pkg.equals("android")
                || pkg.equals("com.android.systemui") || pkg.equals("com.android.settings")
                || pkg.equals("com.google.android.permissioncontroller")
                || pkg.equals("com.android.permissioncontroller") || pkg.equals(launcherPackage)
                || lower.contains("launcher") || lower.contains("inputmethod")
                || lower.contains("permissioncontroller") || lower.contains("packageinstaller")
                || input != null && pkg.equals(input.getPackageName());
    }

    private void record(AdDecisionEngine.Confirmation confirmation) {
        if (confirmation != null && authorized() && store.isAppEnabled(confirmation.sourcePackage)) {
            store.recordConfirmed(confirmation.sourcePackage, confirmation.kind);
        }
    }

    private Snapshot inspect(AccessibilityNodeInfo root, String pkg, AdDecisionEngine.Rule rule,
                             String activity, List<ExactRule> exactRules, long elapsed, long now) {
        Snapshot snapshot = new Snapshot();
        ArrayDeque<NodeDepth> queue = new ArrayDeque<>();
        queue.add(new NodeDepth(root, 0));
        List<Potential> potential = new ArrayList<>();
        boolean diagnosing = RuleDiagnostics.armed(pkg, now);
        List<RuleDiagnostics.Control> diagnosticControls = new ArrayList<>();
        boolean adContext = false;
        boolean guardContext = false;
        boolean complete = true;
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        int visited = 0;
        String pendingKey = engine.pendingCandidateKey();

        try {
        // Like upstream fastQuery, query verified IDs before a large tree can exhaust the BFS budget.
        // Query results are still revalidated as visible, enabled, small and owned by this window.
        java.util.LinkedHashSet<String> queryIds = new java.util.LinkedHashSet<>(BuiltinSplashRules.queryIds(pkg));
        if (!rule.skipViewId.isEmpty()) queryIds.add(rule.skipViewId);
        if (exactRules != null) for (ExactRule exact : exactRules) if (!exact.viewId.isEmpty()) queryIds.add(exact.viewId);
        int queries = 0;
        for (String id : queryIds) {
            if (++queries > 8) break;
            List<AccessibilityNodeInfo> hits = root.findAccessibilityNodeInfosByViewId(id);
            if (hits == null) continue;
            int count = 0;
            for (AccessibilityNodeInfo hit : hits) {
                if (hit == null) continue;
                if (++count > 8) { recycle(hit); complete = false; continue; }
                int depth = queriedDepth(hit, root, pkg, width, height);
                if (depth < 0) { recycle(hit); complete = false; continue; }
                snapshot.ownedNodes.add(hit);
                queue.addFirst(new NodeDepth(hit, depth));
            }
        }
        while (!queue.isEmpty() && visited < MAX_NODES) {
            NodeDepth entry = queue.removeFirst();
            AccessibilityNodeInfo node = entry.node;
            visited++;
            if (!node.isVisibleToUser()) continue;
            if (isInput(node)) {
                if (!pendingKey.isEmpty()) complete = false;
                continue; // Do not read text or descend into input subtrees.
            }
            CharSequence nodePkg = node.getPackageName();
            if (nodePkg != null && !pkg.contentEquals(nodePkg)) { complete = false; continue; }
            CharSequence text = node.getText();
            CharSequence description = node.getContentDescription();
            String viewId = node.getViewIdResourceName();
            if (!pendingKey.isEmpty()) {
                if (viewId != null && pendingKey.equals("id:" + viewId)) snapshot.visibleKeys.add(pendingKey);
                else if (pendingKey.startsWith("label:")) {
                    Rect observed = new Rect(); node.getBoundsInScreen(observed);
                    String key = "label:" + observed.left + ":" + observed.top + ":" + observed.right + ":" + observed.bottom;
                    if (pendingKey.equals(key)) snapshot.visibleKeys.add(pendingKey);
                }
            }
            String combinedLabel = !entry.suppressed && (text == null || text.length() == 0)
                    && (description == null || description.length() == 0)
                    && entry.depth < MAX_DEPTH && visited + queue.size() + node.getChildCount() < MAX_NODES
                    ? combinedDismissLabel(node, pkg, width, height) : "";
            // A rule's text may change after a click while the same control is still present.
            // Keep its identity for confirmation without treating it as an actionable match.
            if (!pendingKey.isEmpty() && exactRules != null && viewId != null) for (ExactRule candidate : exactRules) {
                if (pendingKey.equals("rule:" + candidate.key) && !candidate.viewId.isEmpty()
                        && viewId.equals(candidate.viewId)
                        && (candidate.activity.isEmpty() || candidate.activity.equals(activity))) snapshot.visibleKeys.add(pendingKey);
            }
            // A fake skip caption on a download CTA must not arm other business skip buttons.
            boolean unsafe = AdControlPolicy.isUnsafeAction(text, description, viewId);
            boolean suppressChildren = entry.suppressed;
            if (unsafe) {
                Rect bounds = new Rect(); node.getBoundsInScreen(bounds);
                suppressChildren |= node.isClickable() && bounded(bounds, width, height);
                String key = viewId != null && !viewId.isEmpty() ? "id:" + viewId
                        : "label:" + bounds.left + ":" + bounds.top + ":" + bounds.right + ":" + bounds.bottom;
                if (key.equals(engine.pendingCandidateKey())) snapshot.visibleKeys.add(key);
                if (exactRules != null && viewId != null) for (ExactRule candidate : exactRules) {
                    if (viewId.equals(candidate.viewId) && (candidate.activity.isEmpty() || candidate.activity.equals(activity)))
                        snapshot.visibleKeys.add("rule:" + candidate.key);
                }
            } else if (!entry.suppressed) {
            adContext |= AdDecisionEngine.isAdContext(text) || AdDecisionEngine.isAdContext(description)
                    || AdDecisionEngine.isAdContext(combinedLabel);
            boolean legacyId = !rule.skipViewId.isEmpty() && rule.skipViewId.equals(viewId);
            boolean knownId = AdDecisionEngine.isKnownAdSkipId(viewId);
            boolean builtinSource = BuiltinSplashRules.matches(pkg, viewId, text, description, elapsed);
            if (builtinSource) {
                Rect bounds = new Rect(); node.getBoundsInScreen(bounds);
                builtinSource = AdDecisionEngine.safeEdgeControl(bounds.left, bounds.top, bounds.right, bounds.bottom, width, height);
            }
            boolean conditionalSdkId = AdControlPolicy.isConditionalSdkDismissId(viewId, activity);
            boolean exactLabel = AdDecisionEngine.isSkipLabel(text)
                    || AdDecisionEngine.isSkipLabel(description)
                    || AdDecisionEngine.isAdCloseLabel(text)
                    || AdDecisionEngine.isAdCloseLabel(description)
                    || AdControlPolicy.isExplicitAdSkipLabel(text) || AdControlPolicy.isExplicitAdSkipLabel(description)
                    || !combinedLabel.isEmpty();
            // A bare business “Skip 3” countdown is not independent advertising evidence.
            adContext |= knownId || builtinSource;
            guardContext |= knownId || AdControlPolicy.isMotionAdLabel(text) || AdControlPolicy.isMotionAdLabel(description);

            ExactRule matched = null;
            if (exactRules != null) {
                for (ExactRule candidate : exactRules) {
                    // Limit conversion of UI text to the fields a rule actually configures.
                    if (ExactRule.matches(candidate, pkg, activity, viewId,
                            candidate.text.isEmpty() || text == null ? null : text.toString(),
                            candidate.description.isEmpty() || description == null ? null : description.toString())) {
                        // Retain visibility beyond the action window: expiration is not evidence
                        // that the matched control disappeared after a click request.
                        snapshot.visibleKeys.add("rule:" + candidate.key);
                        if (matched == null && candidate.activeAt(elapsed))
                            matched = candidate;
                    }
                }
            }
            Rect diagnosticBounds = new Rect();
            if (diagnosing) node.getBoundsInScreen(diagnosticBounds);
            if (diagnosing && diagnosticControls.size() < 20 && bounded(diagnosticBounds, width, height)
                    && (matched != null || legacyId || knownId || builtinSource || queryIds.contains(viewId) || conditionalSdkId || diagnosticLabel(text)
                    || diagnosticLabel(description) || diagnosticId(viewId))) {
                diagnosticControls.add(new RuleDiagnostics.Control(shortValue(viewId, 260),
                        diagnosticLabel(text) ? shortValue(text, 48) : "",
                        diagnosticLabel(description) ? shortValue(description, 48) : "", node.isClickable()));
            }
            // Legacy single-ID settings keep their original launch-only scope.
            boolean legacyActive = legacyId && elapsed <= AdDecisionEngine.LAUNCH_WINDOW_MS;
            boolean explicit = matched != null || knownId || builtinSource || conditionalSdkId || legacyActive;
            boolean adSpecificLabel = AdDecisionEngine.isAdCloseLabel(text) || AdDecisionEngine.isAdCloseLabel(description)
                    || AdControlPolicy.isExplicitAdSkipLabel(text) || AdControlPolicy.isExplicitAdSkipLabel(description)
                    || AdDecisionEngine.isAdCloseLabel(combinedLabel) || AdControlPolicy.isExplicitAdSkipLabel(combinedLabel);
            if (explicit || exactLabel) {
                Rect bounds = new Rect();
                node.getBoundsInScreen(bounds);
                String baseKey = viewId != null && !viewId.isEmpty() ? "id:" + viewId
                        : "label:" + bounds.left + ":" + bounds.top + ":" + bounds.right + ":" + bounds.bottom;
                String key = matched != null ? "rule:" + matched.key : baseKey;
                // If an Activity event arrives between observations, a newly matching
                // custom rule must not hide the previously observed legacy control key.
                boolean activeScope = AdControlPolicy.isWithinCandidateScope(explicit, adSpecificLabel, elapsed);
                if (activeScope || baseKey.equals(engine.pendingCandidateKey()) || key.equals(engine.pendingCandidateKey())) {
                    snapshot.visibleKeys.add(baseKey);
                    snapshot.visibleKeys.add(key);
                }
                if (activeScope && node.isEnabled() && !AdControlPolicy.isLockedDismissLabel(text)
                        && !AdControlPolicy.isLockedDismissLabel(description)) {
                    boolean gesture = explicit && store.isGestureAllowed(pkg)
                            && (matched == null || matched.allowGesture);
                    AccessibilityNodeInfo target = findSafeClickTarget(node, root, width, height,
                            explicit || adSpecificLabel, gesture, snapshot);
                    boolean requiresContext = conditionalSdkId && matched == null && !knownId && !builtinSource && !legacyActive && !adSpecificLabel;
                    if (target != null) potential.add(new Potential(key, target, explicit, gesture, adSpecificLabel, requiresContext));
                }
            }
            }
            int children = node.getChildCount();
            if (children > 0 && entry.depth >= MAX_DEPTH) {
                complete = false;
                continue;
            }
            for (int i = 0; i < children; i++) {
                if (visited + queue.size() >= MAX_NODES) { complete = false; break; }
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    snapshot.ownedNodes.add(child);
                    queue.addLast(new NodeDepth(child, entry.depth + 1, suppressChildren));
                } else complete = false;
            }
        }
        if (!queue.isEmpty()) complete = false;
        List<AdDecisionEngine.Candidate> candidates = new ArrayList<>();
        // Explicit source-scoped rules and SDK IDs win over heuristic labels, regardless
        // of their position in the tree. Generic Close labels never produce candidates.
        for (int priority = 0; priority < 2; priority++) {
            for (Potential item : potential) {
                if (item.explicit != (priority == 0)) continue;
                if (AdControlPolicy.hasDismissEvidence(item.explicit, item.adSpecificLabel, adContext, item.requiresContext)
                        && !snapshot.clickTargets.containsKey(item.key)) {
                    snapshot.clickTargets.put(item.key, new ClickTarget(item.target, item.gesture));
                    candidates.add(new AdDecisionEngine.Candidate(item.key));
                }
            }
        }
        snapshot.screen = new AdDecisionEngine.Screen(adContext, complete, candidates, snapshot.visibleKeys, guardContext);
        if (diagnosing) RuleDiagnostics.publish(pkg, activity, visited, complete, adContext,
                !candidates.isEmpty() ? "已找到可安全执行的候选控件"
                        : !complete ? "界面读取不完整或超过扫描上限，本次不能确认成功"
                        : "未找到可安全执行的候选，可从下列控件配置精确规则",
                diagnosticControls, now);
        return snapshot;
        } catch (RuntimeException ex) {
            snapshot.recycle();
            throw ex;
        }
    }

    private AccessibilityNodeInfo findSafeClickTarget(AccessibilityNodeInfo start,
                                                     AccessibilityNodeInfo root,
                                                     int width, int height, boolean explicit,
                                                     boolean gesture,
                                                     Snapshot snapshot) {
        AccessibilityNodeInfo gestureTarget = null;
        Rect originalBounds = new Rect();
        start.getBoundsInScreen(originalBounds);
        if (explicit && gesture && start.isVisibleToUser() && start.isEnabled()
                && !isInput(start) && bounded(originalBounds, width, height)) gestureTarget = start;
        AccessibilityNodeInfo current = start;
        for (int level = 0; level <= 2 && current != null; level++) {
            if (current.equals(root) || !current.isVisibleToUser() || !current.isEnabled()
                    || isInput(current) || current.getWindowId() != root.getWindowId()) break;
            CharSequence currentPackage = current.getPackageName(), rootPackage = root.getPackageName();
            if (currentPackage != null && rootPackage != null
                    && !rootPackage.toString().contentEquals(currentPackage)) break;
            Rect bounds = new Rect();
            current.getBoundsInScreen(bounds);
            // Even explicit identifiers must not turn an entire page into a click target.
            boolean adSpecific = AdDecisionEngine.isAdCloseLabel(start.getText()) || AdDecisionEngine.isAdCloseLabel(start.getContentDescription())
                    || AdControlPolicy.isExplicitAdSkipLabel(start.getText()) || AdControlPolicy.isExplicitAdSkipLabel(start.getContentDescription());
            if (!bounded(bounds, width, height) || (!explicit && !adSpecific && !AdDecisionEngine.safeEdgeControl(
                    bounds.left, bounds.top, bounds.right, bounds.bottom, width, height))) break;
            if (current.isClickable()) return safeActionSubtree(current, rootPackage == null ? "" : rootPackage.toString()) ? current : null;
            if (level == 2) break;
            AccessibilityNodeInfo parent = current.getParent();
            if (parent != null) snapshot.ownedNodes.add(parent);
            current = parent;
        }
        return gestureTarget;
    }

    /** A direct ID query must not jump past an excluded input/CTA ancestor or the depth limit. */
    private int queriedDepth(AccessibilityNodeInfo hit, AccessibilityNodeInfo root, String pkg, int width, int height) {
        AccessibilityNodeInfo current = hit;
        List<AccessibilityNodeInfo> ancestors = new ArrayList<>();
        try {
            for (int depth = 0; current != null && depth <= MAX_DEPTH; depth++) {
                if (isInput(current) || current.getWindowId() != root.getWindowId()) return -1;
                CharSequence owner = current.getPackageName();
                if (owner != null && !pkg.contentEquals(owner)) return -1;
                if (current.equals(root)) {
                    // Only inspect labels after the entire path is known to contain no input.
                    for (AccessibilityNodeInfo ancestor : ancestors) {
                        if (!ancestor.isClickable()) continue;
                        Rect bounds = new Rect(); ancestor.getBoundsInScreen(bounds);
                        if (bounded(bounds, width, height) && AdControlPolicy.isUnsafeAction(
                                ancestor.getText(), ancestor.getContentDescription(), ancestor.getViewIdResourceName())) return -1;
                    }
                    return depth;
                }
                if (depth == MAX_DEPTH) break;
                current = current.getParent();
                if (current != null) ancestors.add(current);
            }
            return -1;
        } finally { for (AccessibilityNodeInfo ancestor : ancestors) recycle(ancestor); }
    }

    private boolean safeGesturePath(AccessibilityNodeInfo node, AccessibilityNodeInfo root, String pkg) {
        List<AccessibilityNodeInfo> ancestors = new ArrayList<>();
        AccessibilityNodeInfo current = node;
        boolean reachedRoot = false;
        try {
            // Metadata-only first pass; a queried descendant must not expose an input ancestor's contents.
            for (int depth = 0; current != null && depth <= MAX_DEPTH; depth++) {
                if (isInput(current) || current.getWindowId() != root.getWindowId()) return false;
                CharSequence owner = current.getPackageName();
                if (owner != null && !pkg.contentEquals(owner)) return false;
                if (current.equals(root)) { reachedRoot = true; break; }
                if (depth == MAX_DEPTH) break;
                current = current.getParent();
                if (current != null) ancestors.add(current);
            }
            if (!reachedRoot) return false;
            for (AccessibilityNodeInfo ancestor : ancestors) {
                if (!ancestor.isClickable()) continue;
                if (AdControlPolicy.isUnsafeAction(ancestor.getText(), ancestor.getContentDescription(),
                        ancestor.getViewIdResourceName())) return false;
                // An unclickable label can hand touch handling to its clickable ancestor.
                if (!node.isClickable() && !safeActionSubtree(ancestor, pkg)) return false;
            }
            return true;
        } finally { for (AccessibilityNodeInfo ancestor : ancestors) recycle(ancestor); }
    }

    /** Direct leaves only: no merging paragraphs, inputs, separate buttons or hidden subtrees. */
    private String combinedDismissLabel(AccessibilityNodeInfo node, String pkg, int width, int height) {
        int count = node.getChildCount();
        if (!node.isClickable() || count < 2 || count > 5) return "";
        Rect bounds = new Rect(); node.getBoundsInScreen(bounds);
        if (!bounded(bounds, width, height)) return "";
        List<String> fragments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) return "";
            try {
                CharSequence owner = child.getPackageName();
                if (!child.isVisibleToUser() || isInput(child) || child.isClickable() || child.getChildCount() != 0
                        || child.getWindowId() != node.getWindowId()
                        || owner != null && !pkg.contentEquals(owner)) return "";
                CharSequence label = child.getText(), desc = child.getContentDescription();
                if (AdControlPolicy.isUnsafeAction(label, desc, child.getViewIdResourceName())) return "";
                if (label == null || label.length() == 0) label = desc;
                if (label == null || label.length() > 10) return "";
                fragments.add(label.toString());
            } finally { recycle(child); }
        }
        return AdControlPolicy.combineDismissLabel(fragments);
    }

    /** Never let a small ancestor merge a close label with a download/payment action. */
    private boolean safeActionSubtree(AccessibilityNodeInfo node, String pkg) {
        if (isInput(node) || AdControlPolicy.isUnsafeAction(node.getText(), node.getContentDescription(), node.getViewIdResourceName())) return false;
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        List<AccessibilityNodeInfo> owned = new ArrayList<>();
        queue.add(node);
        int seen = 0;
        try {
            while (!queue.isEmpty()) {
                AccessibilityNodeInfo current = queue.removeFirst();
                if (++seen > 40) return false;
                if (!current.isVisibleToUser()) continue;
                if (isInput(current)) return false;
                CharSequence owner = current.getPackageName();
                if (owner != null && !pkg.contentEquals(owner)) return false;
                if (AdControlPolicy.isUnsafeAction(current.getText(), current.getContentDescription(), current.getViewIdResourceName())
                        || AdControlPolicy.isLockedDismissLabel(current.getText())
                        || AdControlPolicy.isLockedDismissLabel(current.getContentDescription())) return false;
                for (int i = 0; i < current.getChildCount(); i++) {
                    if (seen + queue.size() >= 40) return false;
                    AccessibilityNodeInfo child = current.getChild(i);
                    if (child == null) return false;
                    owned.add(child); queue.add(child);
                }
            }
            return true;
        } finally { for (AccessibilityNodeInfo child : owned) recycle(child); }
    }

    private static boolean bounded(Rect bounds, int width, int height) {
        return width > 0 && height > 0 && bounds.width() > 0 && bounds.height() > 0
                && bounds.left >= 0 && bounds.top >= 0 && bounds.right <= width
                && bounds.bottom <= height && bounds.width() <= width * 0.48
                && bounds.height() <= height * 0.18;
    }

    private static boolean diagnosticLabel(CharSequence value) {
        if (value == null || value.length() > 48) return false;
        if (AdDecisionEngine.isSkipLabel(value) || AdDecisionEngine.isAdCloseLabel(value)) return true;
        String label = value.toString().trim().toLowerCase(java.util.Locale.ROOT);
        return label.equals("关闭广告") || label.equals("关闭") || label.equals("close")
                || label.equals("close ad") || label.equals("close advertisement")
                || label.equals("×") || label.equals("✕") || label.equals("✖") || label.equals("x");
    }

    private static boolean diagnosticId(String value) {
        if (value == null || value.length() > 260) return false;
        String id = value.toLowerCase(java.util.Locale.ROOT);
        return id.contains(":id/close") || id.contains(":id/skip")
                || id.contains("_close") || id.contains("_skip");
    }

    private static String shortValue(CharSequence value, int maximum) {
        return value == null || value.length() > maximum ? "" : value.toString();
    }

    @Override public void onInterrupt() {
        generation++; gestureInFlight = false; engine.reset();
        semanticClicks.clear();
        handler.removeCallbacks(scanTask); scanScheduled = false;
    }

    private static boolean isInput(AccessibilityNodeInfo node) {
        CharSequence cls = node.getClassName();
        return node.isPassword() || node.isEditable()
                || cls != null && cls.toString().endsWith("EditText");
    }

    @Override public boolean onUnbind(Intent intent) {
        disconnected();
        return super.onUnbind(intent);
    }

    @Override public void onDestroy() {
        disconnected();
        super.onDestroy();
    }

    private void disconnected() {
        generation++;
        gestureInFlight = false;
        semanticClicks.clear();
        RuleDiagnostics.clear();
        connected = false;
        handler.removeCallbacksAndMessages(null);
        scanScheduled = false;
        engine.reset();
        if (receiverRegistered) { unregisterReceiver(screenReceiver); receiverRegistered = false; }
        if (store != null) {
            store.preferences().unregisterOnSharedPreferenceChangeListener(settingsListener);
            store.setServiceConnected(false);
        }
    }

    @SuppressWarnings("deprecation")
    private static void recycle(AccessibilityNodeInfo node) { if (node != null) node.recycle(); }

    private static final class NodeDepth {
        final AccessibilityNodeInfo node;
        final int depth;
        final boolean suppressed;
        NodeDepth(AccessibilityNodeInfo node, int depth) { this(node, depth, false); }
        NodeDepth(AccessibilityNodeInfo node, int depth, boolean suppressed) {
            this.node = node; this.depth = depth; this.suppressed = suppressed;
        }
    }
    private static final class Potential {
        final String key;
        final AccessibilityNodeInfo target;
        final boolean explicit, gesture, adSpecificLabel, requiresContext;
        Potential(String key, AccessibilityNodeInfo target, boolean explicit, boolean gesture, boolean adSpecificLabel, boolean requiresContext) {
            this.key = key; this.target = target; this.explicit = explicit; this.gesture = gesture;
            this.adSpecificLabel = adSpecificLabel; this.requiresContext = requiresContext;
        }
    }
    private static final class ClickTarget {
        final AccessibilityNodeInfo node;
        final boolean allowGesture;
        ClickTarget(AccessibilityNodeInfo node, boolean allowGesture) {
            this.node = node; this.allowGesture = allowGesture;
        }
    }
    private static final class Snapshot {
        final List<AccessibilityNodeInfo> ownedNodes = new ArrayList<>();
        final Map<String, ClickTarget> clickTargets = new HashMap<>();
        final Set<String> visibleKeys = new HashSet<>();
        AdDecisionEngine.Screen screen;
        void recycle() { for (AccessibilityNodeInfo node : ownedNodes) QingPingAccessibilityService.recycle(node); }
    }
}
