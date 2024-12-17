package com.extazy.web.job;

import cn.hutool.core.util.StrUtil;
import com.extazy.web.manager.CosManager;
import com.extazy.web.mapper.GeneratorMapper;
import com.extazy.web.model.entity.Generator;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ClearCosJobHandler {

    @Resource
    private CosManager cosManager;

    @Resource
    private GeneratorMapper generatorMapper;

    /**
     * 每天执行
     */
    @XxlJob("clearCosJobHandler")
    public void clearCosJobHandler() throws Exception {
        log.info("clearCosJobHandler start");

        try {
            // 1. 删除用户上传的模板制作文件（generator_make_template）
            cosManager.deleteDir("/generator_make_template/");
            log.info("Deleted directory: /generator_make_template/");

            // 2. 删除已删除代码生成器的产物包文件（generator_dist）
            List<Generator> generatorList = generatorMapper.listDeletedGenerator();
            if (generatorList == null || generatorList.isEmpty()) {
                log.info("No deleted generators found, skipping generator_dist cleanup.");
            } else {
                List<String> keyList = generatorList.stream()
                        .map(Generator::getDistPath)
                        .filter(StrUtil::isNotBlank)
                        .map(distPath -> distPath.startsWith("/") ? distPath.substring(1) : distPath) // 移除 '/' 前缀
                        .collect(Collectors.toList());

                if (keyList.isEmpty()) {
                    log.info("No dist files to delete for deleted generators.");
                } else {
                    cosManager.deleteObjects(keyList);
                    log.info("Successfully deleted dist files: {}", keyList);
                }
            }

            // 明确通知任务执行成功
            log.info("clearCosJobHandler completed successfully.");
        } catch (Exception e) {
            // 任务执行失败时打印错误日志
            log.error("clearCosJobHandler execution failed: ", e);
            throw e; // 抛出异常，确保 XXL-JOB 记录为失败
        }
    }
}

