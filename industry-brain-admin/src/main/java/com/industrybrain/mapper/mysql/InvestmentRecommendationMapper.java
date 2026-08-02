package com.industrybrain.mapper.mysql;

import com.industrybrain.domain.model.InvestmentRecommendation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 招商推荐 MySQL Mapper
 * 负责招商推荐记录的 CRUD 和复杂查询
 */
@Mapper
public interface InvestmentRecommendationMapper {

    /**
     * 根据ID查询
     */
    InvestmentRecommendation selectById(@Param("id") Long id);

    /**
     * 分页查询推荐列表（多条件筛选）
     */
    List<InvestmentRecommendation> selectMatchList(@Param("regionId") Long regionId,
                                                    @Param("weakness") String weakness,
                                                    @Param("matchMin") Integer matchMin,
                                                    @Param("scale") String scale,
                                                    @Param("sortBy") String sortBy,
                                                    @Param("keyword") String keyword,
                                                    @Param("nodeId") Long nodeId,
                                                    @Param("tab") String tab,
                                                    @Param("offset") Integer offset,
                                                    @Param("limit") Integer limit);

    /**
     * 统计推荐列表数量
     */
    int countMatchList(@Param("regionId") Long regionId,
                       @Param("weakness") String weakness,
                       @Param("matchMin") Integer matchMin,
                       @Param("scale") String scale,
                       @Param("sortBy") String sortBy,
                       @Param("keyword") String keyword,
                       @Param("nodeId") Long nodeId,
                       @Param("tab") String tab);

    /**
     * 查询企业详情（含三维评分）
     */
    InvestmentRecommendation selectEnterpriseDetail(@Param("enterpriseId") Long enterpriseId,
                                                     @Param("nodeId") Long nodeId);

    /**
     * 查询清单列表
     */
    List<InvestmentRecommendation> selectWishlist(@Param("regionId") Long regionId);

    /**
     * 检查是否已在清单中
     */
    int checkInWishlist(@Param("enterpriseId") Long enterpriseId,
                        @Param("nodeId") Long nodeId);

    /**
     * 加入清单（更新状态）
     */
    int addToWishlist(@Param("enterpriseId") Long enterpriseId,
                      @Param("nodeId") Long nodeId);

    /**
     * 移出清单
     */
    int removeFromWishlist(@Param("id") Long id);

    /**
     * 新增推荐记录
     */
    int insert(InvestmentRecommendation recommendation);

    /**
     * 批量插入
     */
    int batchInsert(@Param("list") List<InvestmentRecommendation> recommendations);

    /**
     * 更新推荐记录
     */
    int update(InvestmentRecommendation recommendation);

    /**
     * 删除推荐记录
     */
    int deleteById(@Param("id") Long id);

    int deleteAll();

    /**
     * 根据企业ID和节点ID查询
     */
    InvestmentRecommendation selectByEnterpriseAndNode(@Param("enterpriseId") Long enterpriseId,
                                                        @Param("nodeId") Long nodeId);

    /**
     * 查询企业匹配的所有节点
     */
    List<InvestmentRecommendation> selectByEnterpriseId(@Param("enterpriseId") Long enterpriseId);

    /**
     * 查询节点下的所有推荐
     */
    List<InvestmentRecommendation> selectByNodeId(@Param("nodeId") Long nodeId);

    /**
     * 批量更新状态
     */
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") String status);
}
