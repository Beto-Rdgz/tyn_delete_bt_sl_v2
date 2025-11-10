package com.liverpool.imageValidator.repository;

import com.liverpool.imageValidator.config.AppConfig;
import com.liverpool.imageValidator.models.SkusToDeleteDTO;
import com.liverpool.imageValidator.utils.QuerysDB;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Repository
@Slf4j
public class OracleDBRepository {

    private final JdbcTemplate iuoJdbc;
    private final JdbcTemplate atgJdbc;

    private final AppConfig appConfig;

    private String atgCataSchema;
    private String atgCoreSchema;
    private String iuoSchema;

    @PostConstruct
    public void init() {
       this.atgCataSchema = appConfig.getAtgCataSchema();
       this.atgCoreSchema = appConfig.getAtgCoreSchema();
       this.iuoSchema = appConfig.getIuoSchema();
    }

    public OracleDBRepository(@Qualifier("iuoJdbcTemplate") JdbcTemplate iuoJdbc,
                              @Qualifier("atgJdbcTemplate") JdbcTemplate atgJdbc, AppConfig appConfig) {
        this.iuoJdbc = iuoJdbc;
        this.atgJdbc = atgJdbc;
        this.appConfig = appConfig;
    }

    public List<String> findIuoSkus() {
        String sql = QuerysDB.SELECT_IUO_SKUS.replaceAll("_IUO_SCHE_", this.iuoSchema);
        //log.info("st: {}", sql);
        return iuoJdbc.query(con -> {
            PreparedStatement ps = con.prepareStatement(sql,
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ps.setFetchSize(5000);
            return ps;
        }, rs -> {
            List<String> list = new ArrayList<>();
            while (rs.next()) {
                list.add(rs.getString(1));
            }
            return list;
        });
    }

    public List<String> findIuoSkusManual(List<String> skusArgs) {
        if (skusArgs == null || skusArgs.isEmpty()) {
            return Collections.emptyList();
        }

        String placeholders = skusArgs.stream()
                .map(s -> "?")
                .collect(Collectors.joining(","));

        String iuoSkusManual = QuerysDB.SELECT_IUO_SKUS_MANUAL_PREFIX.replaceAll("_IUO_SCHE_", this.iuoSchema);

        String sql = iuoSkusManual + placeholders + QuerysDB.SELECT_IUO_SKUS_MANUAL_SUFFIX;

        //log.info("st: {}", sql);

        Object[] params = skusArgs.toArray();

        return iuoJdbc.query(sql, params, (rs, rowNum) -> rs.getString("SKU_ID"));
    }

    public List<SkusToDeleteDTO> findValidSkusByBatch(List<String> skusBatch) {
        if (skusBatch == null || skusBatch.isEmpty()) {
            return Collections.emptyList();
        }

        String placeholders = skusBatch.stream().map(s -> "?").collect(Collectors.joining(","));
        String validSkuAtg = QuerysDB.SELECT_VALID_SKUS_PREFIX
                .replaceAll("_ATG_CATA_", this.atgCataSchema)
                .replaceAll("_ATG_CORE_", this.atgCoreSchema);

        String sql = validSkuAtg + placeholders + QuerysDB.SELECT_VALID_SKUS_SUFFIX;

        //log.info("st: {}", sql);

        Object[] args = skusBatch.toArray(new Object[0]);

        return atgJdbc.query(sql, args, (rs, rowNum) ->
                new SkusToDeleteDTO(rs.getString("SKU_ID"), rs.getInt("PRODUCT_TYPE"))
        );
    }

    public int deleteIuoBySkusInBatch(List<String> skusBatch, String productType) {
        if (skusBatch == null || skusBatch.isEmpty()) return 0;

        String table;
        if ("BT".equalsIgnoreCase(productType)) {
            table = QuerysDB.ONLINE_INVENTORY_TABLE.replaceAll("_IUO_SCHE_", this.iuoSchema);;
        } else if ("SL".equalsIgnoreCase(productType)) {
            table = QuerysDB.BTVTA_INVENTORY_TABLE.replaceAll("_IUO_SCHE_", this.iuoSchema);;
        } else {
            throw new IllegalArgumentException("productType desconocido: " + productType);
        }

        String in = buildInClause(skusBatch.size());
        String sql = "DELETE FROM " + table + " WHERE SKU_ID IN (" + in + ")";

        Object[] args = skusBatch.toArray(new Object[0]);
        return iuoJdbc.update(sql, args);
    }

    private String buildInClause(int size) {
        return IntStream.range(0, size).mapToObj(i -> "?").collect(Collectors.joining(","));
    }
}
