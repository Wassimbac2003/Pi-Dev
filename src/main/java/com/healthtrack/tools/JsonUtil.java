package com.healthtrack.tools;

import java.util.ArrayList;
import java.util.List;

public final class JsonUtil {
    private JsonUtil() {
    }

    public static String toJsonArray(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return "[]";
        }
        String[] parts = csv.split(",");
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (String p : parts) {
            String v = p.trim();
            if (v.isEmpty()) {
                continue;
            }
            if (!first) {
                sb.append(",");
            }
            sb.append("\"").append(escape(v)).append("\"");
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toCsv(String jsonArray) {
        if (jsonArray == null || jsonArray.isBlank()) {
            return "";
        }
        String text = jsonArray.trim();
        if (text.equals("[]")) {
            return "";
        }
        text = text.replace('[', ' ').replace(']', ' ').trim();
        if (text.isEmpty()) {
            return "";
        }
        List<String> values = new ArrayList<>();
        boolean inQuote = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
                continue;
            }
            if (c == ',' && !inQuote) {
                String value = current.toString().trim();
                if (!value.isEmpty()) {
                    values.add(value);
                }
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        String value = current.toString().trim();
        if (!value.isEmpty()) {
            values.add(value);
        }
        return String.join(",", values);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
