# enterprise表添加enterprise_type字段方案

## 一、现状分析

当前项目状态：
- **Java实体类** (`Enterprise.java`)：已包含 `enterpriseType` 字段（String类型），注释中已定义枚举值："龙头企业/专精特新/高新技术企业/科技型中小企业/其他"
- **初始化脚本** (`init.sql`)：已创建 `enterprise_type VARCHAR(64)` 字段，并附带CHECK约束
- **数据导入脚本** (`import_data.csv`)：需确认CSV文件中是否包含该列

**问题**：在生产环境中，若数据库基于旧版本初始化创建，则`enterprise`表中可能不存在`enterprise_type`字段，导致应用启动或运行时出现异常。本方案提供完整升级迁移流程。

---

## 二、数据库迁移方案

### 2.1 前置检查

在执行迁移前，需确认目标数据库中是否已存在该字段：

```sql
-- 检查enterprise表是否存在enterprise_type列
SHOW COLUMNS FROM enterprise LIKE 'enterprise_type';

-- 或查询information_schema
SELECT COLUMN_NAME 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'industry_brain' 
  AND TABLE_NAME = 'enterprise' 
  AND COLUMN_NAME = 'enterprise_type';
```

### 2.2 增量迁移脚本

编写独立的迁移SQL文件（建议命名为 `V2_1__add_enterprise_type_to_enterprise.sql`），内容如下：

```sql
-- ============================================================
-- 迁移脚本：为enterprise表添加enterprise_type字段
-- 版本号：V2.1
-- 依赖：init.sql已创建新库时的表结构
-- ============================================================

-- 仅当列不存在时执行添加，避免重复执行报错
ALTER TABLE enterprise 
ADD COLUMN IF NOT EXISTS enterprise_type VARCHAR(64) NULL 
    COMMENT '企业类型：龙头企业/专精特新/高新技术企业/科技型中小企业/其他',
ADD CONSTRAINT IF NOT EXISTS chk_enterprise_type 
    CHECK (enterprise_type IS NULL OR enterprise_type IN ('龙头企业', '专精特新', '高新技术企业', '科技型中小企业', '其他'));

-- 添加索引提升查询性能
ADD INDEX IF NOT EXISTS idx_enterprise_type (enterprise_type);

-- 为存量数据设置默认值（可根据业务需求调整）
UPDATE enterprise SET enterprise_type = '其他' WHERE enterprise_type IS NULL;
```

> **注意**：MySQL 8.0.16+ 支持严格模式下的CHECK约束；早期版本虽语法接受但实际不生效。如使用旧版本，建议在应用层校验。

### 2.3 数据初始化策略

根据数据来源不同，制定以下填充规则：

| 数据来源 | 填充策略 | 说明 |
|---------|---------|------|
| 新入库记录 | 前端/API传入值 | 必填校验 |
| 存量历史数据 | 统一设为'其他' | 无明确分类时兜底 |
| 外部数据源映射 | 根据external source字段转换 | 需建立映射表 |

可编写批量更新脚本按区域/行业进行更精细的分类：

```sql
-- 示例：按企业规模或行业特征推测类型（业务逻辑需与实际匹配）
UPDATE enterprise e
SET enterprise_type = '高新技术企业'
WHERE e.name LIKE '%科技%' 
  AND e.enterprise_type IS NULL
  AND e.registered_capital > '1000万';

UPDATE enterprise e
SET enterprise_type = '专精特新'
WHERE e.name LIKE '%专精特新%' 
  AND e.enterprise_type IS NULL;

-- 未匹配的仍归为'其他'
UPDATE enterprise e
SET enterprise_type = '其他'
WHERE e.enterprise_type IS NULL;
```

### 2.4 数据库变更验证

迁移完成后，验证字段和约束：

```sql
-- 查看表结构
DESCRIBE enterprise;

-- 验证约束是否生效（尝试插入非法值应失败）
INSERT INTO enterprise (name, enterprise_type) VALUES ('测试企业', 'invalid_type'); -- 预期失败

-- 查询统计分布
SELECT enterprise_type, COUNT(*) as count 
FROM enterprise 
GROUP BY enterprise_type;
```

---

## 三、代码层适配

### 3.1 实体类（Enterprise.java）

当前已包含字段，需补充以下增强：

```java
/**
 * 企业类型枚举定义
 */
public enum EnterpriseType {
    LEADER("龙头企业", "行业领军企业"),
    SPECIALIZED专精特新("专精特新", "专业化精细化特色化新颖化"),
    HIGH_TECH("高新技术企业", "国家高新技术企业认证"),
    TECH_SMALL("科技型中小企业", "科技型中小企业登记"),
    OTHER("其他", "未归类企业");

    private final String code;
    private final String description;

    EnterpriseType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean isValid(String type) {
        return Arrays.values().stream().anyMatch(e -> e.code.equals(type));
    }

    public static EnterpriseType fromString(String type) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(type))
                .findFirst()
                .orElse(OTHER);
    }
}
```

**可选改进**：将`String enterpriseType`改为使用枚举类型，实现类型安全：

```java
@TableField(exist = false)
private EnterpriseType enterpriseType; // 通过typeHandler存储为字符串
```

### 3.2 Mapper层

