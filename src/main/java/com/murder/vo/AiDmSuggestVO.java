package com.murder.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * AI DM分配建议 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI DM分配建议结果")
public class AiDmSuggestVO implements Serializable {

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "总排期数")
    private Integer totalSchedules;

    @Schema(description = "已成功推荐DM的排期数")
    private Integer assignedCount;

    @Schema(description = "无可用DM的排期数")
    private Integer unassignedCount;

    @Schema(description = "AI总结说明")
    private String summary;

    @Schema(description = "分配建议列表")
    private List<ScheduleSuggest> suggests;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "单条排期的DM分配建议")
    public static class ScheduleSuggest implements Serializable {

        @Schema(description = "排期ID")
        private Long scheduleId;

        @Schema(description = "排期日期")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate scheduleDate;

        @Schema(description = "开始时间")
        @JsonFormat(pattern = "HH:mm")
        private LocalTime startTime;

        @Schema(description = "结束时间")
        @JsonFormat(pattern = "HH:mm")
        private LocalTime endTime;

        @Schema(description = "剧本名")
        private String scriptName;

        @Schema(description = "剧本标签")
        private String scriptTags;

        @Schema(description = "推荐DM ID")
        private Long recommendDmId;

        @Schema(description = "推荐DM姓名")
        private String recommendDmName;

        @Schema(description = "推荐DM头像")
        private String recommendDmAvatar;

        @Schema(description = "推荐DM评分")
        private BigDecimal recommendDmRating;

        @Schema(description = "推荐DM本期已排场次")
        private Integer dmScheduledCount;

        @Schema(description = "匹配得分（0-100）")
        private Integer matchScore;

        @Schema(description = "推荐理由")
        private String reason;

        @Schema(description = "是否有可用DM")
        private Boolean hasCandidate;

        @Schema(description = "备选DM列表（前3名）")
        private List<DmCandidate> candidates;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "备选DM")
    public static class DmCandidate implements Serializable {
        @Schema(description = "DM ID")
        private Long dmId;

        @Schema(description = "DM姓名")
        private String name;

        @Schema(description = "DM头像")
        private String avatar;

        @Schema(description = "DM评分")
        private BigDecimal rating;

        @Schema(description = "本期已排场次")
        private Integer scheduledCount;

        @Schema(description = "匹配得分")
        private Integer matchScore;
    }
}
