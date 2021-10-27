package com.imooc.enums;

public enum CategoryType {
    LEVEL1(1),
    LEVEL2(2),
    LEVEL3(3);

    public final Integer type;

    CategoryType(Integer type) {
        this.type = type;
    }
}
