package ${basePackage}.maker.cli.command;

import cn.hutool.core.bean.BeanUtil;
import ${basePackage}.maker.generator.file.FileGenerator;
import ${basePackage}.maker.model.DataModel;
import lombok.Data;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(name = "generate", description = "Generate Code", mixinStandardHelpOptions = true)
@Data
public class GenerateCommand implements Callable<Integer> {

    <#list modelConfig.models as modelInfo>

        @Option(names = {<#if modelInfo.abbr??>"-${modelInfo.abbr}", </#if>"--${modelInfo.fieldName}"}, arity = "0..1", <#if modelInfo.description??>description = "${modelInfo.description}", </#if>interactive = true, echo = true)
        private ${modelInfo.type} ${modelInfo.fieldName}<#if modelInfo.defaultValue??> = ${modelInfo.defaultValue?c}</#if>;
    </#list>

    public Integer call() throws Exception {
    DataModel dataModel = new DataModel();
    BeanUtil.copyProperties(this, dataModel);
    System.out.println("config info：" + dataModel);
    FileGenerator.doGenerate(dataModel);
    return 0;
    }
}