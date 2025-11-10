package com.liverpool.imageValidator.utils;

public final class QuerysDB {

    public static final String SELECT_IUO_SKUS =
            "SELECT oi.SKU_ID " +
                    "FROM _IUO_SCHE_.ONLINE_INVENTORY oi " +
                    "INNER JOIN _IUO_SCHE_.BTVTA_INVENTORY bt ON oi.SKU_ID = bt.SKU_ID";

    public static final String SELECT_IUO_SKUS_MANUAL_PREFIX =
            "SELECT DISTINCT oi.SKU_ID " +
                    "FROM _IUO_SCHE_.ONLINE_INVENTORY oi " +
                    "INNER JOIN _IUO_SCHE_.BTVTA_INVENTORY bt ON oi.SKU_ID = bt.SKU_ID " +
                    "WHERE oi.SKU_ID IN (";

    public static final String SELECT_IUO_SKUS_MANUAL_SUFFIX = ")";

    public static final String SELECT_VALID_SKUS_PREFIX =
            "SELECT DISTINCT ds.sku_id, ldp.PRODUCT_TYPE " +
                    "FROM _ATG_CATA_.LP_DCS_Product ldp " +
                    "INNER JOIN _ATG_CATA_.DCS_PRD_CHLDSKU dpc ON ldp.product_id = dpc.product_id " +
                    "INNER JOIN _ATG_CATA_.LP_DCS_SKU lds ON dpc.SKU_ID = lds.SKU_ID " +
                    "INNER JOIN _ATG_CATA_.DCS_SKU ds ON ds.sku_id = lds.SKU_ID " +
                    "INNER JOIN _ATG_CORE_.DCS_PRICE dp ON dp.sku_id = ds.sku_id AND dp.PRICE_LIST = 'Sale_plist00' " +
                    "INNER JOIN _ATG_CATA_.DCS_SKU_SITES dss ON dss.SKU_ID = ds.SKU_ID " +
                    "WHERE dpc.sku_id IN (";

    public static final String SELECT_VALID_SKUS_SUFFIX =
            ") AND ldp.IS_ACTIVE = 1 AND lds.is_active = 1 AND ldp.IS_MARKET_PLACE = 0";

    public static final String ONLINE_INVENTORY_TABLE = "_IUO_SCHE_.ONLINE_INVENTORY";

    public static final String BTVTA_INVENTORY_TABLE = "_IUO_SCHE_.BTVTA_INVENTORY";
}


