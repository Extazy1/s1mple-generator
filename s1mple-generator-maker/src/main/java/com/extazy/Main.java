package com.extazy;

import com.extazy.maker.generator.Main.GenerateTemplate;
import com.extazy.maker.generator.Main.MainGenerator;
import com.extazy.maker.generator.Main.ZipGenerator;
import freemarker.template.TemplateException;

import java.io.IOException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    public static void main(String[] args) throws TemplateException, IOException, InterruptedException {
//        GenerateTemplate generateTemplate = new MainGenerator();
        GenerateTemplate generateTemplate = new ZipGenerator();
        generateTemplate.doGenerate();
    }
}



