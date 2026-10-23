package cn.qingping.app.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Deterministic policy only: no Android APIs, networking, or retained screen text. */
public final class AdDecisionEngine {
    public static final long LAUNCH_WINDOW_MS = 15_000;
    public static final long GUARD_TTL_MS = 2_000;
    public static final long CONFIRM_TIMEOUT_MS = 2_500;
    public static final long EPISODE_CLEAR_MS = 500;
    public static final long DELAYED_CONTROL_RETRY_MS = 10_000;
    private static final int MAX_EPISODE_CLICKS = 4;
    private static final int MAX_CONTROL_CLICKS = 3;
    private static final int MAX_EPISODE_REQUESTS = 8;
    private static final int MAX_CONTROL_REJECTIONS = 3;
    private static final Pattern SKIP = Pattern.compile(
            "^(?:跳过(?:广告)?(?:\\s*[（(]?\\d{1,2}\\s*[sS秒]?[）)]?)?|"
                    + "[（(]?\\d{1,2}\\s*[sS秒]?[）)]?\\s*跳过(?:广告)?|"
                    + "skip(?:\\s+ad)?(?:\\s*[（(]?\\d{1,2}\\s*s?[）)]?)?|"
                    + "\\d{1,2}\\s*s?\\s*skip(?:\\s+ad)?)$",
            Pattern.CASE_INSENSITIVE);

    public enum Action { CLICK_SKIP, BACK }

    public static final class Rule {
        public final boolean skipEnabled;
        public final String skipViewId;
        public final String guardTarget;
        public final long windowMs;

        public Rule(boolean skipEnabled, String skipViewId, String guardTarget) {
            this(skipEnabled, skipViewId, guardTarget, 15);
        }

        public Rule(boolean skipEnabled, String skipViewId, String guardTarget, int windowSeconds) {
            this.skipEnabled = skipEnabled;
            this.skipViewId = clean(skipViewId);
            this.guardTarget = clean(guardTarget);
            this.windowMs = Math.max(3, Math.min(30, windowSeconds)) * 1_000L;
        }
    }

    public static final class Candidate {
        public final String key;
        public Candidate(String key) { this.key = key; }
    }

    public static final class Screen {
        public final boolean adContext;
        public final boolean complete;
        public final List<Candidate> candidates;
        public final Set<String> visibleSkipKeys;
        /** Narrow jump evidence, separate from an ordinary advertising/banner label. */
        public final boolean guardContext;

        public Screen(boolean adContext, boolean complete, List<Candidate> candidates,
                      Set<String> visibleSkipKeys) {
            this(adContext, complete, candidates, visibleSkipKeys, adContext);
        }

        public Screen(boolean adContext, boolean complete, List<Candidate> candidates,
                      Set<String> visibleSkipKeys, boolean guardContext) {
            this.adContext = adContext;
            this.complete = complete;
            this.candidates = candidates == null ? Collections.emptyList() : candidates;
            this.visibleSkipKeys = visibleSkipKeys == null ? Collections.emptySet() : visibleSkipKeys;
            this.guardContext = guardContext;
        }
    }

    public static final class Decision {
        public final Action action;
        public final String sourcePackage;
        public final String targetPackage;
        public final String candidateKey;
        public final long issuedAt;
        private final long generation;

        private Decision(Action action, String source, String target, String key, long now, long generation) {
            this.action = action;
            this.sourcePackage = source;
            this.targetPackage = target;
            this.candidateKey = key;
            this.issuedAt = now;
            this.generation = generation;
        }
    }

    public static final class Confirmation {
        public final String sourcePackage;
        public final String kind;
        private Confirmation(String sourcePackage, String kind) {
            this.sourcePackage = sourcePackage;
            this.kind = kind;
        }
    }

    public static final class Result {
        public final Decision decision;
        public final Confirmation confirmation;
        private Result(Decision decision, Confirmation confirmation) {
            this.decision = decision;
            this.confirmation = confirmation;
        }
        private static Result empty() { return new Result(null, null); }
    }

