package com.murder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * DM 新增/编辑请求 DTO
 */
@Data
@Schema(description = "DM 请求")
public class DmDTO implements Serializable {

    @Schema(description = "DM ID（编辑时必传）")
    private Long id;

    @Schema(description = "所属门店ID")
    private Long storeId;

    @Schema(description = "DM 姓名/艺名")
    private String name;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "简介")
    private String introduction;

    @Schema(description = "风格标签，逗号分隔（推理型,情感型,恐怖型）")
    private String styleTags;

    @Schema(description = "状态：1在职，0离职")
    private Integer status;
}
