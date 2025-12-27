package com.scholar.platform.util;

public class IdPrefixUtil {
    private static final String ID_PREFIX = "https://openalex.org/";

    static public String ensureIdPrefix(String id) {
        if (id != null && !id.startsWith(ID_PREFIX)) {
            return ID_PREFIX + id;
        }
        return id;
    }

    static public String removeIdPrefix(String id) {
        if (id != null && id.startsWith(ID_PREFIX)) {
            return id.substring(ID_PREFIX.length());
        }
        return id;
    }
}
