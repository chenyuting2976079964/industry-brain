package com.industrybrain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private Long total;
    private Integer page;
    private Integer size;
    private List<T> rows;

    public static <T> PageResponse<T> of(List<T> rows, Long total, Integer page, Integer size) {
        return PageResponse.<T>builder()
            .rows(rows).total(total).page(page).size(size).build();
    }
}
