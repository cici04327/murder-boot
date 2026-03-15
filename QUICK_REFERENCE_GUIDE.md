# 剧本杀预约系统 - 快速参考指南

## 📖 项目快速导航

### 🚀 快速启动

1. **环境准备**
   ```bash
   # JDK 17+
   java -version
   
   # Maven 3.8+
   mvn -version
   
   # 启动依赖服务
   mysql --version        # MySQL 8.0+
   redis-server          # Redis
   ```

2. **启动应用**
   ```bash
   # 方式1: Maven运行
   mvn clean install
   mvn spring-boot:run
   
   # 方式2: IDE运行
   # 直接运行 MurderBootApplication.main()
   
   # 方式3: JAR运行
   java -jar murder-boot-1.0.0.jar
   ```

3. **验证启动**
   ```
   本地访问:    http://localhost:8080/
   API文档:     http://localhost:8080/doc.html
   Druid监控:   http://localhost:8080/druid (admin/admin)
   ```

---

## 📁 核心文件速查表

### 启动与配置

| 文件 | 路径 | 说明 |
|------|------|------|
| 启动类 | `MurderBootApplication.java` | Spring Boot应用入口 |
| 主配置 | `application.yml` | 主配置文件 |
| 开发配置 | `application-dev.yml` | 开发环境配置 |
| 生产配置 | `application-prod.yml` | 生产环境配置 |
| AI配置 | `ai-config.yml` | AI服务配置 |
| 日志配置 | `logback-spring.xml` | SLF4j日志配置 |
| Maven配置 | `pom.xml` | 项目依赖管理 |

### 业务核心类

| 功能 | Controller | Service | Entity |
|------|-----------|---------|--------|
| **用户管理** | UserController | UserService | User |
| **预约订单** | ReservationController | ReservationService | Reservation |
| **门店管理** | StoreController | StoreService | Store |
| **剧本管理** | ScriptController | ScriptService | Script |
| **优惠券** | CouponController | CouponService | Coupon |
| **VIP会员** | VipController | VipService | VipPackage |
| **支付** | PaymentController | PaymentService | - |
| **评价** | ReviewController | ReviewService | Review |
| **统计** | StatisticsController | StatisticsService | - |
| **推荐** | RecommendationController | RecommendationService | - |
| **AI客服** | AIController | AIService | - |

### 认证与授权

| 文件 | 路径 | 说明 |
|------|------|------|
| JWT配置 | `JwtProperties.java` | JWT密钥、过期时间 |
| 拦截器 | `JwtTokenInterceptor.java` | Token验证、权限检查 |
| 上下文 | `BaseContext.java` | ThreadLocal存储userId/role/storeId |
| 异常处理 | `GlobalExceptionHandler.java` | 全局异常处理 |
| JWT工具 | `JwtUtil.java` | JWT生成/验证 |

### WebSocket实时通信

| 文件 | 功能 |
|------|------|
| `NotificationWebSocketHandler.java` | 用户通知推送 |
| `AdminNotificationWebSocketHandler.java` | 管理员通知 |
| `ServiceWebSocketHandler.java` | 客服实时通信 |

---

## 🔑 关键业务代码片段

### 1. 用户登录流程

```java
// UserController.java
@PostMapping("/login")
public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO) {
    return Result.success(userService.login(userLoginDTO));
}

// UserServiceImpl.java
public UserLoginVO login(UserLoginDTO userLoginDTO) {
    // 1. 查询用户
    User user = userMapper.selectByUsername(userLoginDTO.getUsername());
    if (user == null) {
        throw new BaseException("用户不存在");
    }
    
    // 2. 验证密码
    if (!passwordEncoder.matches(userLoginDTO.getPassword(), user.getPassword())) {
        throw new PasswordErrorException("密码错误");
    }
    
    // 3. 生成JWT Token
    Map<String, Object> claims = new HashMap<>();
    claims.put(JwtClaimsConstant.USER_ID, user.getId());
    claims.put(JwtClaimsConstant.USERNAME, user.getUsername());
    claims.put(JwtClaimsConstant.ROLE, "USER");
    
    String token = JwtUtil.createJWT(
        jwtProperties.getUserSecretKey(),
        jwtProperties.getUserTtl(),
        claims
    );
    
    // 4. 返回登录信息
    return UserLoginVO.builder()
        .token(token)
        .userId(user.getId())
        .username(user.getUsername())
        .vipLevel(user.getVipLevel())
        .build();
}
```

### 2. 预约创建流程

