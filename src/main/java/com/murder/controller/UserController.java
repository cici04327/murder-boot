package com.murder.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.context.BaseContext;
import com.murder.common.result.PageResult;
import com.murder.common.result.Result;
import com.murder.dto.BindEmailDTO;
import com.murder.dto.BindPhoneDTO;
import com.murder.dto.DeactivateAccountDTO;
import com.murder.dto.RealNameVerifyDTO;
import com.murder.dto.SendEmailCodeDTO;
import com.murder.dto.SendPhoneCodeDTO;
import com.murder.dto.UserLoginDTO;
import com.murder.dto.UserRegisterDTO;
import com.murder.dto.UpdatePasswordDTO;
import com.murder.entity.Reservation;
import com.murder.entity.User;
import com.murder.mapper.ReservationMapper;
import com.murder.service.UserAccountService;
import com.murder.service.UserBrowseHistoryService;
import com.murder.service.UserService;
import com.murder.service.UserSettingsService;
import com.murder.vo.BrowseHistoryVO;
import com.murder.vo.UserLoginLogVO;
import com.murder.vo.UserLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 用户控制器? */
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户接口")
@Slf4j
public class UserController {

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Autowired
    private UserService userService;

    @Autowired
    private UserBrowseHistoryService browseHistoryService;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private UserSettingsService userSettingsService;

    @Autowired
    private UserAccountService userAccountService;

    // ==================== 用户设置接口 ====================

