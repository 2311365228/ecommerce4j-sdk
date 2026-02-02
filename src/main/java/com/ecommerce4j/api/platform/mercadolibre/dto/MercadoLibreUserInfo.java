package com.ecommerce4j.api.platform.mercadolibre.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * @description 用户信息数据传输对象
 */
@Data
public class MercadoLibreUserInfo {

    /**
     * 用户ID
     */
    @JsonProperty("id")
    private Long id;

    /**
     * 用户昵称
     */
    @JsonProperty("nickname")
    private String nickname;

    /**
     * 注册日期
     */
    @JsonProperty("registration_date")
    private String registrationDate;

    /**
     * 名字
     */
    @JsonProperty("first_name")
    private String firstName;

    /**
     * 姓氏
     */
    @JsonProperty("last_name")
    private String lastName;

    /**
     * 性别
     */
    @JsonProperty("gender")
    private String gender;

    /**
     * 国家ID
     */
    @JsonProperty("country_id")
    private String countryId;

    /**
     * 邮箱地址
     */
    @JsonProperty("email")
    private String email;

    /**
     * 身份标识信息
     */
    @JsonProperty("identification")
    private Identification identification;

    /**
     * 内部标签列表
     */
    @JsonProperty("internal_tags")
    private List<String> internalTags;

    /**
     * 地址信息
     */
    @JsonProperty("address")
    private Address address;

    /**
     * 电话信息
     */
    @JsonProperty("phone")
    private Phone phone;

    /**
     * 备用电话信息
     */
    @JsonProperty("alternative_phone")
    private AlternativePhone alternativePhone;

    /**
     * 用户类型
     */
    @JsonProperty("user_type")
    private String userType;

    /**
     * 标签列表
     */
    @JsonProperty("tags")
    private List<String> tags;

    /**
     * Logo链接
     */
    @JsonProperty("logo")
    private String logo;

    /**
     * 积分
     */
    @JsonProperty("points")
    private Integer points;

    /**
     * 站点ID
     */
    @JsonProperty("site_id")
    private String siteId;

    /**
     * 永久链接
     */
    @JsonProperty("permalink")
    private String permalink;

    /**
     * 配送模式
     */
    @JsonProperty("shipping_modes")
    private List<String> shippingModes;

    /**
     * 卖家经验等级
     */
    @JsonProperty("seller_experience")
    private String sellerExperience;

    /**
     * 账单数据
     */
    @JsonProperty("bill_data")
    private BillData billData;

    /**
     * 卖家声誉
     */
    @JsonProperty("seller_reputation")
    private SellerReputation sellerReputation;

    /**
     * 买家声誉
     */
    @JsonProperty("buyer_reputation")
    private BuyerReputation buyerReputation;

    /**
     * 用户状态
     */
    @JsonProperty("status")
    private Status status;

    /**
     * 安全邮箱
     */
    @JsonProperty("secure_email")
    private String secureEmail;

    /**
     * 公司信息
     */
    @JsonProperty("company")
    private Company company;

    /**
     * 信用信息
     */
    @JsonProperty("credit")
    private Credit credit;

    /**
     * 密码生成状态
     */
    @JsonProperty("pwd_generation_status")
    private String pwdGenerationStatus;

    /**
     * 上下文信息
     */
    @JsonProperty("context")
    private Context context;


    /**
     * @description 身份标识信息
     */
    @Data
    public static class Identification {
        /**
         * 证件号码
         */
        @JsonProperty("number")
        private String number;
        /**
         * 证件类型
         */
        @JsonProperty("type")
        private String type;
    }

    /**
     * @description 地址信息
     */
    @Data
    public static class Address {
        /**
         * 详细地址
         */
        @JsonProperty("address")
        private String address;
        /**
         * 城市
         */
        @JsonProperty("city")
        private String city;
        /**
         * 州/省
         */
        @JsonProperty("state")
        private String state;
        /**
         * 邮政编码
         */
        @JsonProperty("zip_code")
        private String zipCode;
    }

    /**
     * @description 电话信息
     */
    @Data
    public static class Phone {
        /**
         * 区号
         */
        @JsonProperty("area_code")
        private String areaCode;
        /**
         * 分机号
         */
        @JsonProperty("extension")
        private String extension;
        /**
         * 电话号码
         */
        @JsonProperty("number")
        private String number;
        /**
         * 是否已验证
         */
        @JsonProperty("verified")
        private Boolean verified;
    }

    /**
     * @description 备用电话信息
     */
    @Data
    public static class AlternativePhone {
        /**
         * 区号
         */
        @JsonProperty("area_code")
        private String areaCode;
        /**
         * 分机号
         */
        @JsonProperty("extension")
        private String extension;
        /**
         * 电话号码
         */
        @JsonProperty("number")
        private String number;
    }