```java
// ReservationController.java
@PostMapping
public Result<Reservation> create(@RequestBody ReservationDTO reservationDTO) {
    Long currentUserId = BaseContext.getCurrentId();
    reservationDTO.setUserId(currentUserId);
    return Result.success(reservationService.create(reservationDTO));
}

// ReservationServiceImpl.java
public Reservation create(ReservationDTO dto) {
    // 1. 验证房间可用性
    if (!checkRoomAvailability(dto.getRoomId(), dto.getReservationTime(), dto.getDuration())) {
        throw new BaseException("房间不可用");
    }
    
    // 2. 计算价格
    Script script = scriptService.getById(dto.getScriptId());
    BigDecimal totalPrice = script.getPrice().multiply(
        BigDecimal.valueOf(dto.getPlayerCount())
    );
    
    // 3. 处理优惠券
    BigDecimal discountAmount = BigDecimal.ZERO;
    if (dto.getUserCouponId() != null) {
        discountAmount = couponService.calculateDiscount(
            dto.getUserCouponId(), 
            totalPrice
        );
    }
    
    // 4. 处理VIP折扣
    BigDecimal vipDiscount = BigDecimal.ONE;
    if (userService.isVip(dto.getUserId())) {
        vipDiscount = vipService.getVipDiscount(dto.getUserId());
    }
    
    // 5. 计算最终价格
    BigDecimal vipDiscountAmount = totalPrice
        .subtract(discountAmount)
        .multiply(BigDecimal.ONE.subtract(vipDiscount));
    BigDecimal actualAmount = totalPrice
        .subtract(discountAmount)
        .subtract(vipDiscountAmount);
    
    // 6. 创建预约
    Reservation reservation = Reservation.builder()
        .orderNo(generateOrderNo())
        .userId(dto.getUserId())
        .storeId(dto.getStoreId())
        .roomId(dto.getRoomId())
        .scriptId(dto.getScriptId())
        .playerCount(dto.getPlayerCount())
        .reservationTime(dto.getReservationTime())
        .totalPrice(totalPrice)
        .couponId(dto.getUserCouponId())
        .discountAmount(discountAmount)
        .vipDiscount(vipDiscount)
        .vipDiscountAmount(vipDiscountAmount)
        .actualAmount(actualAmount)
        .contactName(dto.getContactName())
        .contactPhone(dto.getContactPhone())
        .status(1) // 待支付
        .payStatus(0) // 未支付
        .build();
    
    reservationMapper.insert(reservation);
    
    // 7. 更新排期人数
    if (dto.getScheduleId() != null) {
        scriptScheduleMapper.incrementCurrentPlayers(dto.getScheduleId());
    }
    
    // 8. 发送通知
    notificationService.sendReservationCreated(dto.getUserId(), reservation);
    
    return reservation;
}
```

### 3. JWT拦截器工作流程

```java
// JwtTokenInterceptor.java
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String token = request.getHeader(jwtProperties.getTokenName());
    String clientType = request.getHeader("X-Client-Type");
    
    // 智能选择密钥
    String secretKey;
    if ("admin".equals(clientType)) {
        secretKey = jwtProperties.getAdminSecretKey();
    } else {
        secretKey = jwtProperties.getUserSecretKey();
    }
    
    try {
        // 解析Token
        Claims claims = JwtUtil.parseJWT(secretKey, token);
        
        // 提取用户信息
        Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
        String role = claims.get(JwtClaimsConstant.ROLE).toString();
        Long storeId = claims.get(JwtClaimsConstant.STORE_ID) != null ? 
            Long.valueOf(claims.get(JwtClaimsConstant.STORE_ID).toString()) : null;
        
        // 存入线程本地
        BaseContext.setCurrentId(userId);
        BaseContext.setRole(role);
        BaseContext.setStoreId(storeId);
        
        // 权限检查
        if (request.getRequestURI().startsWith("/api/admin")) {
            if (!("SUPER_ADMIN".equals(role) || "STORE_ADMIN".equals(role))) {
                response.setStatus(403);
                response.getWriter().write("{\"code\":403,\"msg\":\"无管理权限\"}");
                return false;
            }
        }
        
        return true;
    } catch (Exception ex) {
        response.setStatus(401);
        response.getWriter().write("{\"code\":0,\"msg\":\"登录已过期\"}");
        return false;
    }
}
```

### 4. WebSocket推送通知

