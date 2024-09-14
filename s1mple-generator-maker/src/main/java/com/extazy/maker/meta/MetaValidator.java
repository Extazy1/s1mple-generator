package com.extazy.maker.meta;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.extazy.maker.meta.enums.FileGenerateTypeEnum;
import com.extazy.maker.meta.enums.FileTypeEnum;
import com.extazy.maker.meta.enums.ModelTypeEnum;

import java.nio.file.Paths;
import java.util.stream.Collectors;

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
        fileConfig.getFiles().stream() // 使用流来处理集合
                .filter(fileInfo -> !FileTypeEnum.GROUP.getValue().equals(fileInfo.getType())) // 过滤掉type为group的FileInfo
                .forEach(MetaValidator::setDefaultFileInfoValues); // 只对非group的FileInfo调用setDefaultFileInfoValues
    }

    private static void setFileConfigDefaults(Meta.FileConfig fileConfig) {
        if (StrUtil.isBlank(fileConfig.getInputRootPath())) {
            fileConfig.setInputRootPath(getDefaultInputRootPath(fileConfig.getSourceRootPath()));
        }
        if (StrUtil.isBlank(fileConfig.getOutputRootPath())) {
            fileConfig.setOutputRootPath("generated");
        }
        if (StrUtil.isBlank(fileConfig.getType())) {
            fileConfig.setType(FileTypeEnum.DIR.getValue());
        }
    }

    private static void validAndFillModelConfig(Meta meta) {
        Meta.ModelConfig modelConfig = meta.getModelConfig();
        if (modelConfig == null) {
            return;
        }

        // Process models with non-empty groupKey
        modelConfig.getModels().stream()
                .filter(modelInfo -> StrUtil.isNotBlank(modelInfo.getGroupKey()))
                .forEach(modelInfo -> {
                    // Generate intermediate parameters for the group
                    String allArgsStr = modelInfo.getModels().stream()
                            .map(subModelInfo -> String.format("\"--%s\"", subModelInfo.getFieldName()))
                            .collect(Collectors.joining(", "));
                    modelInfo.setAllArgsStr(allArgsStr);
                });

        // Process models with an empty groupKey
        modelConfig.getModels().stream()
                .filter(modelInfo -> StrUtil.isBlank(modelInfo.getGroupKey()))
                .forEach(MetaValidator::setDefaultModelInfoValues);
    }

    private static void setDefaultModelInfoValues(Meta.ModelConfig.ModelInfo modelInfo) {
        if (StrUtil.isBlank(modelInfo.getFieldName())) {
            throw new MetaException("未填写 fieldName");
        }
        if (StrUtil.isBlank(modelInfo.getType())) {
            modelInfo.setType(ModelTypeEnum.STRING.getValue());
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
            fileInfo.setType(FileUtil.getSuffix(fileInfo.getInputPath()) == null ? FileTypeEnum.DIR.getValue() : FileTypeEnum.FILE.getValue());
        }
        if (StrUtil.isBlank(fileInfo.getGenerateType())) {
            fileInfo.setGenerateType(fileInfo.getInputPath().endsWith(".ftl") ? FileGenerateTypeEnum.DYNAMIC.getValue() : FileGenerateTypeEnum.STATIC.getValue());
        }
    }
}
