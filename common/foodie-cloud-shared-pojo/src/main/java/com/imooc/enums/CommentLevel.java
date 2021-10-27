package com.imooc.enums;

/**
 * 商品评价等级 枚举
 */
public enum  CommentLevel {
    GOOD(1),
    NORMAL(2),
    BAD(3);

    public Integer level;
    CommentLevel(Integer level) {
        this.level = level;
    }
}