    @GetMapping("/settings/privacy")
    @Operation(summary = "获取隐私设置")
    public Result<java.util.Map<String, Object>> getPrivacySettings() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) return Result.error("用户未登录");
        return Result.success(userSettingsService.getPrivacySettings(userId));
    }

    @PutMapping("/settings/privacy")
    @Operation(summary = "更新隐私设置")
    public Result<String> updatePrivacySettings(@RequestBody java.util.Map<String, Object> settings) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) return Result.error("用户未登录");
        userSettingsService.updatePrivacySettings(userId, settings);
        return Result.success("保存成功");
    }

    @GetMapping("/settings/notification")
    @Operation(summary = "获取通知设置")
    public Result<java.util.Map<String, Object>> getNotificationSettings() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) return Result.error("用户未登录");
        return Result.success(userSettingsService.getNotificationSettings(userId));
    }

    @PutMapping("/settings/notification")
    @Operation(summary = "更新通知设置")
    public Result<String> updateNotificationSettings(@RequestBody java.util.Map<String, Object> settings) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) return Result.error("用户未登录");
        userSettingsService.updateNotificationSettings(userId, settings);
        return Result.success("保存成功");
    }

    @GetMapping("/settings/preference")
    @Operation(summary = "获取偏好设置")
    public Result<java.util.Map<String, Object>> getPreferenceSettings() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) return Result.error("用户未登录");
        return Result.success(userSettingsService.getPreferenceSettings(userId));
    }

    @PutMapping("/settings/preference")
    @Operation(summary = "更新偏好设置")
    public Result<String> updatePreferenceSettings(@RequestBody java.util.Map<String, Object> settings) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) return Result.error("用户未登录");
        userSettingsService.updatePreferenceSettings(userId, settings);
        return Result.success("保存成功");
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<String> register(@RequestBody UserRegisterDTO userRegisterDTO) {
        log.info("用户注册: {}", userRegisterDTO);
        userService.register(userRegisterDTO);
        return Result.success("注册成功");
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO, HttpServletRequest request) {
        log.info("用户登录: {}", userLoginDTO);
        UserLoginVO userLoginVO = userService.login(userLoginDTO);
        userAccountService.recordLoginSuccess(
                userLoginVO.getId(),
                getClientIp(request),
                resolveLocation(getClientIp(request)),
                resolveDevice(request.getHeader("User-Agent"))
        );
        return Result.success(userLoginVO);
    }

    /**
     * 管理员登录
     */
    @PostMapping("/admin/login")
    @Operation(summary = "管理员登录")
    public Result<UserLoginVO> adminLogin(@RequestBody UserLoginDTO userLoginDTO, HttpServletRequest request) {
        log.info("管理员登录: {}", userLoginDTO);
        UserLoginVO userLoginVO = userService.adminLogin(userLoginDTO);
        userAccountService.recordLoginSuccess(
                userLoginVO.getId(),
                getClientIp(request),
                resolveLocation(getClientIp(request)),
                resolveDevice(request.getHeader("User-Agent"))
        );
        return Result.success(userLoginVO);
    }

    /**
     * 分页查询用户列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询用户列表")
    public Result<PageResult> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer memberLevel
    ) {
        log.info("分页查询用户: page={}, pageSize={}, username={}, phone={}, memberLevel={}", 
                page, pageSize, username, phone, memberLevel);
        
        Page<User> pageInfo = userService.pageQuery(page, pageSize, username, phone, memberLevel);
        
        PageResult pageResult = new PageResult();
        pageResult.setTotal(pageInfo.getTotal());
        pageResult.setRecords(pageInfo.getRecords());
        
        return Result.success(pageResult);
    }

    /**
     * 获取当前用户账号统计（必须放在 /{id} 之前避免路由冲突）
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取当前用户账号统计")
    public Result<java.util.Map<String, Object>> getStatistics() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录");
        }
        log.info("获取用户统计: userId={}", userId);

        // 查询用户积分
        User user = userService.getById(userId);
        int totalPoints = (user != null && user.getPoints() != null) ? user.getPoints() : 0;

        // 查询累计预约数（不含已取消）
        LambdaQueryWrapper<Reservation> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(Reservation::getUserId, userId)
                    .ne(Reservation::getStatus, 4);  // 4=已取消
        long totalReservations = reservationMapper.selectCount(countWrapper);

        // 查询累计消费金额（已支付的订单 actual_amount 求和）
        LambdaQueryWrapper<Reservation> spentWrapper = new LambdaQueryWrapper<>();
        spentWrapper.eq(Reservation::getUserId, userId)
                    .eq(Reservation::getPayStatus, 1);  // 1=已支付
        java.util.List<Reservation> paidList = reservationMapper.selectList(spentWrapper);
        BigDecimal totalSpent = paidList.stream()
                .map(r -> r.getActualAmount() != null ? r.getActualAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalPoints", totalPoints);
        stats.put("totalReservations", totalReservations);
        stats.put("totalSpent", totalSpent);
        stats.put("friendCount", 0);
        return Result.success(stats);
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取用户信息")
    public Result<User> getUserById(@PathVariable Long id) {
        log.info("获取用户信息: {}", id);
        User user = userService.getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }

    /**
     * 新增用户
     */
    @PostMapping
    @Operation(summary = "新增用户")
    public Result<String> add(@RequestBody User user) {
        log.info("新增用户: {}", user);
        userService.save(user);
        return Result.success("新增成功");
    }

    /**
     * 更新用户
     */
    @PutMapping
    @Operation(summary = "更新用户")
    public Result<String> update(@RequestBody User user) {
        log.info("更新用户: {}", user);

        Long currentUserId = BaseContext.getCurrentId();
        String currentRole = BaseContext.getRole();
        boolean isAdmin = "SUPER_ADMIN".equals(currentRole) || "STORE_ADMIN".equals(currentRole);

        if (!isAdmin && currentUserId != null) {
            boolean shouldReward = userAccountService.updateCurrentUserProfile(currentUserId, user);
            if (shouldReward) {
                try {
                    userService.addPointsForProfile(currentUserId);
                    log.info("用户{}完善资料，获得30积分奖励", currentUserId);
                    return Result.success("更新成功，获得30积分奖励");
                } catch (Exception e) {
                    log.error("完善资料积分发放失败", e);
                }
            }
            return Result.success("更新成功");
        }

        if (user.getId() == null) {
            return Result.error("用户ID不能为空");
        }

        userService.updateById(user);
        return Result.success("更新成功");
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除用户: {}", id);
        userService.removeById(id);
        return Result.success("删除成功");
    }

    /**
     * 修改用户状态
     */
    @PutMapping("/status/{id}")
    @Operation(summary = "修改用户状态")
    public Result<String> updateStatus(@PathVariable Long id, @RequestBody User user) {
        log.info("修改用户状态: id={}, status={}", id, user.getStatus());
        user.setId(id);
        userService.updateById(user);
        return Result.success("状态修改成功");
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    @Operation(summary = "修改密码")
    public Result<String> updatePassword(@RequestBody UpdatePasswordDTO dto) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录");
        }
        log.info("修改密码: userId={}", userId);
        userAccountService.updatePassword(userId, dto);
        return Result.success("密码修改成功");
    }

    @PostMapping("/phone/code")
    @Operation(summary = "发送手机验证码")
    public Result<Map<String, Object>> sendPhoneCode(@RequestBody SendPhoneCodeDTO dto) {
        return Result.success("验证码已发送", userAccountService.sendPhoneCode(dto.getPhone()));
    }

    @PostMapping("/phone/bind")
    @Operation(summary = "绑定手机号")
    public Result<String> bindPhone(@RequestBody BindPhoneDTO dto) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录");
        }
        userAccountService.bindPhone(userId, dto);
        return Result.success("手机号绑定成功");
    }

    @PostMapping("/email/code")
    @Operation(summary = "发送邮箱验证码")
    public Result<Map<String, Object>> sendEmailCode(@RequestBody SendEmailCodeDTO dto) {
        return Result.success("验证码已发送", userAccountService.sendEmailCode(dto.getEmail()));
    }

    @PostMapping("/email/bind")
    @Operation(summary = "绑定邮箱")
    public Result<String> bindEmail(@RequestBody BindEmailDTO dto) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录");
        }
        userAccountService.bindEmail(userId, dto);
        return Result.success("邮箱绑定成功");
    }

    @PostMapping("/real-name/verify")
    @Operation(summary = "实名认证")
    public Result<String> verifyRealName(@RequestBody RealNameVerifyDTO dto) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录");
        }
        userAccountService.verifyRealName(userId, dto);
        return Result.success("实名认证提交成功");
    }

    @GetMapping("/login-logs")
    @Operation(summary = "获取登录日志")
    public Result<PageResult<UserLoginLogVO>> getLoginLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录");
        }
        return Result.success(userAccountService.getLoginLogs(userId, page, pageSize));
    }

    @PostMapping("/deactivate")
    @Operation(summary = "注销账号")
    public Result<String> deactivateAccount(@RequestBody(required = false) DeactivateAccountDTO dto) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录");
        }
        userAccountService.deactivateAccount(userId, dto);
        return Result.success("账号已注销");
    }

    /**
     * 上传头像文件
     */
    @PostMapping("/upload/avatar")
    @Operation(summary = "上传头像文件")
    public Result<String> uploadAvatarFile(@RequestParam("file") MultipartFile file) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录");
        }
        
        if (file.isEmpty()) {
            return Result.error("请选择要上传的图片");
        }
        
        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("只能上传图片文件");
        }
        
        // 验证文件大小（最大 5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.error("图片大小不能超过5MB");
        }
        
        try {
            // 生成存储路径：uploads/avatars/2026/02/xxx.jpg
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                    ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                    : ".jpg";
            String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;
            
            String relativePath = "avatars/" + datePath + "/" + newFilename;
            
            // 获取项目根目录的绝对路径
            String projectRoot = System.getProperty("user.dir");
            String basePath = uploadPath.startsWith("./") 
                    ? projectRoot + uploadPath.substring(1) 
                    : uploadPath;
            if (!basePath.endsWith("/") && !basePath.endsWith("\\")) {
                basePath = basePath + "/";
            }
            String fullPath = basePath + relativePath;
            
            log.info("头像上传路径: {}", fullPath);
            
            // 创建目录
            Path directoryPath = Paths.get(fullPath).getParent();
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }
            
            // 保存文件
            File destFile = new File(fullPath);
            file.transferTo(destFile.getAbsoluteFile());
            
            // 更新用户头像
            String avatarUrl = "/uploads/" + relativePath;
            User user = new User();
            user.setId(userId);
            user.setAvatar(avatarUrl);
            userService.updateById(user);
            
            log.info("用户头像上传成功: userId={}, avatarUrl={}", userId, avatarUrl);
            return Result.success(avatarUrl);
        } catch (IOException e) {
            log.error("头像上传失败", e);
            return Result.error("头像上传失败，请稍后重试");
        }
    }
    
    /**
     * 更新头像URL
     */
    @PutMapping("/avatar")
    @Operation(summary = "更新头像URL")
    public Result<String> updateAvatarUrl(@RequestBody User user) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录");
        }
        log.info("更新头像URL: userId={}", userId);
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setAvatar(user.getAvatar());
        userService.updateById(updateUser);
        return Result.success("头像更新成功");
    }

    // ==================== 浏览历史相关接口 ====================

    /**
     * 获取浏览历史统计（需要放在 /browse-history/{id} 之前避免路由冲突）
     */
    @GetMapping("/browse-history/stats")
    @Operation(summary = "获取浏览历史统计")
    public Result<UserBrowseHistoryService.BrowseHistoryStatsVO> getBrowseHistoryStats() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录");
        }
        
        log.info("获取浏览历史统计: userId={}", userId);
        
        UserBrowseHistoryService.BrowseHistoryStatsVO stats = browseHistoryService.getHistoryStats(userId);
        return Result.success(stats);
    }

    /**
     * 清空所有浏览历史（需要放在 /browse-history/{id} 之前避免路由冲突）
     */
    @DeleteMapping("/browse-history/clear")
    @Operation(summary = "清空所有浏览历史")
    public Result<String> clearBrowseHistory() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录");
        }
        
        log.info("清空浏览历史: userId={}", userId);
        
        browseHistoryService.clearHistory(userId);
        return Result.success("清空成功");
    }

    /**
     * 分页获取浏览历史
     */
    @GetMapping("/browse-history")
    @Operation(summary = "分页获取浏览历史")
    public Result<PageResult> getBrowseHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer targetType
    ) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录");
        }
        
        log.info("获取浏览历史: userId={}, page={}, pageSize={}, days={}, targetType={}", 
                userId, page, pageSize, days, targetType);
        
        Page<BrowseHistoryVO> historyPage = browseHistoryService.pageHistory(userId, page, pageSize, days, targetType);
        
        PageResult pageResult = new PageResult();
        pageResult.setTotal(historyPage.getTotal());
        pageResult.setRecords(historyPage.getRecords());
        
        return Result.success(pageResult);
    }

    /**
     * 记录浏览历史
     */
    @PostMapping("/browse-history")
    @Operation(summary = "记录浏览历史")
    public Result<String> recordBrowseHistory(
            @RequestParam Integer targetType,
            @RequestParam Long targetId,
            @RequestParam(required = false, defaultValue = "0") Integer duration
    ) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录");
        }
        
        log.info("记录浏览历史: userId={}, targetType={}, targetId={}, duration={}", 
                userId, targetType, targetId, duration);
        
        browseHistoryService.recordHistory(userId, targetType, targetId, duration);
        return Result.success("记录成功");
    }

    /**
     * 删除单条浏览记录
     */
    @DeleteMapping("/browse-history/{id}")
    @Operation(summary = "删除单条浏览记录")
    public Result<String> deleteBrowseHistory(@PathVariable Long id) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录");
        }
        
        log.info("删除浏览记录: userId={}, historyId={}", userId, id);
        
        boolean success = browseHistoryService.deleteHistory(userId, id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return StringUtils.hasText(remoteAddr) ? remoteAddr : "127.0.0.1";
    }

    private String resolveLocation(String ip) {
        if (!StringUtils.hasText(ip)) {
            return "未知";
        }
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
            return "本地网络";
        }
        return "未知";
    }

    private String resolveDevice(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "未知设备";
        }

        StringBuilder builder = new StringBuilder();
        if (userAgent.contains("Windows")) {
            builder.append("Windows");
        } else if (userAgent.contains("Mac OS")) {
            builder.append("macOS");
        } else if (userAgent.contains("Android")) {
            builder.append("Android");
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            builder.append("iOS");
        } else {
            builder.append("其他系统");
        }

        if (userAgent.contains("Edg")) {
            builder.append(" / Edge");
        } else if (userAgent.contains("Chrome")) {
            builder.append(" / Chrome");
        } else if (userAgent.contains("Safari")) {
            builder.append(" / Safari");
        } else if (userAgent.contains("Firefox")) {
            builder.append(" / Firefox");
        }

        return builder.toString();
    }
}
