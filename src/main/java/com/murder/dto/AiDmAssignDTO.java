package com.murder.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * AI DM智能分配请求 DTO
 */
@Data
@Schema(description = "AI DM智能分配请求")
public class AiDmAssignDTO implements Serializable {

    @Schema(description = "门店ID", required = true)
    private Long storeId;

    @Schema(description = "开始日期", example = "2026-04-01")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Schema(description = "结束日期", example = "2026-04-07")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Schema(description = "是否只处理未分配DM的排期（默认true）")
    private Boolean onlyUnassigned = true;
}
