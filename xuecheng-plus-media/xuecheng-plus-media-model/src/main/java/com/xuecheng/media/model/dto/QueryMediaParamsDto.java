package com.xuecheng.media.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;

/**
 * @author Mr.M
 * @version 1.0
 * @description 媒资文件查询请求模型类
 * @date 2022/9/10 8:53
 */
@Data
@ToString
public class QueryMediaParamsDto {

    @ApiModelProperty("媒资文件名称")
    private String filename;
    @ApiModelProperty("媒资类型")
    private String fileType;
    @ApiModelProperty("审核状态")
    private String auditStatus;

    @ApiModelProperty("排序字段，可选：createDate")
    private String sortBy;

    @ApiModelProperty("排序方向，可选：asc/desc")
    private String sortOrder;
}
