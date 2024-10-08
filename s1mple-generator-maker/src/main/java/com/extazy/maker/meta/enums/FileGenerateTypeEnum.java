package com.extazy.maker.meta.enums;

/**
 * 文件生成类型枚举
 */
public enum FileGenerateTypeEnum {
    STATIC("静态","static"),
    DYNAMIC("动态","dynamic");

    public final String text;
    public final String value;
    FileGenerateTypeEnum(String text,String value) {
        this.text = text;
        this.value=value;
    }

    public String getText() {
        return text;
    }

    public String getValue() {
        return value;
    }
}
