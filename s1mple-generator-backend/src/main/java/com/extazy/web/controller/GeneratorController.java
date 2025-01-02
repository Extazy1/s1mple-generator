package com.extazy.web.controller;

import cn.hutool.core.codec.Base64Encoder;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.extazy.maker.generator.Main.GenerateTemplate;
import com.extazy.maker.generator.Main.ZipGenerator;
import com.extazy.maker.meta.MetaValidator;
import com.extazy.web.annotation.AuthCheck;
import com.extazy.web.common.BaseResponse;
import com.extazy.web.common.DeleteRequest;
import com.extazy.web.common.ErrorCode;
import com.extazy.web.common.ResultUtils;
import com.extazy.web.constant.UserConstant;
import com.extazy.web.exception.BusinessException;
import com.extazy.web.exception.ThrowUtils;
import com.extazy.web.manager.CacheManager;
import com.extazy.web.manager.CosManager;
import com.extazy.maker.meta.Meta;
import com.extazy.web.model.dto.generator.*;
import com.extazy.web.model.entity.Generator;
import com.extazy.web.model.entity.User;
import com.extazy.web.model.vo.GeneratorVO;
import com.extazy.web.service.GeneratorService;
import com.extazy.web.service.UserService;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 生成器配置接口
 */
@RestController
@RequestMapping("/generator")
@Slf4j
public class GeneratorController {

    @Resource
    private GeneratorService generatorService;

    @Resource
    private UserService userService;
    @Resource
    private CosManager cosManager;

    @Resource
    private CacheManager cacheManager;

    private static final String GENERATOR_LIST_VERSION_KEY = "generator:list:version";

    // region 增删改查

