package com.bunny.flix.feign.idgenerator;

import lombok.Getter;

/**
 * 业务类型枚举，用于 ID 号段生成。
 * 调用方通过枚举声明自己的业务，ID 生成服务内部根据枚举映射到对应的号段（biz_type），
 * 避免调用方直接传入裸字符串，保证类型安全。
 */
@Getter
public enum BizTypeEnum {

    USER("user"),
    DRAMA("drama"),
    EPISODE("episode"),
    ACTOR("actor"),
    TAG("tag"),
    DRAMA_ACTOR("drama_actor"),
    DRAMA_TAG("drama_tag"),
    PUBLISHER("publisher"),
    COMMENT("comment"),
    ORDER("order"),
    VIDEO("video"),
    ADMIN("admin");

    /** 对应数据库 t_id_segment 表的 biz_type 值 */
    private final String code;

    BizTypeEnum(String code) {
        this.code = code;
    }

    /** 根据数据库 biz_type 值反查枚举 */
    public static BizTypeEnum valueOfCode(String code) {
        for (BizTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown bizType code: " + code);
    }
}
