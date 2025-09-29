package com.xuecheng.base.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
public class PageParams {
    private Long pageNo = 1L;
    private Long pageSize = 30L;
}
