package cn.qingping.app.rules;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class ExactRuleTest {
    private static final String BASE = "\"key\":\"uc-close\",\"name\":\"关闭广告\",\"packageName\":\"com.UCMobile\",\"viewId\":\"com.byted.pangle:id/tt_splash_skip_btn\"";
    private static String doc(String row) { return "{\"format\":\"qingping-rules-v1\",\"rules\":[{" + row + "}]}"; }
    private static ExactRule parse(String row) { return ExactRule.parseDocument(doc(row)).get(0); }
    private static void rejects(Runnable action) { assertThrows(IllegalArgumentException.class, action::run); }

    @Test public void defaultsAndSdkNamespaceAreAccepted() {
        ExactRule rule = parse(BASE);
        assertEquals(15, rule.windowSeconds);
        assertFalse(rule.allowGesture);
        assertFalse(rule.persistent);
        assertEquals("", rule.activity);
        assertEquals("com.byted.pangle:id/tt_splash_skip_btn", rule.viewId);
        assertTrue(ExactRule.matches(rule, "com.UCMobile", "other.Activity", rule.viewId, null, null));
        assertFalse(ExactRule.matches(rule, "com.byted.pangle", "other.Activity", rule.viewId, null, null));
    }
    @Test public void persistentIsExplicitAndRoundTrips() {
        ExactRule old = parse(BASE);
        assertTrue(old.activeAt(15_000));
        assertFalse(old.activeAt(15_001));
        ExactRule persistent = parse(BASE + ",\"persistent\":true");
        assertTrue(persistent.activeAt(120_000));
        assertFalse(persistent.activeAt(-1));
        assertTrue(ExactRule.parseDocument(ExactRule.serializeDocument(Collections.singletonList(persistent))).get(0).persistent);
        assertFalse(parse(BASE + ",\"persistent\":false").persistent);
        for (String value : new String[]{"\"true\"", "1", "null", "[]", "{}"})
            rejects(() -> parse(BASE + ",\"persistent\":" + value));
    }
    @Test public void allConfiguredConditionsMatchExactly() {
        ExactRule rule = parse(BASE + ",\"activity\":\"com.UCMobile.SplashActivity\",\"text\":\"关闭\",\"description\":\"关闭广告\",\"allowGesture\":true,\"windowSeconds\":30");
        assertTrue(rule.allowGesture);
        assertTrue(ExactRule.matches(rule, rule.packageName, rule.activity, rule.viewId, "关闭", "关闭广告"));
        assertFalse(ExactRule.matches(rule, rule.packageName, rule.activity, rule.viewId, "关闭 ", "关闭广告"));
        assertFalse(ExactRule.matches(rule, rule.packageName, rule.activity, rule.viewId, "关闭", null));
        assertFalse(ExactRule.matches(rule, rule.packageName, ".SplashActivity", rule.viewId, "关闭", "关闭广告"));
        assertFalse(ExactRule.matches(rule, "com.ucmobile", rule.activity, rule.viewId, "关闭", "关闭广告"));
        assertFalse(ExactRule.matches(null, rule.packageName, rule.activity, rule.viewId, "关闭", "关闭广告"));
    }
    @Test public void textOnlyRequiresFullyQualifiedActivity() {
        ExactRule rule = parse("\"key\":\"close\",\"name\":\"关闭\",\"packageName\":\"cn.example.app\",\"activity\":\"cn.example.Splash$Inner\",\"text\":\"×\"");
        assertTrue(ExactRule.matches(rule, rule.packageName, rule.activity, null, "×", null));
        rejects(() -> parse("\"key\":\"close\",\"name\":\"关闭\",\"packageName\":\"cn.example.app\",\"text\":\"关闭\""));
        rejects(() -> parse(BASE + ",\"activity\":\".SplashActivity\""));
    }
    @Test public void selectorsCannotBeAbsentOrWhitespaceOnly() {
        rejects(() -> new ExactRule("k", "name", "cn.example.app", "cn.example.Splash", "", "", "", false, 15));
        rejects(() -> new ExactRule("k", "name", "cn.example.app", "cn.example.Splash", "", " ", "", false, 15));
    }
    @Test public void expressionsAndCoordinatesAreRejectedAsFields() {
        for (String field : new String[]{"matches", "selector", "regex", "x", "y", "action", "excludeMatches"})
            rejects(() -> parse(BASE + ",\"" + field + "\":\"x\""));
        rejects(() -> parse(BASE.replace("com.byted.pangle:id/tt_splash_skip_btn", "*:id/close")));
        rejects(() -> parse(BASE.replace("com.UCMobile", "com.*")));
    }
    @Test public void selectorTextIsLiteralEvenIfItContainsRegexCharacters() {
        ExactRule rule = parse(BASE + ",\"text\":\"关闭.*\"");
        assertTrue(ExactRule.matches(rule, rule.packageName, null, rule.viewId, "关闭.*", null));
        assertFalse(ExactRule.matches(rule, rule.packageName, null, rule.viewId, "关闭广告", null));
    }
    @Test public void unknownRootAndGkdFormatsExplainUnsupportedFormat() {
        rejects(() -> ExactRule.parseDocument("{\"format\":\"gkd\",\"rules\":[]}"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ExactRule.parseDocument("{\"id\":1,\"name\":\"GKD\",\"apps\":[]}"));
        assertTrue(ex.getMessage().contains("GKD"));
        rejects(() -> ExactRule.parseDocument("{\"format\":\"qingping-rules-v1\",\"rules\":[],\"version\":1}"));
    }
    @Test public void strictTypesRejectCoercionAndNull() {
        for (String value : new String[]{"\"true\"", "1", "null", "[]", "{}"})
            rejects(() -> parse(BASE + ",\"allowGesture\":" + value));
        rejects(() -> parse(BASE + ",\"text\":null"));
        rejects(() -> parse(BASE.replace("\"uc-close\"", "1")));
        rejects(() -> ExactRule.parseDocument("{\"format\":\"qingping-rules-v1\",\"rules\":{}}"));
        rejects(() -> ExactRule.parseDocument("{\"format\":\"qingping-rules-v1\",\"rules\":[null]}"));
    }
    @Test public void windowRangeAndIntegerLexemeAreValidated() {
        assertEquals(3, parse(BASE + ",\"windowSeconds\":3").windowSeconds);
        assertEquals(30, parse(BASE + ",\"windowSeconds\":30").windowSeconds);
        for (String value : new String[]{"2", "31", "-1", "15.0", "1.5e1", "\"15\"", "null", "9999999999999999", "015"})
            rejects(() -> parse(BASE + ",\"windowSeconds\":" + value));
    }
    @Test public void duplicateKeysAndJsonFieldsAreRejected() {
        rejects(() -> ExactRule.parseDocument("{\"format\":\"qingping-rules-v1\",\"rules\":[{" + BASE + "},{" + BASE + "}]}"));
        rejects(() -> parse(BASE + ",\"text\":\"first\",\"text\":\"second\""));
        rejects(() -> ExactRule.parseDocument("{\"format\":null,\"format\":\"qingping-rules-v1\",\"rules\":[]}"));
        ExactRule rule = parse(BASE);
        rejects(() -> ExactRule.serializeDocument(java.util.Arrays.asList(rule, rule)));
    }
    @Test public void requiredFieldsAndKeyLengthAreValidated() {
        rejects(() -> parse(BASE.replace("\"key\":\"uc-close\",", "")));
        rejects(() -> parse(BASE.replace("\"name\":\"关闭广告\"", "\"name\":\" \"")));
        rejects(() -> new ExactRule("x".repeat(65), "name", "cn.example.app", "", "cn.example:id/close", "", "", false, 15));
        rejects(() -> new ExactRule("bad key", "name", "cn.example.app", "", "cn.example:id/close", "", "", false, 15));
    }
    @Test public void criticalPackagesAreBlockedAtConstructionAndImport() {
        for (String pkg : new String[]{"android", "cn.qingping.app", "com.android.settings", "com.android.systemui", "com.google.android.permissioncontroller", "com.vendor.launcher", "com.vendor.inputmethod"})
            rejects(() -> parse(BASE.replace("com.UCMobile", pkg)));
    }
    @Test public void documentRuleLimitAccepts100AndRejects101() {
        List<ExactRule> rules = new ArrayList<>();
        for (int i = 0; i < 100; i++) rules.add(new ExactRule("k" + i, "name", "cn.example.app", "", "sdk.vendor:id/close", "", "", false, 15));
        assertEquals(100, ExactRule.parseDocument(ExactRule.serializeDocument(rules)).size());
        rules.add(new ExactRule("k100", "name", "cn.example.app", "", "sdk.vendor:id/close", "", "", false, 15));
        rejects(() -> ExactRule.serializeDocument(rules));
        StringBuilder many = new StringBuilder("{\"format\":\"qingping-rules-v1\",\"rules\":[");
        for (int i = 0; i < 101; i++) { if (i > 0) many.append(','); many.append('{').append(BASE.replace("uc-close", "k" + i)).append('}'); }
        rejects(() -> ExactRule.parseDocument(many.append("]}").toString()));
    }
    @Test public void byteLimitCountsUtf8BytesAndNotOnlyCharacters() {
        rejects(() -> ExactRule.parseDocument(" ".repeat(ExactRule.MAX_DOCUMENT_BYTES + 1)));
        rejects(() -> ExactRule.parseDocument("中".repeat(ExactRule.MAX_DOCUMENT_BYTES / 3 + 1)));
        rejects(() -> ExactRule.parseDocument(null));
    }
    @Test public void malformedJsonAndExcessiveDepthAreRejected() {
        for (String json : new String[]{"", "[]", "{}", "//comment\n{}", "{format:'qingping-rules-v1'}", doc(BASE) + "true", doc(BASE).replace("]}", ",]}"), doc(BASE).replace("uc-close", "bad\nkey")})
            rejects(() -> ExactRule.parseDocument(json));
        rejects(() -> ExactRule.parseDocument("[".repeat(20) + "0" + "]".repeat(20)));
    }
    @Test public void escapedUnicodeAndBOMRoundTripWithoutChangingSelectors() {
        ExactRule original = new ExactRule("close", "清屏😀", "cn.example.app", "cn.example.Splash", "sdk.vendor:id/close", "关\"闭\\\n广告", "关闭😀", true, 15);
        String serialized = ExactRule.serializeDocument(Collections.singletonList(original));
        ExactRule copy = ExactRule.parseDocument("\uFEFF" + serialized).get(0);
        assertEquals(original.name, copy.name);
        assertEquals(original.text, copy.text);
        assertEquals(original.description, copy.description);
        assertEquals(serialized, ExactRule.serializeDocument(Collections.singletonList(copy)));
        assertEquals("关闭", parse(BASE + ",\"text\":\"\\u5173\\u95ed\"").text);
    }
    @Test public void invalidUnicodeAndEscapesAreRejected() {
        rejects(() -> parse(BASE + ",\"text\":\"\\uD800\""));
        rejects(() -> parse(BASE + ",\"text\":\"\\uDC00\""));
        rejects(() -> parse(BASE + ",\"text\":\"\\uZZZZ\""));
        rejects(() -> parse(BASE + ",\"text\":\"\\q\""));
        rejects(() -> new ExactRule("k", "name", "cn.example.app", "", "cn.example:id/close", "\uD800", "", false, 15));
    }
    @Test public void emptyDocumentIsValidAndSerializesDeterministically() {
        assertEquals("{\"format\":\"qingping-rules-v1\",\"rules\":[]}", ExactRule.serializeDocument(Collections.emptyList()));
        assertTrue(ExactRule.parseDocument(ExactRule.serializeDocument(Collections.emptyList())).isEmpty());
        rejects(() -> ExactRule.serializeDocument(null));
        rejects(() -> ExactRule.serializeDocument(Collections.singletonList(null)));
    }
}
