package com.industrybrain.service.impl;

import com.industrybrain.domain.model.Enterprise;
import com.industrybrain.domain.model.*;
import com.industrybrain.dto.request.EnterpriseSearchRequest;
import com.industrybrain.dto.response.*;
import com.industrybrain.mapper.mysql.PatentMapper;
import com.industrybrain.mapper.mysql.RegionMapper;
import com.industrybrain.mapper.mysql.IndustrialParkMapper;
import com.industrybrain.service.IEnterpriseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 企业详情业务实现 (v2.0 合并版 — E01~E05)
 * 所有数据均从数据库查询，不依赖硬编码字符串匹配。
 * @author 何锦鹏
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseServiceImpl implements IEnterpriseService {

    private final com.industrybrain.mapper.mysql.EnterpriseMapper enterpriseMapper;
    private final PatentMapper patentMapper;
    @Resource
    private RegionMapper regionMapper;
    @Resource
    private IndustrialParkMapper parkMapper;

    // ─── E01: 企业基本信息 ───
    @Override
    public EnterpriseResponse getEnterprise(Long id) {
        com.industrybrain.domain.model.Enterprise ent = enterpriseMapper.selectById(id);
        if (ent == null) return null;

        EnterpriseResponse vo = new EnterpriseResponse();
        vo.setId(ent.getId());
        vo.setEntName(ent.getName());
        vo.setShortName(ent.getName());
        vo.setAvatarLetter(ent.getName() != null && !ent.getName().isEmpty()
            ? ent.getName().substring(0, 1) : "企");
        vo.setLegalPerson(ent.getLegalRepresentative());
        vo.setRegisteredCapital(ent.getRegisteredCapital());
        if (ent.getEstablishmentDate() != null) {
            vo.setEstablishYear(ent.getEstablishmentDate().getYear());
        }
        // 从数据库字段获取省市
        vo.setProvince(ent.getRegisteredAddress());
        vo.setCity(ent.getCity() != null ? ent.getCity() : "");
        vo.setDistrict(ent.getDistrict() != null ? ent.getDistrict() : "");
        vo.setScale(ent.getIsLocal() != null && ent.getIsLocal() == 1 ? "本地企业" : "外地企业");

        // 标签从数据库字段推导
        List<String> tags = new ArrayList<>();
        if (ent.getIsLocal() != null && ent.getIsLocal() == 1) tags.add("本地企业");
        if (ent.getEnterpriseType() != null && !ent.getEnterpriseType().isEmpty()) {
            tags.add(ent.getEnterpriseType());
        }
        vo.setTags(tags);
        return vo;
    }

    // ─── E02: 企业技术画像 ───
    @Override
    public EnterpriseProfileResponse getProfile(Long id) {
        EnterpriseProfileResponse vo = new EnterpriseProfileResponse();

        EnterpriseProfileResponse.PatentStats stats = new EnterpriseProfileResponse.PatentStats();
        stats.setTotalPatents(patentMapper.selectCountByEnterpriseId(id));
        stats.setHighValuePatents(patentMapper.selectHighValueCountByEnterpriseId(id));
        stats.setInventionPatents(patentMapper.selectInventionCountByEnterpriseId(id));
        stats.setTechMatchRate(computeTechMatchRate(id, stats.getTotalPatents(), stats.getHighValuePatents()));
        vo.setPatentStats(stats);

        List<Map<String, Object>> ipcDist = enterpriseMapper.selectIpcDistribution(id);
        if (ipcDist != null && !ipcDist.isEmpty()) {
            vo.setAdvantages(ipcDist.stream().limit(3).map(m -> {
                EnterpriseProfileResponse.AdvantageItem adv = new EnterpriseProfileResponse.AdvantageItem();
                adv.setName((String) m.getOrDefault("ipcMain", "未知领域"));
                adv.setPatentCount(toInt(m.get("cnt")));
                adv.setPercentage(toInt(m.get("cnt")) * 100.0 / Math.max(stats.getTotalPatents(), 1));
                adv.setDescription(String.format("在该领域拥有%d件专利", adv.getPatentCount()));
                return adv;
            }).collect(Collectors.toList()));
        }

        vo.setTechTags(Collections.emptyList());
        vo.setWeaknesses(Collections.emptyList());
        return vo;
    }

    // ─── E03: 企业专利列表 ───
    @Override
    public List<EnterprisePatentResponse> getPatents(Long enterpriseId, int page, int size,
                                                      String keyword, String type) {
        int offset = (page - 1) * size;
        List<Patent> patents = patentMapper.selectByEnterprise(enterpriseId, keyword, type, offset, size);
        if (patents == null) return Collections.emptyList();

        return patents.stream().map(p -> {
            EnterprisePatentResponse vo = new EnterprisePatentResponse();
            vo.setPatentId(p.getId());
            vo.setPatentNo(p.getPatentNumber());
            vo.setPatentName(p.getPatentName());
            vo.setPatentType(p.getPatentType());
            vo.setPatentTypeLabel(p.getPatentType());
            if (p.getApplicationDate() != null) vo.setApplicationDate(p.getApplicationDate().toString());
            vo.setLegalStatus(p.getLegalStatus());
            vo.setStatusColor("授权".equals(p.getLegalStatus()) ? "#52c41a" : "#1890ff");
            vo.setIsHighValue(p.getMaintenanceYears() != null && p.getMaintenanceYears() >= 5);
            vo.setIpcMain(p.getIpcCode());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public long countPatents(Long enterpriseId, String keyword, String type) {
        return patentMapper.countByEnterprise(enterpriseId, keyword, type);
    }

    // ─── E04: 企业综合评估 ───
    @Override
    @Cacheable(value = "enterprise:evaluate", key = "#id", unless = "#result == null")
    public EnterpriseEvaluateResponse getEvaluate(Long id) {
        int patentCount = patentMapper.selectCountByEnterpriseId(id);
        int highValueCount = patentMapper.selectHighValueCountByEnterpriseId(id);

        double patentScore = Math.min(highValueCount * 100.0 / Math.max(patentCount, 1) * 0.6 + 40, 100);
        double techScore = computeTechMatchRate(id, patentCount, highValueCount);
        // 从企业自身数据计算成长潜力，而非固定60分
        double growthScore = computeGrowthScore(id, patentCount, highValueCount);
        double composite = patentScore * 0.3 + techScore * 0.35 + growthScore * 0.35;

        EnterpriseEvaluateResponse vo = new EnterpriseEvaluateResponse();
        vo.setCompositeScore((int) composite);
        if (composite >= 85) { vo.setRating("A"); vo.setRatingLabel("强烈推荐"); }
        else if (composite >= 70) { vo.setRating("B"); vo.setRatingLabel("推荐/关注"); }
        else { vo.setRating("C"); vo.setRatingLabel("观察"); }

        vo.setScores(Arrays.asList(
            new EnterpriseEvaluateResponse.DimensionScore("技术匹配度", (int) techScore, "专利与本地产业链需求的重合度"),
            new EnterpriseEvaluateResponse.DimensionScore("专利实力", (int) patentScore, "高价值专利数量与维持年限评估"),
            new EnterpriseEvaluateResponse.DimensionScore("产业带动潜力", (int) growthScore, "产业链关联与影响力")
        ));
        vo.setSummaryText(composite >= 85 ? "该企业技术实力突出，建议优先对接。" :
            composite >= 70 ? "该企业具备较好的技术基础，建议持续关注。" : "技术储备有待加强，可后续评估。");
        return vo;
    }

    // ─── E05: 企业搜索 ───
    @Override
    public List<EnterpriseResponse> searchEnterprise(String keyword) {
        List<com.industrybrain.domain.model.Enterprise> ents = enterpriseMapper.searchByName(keyword, 20);
        if (ents == null) return Collections.emptyList();
        return ents.stream().map(e -> {
            EnterpriseResponse vo = new EnterpriseResponse();
            vo.setId(e.getId());
            vo.setEntName(e.getName());
            vo.setShortName(e.getName());
            vo.setCity(e.getRegisteredAddress());
            vo.setScale(e.getIsLocal() != null && e.getIsLocal() == 1 ? "本地企业" : "外地企业");
            vo.setTags(Collections.emptyList());
            return vo;
        }).collect(Collectors.toList());
    }

    // ─── 本地企业 ───
    @Override
    public List<LocalEnterpriseResponse> getLocalEnterprises(String keyword) {
        List<Map<String, Object>> rows = enterpriseMapper.selectLocalEnterprises(keyword);
        if (rows == null) return Collections.emptyList();
        return rows.stream().map(m -> {
            LocalEnterpriseResponse vo = new LocalEnterpriseResponse();
            vo.setId(toLong(m.get("id")));
            vo.setName((String) m.get("name"));
            vo.setType(deriveTypeFromDb(m));
            vo.setTag(vo.getType());
            vo.setDistrict(extractDistrictFromDb(m));
            vo.setPark(deriveParkFromDb(m));
            vo.setPatentCount(String.valueOf(toInt(m.get("patent_count"))));
            vo.setHighValueCount(toInt(m.get("high_value_count")));
            vo.setEnterpriseCount(toInt(m.get("enterprise_count")));
            vo.setRegisteredCapital((String) m.get("registered_capital"));
            vo.setLegalPerson((String) m.get("legal_representative"));
            vo.setDescription((String) m.get("business_status"));
            return vo;
        }).collect(Collectors.toList());
    }

    // ─── IEnterpriseService (保持兼容) ───
    @Override
    public List<EnterpriseDetailResponse> searchEnterprises(EnterpriseSearchRequest request) {
        log.info("搜索企业, name={}, industry={}, regionCode={}, type={}",
                request.getEnterpriseName(), request.getIndustry(), request.getRegionCode(), request.getEnterpriseType());

        List<com.industrybrain.domain.model.Enterprise> ents = null;

        // 先按条件获取基础列表
        if (request.getEnterpriseName() != null && !request.getEnterpriseName().isEmpty()) {
            ents = enterpriseMapper.searchByName(request.getEnterpriseName(), 50);
        } else if (request.getRegionCode() != null && !request.getRegionCode().isEmpty()) {
            ents = enterpriseMapper.selectByRegionCode(request.getRegionCode());
        } else {
            ents = enterpriseMapper.selectList(null);
        }

        if (ents == null || ents.isEmpty()) return Collections.emptyList();

        // 根据企业类型过滤（如果指定了类型）
        String enterpriseType = request.getEnterpriseType();
        if (enterpriseType != null && !enterpriseType.trim().isEmpty()) {
            ents = ents.stream()
                    .filter(e -> enterpriseType.equals(e.getEnterpriseType()))
                    .collect(Collectors.toList());
        }

        return ents.stream().map(this::toDetailResponse).collect(Collectors.toList());
    }

    /**
     * 按企业类型查询企业列表（新接口）
     */
    @Override
    public List<Enterprise> getEnterprisesByType(String type) {
        return enterpriseMapper.selectByType(type);
    }

    /**
     * 按企业类型统计企业数（新接口）
     */
    @Override
    public Long getEnterprisesCountByType(String type) {
        return enterpriseMapper.selectCountByType(type);
    }

    /**
     * 获取企业类型的分布统计（新接口）
     */
    @Override
    public List<Map<String, Object>> getEnterpriseTypeStats() {
        return enterpriseMapper.selectTypeStats();
    }

    @Override
    public EnterpriseDetailResponse getEnterpriseDetail(Long enterpriseId) {
        log.info("获取企业详情, enterpriseId={}", enterpriseId);
        com.industrybrain.domain.model.Enterprise ent = enterpriseMapper.selectById(enterpriseId);
        if (ent == null) return null;
        return toDetailResponse(ent);
    }

    @Override
    public List<Enterprise> listEnterprises(String industry, String regionCode) {
        log.info("获取企业列表, industry={}, region={}", industry, regionCode);
        List<com.industrybrain.domain.model.Enterprise> ents;
        if (regionCode != null && !regionCode.isEmpty()) {
            ents = enterpriseMapper.selectByRegionCode(regionCode);
        } else {
            ents = enterpriseMapper.selectList(null);
        }
        if (ents == null) return Collections.emptyList();
        return ents.stream().map(e -> {
            Enterprise enterprise = new Enterprise();
            enterprise.setId(e.getId());
            enterprise.setName(e.getName());
            enterprise.setUnifiedCode(e.getUnifiedCode());
            enterprise.setLegalRepresentative(e.getLegalRepresentative());
            enterprise.setRegisteredAddress(e.getRegisteredAddress());
            return enterprise;
        }).collect(Collectors.toList());
    }

    // ─── helpers ───
    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Long) return (Long) val;
        if (val instanceof Integer) return ((Integer) val).longValue();
        return Long.valueOf(val.toString());
    }

    private Integer toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Long) return ((Long) val).intValue();
        return Integer.valueOf(val.toString());
    }

    private double computeTechMatchRate(Long enterpriseId, int totalPatents, int highValuePatents) {
        if (totalPatents == 0) return 0.0;
        double ratio = (double) highValuePatents / totalPatents;
        return Math.min(50.0 + ratio * 55.0, 95.0);
    }

    /**
     * 基于企业真实数据计算成长潜力分(0-100)，不再使用固定60分。
     */
    private double computeGrowthScore(Long enterpriseId, int patentCount, int highValueCount) {
        double score = 50.0; // 基础分
        try {
            com.industrybrain.domain.model.Enterprise ent = enterpriseMapper.selectById(enterpriseId);
            if (ent != null) {
                // 企业年限加分
                if (ent.getEstablishmentDate() != null) {
                    int age = java.time.LocalDate.now().getYear() - ent.getEstablishmentDate().getYear();
                    if (age > 15) score += 20;
                    else if (age > 5) score += 10;
                }
                // 注册资本加分
                if (ent.getRegisteredCapital() != null) {
                    try {
                        String cap = ent.getRegisteredCapital().replace("万元", "").replace("万", "");
                        if (cap.contains("亿")) {
                            score += 20;
                        } else {
                            double val = Double.parseDouble(cap);
                            if (val >= 10000) score += 20;
                            else if (val >= 1000) score += 10;
                        }
                    } catch (Exception ignored) {}
                }
            }
            // 专利数量加分
            if (patentCount > 100) score += 15;
            else if (patentCount > 10) score += 5;
            // 高价值专利加分
            if (highValueCount > 20) score += 15;
            else if (highValueCount > 5) score += 10;
        } catch (Exception e) {
            log.debug("计算成长潜力分异常: {}", e.getMessage());
        }
        return Math.min(score, 100.0);
    }

    /**
     * 从数据库字段直接获取企业类型。
     */
    private String deriveTypeFromDb(Map<String, Object> m) {
        String dbType = (String) m.get("enterprise_type");
        if (dbType != null && !dbType.isEmpty()) return dbType;
        // 用专利数据推导企业类型
        int patentCount = toInt(m.get("patent_count"));
        int highValueCount = toInt(m.get("high_value_count"));
        if (patentCount >= 20 && highValueCount >= 5) return "龙头企业";
        if (patentCount >= 10) return "高新技术企业";
        if (patentCount >= 3) return "科技型中小企业";
        return "其他";
    }

    /**
     * 从数据库字段提取区县。
     */
    private String extractDistrictFromDb(Map<String, Object> m) {
        // 优先使用 region 表 JOIN 出来的区县名称
        String districtName = (String) m.get("district_name");
        if (districtName != null && !districtName.isEmpty()) return districtName;
        return "其他";
    }

    /**
     * 从数据库字段获取园区名称。
     */
    private String deriveParkFromDb(Map<String, Object> m) {
        // 直接使用park_id关联查询
        Object parkId = m.get("park_id");
        if (parkId != null) {
            try {
                Long pid = toLong(parkId);
                IndustrialPark park = parkMapper.selectById(pid);
                if (park != null) return park.getName();
            } catch (Exception e) {
                log.debug("通过park_id查询园区失败: {}", e.getMessage());
            }
        }
        // 后备：判断本地/外地
        Integer isLocal = m.get("is_local") instanceof Integer ? (Integer) m.get("is_local") : null;
        return isLocal != null && isLocal == 1 ? "本地园区" : "外地";
    }

    @Override
    public List<Enterprise> getEnterprisesByType(String type) {
        return enterpriseMapper.selectByType(type);
    }

    @Override
    public Long getEnterprisesCountByType(String type) {
        return enterpriseMapper.selectCountByType(type);
    }

    @Override
    public List<Map<String, Object>> getEnterpriseTypeStats() {
        return enterpriseMapper.selectTypeStats();
    }

    private EnterpriseDetailResponse toDetailResponse(com.industrybrain.domain.model.Enterprise ent) {
        EnterpriseDetailResponse vo = new EnterpriseDetailResponse();
        vo.setId(ent.getId());
        vo.setEnterpriseName(ent.getName());
        vo.setCreditCode(ent.getUnifiedCode());
        vo.setLegalPerson(ent.getLegalRepresentative());
        if (ent.getRegisteredCapital() != null) {
            try {
                vo.setRegisteredCapital(new java.math.BigDecimal(
                        ent.getRegisteredCapital().replaceAll("[^0-9.]", "")));
            } catch (Exception ignored) {}
        }
        vo.setAddress(ent.getRegisteredAddress());
        return vo;
    }
}
