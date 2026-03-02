package com.murder.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户积分记录VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPointsRecordVO implements Serializable {
    
    private Long id;
    
    /**
     * 积分变化
     */
    private Integer points;
    
    /**
     * 类型：1签到，2消费，3评价，4退款，5兑换，6过期
     */
    private Integer type;
    
    /**
     * 类型名称
     */
    private String typeName;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