    /**
     * @description 账单数据
     */
    @Data
    public static class BillData {
        /**
         * 是否接受贷记单
         */
        @JsonProperty("accept_credit_note")
        private Object acceptCreditNote; // 类型不确定，使用Object
    }

    /**
     * @description 卖家声誉
     */
    @Data
    public static class SellerReputation {
        /**
         * 声誉等级ID
         */
        @JsonProperty("level_id")
        private String levelId;
        /**
         * Power Seller状态
         */
        @JsonProperty("power_seller_status")
        private String powerSellerStatus;
        /**
         * 交易信息
         */
        @JsonProperty("transactions")
        private Transactions transactions;
        /**
         * 卖家指标
         */
        @JsonProperty("metrics")
        private Metrics metrics;

        @Data
        public static class Transactions {
            /**
             * 已取消的交易数
             */
            @JsonProperty("canceled")
            private Integer canceled;
            /**
             * 已完成的交易数
             */
            @JsonProperty("completed")
            private Integer completed;
            /**
             * 统计周期
             */
            @JsonProperty("period")
            private String period;
            /**
             * 评价信息
             */
            @JsonProperty("ratings")
            private Ratings ratings;
            /**
             * 总交易数
             */
            @JsonProperty("total")
            private Integer total;

            @Data
            public static class Ratings {
                /**
                 * 差评数
                 */
                @JsonProperty("negative")
                private Integer negative;
                /**
                 * 中评数
                 */
                @JsonProperty("neutral")
                private Integer neutral;
                /**
                 * 好评数
                 */
                @JsonProperty("positive")
                private Integer positive;
            }
        }

        @Data
        public static class Metrics {
            /**
             * 销售指标
             */
            @JsonProperty("sales")
            private Sales sales;
            /**
             * 纠纷指标
             */
            @JsonProperty("claims")
            private RateValueMetric claims;
            /**
             * 延迟处理时间指标
             */
            @JsonProperty("delayed_handling_time")
            private RateValueMetric delayedHandlingTime;
            /**
             * 取消率指标
             */
            @JsonProperty("cancellations")
            private RateValueMetric cancellations;

            @Data
            public static class Sales {
                /**
                 * 统计周期
                 */
                @JsonProperty("period")
                private String period;
                /**
                 * 完成数
                 */
                @JsonProperty("completed")
                private Integer completed;
            }

            @Data
            public static class RateValueMetric {
                /**
                 * 统计周期
                 */
                @JsonProperty("period")
                private String period;
                /**
                 * 比率
                 */
                @JsonProperty("rate")
                private Double rate;
                /**
                 * 数值
                 */
                @JsonProperty("value")
                private Integer value;
            }
        }
    }

    /**
     * @description 买家声誉
     */
    @Data
    public static class BuyerReputation {
        /**
         * 已取消的交易数
         */
        @JsonProperty("canceled_transactions")
        private Integer canceledTransactions;
        /**
         * 标签列表
         */
        @JsonProperty("tags")
        private List<String> tags;
        /**
         * 交易信息
         */
        @JsonProperty("transactions")
        private BuyerTransactions transactions;

        @Data
        public static class BuyerTransactions {
            /**
             * 已取消交易详情
             */
            @JsonProperty("canceled")
            private Canceled canceled;
            /**
             * 已完成交易详情
             */
            @JsonProperty("completed")
            private Object completed;
            /**
             * 尚未评价的交易
             */
            @JsonProperty("not_yet_rated")
            private NotYetRated notYetRated;
            /**
             * 统计周期
             */
            @JsonProperty("period")
            private String period;
            /**
             * 总数
             */
            @JsonProperty("total")
            private Object total;
            /**
             * 未评价的交易
             */
            @JsonProperty("unrated")
            private Unrated unrated;

            @Data
            public static class Canceled {
                @JsonProperty("paid")
                private Object paid;
                @JsonProperty("total")
                private Object total;
            }

            @Data
            public static class NotYetRated {
                @JsonProperty("paid")
                private Object paid;
                @JsonProperty("total")
                private Object total;
                @JsonProperty("units")
                private Object units;
            }

            @Data
            public static class Unrated {
                @JsonProperty("paid")
                private Object paid;
                @JsonProperty("total")
                private Object total;
            }
        }
    }

