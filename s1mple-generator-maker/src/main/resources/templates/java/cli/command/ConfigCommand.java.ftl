package ${basePackage}.maker.cli.command;

import cn.hutool.core.util.ReflectUtil;
import ${basePackage}.maker.model.DataModel;
import picocli.CommandLine.Command;

import java.lang.reflect.Field;

@Command(name = "config", description = "Show Config Info", mixinStandardHelpOptions = true)
public class ConfigCommand implements Runnable {

public void run() {
// 实现 config 命令的逻辑
System.out.println("Show Config Info");

Field[] fields = ReflectUtil.getFields(DataModel.class);

// 遍历并打印每个字段的信息
for (Field field : fields) {
System.out.println("Field Name：" + field.getName());
System.out.println("Field Type：" + field.getType());
System.out.println("---");
}
}
}

