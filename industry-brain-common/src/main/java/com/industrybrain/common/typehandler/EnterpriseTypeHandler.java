package com.industrybrain.common.typehandler;

import org.apache.ibatis.type.*;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 企业类型处理器
 * 直接存储字符串，应用层负责校验合法值（企业类型：龙头企业/专精特新/高新技术企业/科技型中小企业/其他）
 */
@MappedTypes(String.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class EnterpriseTypeHandler implements TypeHandler<String> {

    @Override
    public void setParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null || parameter.trim().isEmpty()) {
            ps.setString(i, null);
        } else {
            ps.setString(i, parameter.trim());
        }
    }

    @Override
    public String getResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? "" : value.trim();
    }

    @Override
    public String getResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? "" : value.trim();
    }

    @Override
    public String getResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? "" : value.trim();
    }
}
