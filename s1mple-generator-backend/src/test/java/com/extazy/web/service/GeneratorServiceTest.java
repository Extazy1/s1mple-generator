package com.extazy.web.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.extazy.web.model.entity.Generator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class GeneratorServiceTest {

    @Resource
    private GeneratorService generatorService;

    @Test
    public void testInsert() {
        Generator generator = generatorService.getById(18L);
        for (int i = 0; i < 100000; i++) {
            generator.setId(null);
            generatorService.save(generator);
        }
    }

    @Test
    public void testDeleteGeneratorsGreaterThan20() {
        // 构建查询条件，删除所有 id > 18 的数据
        QueryWrapper<Generator> queryWrapper = new QueryWrapper<>();
        queryWrapper.gt("id", 20); // 添加条件：id > 18

        // 调用 service 方法进行删除
        boolean result = generatorService.remove(queryWrapper);

        // 打印删除结果
        System.out.println("删除所有 id > 20 的数据，结果：" + result);
    }

    @Test
    public void testDeletePhysicGeneratorsGreaterThan20() {
        // 强制物理删除而非逻辑删除
        QueryWrapper<Generator> queryWrapper = new QueryWrapper<>();
        queryWrapper.gt("id", 20); // 查询条件：id > 18

        // 直接调用删除方法，确保不启用逻辑删除
        boolean result = generatorService.getBaseMapper().delete(queryWrapper) > 0;

        System.out.println("彻底删除所有 id > 20 的数据，结果：" + result);
    }

}
