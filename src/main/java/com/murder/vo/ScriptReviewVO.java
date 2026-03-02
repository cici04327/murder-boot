package com.murder.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 剧本评价VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "剧本评价信息")
public class ScriptReviewVO implements Serializable {

    @Schema(description = "评价ID")
    private Long id;
    
    @Schema(description = "剧本ID")
    private Long scriptId;
    
    @Schema(description = "剧本名称")
    private String scriptName;
    
    @Schema(description = "用户ID")
    private Long userId;
    
    @Schema(description = "用户昵称")
    private String userNickname;
    
    @Schema(description = "预约ID")
    private Long reservationId;
    
    @Schema(description = "评分：1-5星")
    private Integer rating;
    
    @Schema(description = "剧情评分")
    private Integer storyRating;
    
    @Schema(description = "难度评分")
    private Integer difficultyRating;
    
    @Schema(description = "评价内容")
    private String content;
    
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
