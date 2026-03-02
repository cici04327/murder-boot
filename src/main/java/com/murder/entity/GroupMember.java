package com.murder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 拼单成员实体类
 */
@Data
@TableName("group_member")
public class GroupMember {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 拼单ID */
    private Long groupId;
    
    /** 用户ID */
    private Long userId;
    
    /** 用户昵称 */
    private String nickname;
    
    /** 用户头像 */
    private String avatar;
    
    /** 是否发起人 0-否 1-是 */
    private Integer isCreator;
    
    /** 参与人数（该用户带的人数） */
    private Integer joinCount;
    
    /** 状态 0-已退出 1-已加入 */
    private Integer status;
    
    /** 加入时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
