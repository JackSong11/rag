package com.example.rag.common.enums;

import lombok.Getter;

@Getter
public enum FileTypeEnum {

    PDF("pdf", "PDF文件"),
    DOC("doc", "文档文件"),
    VISUAL("visual", "视觉文件"),
    AURAL("aural", "音频文件"),
    VIRTUAL("virtual", "虚拟文件"),
    FOLDER("folder", "文件夹"),
    OTHER("other", "其他");

    private final String code;

    private final String desc;

    FileTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}