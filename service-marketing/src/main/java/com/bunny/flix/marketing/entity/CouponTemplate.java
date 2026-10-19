package com.bunny.flix.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 优惠券模板实体类，对应数据库 t_coupon_template 表
 * 定义优惠券的规则：面值、门槛、库存等
 */
@Data
@TableName("t_coupon_template")
public class CouponTemplate {
    @TableId private Long id;
    /** 优惠券名称 */
    private String name;
    /** 类型：full_subtract-满减，discount-折扣 */
    private String type;
    /** 面值（分） */
    private Integer faceValue;
    /** 使用门槛（分） */
    private Integer threshold;
    /** 总库存 */
    private Integer totalStock;
    /** 剩余库存 */
    private Integer remainingStock;
    /** 领取后有效天数 */
    private Integer validDays;
    /** 有效期开始 */
    private LocalDate validStart;
    /** 有效期结束 */
    private LocalDate validEnd;
    /** 适用范围 */
    private String scope;
    /** 是否启用 */
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