    private String foreground = "";
    private long enteredAt;
    private boolean skippedInEpisode;
    private boolean returnedInEpisode;
    private boolean episodeObserved;
    private int acceptedClicks;
    private int requestAttempts;
    private final Map<String, Integer> acceptedByControl = new HashMap<>();
    private final Map<String, Integer> rejectedByControl = new HashMap<>();
    private final Map<String, Long> lastControlAcceptance = new HashMap<>();
    private Decision lastAcceptedDecision;
    private Decision offeredDecision;
    private long generation;
    private long retryAfter;
    private String armedSource = "";
    private String armedTarget = "";
    private long armedUntil;
    private Decision pending;
    private long firstMissingAt = -1;
    private long firstClearAt = -1;

    public String armedSource() { return armedSource; }
    public String armedTarget() { return armedTarget; }
    /** Escalation remains adapter-authorized and only occurs after an unconfirmed accepted request expires. */
    public boolean shouldEscalateGesture(String key, long now) {
        return pending == null && acceptedByControl.getOrDefault(key, 0) > 0
                && now - lastControlAcceptance.getOrDefault(key, now) >= CONFIRM_TIMEOUT_MS;
    }
    /** Read-only identity for conservative visibility checks in the Android adapter. */
    public String pendingCandidateKey() {
        return pending != null && pending.action == Action.CLICK_SKIP ? pending.candidateKey : "";
    }
    public long enteredAt() { return enteredAt; }
    public long elapsedInSession(long now) { return Math.max(0, now - enteredAt); }
    /** Foreground monitoring continues after success/exhaustion to detect a new ad episode. */
    public boolean isSessionComplete() { return false; }
    public boolean hasPendingAction(long now) { expire(now); return pending != null; }
    public boolean shouldInspect(long now) {
        return shouldInspect(now, LAUNCH_WINDOW_MS);
    }
    public boolean shouldInspect(long now, long windowMs) {
        expire(now);
        // Entry windows select adapter rules and polling speed, not the lifetime of monitoring.
        return !foreground.isEmpty() && now >= enteredAt;
    }

    /** Call with the package of the actual active root, not an event's stale source. */
    public Result onForeground(String packageName, long now, boolean guardAllowed) {
        String next = clean(packageName);
        expire(now);
        if (next.equals(foreground)) return Result.empty();

        Confirmation confirmed = null;
        if (pending != null && pending.action == Action.BACK
                && next.equals(pending.sourcePackage) && now > pending.issuedAt) {
            confirmed = new Confirmation(pending.sourcePackage, "return");
            pending = null;
        } else if (pending != null) {
            // Leaving the source after clicking Skip is not evidence of successful skipping.
            pending = null;
        }

        String backSource = guardAllowed && foreground.equals(armedSource) && next.equals(armedTarget)
                && now <= armedUntil && now >= enteredAt && confirmed == null ? armedSource : "";
        clearArm();
        foreground = next;
        enteredAt = now;
        clearEpisode(now);
        // Returning to the source is still the same advertisement. It must visibly clear
        // before another return or click can be attempted, preventing cross-app loops.
        returnedInEpisode = confirmed != null;
        episodeObserved = confirmed != null;
        Decision back = backSource.isEmpty() ? null
                : offer(Action.BACK, backSource, next, "", now);
        return new Result(back, confirmed);
    }