```java
// NotificationService.java
@Autowired
private NotificationWebSocketHandler webSocketHandler;

public void sendNotification(Long userId, Notification notification) {
    Map<String, Object> message = new HashMap<>();
    message.put("type", notification.getType());
    message.put("title", notification.getTitle());
    message.put("content", notification.getContent());
    message.put("data", notification.getData());
    message.put("timestamp", System.currentTimeMillis());
    
    webSocketHandler.pushNotification(userId, message);
}

// 前端接收
const ws = new WebSocket(`ws://localhost:8080/api/ws/notification?userId=${userId}`);

ws.onmessage = (event) => {
    const notification = JSON.parse(event.data);
    console.log('收到通知:', notification);
    // 更新UI
    showNotificationDialog(notification);
};

ws.onopen = () => {
    // 定期发送心跳
    setInterval(() => ws.send('ping'), 30000);
};
```

### 5. 定时任务示例

```java
// ReservationTask.java
@Component
@Slf4j
public class ReservationTask {
    
    @Autowired
    private ReservationService reservationService;
    
    // 每小时检查一次待支付预约
    @Scheduled(cron = "0 0 * * * *")
    public void checkUnpaidReservations() {
        log.info("执行定时任务: 检查待支付预约");
        
        LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(30);
        List<Reservation> unpaid = reservationService.getUnpaidReservations(timeoutTime);
        
        for (Reservation reservation : unpaid) {
            // 自动取消
            reservationService.cancel(reservation.getId(), "支付超时自动取消");
        }
        
        log.info("完成待支付预约检查, 共处理 {} 个", unpaid.size());
    }
    
    // 每天早上8点检查即将开始的预约
    @Scheduled(cron = "0 0 8 * * *")
    public void sendReminder() {
        log.info("执行定时任务: 发送预约提醒");
        
        List<Reservation> upcoming = reservationService.getUpcomingReservations(24);
        for (Reservation reservation : upcoming) {
            // 发送提醒通知
            notificationService.sendReminder(reservation.getUserId(), reservation);
        }
        
        log.info("完成预约提醒发送, 共发送 {} 条", upcoming.size());
    }
}
```

---

## 🗂️ 常用查询示例

### 分页查询

```java
// 预约列表 (用户端)
@GetMapping("/page")
public Result<PageResult<ReservationVO>> page(
    @RequestParam(defaultValue = "1") Integer page,
    @RequestParam(defaultValue = "10") Integer pageSize
) {
    Long userId = BaseContext.getCurrentId();
    
    PageResult<ReservationVO> result = reservationService.pageQueryWithDetails(
        page, pageSize, userId, null, null, null, null, null, null
    );
    
    return Result.success(result);
}

// 后端实现
public PageResult<ReservationVO> pageQueryWithDetails(
    Integer page, Integer pageSize, Long userId, ...) {
    
    Page<ReservationVO> pageInfo = reservationMapper.selectPage(
        new Page<>(page, pageSize),
        new QueryWrapper<Reservation>()
            .eq("user_id", userId)
            .orderByDesc("create_time")
    );
    
    return new PageResult<>(pageInfo.getRecords(), pageInfo.getTotal());
}
```

### 条件查询

```java
// 搜索门店
@GetMapping("/page")
public Result<PageResult<StoreVO>> pageQuery(
    @RequestParam(required = false) String name,
    @RequestParam(required = false) Double latitude,
    @RequestParam(required = false) Double longitude,
    @RequestParam(required = false) Integer rating
) {
    StoreQueryDTO queryDTO = StoreQueryDTO.builder()
        .name(name)
        .latitude(latitude)
        .longitude(longitude)
        .rating(rating)
        .build();
    
    return Result.success(storeService.pageQueryAdvanced(queryDTO));
}

// 后端实现
public PageResult<StoreVO> pageQueryAdvanced(StoreQueryDTO queryDTO) {
    QueryWrapper<Store> wrapper = new QueryWrapper<>();
    
    if (StringUtils.hasText(queryDTO.getName())) {
        wrapper.like("name", queryDTO.getName());
    }
    if (queryDTO.getRating() != null) {
        wrapper.ge("rating", queryDTO.getRating());
    }
    if (queryDTO.getStatus() != null) {
        wrapper.eq("status", queryDTO.getStatus());
    }
    
    wrapper.orderByDesc("rating", "create_time");
    
    Page<Store> pageInfo = storeMapper.selectPage(
        new Page<>(queryDTO.getPage(), queryDTO.getPageSize()),
        wrapper
    );
    
    return new PageResult<>(
        pageInfo.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList()),
        pageInfo.getTotal()
    );
}
```

---

## 📊 常用操作命令

### 数据库

```sql
-- 查看预约统计
SELECT status, COUNT(*) as count FROM reservation_order GROUP BY status;

