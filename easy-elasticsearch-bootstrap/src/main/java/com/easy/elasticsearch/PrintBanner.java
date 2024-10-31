package com.easy.elasticsearch;

import java.io.PrintStream;

import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @Description: 启动成功日志
 * @Author: liangbaole 
 * @Date: 2023/5/23
 **/
@Component
public class PrintBanner implements ApplicationListener<ApplicationReadyEvent> {

    private static final String STARTTED = " " + "######  ########    ###    ########  ######## ######## ########  \n"
                                           + "##    ##    ##      ## ##   ##     ##    ##    ##       ##     ## \n"
                                           + "##    %s   ##     ##   ##  ##     ##    ##    ##  %s ##     ## \n"
                                           + " ######     ##    ##     ## ########     ##    ######   ##     ## \n"
                                           + "      ##    ##    ######### ##   ##      ##    ##       ##     ## \n"
                                           + "##    ##    ##    ##     ## ##    ##     ##    ##       ##     ## \n"
                                           + " ######     ##    ##     ## ##     ##    ##    ######## ########  \n";

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        PrintStream printStream = System.out;
        String format = String.format(AnsiOutput.toString(STARTTED, AnsiColor.YELLOW),
            AnsiOutput.toString(AnsiColor.GREEN, "启动"), AnsiOutput.toString(AnsiColor.GREEN, "成功"));
        printStream.println(format);
        String port = event.getApplicationContext().getEnvironment().getProperty("server.port");
        printStream.println(
            AnsiOutput.toString(AnsiColor.GREEN, " :: Spring Boot Application Start Port :: ", AnsiColor.RED, port));
    }
}
