package com.sifa.core_sifa.util;

import java.util.ArrayList;
import java.util.List;

public final class SqlStatementSplitter {

    private SqlStatementSplitter() {}

    public static List<String> split(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        char stringChar = 0;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);

            if (inString) {
                current.append(c);
                if (c == stringChar) {
                    inString = false;
                }
            } else if (c == '\'' || c == '"') {
                inString = true;
                stringChar = c;
                current.append(c);
            } else if (c == ';') {
                String s = current.toString().trim();
                if (!s.isEmpty()) {
                    statements.add(s);
                }
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        String s = current.toString().trim();
        if (!s.isEmpty()) {
            statements.add(s);
        }

        return statements;
    }
}