-- 查看用户VIP分布
SELECT vip_level, COUNT(*) as count FROM user WHERE vip_level > 0 GROUP BY vip_level;

-- 查看优惠券使用情况
SELECT c.name, COUNT(*) as used_count 
FROM user_coupon uc 
JOIN coupon c ON uc.coupon_id = c.id 
WHERE uc.status = 1 
GROUP BY c.id;

-- 查看预约金额统计
SELECT DATE(create_time) as date, SUM(actual_amount) as revenue 
FROM reservation_order 
WHERE status = 3 
GROUP BY DATE(create_time) 
ORDER BY date DESC;
```

### Redis

```bash
# 查看所有缓存键
redis-cli KEYS '*'

# 查看特定用户缓存
redis-cli GET user:123:profile

# 查看热门剧本缓存
redis-cli ZRANGE script:hot:ranking 0 -1

# 清除所有缓存
redis-cli FLUSHDB

# 检查缓存大小
redis-cli DBSIZE
```

### cURL API测试

```bash
# 用户登录
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"123456"}'

# 创建预约
curl -X POST http://localhost:8080/api/reservation \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "storeId": 1,
    "roomId": 1,
    "scriptId": 1,
    "playerCount": 4,
    "reservationTime": "2026-02-15 14:00",
    "totalPrice": 299.00
  }'

# 查询预约
curl -X GET "http://localhost:8080/api/reservation/page?page=1&pageSize=10" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 支付预约
curl -X PUT http://localhost:8080/api/reservation/123/pay \
  -H "Authorization: Bearer YOUR_TOKEN"

# 核销预约
curl -X PUT http://localhost:8080/api/reservation/123/check-in \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"checkInCode":"ABC123"}'
```

---

## 🔍 常见问题排查

### 1. Token过期
**症状**: 401 "登录已过期"  
**解决**:
```java
// 在前端刷新Token
POST /api/auth/refresh
Authorization: Bearer OLD_TOKEN

// 或重新登录获取新Token
POST /api/user/login
```

### 2. 权限不足
**症状**: 403 "没有管理端访问权限"  
**解决**:
- 检查用户role是否为SUPER_ADMIN或STORE_ADMIN
- 确保请求头包含 X-Client-Type: admin
- 检查用户是否被禁用

### 3. 数据库连接失败
**症状**: DataAccessException  
**解决**:
```bash
# 检查MySQL连接
mysql -h localhost -u root -p murder

# 检查application.yml配置
spring.datasource.url
spring.datasource.username
spring.datasource.password
```

### 4. Redis连接失败
**症状**: RedisConnectionException  
**解决**:
```bash
# 检查Redis服务
redis-cli ping
# 应返回: PONG

# 检查application.yml配置
spring.data.redis.host
spring.data.redis.port
spring.data.redis.database
```

### 5. WebSocket连接失败
**症状**: WebSocket连接错误  
**解决**:
```javascript
// 检查URL格式
const userId = localStorage.getItem('userId');
const ws = new WebSocket(
  `ws://localhost:8080/api/ws/notification?userId=${userId}`
);

// 检查CORS配置
// 确保 WebMvcConfig 中 addCorsMapping 已配置
```

---

## 📚 推荐阅读顺序

1. **项目摘要** → `PROJECT_STRUCTURE_SUMMARY.md`
2. **架构设计** → `ARCHITECTURE_DIAGRAM.md` (当前文件)
3. **快速参考** → `QUICK_REFERENCE_GUIDE.md` (当前文件)
4. **源代码探索** → 按模块查看源代码
   - 认证: `JwtTokenInterceptor.java`, `UserController.java`
   - 预约: `ReservationController.java`, `ReservationServiceImpl.java`
   - 门店: `StoreController.java`, `StoreServiceImpl.java`
   - 营销: `CouponService.java`, `VipService.java`
5. **API文档** → `http://localhost:8080/doc.html`

---

## 🚀 下一步建议

1. **部署**
   - 配置生产环境 `application-prod.yml`
   - 修改支付宝配置
   - 部署前端应用

2. **功能扩展**
   - 添加更多AI功能
   - 实现更复杂的推荐算法
   - 添加数据分析报表

3. **性能优化**
   - 优化数据库查询（添加索引）
   - 扩展缓存策略
   - 实现消息队列处理

4. **安全加固**
   - 实现速率限制
   - 添加审计日志
   - 加密敏感数据

---

**更新日期**: 2025-01-15  
**版本**: 1.0.0

