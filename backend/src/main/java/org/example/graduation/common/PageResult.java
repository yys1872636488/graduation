package org.example.graduation.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一分页结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private long total;
    private List<T> records;
    private long page;
    private long size;

    public static <T> PageResult<T> of(long total, List<T> records) {
        return new PageResult<>(total, records, 0, 0);
    }
}