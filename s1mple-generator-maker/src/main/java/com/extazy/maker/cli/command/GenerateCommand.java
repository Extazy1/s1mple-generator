package com.extazy.maker.cli.command;

import cn.hutool.core.bean.BeanUtil;
import com.extazy.maker.generator.file.FileGenerator;
import com.extazy.maker.model.DataModel;
import lombok.Data;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(name = "generate", description = "Generate Code", mixinStandardHelpOptions = true)
@Data
public class GenerateCommand implements Callable<Integer> {

    @Option(names = {"-l", "--loop"}, arity = "0..1", description = "Is Loop", interactive = true, echo = true)
    private boolean loop;

    @Option(names = {"-a", "--author"}, arity = "0..1", description = "Author", interactive = true, echo = true)
    private String author = "extazy";

    @Option(names = {"-o", "--outputText"}, arity = "0..1", description = "outputText", interactive = true, echo = true)
    private String outputText = "sum = ";

    public Integer call() throws Exception {
        DataModel dataModel = new DataModel();
        BeanUtil.copyProperties(this, dataModel);
        System.out.println("config info：" + dataModel);
        FileGenerator.doGenerate(dataModel);
        return 0;
    }
}