    /**
     * @description 用户状态信息
     */
    @Data
    public static class Status {
        /**
         * 账单状态
         */
        @JsonProperty("billing")
        private BillingStatus billing;
        /**
         * 购买状态
         */
        @JsonProperty("buy")
        private BuyStatus buy;
        /**
         * 邮箱是否已确认
         */
        @JsonProperty("confirmed_email")
        private Boolean confirmedEmail;
        /**
         * 购物车状态
         */
        @JsonProperty("shopping_cart")
        private ShoppingCartStatus shoppingCart;
        /**
         * 是否需要立即支付
         */
        @JsonProperty("immediate_payment")
        private Boolean immediatePayment;
        /**
         * 刊登状态
         */
        @JsonProperty("list")
        private ListStatus list;
        /**
         * MercadoEnvios 状态
         */
        @JsonProperty("mercadoenvios")
        private String mercadoenvios;
        /**
         * MercadoPago 账户类型
         */
        @JsonProperty("mercadopago_account_type")
        private String mercadopagoAccountType;
        /**
         * 是否接受 MercadoPago TC
         */
        @JsonProperty("mercadopago_tc_accepted")
        private Boolean mercadopagoTcAccepted;
        /**
         * 需要执行的操作
         */
        @JsonProperty("required_action")
        private String requiredAction;
        /**
         * 销售状态
         */
        @JsonProperty("sell")
        private SellStatus sell;
        /**
         * 站点状态
         */
        @JsonProperty("site_status")
        private String siteStatus;
        /**
         * 用户类型
         */
        @JsonProperty("user_type")
        private String userType;

        @Data
        public static class BillingStatus {
            /**
             * 是否允许
             */
            @JsonProperty("allow")
            private Boolean allow;
            /**
             * 代码列表
             */
            @JsonProperty("codes")
            private List<String> codes;
        }

        @Data
        public static class BuyStatus {
            /**
             * 是否允许
             */
            @JsonProperty("allow")
            private Boolean allow;
            /**
             * 代码列表
             */
            @JsonProperty("codes")
            private List<String> codes;
            /**
             * 立即支付信息
             */
            @JsonProperty("immediate_payment")
            private ImmediatePayment immediatePayment;
        }

        @Data
        public static class ImmediatePayment {
            /**
             * 原因列表
             */
            @JsonProperty("reasons")
            private List<String> reasons;
            /**
             * 是否必须
             */
            @JsonProperty("required")
            private Boolean required;
        }

        @Data
        public static class ShoppingCartStatus {
            @JsonProperty("buy")
            private String buy;
            @JsonProperty("sell")
            private String sell;
        }

        @Data
        public static class ListStatus {
            /**
             * 是否允许
             */
            @JsonProperty("allow")
            private Boolean allow;
            /**
             * 代码列表
             */
            @JsonProperty("codes")
            private List<String> codes;
            /**
             * 立即支付信息
             */
            @JsonProperty("immediate_payment")
            private ImmediatePayment immediatePayment;
        }

        @Data
        public static class SellStatus {
            /**
             * 是否允许
             */
            @JsonProperty("allow")
            private Boolean allow;
            /**
             * 代码列表
             */
            @JsonProperty("codes")
            private List<String> codes;
            /**
             * 立即支付信息
             */
            @JsonProperty("immediate_payment")
            private ImmediatePayment immediatePayment;
        }
    }

    /**
     * @description 公司信息
     */
    @Data
    public static class Company {
        /**
         * 品牌名称
         */
        @JsonProperty("brand_name")
        private String brandName;
        /**
         * 城市税号
         */
        @JsonProperty("city_tax_id")
        private String cityTaxId;
        /**
         * 公司法定名称
         */
        @JsonProperty("corporate_name")
        private String corporateName;
        /**
         * 标识
         */
        @JsonProperty("identification")
        private String identification;
        /**
         * 州税号
         */
        @JsonProperty("state_tax_id")
        private String stateTaxId;
        /**
         * 软描述符
         */
        @JsonProperty("soft_descriptor")
        private String softDescriptor;
    }

    /**
     * @description 信用信息
     */
    @Data
    public static class Credit {
        /**
         * 已消费额度
         */
        @JsonProperty("consumed")
        private Integer consumed;
        /**
         * 信用等级ID
         */
        @JsonProperty("credit_level_id")
        private String creditLevelId;
        /**
         * 信用排名
         */
        @JsonProperty("rank")
        private String rank;
    }

    /**
     * @description 上下文信息
     */
    @Data
    public static class Context {
        /**
         * 设备类型
         */
        @JsonProperty("device")
        private String device;
        /**
         * 流程
         */
        @JsonProperty("flow")
        private String flow;
        /**
         * 来源
         */
        @JsonProperty("source")
        private String source;
    }
}