当前EnterpriseMapper中已有对`enterprise_type`字段的引用（`selectLocalEnterprises`方法），无需额外修改。如需按类型查询，可扩展新的查询方法：

```java
/**
 * 按企业类型查询企业列表
 */
@Select("SELECT * FROM enterprise WHERE enterprise_type = #{type} AND is_deleted = 0")
List<Enterprise> selectByType(@Param("type") String type);

/**
 * 按企业类型统计企业数
 */
@Select("SELECT COUNT(*) FROM enterprise WHERE enterprise_type = #{type} AND is_deleted = 0")
Long selectCountByType(@Param("type") String type);
```

### 3.3 Service层

在`EnterpriseServiceImpl`中添加按类型筛选的服务方法：

```java
@Override
public List<Enterprise> getEnterprisesByType(String type) {
    return enterpriseMapper.selectByType(type);
}

@Override
public Map<String, Long> getEnterprisesTypeStats() {
    Map<String, Long> stats = new HashMap<>();
    for (EnterpriseType type : EnterpriseType.values()) {
        stats.put(type.code.toString(), enterpriseMapper.selectCountByType(type.code.toString()));
    }
    return stats;
}
```

### 3.4 DTO与Controller

在查询请求DTO中添加企业类型筛选条件：

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnterpriseSearchRequest {
    private String keyword;
    private String enterpriseType;  // 新增筛选条件
    private Long regionId;
    private Integer isLocal;
}
```

Controller层实现筛选逻辑：

```java
@ApiOperation(value = "按企业查询，支持多维度筛选")
@GetMapping("/list")
public ApiResponse<List<Enterprise>> searchEnterprises(@ApiParam(name = "搜索关键字", required = true) @RequestParam String keyword,
                                                      @ApiParam(name = "企业类型", allowableValues = "LEADER,SPECIALIZED,OTHER,TECH_SMALL,HIGH_TECH") 
                                                              @RequestParam(required = false) String enterpriseType,
                                                      @ApiParam(name = "区域ID") @RequestParam(required = false) Long regionId) {
    
    EnterpriseSearchRequest request = new EnterpriseSearchRequest(keyword, enterpriseType, regionId, 1);
    List<Enterprise> enterprises = enterpriseService.searchEnterprises(request);
    return success(enterprises);
}
```

### 3.5 前端Vue组件

在`HomeDashboard.vue`等企业列表页面添加企业类型筛选器：

```vue
<el-select v-model="filterEnterpriseType" placeholder="请选择企业类型">
  <el-option 
    v-for="type in enterpriseTypes" 
    :key="type.code"
    :label="type.description" 
    :value="type.code"
  />
</el-select>

// 数据类型定义
const enterpriseTypes = ref([
  { code: 'LEADER', description: '龙头企业' },
  { code: 'SPECIALIZED', description: '专精特新' },
  { code: 'HIGH_TECH', description: '高新技术企业' },
  { code: 'TECH_SMALL', description: '科技型中小企业' },
  { code: 'OTHER', description: '其他' }
]);
```

---

## 四、数据迁移风险与回滚

### 4.1 风险提示

| 风险点 | 影响等级 | 应对措施 |
|-------|---------|---------|
| 大表ALTER TABLE锁表 | 高 | 使用pt-online-schema-change或gh-onet在线工具 |
| CHECK约束不生效（旧MySQL） | 中 | 增加应用层校验 + Trigger触发器 |
| 存量数据分类不准确 | 低 | 后续人工修正入口 |
| 业务兼容性问题 | 低 | 字段允许NULL，向下兼容 |

### 4.2 回滚方案

如迁移后发现问题，需回滚：

```sql
-- 删除约束和索引
ALTER TABLE enterprise DROP CONSTRAINT chk_enterprise_type;
DROP INDEX IF EXISTS idx_enterprise_type ON enterprise;

-- 删除字段（谨慎操作，如有依赖需先清理）
ALTER TABLE enterprise DROP COLUMN enterprise_type;
```

---

## 五、完整迁移流程清单

```bash
# 1. 备份生产数据库
mysqldump -u root -p industry_brain > backup/enterprise_backup_$(date +%F).sql

# 2. 执行迁移脚本
mysql -u root -p industry_brain < migrations/V2_1__add_enterprise_type.sql

# 3. 验证字段存在
mysql -u root -p -e "SHOW COLUMNS FROM enterprise LIKE 'enterprise_type'"

# 4. 重启应用服务
cd /app
./stop.sh
./start.sh

# 5. 健康检查检查
curl http://localhost:8080/health

# 6. 业务功能验证
curl http://localhost:8080/api/enterprises/list?keyword=test&enterpriseType=龙头企业

# 7. 日志监控观察是否有异常
tail -f application.log | grep -i enterprise_type
```

---

## 六、扩展建议

1. **独立企业类型表**：如企业分类体系复杂，建议拆分为独立的`enterprise_type`参考表，支持动态维护和多级分类。
2. **类型审核流程**：企业类型变更需经过审核流程，避免随意修改。
3. **分类统计分析**：基于企业类型生成统计报表，辅助产业决策。
4. **智能推荐关联**：将企业类型作为招商推荐的权重因子，不同类型匹配策略不同。