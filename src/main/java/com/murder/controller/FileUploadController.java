package com.murder.controller;

import com.murder.common.config.CdnConfig;
import com.murder.common.result.Result;
import com.murder.service.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器
 * 支持本地存储和CDN加速
 */
@RestController
@RequestMapping("/api/admin/file")
@Slf4j
public class FileUploadController {

    @Value("${file.upload.path:/data/upload}")
    private String uploadPath;

    /**
     * 历史遗留字段：早期用于拼接绝对访问地址。
     *
     * 单体部署/同源访问时，后端会直接返回相对路径（/upload/xxx），不再依赖该配置。
     */
    @Value("${file.upload.base-url:http://localhost:8080}")
    private String baseUrl;

    @Autowired
    private ImageService imageService;

    @Autowired
    private CdnConfig cdnConfig;

    /**
     * 上传单张图片
     */
    @PostMapping("/upload")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        log.info("上传文件：{}", file.getOriginalFilename());

        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("只能上传图片文件");
        }

        // 验证文件大小（限制5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.error("文件大小不能超过5MB");
        }

        try {
            String url = saveFile(file, "images");
            log.info("文件上传成功：{}", url);
            return Result.success(url);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 批量上传图片
     */
    @PostMapping("/upload/batch")
    public Result<List<String>> uploadImages(@RequestParam("files") MultipartFile[] files) {
        log.info("批量上传文件，数量：{}", files.length);

        if (files.length == 0) {
            return Result.error("请选择要上传的文件");
        }

        if (files.length > 10) {
            return Result.error("一次最多上传10张图片");
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.error("只能上传图片文件：" + file.getOriginalFilename());
            }

            // 验证文件大小
            if (file.getSize() > 5 * 1024 * 1024) {
                return Result.error("文件大小不能超过5MB：" + file.getOriginalFilename());
            }

            try {
                String url = saveFile(file, "images");
                urls.add(url);
            } catch (IOException e) {
                log.error("文件上传失败：{}", file.getOriginalFilename(), e);
                return Result.error("文件上传失败：" + file.getOriginalFilename());
            }
        }

        log.info("批量上传成功，数量：{}", urls.size());
        return Result.success(urls);
    }

    /**
     * 上传剧本封面
     */
    @PostMapping("/upload/script-cover")
    public Result<String> uploadScriptCover(@RequestParam("file") MultipartFile file) {
        log.info("上传剧本封面：{}", file.getOriginalFilename());
        
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("只能上传图片文件");
        }

        // 验证文件大小（限制5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.error("文件大小不能超过5MB");
        }
        
        try {
            String url = saveFile(file, "scripts/covers");
            log.info("剧本封面上传成功：{}", url);
            return Result.success(url);
        } catch (IOException e) {
            log.error("剧本封面上传失败", e);
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传角色头像
     */
    @PostMapping("/upload/role-avatar")
    public Result<String> uploadRoleAvatar(@RequestParam("file") MultipartFile file) {
        try {
            String url = saveFile(file, "roles/avatars");
            return Result.success(url);
        } catch (IOException e) {
            log.error("角色头像上传失败", e);
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传角色立绘
     */
    @PostMapping("/upload/role-character")
    public Result<String> uploadRoleCharacter(@RequestParam("file") MultipartFile file) {
        try {
            String url = saveFile(file, "roles/characters");
            return Result.success(url);
        } catch (IOException e) {
            log.error("角色立绘上传失败", e);
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传门店封面
     */
    @PostMapping("/upload/store-cover")
    public Result<String> uploadStoreCover(@RequestParam("file") MultipartFile file) {
        log.info("上传门店封面：{}", file.getOriginalFilename());
        
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("只能上传图片文件");
        }

        // 验证文件大小（限制5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.error("文件大小不能超过5MB");
        }
        
        try {
            String url = saveFile(file, "stores/covers");
            log.info("门店封面上传成功：{}", url);
            return Result.success(url);
        } catch (IOException e) {
            log.error("门店封面上传失败", e);
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    /**
     * 保存文件
     */
    private String saveFile(MultipartFile file, String subDir) throws IOException {
        // 创建日期目录
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        
        // 转换为绝对路径
        Path uploadBasePath = Paths.get(uploadPath).toAbsolutePath();
        Path dirPath = uploadBasePath.resolve(subDir).resolve(dateDir);
        
        // 确保目录存在
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
            log.info("创建上传目录：{}", dirPath);
        }

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;

        // 保存文件
        Path filePath = dirPath.resolve(filename);
        file.transferTo(filePath.toFile());
        
        log.info("文件已保存到：{}", filePath);

        // 返回访问URL：优先返回同源相对路径，避免端口/域名变化导致前端访问失败
        // 例如：/upload/scripts/covers/2026/01/04/xxx.png
        String relativePath = subDir + "/" + dateDir + "/" + filename;
        String localUrl = "/upload/" + relativePath.replace("\\", "/");

        return imageService.toCdnUrl(localUrl);
    }

    /**
     * 上传图片并返回多种尺寸URL（支持CDN）
     */
    @PostMapping("/upload/with-variants")
    public Result<Map<String, String>> uploadImageWithVariants(@RequestParam("file") MultipartFile file) {
        log.info("上传图片（含多尺寸）：{}", file.getOriginalFilename());

        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("只能上传图片文件");
        }

        // 验证文件大小（限制5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.error("文件大小不能超过5MB");
        }

        try {
            String originalUrl = saveFile(file, "images");
            
            // 返回多种尺寸的URL
            Map<String, String> urls = new HashMap<>();
            urls.put("original", originalUrl);
            urls.put("thumbnail", imageService.getThumbnailUrl(originalUrl));
            urls.put("medium", imageService.getMediumUrl(originalUrl));
            urls.put("large", imageService.getLargeUrl(originalUrl));
            urls.put("thumbnailOptimized", imageService.getOptimizedThumbnailUrl(originalUrl));
            urls.put("mediumOptimized", imageService.getOptimizedMediumUrl(originalUrl));
            
            log.info("图片上传成功，已生成多尺寸URL");
            return Result.success(urls);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 获取CDN状态信息
     */
    @GetMapping("/cdn/status")
    public Result<Map<String, Object>> getCdnStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", cdnConfig.isEnabled());
        status.put("domain", cdnConfig.getDomain());
        status.put("imageProcessEnabled", cdnConfig.getImageProcess().isEnabled());
        return Result.success(status);
    }

    /**
     * 转换URL为CDN地址
     */
    @PostMapping("/cdn/convert")
    public Result<String> convertToCdnUrl(@RequestParam("url") String url) {
        String cdnUrl = imageService.toCdnUrl(url);
        return Result.success(cdnUrl);
    }

    /**
     * 批量转换URL为CDN地址
     */
    @PostMapping("/cdn/convert-batch")
    public Result<String> batchConvertToCdnUrl(@RequestParam("urls") String urls) {
        String cdnUrls = imageService.batchToCdnUrl(urls);
        return Result.success(cdnUrls);
    }
}
