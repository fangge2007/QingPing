package cn.qingping.app.rules;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Local, explicit selectors. This format deliberately has no executable expressions. */
public final class ExactRule {
    public static final String FORMAT = "qingping-rules-v1";
    public static final int MAX_DOCUMENT_BYTES = 256 * 1024;
    public static final int MAX_RULES = 100;
    public static final int DEFAULT_WINDOW_SECONDS = 15;
    private static final Pattern PACKAGE = Pattern.compile("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+");
    private static final Pattern ACTIVITY = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)+");
    private static final Pattern VIEW_ID = Pattern.compile("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+:id/[A-Za-z_][A-Za-z0-9_]*");
    private static final Set<String> FIELDS = fields("key", "name", "packageName", "activity", "viewId", "text", "description", "allowGesture", "windowSeconds", "persistent");

    public final String key, name, packageName, activity, viewId, text, description;
    public final boolean allowGesture;
    public final boolean persistent;
    public final int windowSeconds;

    public ExactRule(String key, String name, String packageName, String activity,
                     String viewId, String text, String description,
                     boolean allowGesture, int windowSeconds) {
        this(key, name, packageName, activity, viewId, text, description, allowGesture, windowSeconds, false);
    }

    public ExactRule(String key, String name, String packageName, String activity,
                     String viewId, String text, String description,
                     boolean allowGesture, int windowSeconds, boolean persistent) {
        this.key = required(key, "key", 64);
        if (!this.key.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")) fail("key 只能包含字母、数字、点、下划线和连字符，且须以字母或数字开头");
        this.name = required(name, "name", 80);
        this.packageName = required(packageName, "packageName", 200);
        if (!PACKAGE.matcher(this.packageName).matches() || isCriticalPackage(this.packageName)) fail("来源包名无效或属于系统关键应用");
        this.activity = optional(activity, "activity", 240);
        if (!this.activity.isEmpty() && !ACTIVITY.matcher(this.activity).matches()) fail("activity 必须为完整类名，不能使用相对名称或通配符");
        this.viewId = optional(viewId, "viewId", 260);
        // SDK resources legitimately use a namespace different from the host app.
        if (!this.viewId.isEmpty() && !VIEW_ID.matcher(this.viewId).matches()) fail("viewId 必须为完整 namespace:id/name");
        this.text = optional(text, "text", 256);
        this.description = optional(description, "description", 256);
        if (this.viewId.isEmpty() && this.text.isEmpty() && this.description.isEmpty()) fail("至少配置 viewId、text、description 中的一项精确条件");
        if (this.viewId.isEmpty() && this.activity.isEmpty()) fail("没有 viewId 时必须同时指定完整 activity，避免误点通用关闭按钮");
        if (windowSeconds < 3 || windowSeconds > 30) fail("windowSeconds 必须为 3 到 30 的整数");
        this.allowGesture = allowGesture;
        this.windowSeconds = windowSeconds;
        this.persistent = persistent;
    }

    /** Existing rules retain their entry window unless the owner explicitly enables persistence. */
    public boolean activeAt(long elapsed) {
        return elapsed >= 0 && (persistent || elapsed <= windowSeconds * 1_000L);
    }

    /** Match every configured selector literally; no wildcard, regex or coordinate language. */
    public static boolean matches(ExactRule rule, String pkg, String activity,
                                  String viewId, String text, String description) {
        return rule != null && rule.packageName.equals(pkg)
                && (rule.activity.isEmpty() || rule.activity.equals(activity))
                && (rule.viewId.isEmpty() || rule.viewId.equals(viewId))
                && (rule.text.isEmpty() || rule.text.equals(text))
                && (rule.description.isEmpty() || rule.description.equals(description));
    }

    public static List<ExactRule> parseDocument(String json) {
        checkSize(json);
        Object parsed = new JsonReader(json).read();
        Map<String, Object> document = object(parsed, "规则文件");
        checkFields(document, fields("format", "rules"));
        if (!FORMAT.equals(document.get("format"))) fail("仅支持 qingping-rules-v1；不支持 GKD 订阅或其他规则语法");
        Object value = document.get("rules");
        if (!(value instanceof List<?>)) fail("rules 必须为数组");
        List<?> rows = (List<?>) value;
        if (rows.size() > MAX_RULES) fail("最多导入 100 条规则");
        List<ExactRule> result = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (Object row : rows) {
            Map<String, Object> map = object(row, "每条规则");
            checkFields(map, FIELDS);
            Object gesture = map.get("allowGesture");
            if (map.containsKey("allowGesture") && !(gesture instanceof Boolean)) fail("allowGesture 必须为布尔值 true 或 false");
            Object persistent = map.get("persistent");
            if (map.containsKey("persistent") && !(persistent instanceof Boolean)) fail("persistent 必须为布尔值 true 或 false");
            int seconds = DEFAULT_WINDOW_SECONDS;
            if (map.containsKey("windowSeconds")) {
                Object number = map.get("windowSeconds");
                if (!(number instanceof JsonNumber) || !((JsonNumber) number).value.matches("-?(?:0|[1-9][0-9]*)")) fail("windowSeconds 必须为整数");
                try { seconds = Integer.parseInt(((JsonNumber) number).value); }
                catch (NumberFormatException ex) { throw new IllegalArgumentException("windowSeconds 超出允许范围", ex); }
            }
            ExactRule rule = new ExactRule(string(map, "key", true), string(map, "name", true),
                    string(map, "packageName", true), string(map, "activity", false),
                    string(map, "viewId", false), string(map, "text", false),
                    string(map, "description", false), Boolean.TRUE.equals(gesture), seconds, Boolean.TRUE.equals(persistent));
            if (!keys.add(rule.key)) fail("重复的规则 key：" + rule.key);
            result.add(rule);
        }
        return result;
    }

    public static String serializeDocument(List<ExactRule> rules) {
        if (rules == null || rules.size() > MAX_RULES) fail("规则列表不能为空，且最多 100 条");
        StringBuilder out = new StringBuilder("{\"format\":\"" + FORMAT + "\",\"rules\":[");
        Set<String> keys = new HashSet<>();
        boolean comma = false;
        for (ExactRule rule : rules) {
            if (rule == null) fail("规则不能为 null");
            if (!keys.add(rule.key)) fail("重复的规则 key：" + rule.key);
            if (comma) out.append(',');
            comma = true;
            out.append('{');
            field(out, "key", rule.key); out.append(',');
            field(out, "name", rule.name); out.append(',');
            field(out, "packageName", rule.packageName); out.append(',');
            field(out, "activity", rule.activity); out.append(',');
            field(out, "viewId", rule.viewId); out.append(',');
            field(out, "text", rule.text); out.append(',');
            field(out, "description", rule.description);
            out.append(",\"allowGesture\":").append(rule.allowGesture)
                    .append(",\"windowSeconds\":").append(rule.windowSeconds);
            if (rule.persistent) out.append(",\"persistent\":true");
            out.append('}');
        }
        String json = out.append("]}").toString();
        checkSize(json);
        return json;
    }

    public static boolean isCriticalPackage(String pkg) {
        return cn.qingping.app.engine.PackagePolicy.isCritical(pkg);
    }

    private static void checkSize(String json) {
        if (json == null || json.length() > MAX_DOCUMENT_BYTES
                || json.getBytes(StandardCharsets.UTF_8).length > MAX_DOCUMENT_BYTES) fail("规则文件不能为空，且不能超过 256KB");
    }
    private static String required(String value, String field, int max) {
        if (value == null || value.trim().isEmpty()) fail(field + " 不能为空");
        return optional(value, field, max);
    }
    private static String optional(String value, String field, int max) {
        String result = value == null ? "" : value;
        if (result.length() > max) fail(field + " 过长");
        if (!result.isEmpty() && result.trim().isEmpty()) fail(field + " 不能只有空白");
        validateUnicode(result);
        return result;
    }
    private static String string(Map<String, Object> map, String field, boolean required) {
        if (!map.containsKey(field)) {
            if (required) fail("缺少字段：" + field);
            return "";
        }
        Object value = map.get(field);
        if (!(value instanceof String)) fail(field + " 必须为字符串");
        return (String) value;
    }
    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value, String what) {
        if (!(value instanceof Map<?, ?>)) fail(what + "必须为 JSON 对象；不支持 GKD 规则语法");
        return (Map<String, Object>) value;
    }
    private static void checkFields(Map<String, Object> map, Set<String> allowed) {
        for (String key : map.keySet()) if (!allowed.contains(key)) fail("不支持字段：" + key + "；仅支持本地精确规则，不支持 GKD/表达式/坐标语法");
    }
    private static Set<String> fields(String... names) { return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(names))); }
    private static void validateUnicode(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (++i >= value.length() || !Character.isLowSurrogate(value.charAt(i))) fail("字符串包含无效 Unicode");
            } else if (Character.isLowSurrogate(c)) fail("字符串包含无效 Unicode");
        }
    }
    private static void field(StringBuilder out, String key, String value) { quote(out, key); out.append(':'); quote(out, value); }
    private static void quote(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        out.append("\\u");
                        String hex = Integer.toHexString(c);
                        for (int j = hex.length(); j < 4; j++) out.append('0');
                        out.append(hex);
                    } else out.append(c);
            }
        }
        out.append('"');
    }
    private static void fail(String message) { throw new IllegalArgumentException(message); }
    private static final class JsonNumber { final String value; JsonNumber(String value) { this.value = value; } }

    /** Strict JSON, bounded before parsing and again by depth/node/container limits. */
    private static final class JsonReader {
        private final String source;
        private int position, nodes;
        JsonReader(String source) { this.source = source; if (source.startsWith("\uFEFF")) position = 1; }
        Object read() {
            Object value = value(0); whitespace();
            if (position != source.length()) error("JSON 后存在多余内容");
            return value;
        }
        private Object value(int depth) {
            if (depth > 16 || ++nodes > 4096) error("JSON 嵌套或节点数量超限");
            whitespace();
            if (position >= source.length()) error("JSON 意外结束");
            char c = source.charAt(position);
            if (c == '{') return object(depth + 1);
            if (c == '[') return array(depth + 1);
            if (c == '"') return string();
            if (c == 't') { literal("true"); return Boolean.TRUE; }
            if (c == 'f') { literal("false"); return Boolean.FALSE; }
            if (c == 'n') { literal("null"); return null; }
            if (c == '-' || c >= '0' && c <= '9') return number();
            error("无效 JSON 值"); return null;
        }
        private Map<String, Object> object(int depth) {
            position++; whitespace();
            Map<String, Object> map = new LinkedHashMap<>();
            if (take('}')) return map;
            do {
                whitespace();
                if (position >= source.length() || source.charAt(position) != '"') error("JSON 字段名必须带双引号");
                String key = string();
                if (map.containsKey(key)) error("重复 JSON 字段：" + key);
                if (map.size() >= 32) error("JSON 对象字段过多");
                whitespace(); expect(':');
                map.put(key, value(depth)); whitespace();
                if (take('}')) return map;
                expect(',');
            } while (true);
        }
        private List<Object> array(int depth) {
            position++; whitespace();
            List<Object> values = new ArrayList<>();
            if (take(']')) return values;
            do {
                if (values.size() >= 128) error("JSON 数组过长");
                values.add(value(depth)); whitespace();
                if (take(']')) return values;
                expect(',');
            } while (true);
        }
        private String string() {
            expect('"'); StringBuilder out = new StringBuilder();
            while (position < source.length()) {
                char c = source.charAt(position++);
                if (c == '"') { String value = out.toString(); validateUnicode(value); return value; }
                if (c < 0x20) error("JSON 字符串包含未转义控制字符");
                if (c != '\\') { out.append(c); continue; }
                if (position >= source.length()) error("JSON 转义意外结束");
                char escape = source.charAt(position++);
                switch (escape) {
                    case '"': case '\\': case '/': out.append(escape); break;
                    case 'b': out.append('\b'); break;
                    case 'f': out.append('\f'); break;
                    case 'n': out.append('\n'); break;
                    case 'r': out.append('\r'); break;
                    case 't': out.append('\t'); break;
                    case 'u':
                        if (position + 4 > source.length()) error("JSON Unicode 转义不完整");
                        int number = 0;
                        for (int i = 0; i < 4; i++) {
                            char hex = source.charAt(position++);
                            int digit = hex >= '0' && hex <= '9' ? hex - '0' : hex >= 'a' && hex <= 'f' ? hex - 'a' + 10 : hex >= 'A' && hex <= 'F' ? hex - 'A' + 10 : -1;
                            if (digit < 0) error("JSON Unicode 转义无效");
                            number = number * 16 + digit;
                        }
                        out.append((char) number); break;
                    default: error("JSON 转义无效");
                }
            }
            error("JSON 字符串未结束"); return "";
        }
        private JsonNumber number() {
            int start = position;
            take('-');
            if (!take('0')) { if (!digit()) error("无效 JSON 数字"); while (digit()) position++; }
            if (take('.')) { if (!digit()) error("无效 JSON 小数"); while (digit()) position++; }
            if (take('e') || take('E')) { if (!take('+')) take('-'); if (!digit()) error("无效 JSON 指数"); while (digit()) position++; }
            return new JsonNumber(source.substring(start, position));
        }
        private boolean digit() { return position < source.length() && source.charAt(position) >= '0' && source.charAt(position) <= '9'; }
        private void literal(String token) { if (!source.startsWith(token, position)) error("无效 JSON 字面量"); position += token.length(); }
        private void whitespace() { while (position < source.length() && " \t\r\n".indexOf(source.charAt(position)) >= 0) position++; }
        private boolean take(char c) { if (position < source.length() && source.charAt(position) == c) { position++; return true; } return false; }
        private void expect(char c) { if (!take(c)) error("JSON 缺少字符 " + c); }
        private void error(String message) { fail(message + "（位置 " + position + "）"); }
    }
}
