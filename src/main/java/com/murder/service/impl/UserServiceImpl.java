package com.murder.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.constant.JwtClaimsConstant;
import com.murder.common.constant.MessageConstant;
import com.murder.common.constant.RedisConstant;
import com.murder.common.constant.StatusConstant;
import com.murder.common.exception.PasswordErrorException;
import com.murder.common.exception.UserNotFoundException;
import com.murder.common.properties.JwtProperties;
import com.murder.common.utils.JwtUtil;
import com.murder.dto.UserLoginDTO;
import com.murder.dto.UserRegisterDTO;
import com.murder.entity.User;
import com.murder.vo.UserLoginVO;
import com.murder.mapper.UserMapper;
import com.murder.service.UserService;
import com.murder.service.UserPointsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现?
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private JwtProperties jwtProperties;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private UserPointsService userPointsService;

    /**
     * 用户注册
     */
    @Override
    public void register(UserRegisterDTO userRegisterDTO) {
        if (!StringUtils.hasText(userRegisterDTO.getUsername())) {
            throw new RuntimeException("用户名不能为空");
        }
        if (!StringUtils.hasText(userRegisterDTO.getPassword())) {
            throw new RuntimeException("密码不能为空");
        }

        // 检查用户名是否已存?
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, userRegisterDTO.getUsername());
        User existUser = userMapper.selectOne(wrapper);
        
        if (existUser != null) {
            throw new RuntimeException(MessageConstant.USER_ALREADY_EXISTS);
        }
        
        // 创建新用?
        User user = User.builder()
                .username(userRegisterDTO.getUsername())
                .password(DigestUtils.md5DigestAsHex(userRegisterDTO.getPassword().getBytes()))
                .nickname(userRegisterDTO.getNickname())
                .phone(userRegisterDTO.getPhone())
                .memberLevel(1)
                .points(0)
                .status(StatusConstant.ENABLE)
                .build();
        
        userMapper.insert(user);
    }

    /**
     * 用户登录（仅限普通用户）
     */
    @Override
    public UserLoginVO login(UserLoginDTO userLoginDTO) {
        // 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, userLoginDTO.getUsername())
               .or()
               .eq(User::getPhone, userLoginDTO.getUsername());
        User user = userMapper.selectOne(wrapper);
        
        if (user == null) {
            throw new UserNotFoundException(MessageConstant.USER_NOT_FOUND);
        }
        
        // 检查用户角色 - 只允许普通用户登录
        if ("ADMIN".equals(user.getRole())) {
            throw new RuntimeException("管理员账号不能在用户端登录，请使用管理后台");
        }
        
        // 验证密码
        String password = DigestUtils.md5DigestAsHex(userLoginDTO.getPassword().getBytes());
        if (!password.equals(user.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        
        // 生成JWT令牌 - 使用用户密钥
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        claims.put(JwtClaimsConstant.USERNAME, user.getUsername());
        claims.put(JwtClaimsConstant.PHONE, user.getPhone());
        claims.put(JwtClaimsConstant.ROLE, user.getRole());
        
        String token = JwtUtil.createJWT(
                jwtProperties.getUserSecretKey(),
                jwtProperties.getUserTtl(),
                claims
        );
        
        log.info("用户登录成功: userId={}, username={}, role={}", 
                user.getId(), user.getUsername(), user.getRole());
        
        // 构建返回对象
        UserLoginVO userLoginVO = UserLoginVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .memberLevel(user.getMemberLevel())
                .points(user.getPoints())
                .token(token)
                .build();
        
        return userLoginVO;
    }
    
    /**
     * 管理员登录（仅限管理员）
     */
    public UserLoginVO adminLogin(UserLoginDTO userLoginDTO) {
        // 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, userLoginDTO.getUsername())
               .or()
               .eq(User::getPhone, userLoginDTO.getUsername());
        User user = userMapper.selectOne(wrapper);
        
        if (user == null) {
            throw new UserNotFoundException(MessageConstant.USER_NOT_FOUND);
        }
        
        // 检查用户角色 - 允许总部/门店管理员登录
        if (!("SUPER_ADMIN".equals(user.getRole()) || "STORE_ADMIN".equals(user.getRole()))) {
            throw new RuntimeException("该账号没有管理权限，无法登录管理后台");
        }

        // STORE_ADMIN 必须绑定门店
        if ("STORE_ADMIN".equals(user.getRole()) && user.getStoreId() == null) {
            throw new RuntimeException("门店管理员账号未绑定门店(storeId)，无法登录");
        }
        
        // 验证密码
        String password = DigestUtils.md5DigestAsHex(userLoginDTO.getPassword().getBytes());
        if (!password.equals(user.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        
        // 生成JWT令牌 - 使用管理员密钥
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        claims.put(JwtClaimsConstant.USERNAME, user.getUsername());
        claims.put(JwtClaimsConstant.PHONE, user.getPhone());
        claims.put(JwtClaimsConstant.ROLE, user.getRole());
        if (user.getStoreId() != null) {
            claims.put(JwtClaimsConstant.STORE_ID, user.getStoreId());
        }
        
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims
        );
        
        log.info("管理员登录成功: userId={}, username={}, role={}, storeId={}",
                user.getId(), user.getUsername(), user.getRole(), user.getStoreId());

        // 构建返回对象
        UserLoginVO userLoginVO = UserLoginVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .memberLevel(user.getMemberLevel())
                .points(user.getPoints())
                .token(token)
                .role(user.getRole())
                .storeId(user.getStoreId())
                .build();

        return userLoginVO;
    }

    /**
     * 根据ID获取用户信息
     */
    @Override
    @Cacheable(value = "user", key = "#id", unless = "#result == null")
    public UserLoginVO getUserById(Long id) {
        // 先从Redis缓存中获?
        String cacheKey = RedisConstant.USER_CACHE_PREFIX + id;
        UserLoginVO cachedUser = (UserLoginVO) redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedUser != null) {
            return cachedUser;
        }
        
        // 缓存未命中，从数据库查询
        User user = userMapper.selectById(id);
        
        if (user == null) {
            throw new UserNotFoundException(MessageConstant.USER_NOT_FOUND);
        }
        
        UserLoginVO userLoginVO = new UserLoginVO();
        BeanUtils.copyProperties(user, userLoginVO);
        
        // 存入缓存
        redisTemplate.opsForValue().set(cacheKey, userLoginVO, RedisConstant.CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        
        return userLoginVO;
    }

    /**
     * 分页查询用户列表
     */
    @Override
    public Page<User> pageQuery(Integer page, Integer pageSize, String username, String phone, Integer memberLevel) {
        Page<User> pageInfo = new Page<>(page, pageSize);
        
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(username != null && !username.isEmpty(), User::getUsername, username)
               .like(phone != null && !phone.isEmpty(), User::getPhone, phone)
               .eq(memberLevel != null, User::getMemberLevel, memberLevel)
               .eq(User::getIsDeleted, 0)
               .orderByDesc(User::getCreateTime);
        
        return userMapper.selectPage(pageInfo, wrapper);
    }

    /**
     * 修改密码
     */
    @Override
    @org.springframework.cache.annotation.CacheEvict(value = "user", key = "#user.id")
    public void updatePassword(User user) {
        // 获取用户信息
        User existUser = userMapper.selectById(user.getId());
        if (existUser == null) {
            throw new UserNotFoundException(MessageConstant.USER_NOT_FOUND);
        }
        
        // 加密新密?
        String encryptedPassword = DigestUtils.md5DigestAsHex(user.getPassword().getBytes());
        existUser.setPassword(encryptedPassword);
        
        // 更新密码
        userMapper.updateById(existUser);
        
        // 清除缓存
        String cacheKey = RedisConstant.USER_CACHE_PREFIX + user.getId();
        redisTemplate.delete(cacheKey);
    }
    
    /**
     * 完善资料奖励积分
     */
    @Override
    public void addPointsForProfile(Long userId) {
        try {
            userPointsService.addPoints(userId, 30, "完善个人资料");
            log.info("用户{}完善资料，获?0积分", userId);
        } catch (Exception e) {
            log.error("完善资料积分发放失败", e);
            throw e;
        }
    }
}
