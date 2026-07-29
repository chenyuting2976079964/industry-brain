package com.industrybrain.common.core.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 实体基类
 * 若依风格：所有领域实体继承此类，提供公共字段
 */
@Data
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    private Date updateTime;

    /** 备注 */
    private String remark;
}