    /** Only pass snapshots of explicitly enabled source applications. */
    public Result onScreen(String packageName, Rule rule, Screen screen, long now) {
        expire(now);
        if (!foreground.equals(packageName) || now < enteredAt) return Result.empty();

        boolean clearScreen = screen.complete && !screen.adContext
                && screen.visibleSkipKeys.isEmpty() && screen.candidates.isEmpty();
        if (!clearScreen) {
            firstClearAt = -1;
            if (screen.adContext || !screen.visibleSkipKeys.isEmpty() || !screen.candidates.isEmpty())
                episodeObserved = true;
        } else if (firstClearAt < 0) {
            firstClearAt = now;
        }
        Confirmation confirmed = null;
        if (pending != null && pending.action == Action.CLICK_SKIP
                && pending.sourcePackage.equals(packageName) && clearScreen
                && now - pending.issuedAt >= 200) {
            if (firstMissingAt < 0) {
                firstMissingAt = now;
            } else if (now - firstMissingAt >= 180) {
                confirmed = new Confirmation(pending.sourcePackage, "skip");
                pending = null;
                skippedInEpisode = true;
                clearArm();
                firstMissingAt = -1;
            }
        } else if (pending != null && pending.action == Action.CLICK_SKIP) {
            firstMissingAt = -1;
        }

        if (clearScreen && episodeObserved && pending == null
                && now - firstClearAt >= EPISODE_CLEAR_MS) clearEpisode(now);

        // Any fresh advertising episode may arm this exact configured target. The arm is
        // still short-lived and a confirmed return suppresses it until a stable clean screen.
        if (!returnedInEpisode && !skippedInEpisode && screen.adContext && screen.guardContext
                && !rule.guardTarget.isEmpty()
                && !rule.guardTarget.equals(packageName) && pending == null) {
            armedSource = packageName;
            armedTarget = rule.guardTarget;
            armedUntil = now + GUARD_TTL_MS;
        } else if (!screen.adContext || !screen.guardContext || rule.guardTarget.isEmpty()
                || returnedInEpisode || skippedInEpisode) {
            clearArm();
        }

        if (confirmed != null || !rule.skipEnabled || skippedInEpisode || returnedInEpisode
                || acceptedClicks >= MAX_EPISODE_CLICKS || requestAttempts >= MAX_EPISODE_REQUESTS
                || pending != null || now < retryAfter) {
            return new Result(null, confirmed);
        }
        // A video close button may replace its skip button without a clean page between
        // phases. New controls share a total budget. An unchanged control gets two quick
        // tries and one final delayed try for SDKs that accept clicks before unlocking.
        for (Candidate candidate : screen.candidates) {
            if (candidate == null || candidate.key == null || candidate.key.isEmpty()) continue;
            if (rejectedByControl.getOrDefault(candidate.key, 0) >= MAX_CONTROL_REJECTIONS) continue;
            int accepted = acceptedByControl.getOrDefault(candidate.key, 0);
            if (accepted >= MAX_CONTROL_CLICKS || accepted >= 2
                    && now - lastControlAcceptance.getOrDefault(candidate.key, now)
                    < DELAYED_CONTROL_RETRY_MS) continue;
            return new Result(offer(Action.CLICK_SKIP, packageName, "", candidate.key, now), null);
        }
        return Result.empty();
    }

    /** True means Android accepted a request; statistics still wait for a confirmation. */
    public void onActionResult(Decision decision, boolean accepted, long now) {
        if (decision == null || decision == lastAcceptedDecision || decision != offeredDecision
                || decision.generation != generation || now < decision.issuedAt
                || now - decision.issuedAt > CONFIRM_TIMEOUT_MS) return;
        offeredDecision = null;
        if (decision.action == Action.CLICK_SKIP && !decision.sourcePackage.equals(foreground)
                || decision.action == Action.BACK && !decision.targetPackage.equals(foreground)) return;
        if (decision.action == Action.CLICK_SKIP) requestAttempts++;
        if (!accepted) {
            if (decision.action == Action.CLICK_SKIP) rejectedByControl.put(decision.candidateKey,
                    rejectedByControl.getOrDefault(decision.candidateKey, 0) + 1);
            retryAfter = now + 750;
            return;
        }
        pending = decision;
        lastAcceptedDecision = decision;
        firstMissingAt = -1;
        if (decision.action == Action.CLICK_SKIP) {
            acceptedClicks++;
            acceptedByControl.put(decision.candidateKey,
                    acceptedByControl.getOrDefault(decision.candidateKey, 0) + 1);
            lastControlAcceptance.put(decision.candidateKey, now);
            episodeObserved = true;
        }
    }

    public void reset() {
        foreground = "";
        enteredAt = 0;
        clearEpisode(0);
        pending = null;
        clearArm();
    }

    private void clearEpisode(long now) {
        generation++;
        skippedInEpisode = false;
        returnedInEpisode = false;
        episodeObserved = false;
        acceptedClicks = 0;
        requestAttempts = 0;
        acceptedByControl.clear();
        rejectedByControl.clear();
        lastControlAcceptance.clear();
        lastAcceptedDecision = null;
        offeredDecision = null;
        retryAfter = now;
        firstMissingAt = -1;
        firstClearAt = -1;
    }

    private Decision offer(Action action, String source, String target, String key, long now) {
        offeredDecision = new Decision(action, source, target, key, now, generation);
        return offeredDecision;
    }

