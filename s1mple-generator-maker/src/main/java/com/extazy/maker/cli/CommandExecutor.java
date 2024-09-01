package com.extazy.maker.cli;

import com.extazy.maker.cli.command.ConfigCommand;
import com.extazy.maker.cli.command.GenerateCommand;
import com.extazy.maker.cli.command.ListCommand;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "s1mple", mixinStandardHelpOptions = true)
public class CommandExecutor implements Runnable {

    private final CommandLine commandLine;

    {
        commandLine = new CommandLine(this)
                .addSubcommand(new GenerateCommand())
                .addSubcommand(new ConfigCommand())
                .addSubcommand(new ListCommand());
    }

    @Override
    public void run() {
        // 不输入子命令时，给出友好提示
        System.out.println("The input is not a generate command. See 'generate --help'");
    }

    /**
     * 执行命令
     *
     * @param args
     * @return
     */
    public Integer doExecute(String[] args) {
        return commandLine.execute(args);
    }
}