    /**
     * 创建
     *
     * @param generatorAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addGenerator(@RequestBody GeneratorAddRequest generatorAddRequest, HttpServletRequest request) {
        if (generatorAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Generator generator = new Generator();
        BeanUtils.copyProperties(generatorAddRequest, generator);
        List<String> tags = generatorAddRequest.getTags();
        generator.setTags(JSONUtil.toJsonStr(tags));
        Meta.FileConfig fileConfig = generatorAddRequest.getFileConfig();
        generator.setFileConfig(JSONUtil.toJsonStr(fileConfig));
        Meta.ModelConfig modelConfig = generatorAddRequest.getModelConfig();
        generator.setModelConfig(JSONUtil.toJsonStr(modelConfig));

        // 参数校验
        generatorService.validGenerator(generator, true);
        User loginUser = userService.getLoginUser(request);
        generator.setUserId(loginUser.getId());
        generator.setStatus(0);
        boolean result = generatorService.save(generator);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        updateGeneratorListCacheVersion();

        long newGeneratorId = generator.getId();
        return ResultUtils.success(newGeneratorId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteGenerator(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Generator oldGenerator = generatorService.getById(id);
        ThrowUtils.throwIf(oldGenerator == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldGenerator.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        updateGeneratorListCacheVersion();

        boolean b = generatorService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param generatorUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateGenerator(@RequestBody GeneratorUpdateRequest generatorUpdateRequest) {
        if (generatorUpdateRequest == null || generatorUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Generator generator = new Generator();
        BeanUtils.copyProperties(generatorUpdateRequest, generator);
        List<String> tags = generatorUpdateRequest.getTags();
        generator.setTags(JSONUtil.toJsonStr(tags));
        Meta.FileConfig fileConfig = generatorUpdateRequest.getFileConfig();
        generator.setFileConfig(JSONUtil.toJsonStr(fileConfig));
        Meta.ModelConfig modelConfig = generatorUpdateRequest.getModelConfig();
        generator.setModelConfig(JSONUtil.toJsonStr(modelConfig));

        // 参数校验
        generatorService.validGenerator(generator, false);
        long id = generatorUpdateRequest.getId();
        // 判断是否存在
        Generator oldGenerator = generatorService.getById(id);
        ThrowUtils.throwIf(oldGenerator == null, ErrorCode.NOT_FOUND_ERROR);

        updateGeneratorListCacheVersion();

        boolean result = generatorService.updateById(generator);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<GeneratorVO> getGeneratorVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Generator generator = generatorService.getById(id);
        if (generator == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(generatorService.getGeneratorVO(generator, request));
    }

    /**
     * 分页获取列表（仅管理员）
     *
     * @param generatorQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Generator>> listGeneratorByPage(@RequestBody GeneratorQueryRequest generatorQueryRequest) {
        long current = generatorQueryRequest.getCurrent();
        long size = generatorQueryRequest.getPageSize();
        Page<Generator> generatorPage = generatorService.page(new Page<>(current, size),
                generatorService.getQueryWrapper(generatorQueryRequest));
        return ResultUtils.success(generatorPage);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param generatorQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<GeneratorVO>> listGeneratorVOByPage(@RequestBody GeneratorQueryRequest generatorQueryRequest,
                                                                 HttpServletRequest request) {
        long current = generatorQueryRequest.getCurrent();
        long size = generatorQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Generator> generatorPage = generatorService.page(new Page<>(current, size),
                generatorService.getQueryWrapper(generatorQueryRequest));
        return ResultUtils.success(generatorService.getGeneratorVOPage(generatorPage, request));
    }

    /**
     * 快速分页获取列表（封装类）
     *
     * @param generatorQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo/fast")
    public BaseResponse<Page<GeneratorVO>> listGeneratorVOByPageFast(@RequestBody GeneratorQueryRequest generatorQueryRequest,
                                                                     HttpServletRequest request) {
        long current = generatorQueryRequest.getCurrent();
        long size = generatorQueryRequest.getPageSize();
        // 优先从缓存读取
        String cacheKey = getPageCacheKey(generatorQueryRequest);
        Object cacheValue = cacheManager.get(cacheKey);
        if (cacheValue != null) {
            return ResultUtils.success((Page<GeneratorVO>) cacheValue);
        }

        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        QueryWrapper<Generator> queryWrapper = generatorService.getQueryWrapper(generatorQueryRequest);
        queryWrapper.select("id",
                "name",
                "description",
                "tags",
                "picture",
                "status",
                "userId",
                "createTime",
                "updateTime"
        );
        Page<Generator> generatorPage = generatorService.page(new Page<>(current, size), queryWrapper);
        Page<GeneratorVO> generatorVOPage = generatorService.getGeneratorVOPage(generatorPage, request);
        // 写入缓存
        cacheManager.put(cacheKey, generatorVOPage);
        return ResultUtils.success(generatorVOPage);
    }

    private void updateGeneratorListCacheVersion() {
        // 这里简单调用 cacheManager.put()，令其自增 version
        // 不过因为它本身是一个普通 key，你需要先从 cacheManager 拿到旧值 + 1
        Long oldVersion = (Long) cacheManager.get(GENERATOR_LIST_VERSION_KEY);
        if (oldVersion == null) {
            oldVersion = 0L;
        }
        Long newVersion = oldVersion + 1;
        cacheManager.put(GENERATOR_LIST_VERSION_KEY, newVersion);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param generatorQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<GeneratorVO>> listMyGeneratorVOByPage(@RequestBody GeneratorQueryRequest generatorQueryRequest,
                                                                   HttpServletRequest request) {
        if (generatorQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        generatorQueryRequest.setUserId(loginUser.getId());
        long current = generatorQueryRequest.getCurrent();
        long size = generatorQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Generator> generatorPage = generatorService.page(new Page<>(current, size),
                generatorService.getQueryWrapper(generatorQueryRequest));
        return ResultUtils.success(generatorService.getGeneratorVOPage(generatorPage, request));
    }

    // endregion

    /**
     * 编辑（用户）
     *
     * @param generatorEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editGenerator(@RequestBody GeneratorEditRequest generatorEditRequest, HttpServletRequest request) {
        if (generatorEditRequest == null || generatorEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Generator generator = new Generator();
        BeanUtils.copyProperties(generatorEditRequest, generator);
        List<String> tags = generatorEditRequest.getTags();
        generator.setTags(JSONUtil.toJsonStr(tags));
        Meta.FileConfig fileConfig = generatorEditRequest.getFileConfig();
        generator.setFileConfig(JSONUtil.toJsonStr(fileConfig));
        Meta.ModelConfig modelConfig = generatorEditRequest.getModelConfig();
        generator.setModelConfig(JSONUtil.toJsonStr(modelConfig));

        // 参数校验
        generatorService.validGenerator(generator, false);
        User loginUser = userService.getLoginUser(request);
        long id = generatorEditRequest.getId();
        // 判断是否存在
        Generator oldGenerator = generatorService.getById(id);
        ThrowUtils.throwIf(oldGenerator == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldGenerator.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = generatorService.updateById(generator);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 下载
     *
     * @param id
     * @return
     */
    @GetMapping("/download")
    public void downloadGeneratorById(long id, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Generator generator = generatorService.getById(id);
        if (generator == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        String filepath = generator.getDistPath();
        if (StrUtil.isBlank(filepath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "产物包不存在");
        }

        // 追踪事件
        log.info("用户 {} 下载了 {}", loginUser, filepath);

        // 设置响应头
        response.setContentType("application/octet-stream;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + filepath);

        // 优先从缓存读取
        String zipFilePath = getCacheFilePath(id, filepath);
        if (FileUtil.exist(zipFilePath)) {
            // 写入响应
            Files.copy(Paths.get(zipFilePath), response.getOutputStream());
            return;
        }

        COSObjectInputStream cosObjectInput = null;
        try {
            COSObject cosObject = cosManager.getObject(filepath);
            cosObjectInput = cosObject.getObjectContent();
            // 处理下载到的流
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }
    }

    /**
     * 使用代码生成器
     *
     * @param generatorUseRequest
     * @param request
     * @param response
     * @return
     */
    @PostMapping("/use")
    public void useGenerator(@RequestBody GeneratorUseRequest generatorUseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 获取用户输入的请求参数
        Long id = generatorUseRequest.getId();
        Map<String, Object> dataModel = generatorUseRequest.getDataModel();

        // 需要用户登录
        User loginUser = userService.getLoginUser(request);
        log.info("userId = {} 使用了生成器 id = {}", loginUser.getId(), id);

        Generator generator = generatorService.getById(id);
        if (generator == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 生成器的存储路径
        String distPath = generator.getDistPath();
        if (StrUtil.isBlank(distPath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "产物包不存在");
        }

        // 从对象存储下载生成器的压缩包

        // 定义独立的工作空间
        String projectPath = System.getProperty("user.dir");
        String tempDirPath = String.format("%s/.temp/use/%s", projectPath, id);
        String zipFilePath = tempDirPath + "/dist.zip";

        if (!FileUtil.exist(zipFilePath)) {
            FileUtil.touch(zipFilePath);
        }

        try {
            cosManager.download(distPath, zipFilePath);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成器下载失败");
        }

        // 解压压缩包，得到脚本文件
        File unzipDistDir = ZipUtil.unzip(zipFilePath);

        // 将用户输入的参数写到 json 文件中
        String dataModelFilePath = tempDirPath + "/dataModel.json";
        String jsonStr = JSONUtil.toJsonStr(dataModel);
        FileUtil.writeUtf8String(jsonStr, dataModelFilePath);

        // 执行脚本
        // 找到脚本文件所在路径
        // 要注意，如果不是 windows 系统，找 generator 文件而不是 bat
        File scriptFile = FileUtil.loopFiles(unzipDistDir, 2, null)
                .stream()
                .filter(file -> file.isFile()
                        && "generator.bat".equals(file.getName()))
                .findFirst()
                .orElseThrow(RuntimeException::new);

        // 添加可执行权限
        try {
            Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxrwxrwx");
            Files.setPosixFilePermissions(scriptFile.toPath(), permissions);
        } catch (Exception e) {

        }

        // 构造命令
        File scriptDir = scriptFile.getParentFile();
        // 注意，如果是 mac / linux 系统，要用 "./generator"
        String scriptAbsolutePath = scriptFile.getAbsolutePath().replace("\\", "/");
        String[] commands = new String[] {scriptAbsolutePath, "json-generate", "--file=" + dataModelFilePath};

        // 这里一定要拆分！
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.directory(scriptDir);

        try {
            Process process = processBuilder.start();

            // 读取命令的输出
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // 等待命令执行完成
            int exitCode = process.waitFor();
            System.out.println("命令执行结束，退出码：" + exitCode);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "执行生成器脚本错误");
        }

        // 压缩得到的生成结果，返回给前端
        String generatedPath = scriptDir.getAbsolutePath() + "/generated";
        String resultPath = tempDirPath + "/result.zip";
        File resultFile = ZipUtil.zip(generatedPath, resultPath);

        // 设置响应头
        response.setContentType("application/octet-stream;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + resultFile.getName());
        Files.copy(resultFile.toPath(), response.getOutputStream());

        // 清理文件
        CompletableFuture.runAsync(() -> {
            FileUtil.del(tempDirPath);
        });
    }

    /**
     * 制作代码生成器
     *
     * @param generatorMakeRequest
     * @param request
     * @param response
     */
    @PostMapping("/make")
    public void makeGenerator(@RequestBody GeneratorMakeRequest generatorMakeRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 1）输入参数
        String zipFilePath = generatorMakeRequest.getZipFilePath();
        Meta meta = generatorMakeRequest.getMeta();

        // 需要登录
        User loginUser = userService.getLoginUser(request);

        // 2）创建独立工作空间，下载压缩包到本地
        if (StrUtil.isBlank(zipFilePath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "压缩包不存在");
        }

        // 工作空间
        String projectPath = System.getProperty("user.dir");
        // 随机 id
        String id = IdUtil.getSnowflakeNextId() + RandomUtil.randomString(6);
        String tempDirPath = String.format("%s/.temp/make/%s", projectPath, id);
        String localZipFilePath = tempDirPath + "/project.zip";

        // 新建文件
        if (!FileUtil.exist(localZipFilePath)) {
            FileUtil.touch(localZipFilePath);
        }

        try {
            cosManager.download(zipFilePath, localZipFilePath);
        } catch (InterruptedException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "压缩包下载失败");
        }

        // 3）解压，得到项目模板文件
        File unzipDistDir = ZipUtil.unzip(localZipFilePath);

        // 4）构造 meta 对象和输出路径
        String sourceRootPath = unzipDistDir.getAbsolutePath();
        meta.getFileConfig().setSourceRootPath(sourceRootPath);
        MetaValidator.doValidAndFill(meta);
        String outputPath = String.format("%s/generated/%s", tempDirPath, meta.getName());

        // 5）调用 maker 方法制作生成器
        GenerateTemplate generateTemplate = new ZipGenerator();
        try {
            generateTemplate.doGenerate(meta, outputPath);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "制作失败");
        }

        // 6）下载压缩的产物包文件
        String suffix = "-dist.zip";
        String zipFileName = meta.getName() + suffix;
        String distZipFilePath = outputPath + suffix;

        // 下载文件
        // 设置响应头
        response.setContentType("application/octet-stream;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + zipFileName);
        // 写入响应
        Files.copy(Paths.get(distZipFilePath), response.getOutputStream());

        // 7）清理文件
        CompletableFuture.runAsync(() -> {
            FileUtil.del(tempDirPath);
        });
    }

    /**
     * 缓存代码生成器
     *
     * @param generatorCacheRequest
     * @param request
     * @param response
     */
    @PostMapping("/cache")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public void cacheGenerator(@RequestBody GeneratorCacheRequest generatorCacheRequest, HttpServletRequest request, HttpServletResponse response) {
        if (generatorCacheRequest == null || generatorCacheRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取生成器
        long id = generatorCacheRequest.getId();
        Generator generator = generatorService.getById(id);
        if (generator == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        String distPath = generator.getDistPath();
        if (StrUtil.isBlank(distPath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "产物包不存在");
        }

        // 缓存空间
        String zipFilePath = getCacheFilePath(id, distPath);

        // 新建文件
        if (!FileUtil.exist(zipFilePath)) {
            FileUtil.touch(zipFilePath);
        }

        // 下载生成器
        try {
            cosManager.download(distPath, zipFilePath);
        } catch (InterruptedException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "压缩包下载失败");
        }
    }

    /**
     * 获取缓存文件路径
     *
     * @param id
     * @param distPath
     * @return
     */
    public String getCacheFilePath(long id, String distPath) {
        String projectPath = System.getProperty("user.dir");
        String tempDirPath = String.format("%s/.temp/cache/%s", projectPath, id);
        String zipFilePath = String.format("%s/%s", tempDirPath, distPath);
        return zipFilePath;
    }


    private static String getPageCacheKey(GeneratorQueryRequest generatorQueryRequest) {
        String jsonStr = JSONUtil.toJsonStr(generatorQueryRequest);
        // 编码请求参数
        String base64 = Base64Encoder.encode(jsonStr);
        String key = "generator:page:" + base64;

        return key;
    }

    /**
     * 解析用户上传的模板包，生成文件配置并返回
     */
    /**
     * 接收 fileUrl，解析模板压缩包中的所有文件 / 文件夹信息，存入 Meta.FileConfig.files，然后塞到 Generator.fileConfig 里返回
     */
    @PostMapping("/parseTemplate")
    public BaseResponse<Generator> parseTemplate(@RequestBody GeneratorParseTemplateRequest generatorParseTemplateRequest, HttpServletRequest request, HttpServletResponse response) {
        // 1) 参数校验
        String fileUrl = generatorParseTemplateRequest.getFileUrl();
        if (request == null || StrUtil.isBlank(fileUrl)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "fileUrl 不能为空");
        }

        // 需要登录
        User loginUser = userService.getLoginUser(request);

        // 2) 下载 zip 到本地
        if (StrUtil.isBlank(fileUrl)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件不存在");
        }

        String projectPath = System.getProperty("user.dir");
        String uniqueId = IdUtil.getSnowflakeNextIdStr() + RandomUtil.randomString(6);
        String tempDirPath = StrUtil.format("{}/.temp/parse/{}", projectPath, uniqueId);
        String localZipFilePath = tempDirPath + "/project.zip";

        FileUtil.mkdir(tempDirPath);
        FileUtil.touch(localZipFilePath);

        try {
            cosManager.download(fileUrl, localZipFilePath);
        } catch (Exception e) {
            log.error("下载模板失败, fileUrl = {}", fileUrl, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载模板失败");
        }

        // 3) 解压
        File unzipDistDir = ZipUtil.unzip(localZipFilePath);

        // 4) 遍历解压后的文件 / 文件夹，填充 FileInfo 列表
        List<Meta.FileConfig.FileInfo> fileInfoList = FileUtil.loopFiles(unzipDistDir).stream().map(file -> {
            // 计算相对路径
            String relativePath = FileUtil.subPath(unzipDistDir.getAbsolutePath(), file).replace("\\", "/");

            Meta.FileConfig.FileInfo info = new Meta.FileConfig.FileInfo();
            info.setInputPath(relativePath);

            // 如果文件是以 .ftl 结尾，则去掉 .ftl 设置为 outputPath
            if (relativePath.endsWith(".ftl")) {
                info.setOutputPath(relativePath.substring(0, relativePath.length() - 4)); // 去掉 .ftl
                info.setGenerateType("dynamic");
            } else {
                info.setOutputPath(relativePath); // 保持原路径
                info.setGenerateType("static");
            }

            info.setType(file.isDirectory() ? "dir" : "file");
            return info;
        }).toList();

        // 5) 构造一个 Meta，并往 fileConfig 里塞进 fileInfoList
        Meta meta = new Meta();
        // 你可以在这里给 meta.name / meta.description 等字段赋值
        Meta.FileConfig fileConfig = new Meta.FileConfig();
        // 这几个路径属性按需填
        fileConfig.setInputRootPath(unzipDistDir.getAbsolutePath());
        fileConfig.setOutputRootPath(unzipDistDir.getAbsolutePath());
        fileConfig.setSourceRootPath(unzipDistDir.getAbsolutePath());
        // ...
        fileConfig.setFiles(fileInfoList);

        // 赋值给 meta
        meta.setFileConfig(fileConfig);

        // 6) 组装一个 Generator，把 meta.fileConfig 序列化后赋值给 generator.fileConfig
        Generator generator = new Generator();
        // generator.setName("parsed-template");  // 看需求
        // 直接把 `meta.fileConfig` 写入 generator.fileConfig (JSON)
        generator.setFileConfig(JSONUtil.toJsonStr(meta.getFileConfig()));

        // 如果想写更多字段，也可以
        // generator.setDescription("根据模板自动解析的文件配置");
        // generator.setAuthor("system");
        // generator.setStatus(0);

        // 7) 异步清理临时目录
        CompletableFuture.runAsync(() -> {
            FileUtil.del(tempDirPath);
        });

        // 8) 返回
        return ResultUtils.success(generator);
    }

}
