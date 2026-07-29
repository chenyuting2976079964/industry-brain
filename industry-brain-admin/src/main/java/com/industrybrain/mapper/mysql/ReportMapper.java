package com.industrybrain.mapper.mysql;

import com.industrybrain.domain.model.ReportRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 报告 MySQL Mapper
 * 负责招商报告的 CRUD 和统计
 */
@Mapper
public interface ReportMapper {

    /**
     * 根据ID查询
     */
    ReportRecord selectById(@Param("id") Long id);

    /**
     * 分页查询报告列表
     */
    List<ReportRecord> selectList(@Param("offset") Integer offset,
                                   @Param("limit") Integer limit);

    /**
     * 统计报告总数
     */
    int countAll();

    /**
     * 新增报告
     */
    int insert(ReportRecord report);

    /**
     * 更新报告
     */
    int update(ReportRecord report);

    /**
     * 删除报告
     */
    int deleteById(@Param("id") Long id);

    /**
     * 本月报告统计
     */
    int countMonthly();

    /**
     * 累计报告总数
     */
    int countTotal();

    /**
     * 覆盖企业数量（去重）
     */
    int countCoveredEnterprises();

    /**
     * 统计上月报告数（用于计算增长率）
     */
    int countLastMonth();
}
