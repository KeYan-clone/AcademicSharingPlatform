package com.scholar.platform.util;

import org.springframework.data.domain.Pageable;

public final class CacheKeyUtil {

    private CacheKeyUtil() {
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    public static String advancedSearchKey(String keyword, String field,
                                           String startDate, String endDate,
                                           String authorName, String institutionName,
                                           String sortBy, String sortOrder,
                                           Pageable pageable) {
        return new StringBuilder("adv::")
            .append(sanitize(keyword)).append("::")
            .append(sanitize(field)).append("::")
            .append(sanitize(startDate)).append("::")
            .append(sanitize(endDate)).append("::")
            .append(sanitize(authorName)).append("::")
            .append(sanitize(institutionName)).append("::")
            .append(sanitize(sortBy)).append("::")
            .append(sanitize(sortOrder)).append("::")
            .append(pageable.getPageNumber()).append("::")
                .append(pageable.getPageSize())
                .toString();
    }

    public static String patentSearchKey(String keyword, Integer applicationYear,
                                         Integer grantYear, Pageable pageable) {
        return new StringBuilder("patent::")
            .append(sanitize(keyword)).append("::")
            .append(applicationYear == null ? "" : applicationYear).append("::")
            .append(grantYear == null ? "" : grantYear).append("::")
            .append(pageable.getPageNumber()).append("::")
                .append(pageable.getPageSize())
                .toString();
    }
}
