package com.scholar.platform.service.cache;

import java.util.List;

public class CachedPage<T> {

    private List<T> records;
    private long total;

    public CachedPage() {
    }

    public CachedPage(List<T> records, long total) {
        this.records = records;
        this.total = total;
    }

    public static <T> CachedPage<T> of(List<T> records, long total) {
        return new CachedPage<>(records, total);
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
