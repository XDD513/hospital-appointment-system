package com.hospital.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 阿里云区域API响应DTO
 *
 * @author Hospital Team
 * @since 2025-12-19
 */
@Data
public class AreaResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应消息
     */
    @JsonProperty("msg")
    private String msg;

    /**
     * 请求流水号
     */
    @JsonProperty("requestid")
    private String requestId;

    /**
     * 响应码（200表示成功）
     */
    @JsonProperty("code")
    private Integer code;

    /**
     * 响应数据
     */
    @JsonProperty("data")
    private AreaData data;

    @Data
    public static class AreaData implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 区域列表
         */
        @JsonProperty("list")
        private List<AreaItem> list;
    }

    @Data
    public static class AreaItem implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * ID
         */
        @JsonProperty("id")
        private Integer id;

        /**
         * 省份ID
         */
        @JsonProperty("provinceid")
        private String provinceId;

        /**
         * 省份名称
         */
        @JsonProperty("provincename")
        private String provinceName;

        /**
         * 城市ID
         */
        @JsonProperty("cityid")
        private String cityId;

        /**
         * 城市名称
         */
        @JsonProperty("cityname")
        private String cityName;

        /**
         * 区县ID
         */
        @JsonProperty("countyid")
        private String countyId;

        /**
         * 区县名称
         */
        @JsonProperty("countyname")
        private String countyName;

        /**
         * 乡镇ID
         */
        @JsonProperty("townid")
        private String townId;

        /**
         * 乡镇名称
         */
        @JsonProperty("townname")
        private String townName;

        /**
         * 村社区ID
         */
        @JsonProperty("villageid")
        private String villageId;

        /**
         * 村社区名称
         */
        @JsonProperty("villagename")
        private String villageName;
    }
}

