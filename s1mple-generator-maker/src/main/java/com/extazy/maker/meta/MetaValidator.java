package com.extazy.maker.meta;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

/**
 * 元信息校验
 */
public class MetaValidator {

    public static void doValidAndFill(Meta meta) {
        validAndFillMetaRoot(meta);
        validAndFillFileConfig(meta);
        validAndFillModelConfig(meta);
    }

    private static String getDefaultInputRootPath(String sourceRootPath) {
        return "source/" + FileUtil.getLastPathEle(Paths.get(sourceRootPath)).getFileName().toString();
    }

    private static void validAndFillMetaRoot(Meta meta) {
        meta.setName(StrUtil.isBlank(meta.getName()) ? "my-generator" : meta.getName());
        meta.setDescription(StrUtil.isBlank(meta.getDescription()) ? "我的模板代码生成器" : meta.getDescription());
        meta.setAuthor(StrUtil.isBlank(meta.getAuthor()) ? "extazy" : meta.getAuthor());
        meta.setBasePackage(StrUtil.isBlank(meta.getBasePackage()) ? "com.extazy" : meta.getBasePackage());
        meta.setVersion(StrUtil.isBlank(meta.getVersion()) ? "1.0" : meta.getVersion());
        meta.setCreateTime(StrUtil.isBlank(meta.getCreateTime()) ? DateUtil.now() : meta.getCreateTime());
    }

    private static void validAndFillFileConfig(Meta meta) {
        Meta.FileConfig fileConfig = meta.getFileConfig();
        if (fileConfig == null) {
            return;
        }
        setFileConfigDefaults(fileConfig);
        fileConfig.getFiles().forEach(MetaValidator::setDefaultFileInfoValues);
    }

    private static void setFileConfigDefaults(Meta.FileConfig fileConfig) {
        if (StrUtil.isBlank(fileConfig.getInputRootPath())) {
            fileConfig.setInputRootPath(getDefaultInputRootPath(fileConfig.getSourceRootPath()));
        }
        if (StrUtil.isBlank(fileConfig.getOutputRootPath())) {
            fileConfig.setOutputRootPath("generated");
        }
        if (StrUtil.isBlank(fileConfig.getType())) {
            fileConfig.setType("dir");
        }
    }

    private static void validAndFillModelConfig(Meta meta) {
        Meta.ModelConfig modelConfig = meta.getModelConfig();
        if (modelConfig == null) {
            return;
        }
        modelConfig.getModels().forEach(MetaValidator::setDefaultModelInfoValues);
    }

    private static void setDefaultModelInfoValues(Meta.ModelConfig.ModelInfo modelInfo) {
        if (StrUtil.isBlank(modelInfo.getFieldName())) {
            throw new MetaException("未填写 fieldName");
        }
        if (StrUtil.isBlank(modelInfo.getType())) {
            modelInfo.setType("String");
        }
    }

    private static void setDefaultFileInfoValues(Meta.FileConfig.FileInfo fileInfo) {
        if (StrUtil.isBlank(fileInfo.getInputPath())) {
            throw new MetaException("未填写 inputPath");
        }
        if (StrUtil.isBlank(fileInfo.getOutputPath())) {
            fileInfo.setOutputPath(fileInfo.getInputPath());
        }
        if (StrUtil.isBlank(fileInfo.getType())) {
            fileInfo.setType(FileUtil.getSuffix(fileInfo.getInputPath()) != null ? "file" : "dir");
        }
        if (StrUtil.isBlank(fileInfo.getGenerateType())) {
            fileInfo.setGenerateType(fileInfo.getInputPath().endsWith(".ftl") ? "dynamic" : "static");
        }
    }
}