    private void expire(long now) {
        if (pending != null && (now < pending.issuedAt
                || now - pending.issuedAt > CONFIRM_TIMEOUT_MS)) {
            pending = null;
            firstMissingAt = -1;
        }
        if (now > armedUntil) clearArm();
    }

    private void clearArm() {
        armedSource = "";
        armedTarget = "";
        armedUntil = 0;
    }

    public static boolean isSkipLabel(CharSequence label) {
        if (label == null || label.length() > 24) return false;
        String text = label.toString().trim().replace('\u00a0', ' ')
                .replace('|', ' ').replace('｜', ' ').replace('·', ' ').replace('•', ' ');
        return !text.isEmpty() && SKIP.matcher(text).matches();
    }

    /** Explicit advertising dismissal wording only; a generic Close is never sufficient. */
    public static boolean isAdCloseLabel(CharSequence label) {
        if (label == null || label.length() > 48) return false;
        String value = label.toString().trim().toLowerCase(Locale.ROOT);
        return value.equals("关闭广告") || value.equals("关闭此广告")
                || value.equals("关闭此条广告") || value.equals("关闭视频广告")
                || value.equals("关闭本广告")
                || value.equals("close ad") || value.equals("close advertisement")
                || value.equals("close this ad") || value.equals("dismiss this ad")
                || value.equals("dismiss ad");
    }

    /** A countdown plus exact skip wording is stronger evidence than a bare Skip label. */
    public static boolean isCountdownSkipLabel(CharSequence label) {
        if (!isSkipLabel(label)) return false;
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            if (c >= '0' && c <= '9') return true;
        }
        return false;
    }

    /**
     * Resource names verified in publicly available MIT-licensed rules, not generic close IDs.
     * ganlinte/GKD-subscription, commit 4746d192b4c57a4b62ed92a7f5deaf79e64fa811:
     * src/apps/com.UCMobile.ts and src/apps/gov.pianzong.androidnga.ts.
     * Names identify possible splash controls; the Android adapter must still enforce app scope,
     * visibility, bounded geometry, and the user's chosen action permissions.
     */
    public static boolean isKnownAdSkipId(CharSequence viewId) {
        if (viewId == null || viewId.length() > 240) return false;
        String name = viewId.toString();
        int marker = name.indexOf(":id/");
        if (marker >= 0) name = name.substring(marker + 4);
        return name.equals("tt_splash_skip_btn") || name.equals("ksad_splash_circle_skip_view")
                || name.equals("ksad_skip_view_skip");
    }

    public static boolean isAdContext(CharSequence text) {
        if (text == null || text.length() > 100) return false;
        String value = text.toString().trim().toLowerCase(Locale.ROOT);
        // Do not interpret generic “了解更多” or “点击进入” as advertising evidence.
        return value.equals("广告") || value.equals("广告标识") || value.equals("advertisement")
                || value.equals("sponsored") || value.equals("ad")
                || value.contains("摇一摇") || value.contains("扭一扭")
                || value.equals("跳过广告") || value.equals("skip ad") || isAdCloseLabel(text);
    }

    public static boolean safeTopRight(int left, int top, int right, int bottom,
                                       int screenWidth, int screenHeight) {
        if (screenWidth <= 0 || screenHeight <= 0 || left < 0 || top < 0
                || right > screenWidth || bottom > screenHeight) return false;
        int width = right - left;
        int height = bottom - top;
        return width > 0 && height > 0 && width <= screenWidth * 0.42
                && height <= screenHeight * 0.14
                && left >= screenWidth * 0.5 && bottom <= screenHeight * 0.4;
    }

    /** Geometry only: callers also need exact ad-skip evidence, never a generic Close label. */
    public static boolean safeEdgeControl(int left, int top, int right, int bottom,
                                         int screenWidth, int screenHeight) {
        if (screenWidth <= 0 || screenHeight <= 0 || left < 0 || top < 0
                || right > screenWidth || bottom > screenHeight) return false;
        int width = right - left;
        int height = bottom - top;
        return width > 0 && height > 0 && width <= screenWidth * 0.45
                && height <= screenHeight * 0.16
                && (bottom <= screenHeight * 0.4 || top >= screenHeight * 0.7);
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }
}
