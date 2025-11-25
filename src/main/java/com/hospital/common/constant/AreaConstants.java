package com.hospital.common.constant;

/**
 * 区域API常量类
 * 定义阿里云区域API相关的常量
 *
 * @author Hospital Team
 * @since 2025-12-19
 */
public class AreaConstants {

    // ==================== API配置常量 ====================
    // 注意：API地址应从配置文件读取，使用 AreaConfig.getHost() 和 AreaConfig.getPath()
    // 不再在此处硬编码API地址

    /**
     * 请求方式
     */
    public static final String HTTP_METHOD = "GET";

    /**
     * 认证Header名称
     */
    public static final String AUTH_HEADER_NAME = "Authorization";

    /**
     * AppCode认证前缀
     */
    public static final String APPCODE_PREFIX = "APPCODE ";

    // ==================== 缓存配置常量 ====================

    /**
     * 区域数据缓存键前缀
     */
    public static final String AREA_CACHE_PREFIX = "hospital:area:cache:";

    /**
     * 省份列表缓存键
     */
    public static final String PROVINCE_LIST_CACHE_KEY = AREA_CACHE_PREFIX + "province:list";

    /**
     * 市级列表缓存键前缀（需要加上provinceId）
     */
    public static final String CITY_LIST_CACHE_KEY_PREFIX = AREA_CACHE_PREFIX + "city:list:province:";

    /**
     * 区县列表缓存键前缀（需要加上cityId）
     */
    public static final String COUNTY_LIST_CACHE_KEY_PREFIX = AREA_CACHE_PREFIX + "county:list:city:";

    /**
     * 乡镇列表缓存键前缀（需要加上countyId）
     */
    public static final String TOWN_LIST_CACHE_KEY_PREFIX = AREA_CACHE_PREFIX + "town:list:county:";

    /**
     * 村社区列表缓存键前缀（需要加上townId）
     */
    public static final String VILLAGE_LIST_CACHE_KEY_PREFIX = AREA_CACHE_PREFIX + "village:list:town:";

    // 注意：区域数据缓存TTL应从配置文件读取，使用 AreaConfig.getCache().getTtlSeconds()
    // 不再在此处硬编码缓存TTL

    // ==================== 请求参数常量 ====================

    /**
     * 省份ID参数名
     */
    public static final String PARAM_PROVINCE_ID = "provinceId";

    /**
     * 城市ID参数名
     */
    public static final String PARAM_CITY_ID = "cityId";

    /**
     * 区县ID参数名
     */
    public static final String PARAM_COUNTY_ID = "countyId";

    /**
     * 乡镇ID参数名
     */
    public static final String PARAM_TOWN_ID = "townId";

    /**
     * 村社区ID参数名
     */
    public static final String PARAM_VILLAGE_ID = "villageId";

    private AreaConstants() {
        // 防止实例化
    }
}

