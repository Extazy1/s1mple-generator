package com.extazy.web.model.dto.generator;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.extazy.maker.meta.Meta;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 文件路径解析请求
 */
@Data
public class GeneratorParseTemplateRequest implements Serializable {

    private String fileUrl;

    private static final long serialVersionUID = 1L;
}