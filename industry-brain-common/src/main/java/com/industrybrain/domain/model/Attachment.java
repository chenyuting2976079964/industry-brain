package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 附件实体 (attachment表)
 * @author 何锦鹏
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("attachment")
public class Attachment {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务类型 */
    private String bizType;

    /** 业务ID */
    private Long bizId;

    /** 文件名 */
    private String fileName;

    /** 文件URL */
    private String fileUrl;

    /** 文件大小 (字节) */
    private Long fileSize;

    /** 文件类型/MIME */
    private String fileType;

    /** 排序 */
    private Integer sortOrder;

    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
