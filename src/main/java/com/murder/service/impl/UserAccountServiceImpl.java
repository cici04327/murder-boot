package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.exception.PasswordErrorException;
import com.murder.common.exception.UserNotFoundException;
import com.murder.common.result.PageResult;
import com.murder.dto.BindEmailDTO;
import com.murder.dto.BindPhoneDTO;
import com.murder.dto.DeactivateAccountDTO;
import com.murder.dto.RealNameVerifyDTO;
import com.murder.dto.UpdatePasswordDTO;
import com.murder.entity.User;
import com.murder.entity.UserLoginLog;
import com.murder.mapper.UserLoginLogMapper;
import com.murder.mapper.UserMapper;
import com.murder.service.UserAccountService;
import com.murder.vo.UserLoginLogVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserAccountServiceImpl implements UserAccountService {

    private static final String PHONE_CODE_PREFIX = "user:code:phone:";
    private static final String EMAIL_CODE_PREFIX = "user:code:email:";
    private static final DateTimeFormatter LOGIN_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserLoginLogMapper userLoginLogMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private Environment environment;

    @Override
    public boolean updateCurrentUserProfile(Long userId, User user) {
        User existUser = requireUser(userId);

        boolean shouldReward = isProfileIncomplete(existUser);
        User updateUser = new User();
        updateUser.setId(userId);

        if (StringUtils.hasText(user.getUsername())) {
            String username = user.getUsername().trim();
            if (!Objects.equals(username, existUser.getUsername())) {
                ensureUsernameAvailable(username, userId);
            }
            updateUser.setUsername(username);
        }

        if (StringUtils.hasText(user.getNickname())) {
            updateUser.setNickname(user.getNickname().trim());
        }

        if (user.getGender() != null) {
            updateUser.setGender(user.getGender());
        }

        updateUser.setBirthday(user.getBirthday());

        if (user.getBio() != null) {
            updateUser.setBio(user.getBio().trim());
        }

        if (StringUtils.hasText(user.getAvatar())) {
            updateUser.setAvatar(user.getAvatar().trim());
        }

        if (StringUtils.hasText(user.getPhone())) {
            String phone = user.getPhone().trim();
            if (!Objects.equals(phone, existUser.getPhone())) {
                ensurePhoneAvailable(phone, userId);
                updateUser.setPhoneVerified(0);
            }
            updateUser.setPhone(phone);
        }

        if (StringUtils.hasText(user.getEmail())) {
            String email = user.getEmail().trim();
            if (!Objects.equals(email, existUser.getEmail())) {
                ensureEmailAvailable(email, userId);
                updateUser.setEmailVerified(0);
            }
            updateUser.setEmail(email);
        }

        userMapper.updateById(updateUser);

        User refreshedUser = userMapper.selectById(userId);
        boolean isNowComplete = refreshedUser != null
                && StringUtils.hasText(refreshedUser.getNickname())
                && StringUtils.hasText(refreshedUser.getPhone());
        return shouldReward && isNowComplete;
    }

    @Override
    public void updatePassword(Long userId, UpdatePasswordDTO dto) {
        User existUser = requireUser(userId);

        if (!StringUtils.hasText(dto.getOldPassword()) || !StringUtils.hasText(dto.getNewPassword())) {
            throw new IllegalArgumentException("原密码和新密码不能为空");
        }

        String oldEncrypted = DigestUtils.md5DigestAsHex(dto.getOldPassword().getBytes());
        if (!Objects.equals(existUser.getPassword(), oldEncrypted)) {
            throw new PasswordErrorException("原密码错误");
        }

        String newEncrypted = DigestUtils.md5DigestAsHex(dto.getNewPassword().getBytes());
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setPassword(newEncrypted);
        userMapper.updateById(updateUser);
    }

    @Override
    public Map<String, Object> sendPhoneCode(String phone) {
        if (!StringUtils.hasText(phone) || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new IllegalArgumentException("手机号格式不正确");
        }

        String code = generateCode();
        redisTemplate.opsForValue().set(PHONE_CODE_PREFIX + phone, code, 5, TimeUnit.MINUTES);
        log.info("已生成手机验证码: phone={}, code={}", phone, code);

        Map<String, Object> result = new HashMap<>();
        if (isDevelopmentProfile()) {
            result.put("debugCode", code);
        }
        return result;
    }

    @Override
    public void bindPhone(Long userId, BindPhoneDTO dto) {
        if (!StringUtils.hasText(dto.getPhone()) || !dto.getPhone().matches("^1[3-9]\\d{9}$")) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        verifyCode(PHONE_CODE_PREFIX + dto.getPhone(), dto.getCode());
        ensurePhoneAvailable(dto.getPhone().trim(), userId);

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setPhone(dto.getPhone().trim());
        updateUser.setPhoneVerified(1);
        userMapper.updateById(updateUser);
        redisTemplate.delete(PHONE_CODE_PREFIX + dto.getPhone());
    }

    @Override
    public Map<String, Object> sendEmailCode(String email) {
        if (!StringUtils.hasText(email) || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }

        String code = generateCode();
        redisTemplate.opsForValue().set(EMAIL_CODE_PREFIX + email, code, 5, TimeUnit.MINUTES);
        log.info("已生成邮箱验证码: email={}, code={}", email, code);

        Map<String, Object> result = new HashMap<>();
        if (isDevelopmentProfile()) {
            result.put("debugCode", code);
        }
        return result;
    }

    @Override
    public void bindEmail(Long userId, BindEmailDTO dto) {
        if (!StringUtils.hasText(dto.getEmail()) || !dto.getEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
        verifyCode(EMAIL_CODE_PREFIX + dto.getEmail(), dto.getCode());
        ensureEmailAvailable(dto.getEmail().trim(), userId);

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setEmail(dto.getEmail().trim());
        updateUser.setEmailVerified(1);
        userMapper.updateById(updateUser);
        redisTemplate.delete(EMAIL_CODE_PREFIX + dto.getEmail());
    }

    @Override
    public void verifyRealName(Long userId, RealNameVerifyDTO dto) {
        if (!StringUtils.hasText(dto.getRealName()) || !StringUtils.hasText(dto.getIdCard())) {
            throw new IllegalArgumentException("实名认证信息不完整");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getIdCard, dto.getIdCard().trim())
                .ne(User::getId, userId);
        if (userMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("该身份证号已绑定其他账号");
        }

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setRealName(dto.getRealName().trim());
        updateUser.setIdCard(dto.getIdCard().trim());
        updateUser.setRealNameVerified(1);
        userMapper.updateById(updateUser);
    }

    @Override
    public PageResult<UserLoginLogVO> getLoginLogs(Long userId, Integer page, Integer pageSize) {
        Page<UserLoginLog> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<UserLoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLoginLog::getUserId, userId)
                .orderByDesc(UserLoginLog::getLoginTime)
                .orderByDesc(UserLoginLog::getId);

        Page<UserLoginLog> result = userLoginLogMapper.selectPage(pageInfo, wrapper);
        List<UserLoginLogVO> records = result.getRecords().stream()
                .map(log -> UserLoginLogVO.builder()
                        .time(log.getLoginTime() != null ? log.getLoginTime().format(LOGIN_TIME_FORMATTER) : "")
                        .ip(log.getIp())
                        .location(log.getLocation())
                        .device(log.getDevice())
                        .status(log.getStatus())
                        .build())
                .collect(Collectors.toList());
        return new PageResult<>(result.getTotal(), records);
    }

    @Override
    public void recordLoginSuccess(Long userId, String ip, String location, String device) {
        if (userId == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        UserLoginLog loginLog = UserLoginLog.builder()
                .userId(userId)
                .ip(ip)
                .location(location)
                .device(device)
                .status("成功")
                .loginTime(now)
                .build();
        userLoginLogMapper.insert(loginLog);

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setLastLoginTime(now);
        userMapper.updateById(updateUser);
    }

    @Override
    public void deactivateAccount(Long userId, DeactivateAccountDTO dto) {
        User existUser = requireUser(userId);

        if (dto != null && StringUtils.hasText(dto.getConfirmText()) && !"确认注销".equals(dto.getConfirmText().trim())) {
            throw new IllegalArgumentException("确认信息不正确");
        }

        if (dto != null && StringUtils.hasText(dto.getPassword())) {
            String encrypted = DigestUtils.md5DigestAsHex(dto.getPassword().getBytes());
            if (!Objects.equals(existUser.getPassword(), encrypted)) {
                throw new PasswordErrorException("密码错误");
            }
        }

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId)
                .set(User::getStatus, 0)
                .set(User::getPhoneVerified, 0)
                .set(User::getEmailVerified, 0)
                .set(User::getRealNameVerified, 0)
                .set(User::getIsDeleted, 1)
                .set(User::getPhone, null)
                .set(User::getEmail, null);
        userMapper.update(null, updateWrapper);
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new UserNotFoundException("用户不存在");
        }
        return user;
    }

    private boolean isProfileIncomplete(User user) {
        return user != null
                && (!StringUtils.hasText(user.getNickname()) || !StringUtils.hasText(user.getPhone()));
    }

    private void ensureUsernameAvailable(String username, Long excludeUserId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username).ne(User::getId, excludeUserId);
        if (userMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }
    }

    private void ensurePhoneAvailable(String phone, Long excludeUserId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone).ne(User::getId, excludeUserId);
        if (userMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("手机号已绑定其他账号");
        }
    }

    private void ensureEmailAvailable(String email, Long excludeUserId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email).ne(User::getId, excludeUserId);
        if (userMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("邮箱已绑定其他账号");
        }
    }

    private void verifyCode(String key, String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("验证码不能为空");
        }
        Object cacheValue = redisTemplate.opsForValue().get(key);
        if (cacheValue == null) {
            throw new IllegalArgumentException("验证码已过期，请重新获取");
        }
        if (!Objects.equals(String.valueOf(cacheValue), code.trim())) {
            throw new IllegalArgumentException("验证码错误");
        }
    }

    private boolean isDevelopmentProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("dev".equalsIgnoreCase(profile) || "test".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return activeProfiles.length == 0;
    }

    private String generateCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }
}
