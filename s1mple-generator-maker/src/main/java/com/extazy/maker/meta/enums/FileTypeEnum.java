package com.extazy.maker.meta.enums;

/**
 * 文件类型枚举
 */
public enum FileTypeEnum {
    DIR("目录","dir"),
    FILE("文件","file"),
    GROUP("文件组","group");

    public final String text;
    public final String value;
    FileTypeEnum(String text,String value) {
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
