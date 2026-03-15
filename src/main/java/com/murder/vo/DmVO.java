package com.murder.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DM VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DM 信息")
public class DmVO implements Serializable {

    @Schema(description = "DM ID")
    private Long id;

    @Schema(description = "所属门店ID")
    private Long storeId;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "DM 姓名/艺名")
    private String name;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "简介")
    private String introduction;

    @Schema(description = "风格标签，逗号分隔")
    private String styleTags;

    @Schema(description = "风格标签列表（前端展示用）")
    private List<String> styleTagList;

    @Schema(description = "综合评分")
    private BigDecimal rating;

    @Schema(description = "累计主持场次")
    private Integer gameCount;

    @Schema(description = "状态：1在职，0离职")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
