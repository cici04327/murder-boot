package com.murder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 确认AI DM分配方案请求 DTO
 */
@Data
@Schema(description = "确认AI DM分配方案")
public class AiDmConfirmDTO implements Serializable {

    @Schema(description = "分配项列表")
    private List<AssignItem> items;

    @Data
    @Schema(description = "单条分配项")
    public static class AssignItem implements Serializable {
        @Schema(description = "排期ID")
        private Long scheduleId;

        @Schema(description = "要分配的DM ID")
        private Long dmId;
    }
}
