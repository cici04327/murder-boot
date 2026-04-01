package com.murder.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.murder.common.config.AlipayConfig;
import com.murder.common.result.PageResult;
import com.murder.dto.VipPackageDTO;
import com.murder.entity.User;
import com.murder.entity.UserVip;
import com.murder.entity.VipPackage;
import com.murder.entity.Coupon;
import com.murder.entity.UserCoupon;
import com.murder.mapper.CouponMapper;
import com.murder.mapper.UserMapper;
import com.murder.mapper.UserCouponMapper;
import com.murder.mapper.UserVipMapper;
import com.murder.mapper.VipPackageMapper;
import com.murder.service.CouponService;
import com.murder.service.VipService;
import com.murder.vo.VipPackageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * VIP服务实现?
 */
@Service
@Slf4j
public class VipServiceImpl implements VipService {

    @Autowired
    private VipPackageMapper vipPackageMapper;

    @Autowired
    private UserVipMapper userVipMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired(required = false)
    private CouponService couponService;

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private AlipayConfig alipayConfig;

    @Autowired(required = false)
    private com.murder.service.NotificationService notificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<VipPackageVO> getVipPackages() {
        LambdaQueryWrapper<VipPackage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VipPackage::getStatus, 1) // 只查询上架的
                .orderByAsc(VipPackage::getSortOrder);

        List<VipPackage> packages = vipPackageMapper.selectList(wrapper);
        return packages.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public VipPackageVO getVipPackageById(Long id) {
        VipPackage vipPackage = vipPackageMapper.selectById(id);
        if (vipPackage == null) {
            throw new RuntimeException("VIP套餐不存在");
        }
        return convertToVO(vipPackage);
    }

    @Override
    @Transactional
    public String purchaseVip(Long userId, Long packageId, String paymentMethod) {
        VipPackage vipPackage = vipPackageMapper.selectById(packageId);
        if (vipPackage == null || vipPackage.getStatus() != 1) {
            throw new RuntimeException("VIP套餐不存在或已下架");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        String normalizedMethod = normalizePaymentMethod(paymentMethod);
        if (!"ALIPAY".equals(normalizedMethod)) {
            throw new RuntimeException("VIP当前仅支持支付宝支付");
        }

        LocalDateTime provisionalStartTime = LocalDateTime.now();
        UserVip currentVip = userVipMapper.getCurrentVip(userId);
        if (currentVip != null && currentVip.getEndTime() != null && currentVip.getEndTime().isAfter(provisionalStartTime)) {
            provisionalStartTime = currentVip.getEndTime();
        }
        LocalDateTime provisionalEndTime = provisionalStartTime.plusDays(vipPackage.getDurationDays());

        UserVip userVip = UserVip.builder()
                .userId(userId)
                .packageId(packageId)
                .level(vipPackage.getLevel())
                .startTime(provisionalStartTime)
                .endTime(provisionalEndTime)
                .originalPrice(vipPackage.getOriginalPrice())
                .actualPrice(vipPackage.getCurrentPrice())
                .paymentMethod(normalizedMethod)
                .orderNo(generateOrderNo(userId))
                .status(0)
                .autoRenew(0)
                .build();

        userVipMapper.insert(userVip);

        log.info("用户 {} 创建VIP待支付订单成功，套餐：{}，订单号：{}", userId, vipPackage.getName(), userVip.getOrderNo());
        return createAlipayPayment(userVip, vipPackage);
    }

    @Override
    @Transactional
    public String handleAlipayNotify(Map<String, String> params) {
        try {
            UserVip vipOrder = verifyAndLoadVipOrder(params);
            if (!isAlipayTradeSuccess(params.get("trade_status"))) {
                return "success";
            }

            if (vipOrder != null && Integer.valueOf(0).equals(vipOrder.getStatus())) {
                activateVipOrder(vipOrder, params.get("trade_no"));
            }
            return "success";
        } catch (Exception e) {
            log.error("处理VIP支付宝异步通知失败", e);
            return "fail";
        }
    }

    @Override
    @Transactional
    public String handleAlipayReturn(Map<String, String> params) {
        try {
            UserVip vipOrder = verifyAndLoadVipOrder(params);
            if (vipOrder == null) {
                return buildVipResultRedirect(null, false, "未找到对应VIP订单");
            }
            if (!isAlipayReturnSuccess(params, vipOrder)) {
                return buildVipResultRedirect(vipOrder, false, "支付宝支付结果确认中，请稍后查看会员状态");
            }

            if (Integer.valueOf(0).equals(vipOrder.getStatus())) {
                activateVipOrder(vipOrder, params.get("trade_no"));
            }
            return buildVipResultRedirect(vipOrder, true, null);
        } catch (Exception e) {
            log.error("处理VIP支付宝同步回跳失败", e);
            return buildVipResultRedirect(null, false, e.getMessage());
        }
    }

    private String createAlipayPayment(UserVip userVip, VipPackage vipPackage) {
        try {
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setNotifyUrl(getVipNotifyUrl());
            request.setReturnUrl(getVipReturnUrl());

            AlipayTradePagePayModel model = new AlipayTradePagePayModel();
            model.setOutTradeNo(userVip.getOrderNo());
            model.setTotalAmount(defaultAmount(userVip.getActualPrice()).toString());
            model.setSubject("VIP会员购买 - " + vipPackage.getName());
            model.setBody("VIP套餐：" + vipPackage.getName() + "，有效期" + vipPackage.getDurationDays() + "天");
            model.setProductCode("FAST_INSTANT_TRADE_PAY");
            request.setBizModel(model);

            return alipayClient.pageExecute(request).getBody();
        } catch (AlipayApiException e) {
            log.error("创建VIP支付宝支付订单失败", e);
            throw new RuntimeException("创建VIP支付订单失败: " + e.getMessage(), e);
        }
    }

    private UserVip verifyAndLoadVipOrder(Map<String, String> params) {
        try {
            boolean signVerified = com.alipay.api.internal.util.AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getPublicKey(),
                    alipayConfig.getCharset(),
                    alipayConfig.getSignType()
            );
            if (!signVerified) {
                throw new RuntimeException("支付宝验签失败");
            }

            String outTradeNo = params.get("out_trade_no");
            if (!StringUtils.hasText(outTradeNo)) {
                throw new RuntimeException("支付宝回调缺少商户订单号");
            }

            UserVip vipOrder = userVipMapper.selectOne(
                    new LambdaQueryWrapper<UserVip>().eq(UserVip::getOrderNo, outTradeNo)
            );
            if (vipOrder == null) {
                throw new RuntimeException("未找到对应VIP订单");
            }

            String callbackAppId = params.get("app_id");
            if (StringUtils.hasText(callbackAppId) && StringUtils.hasText(alipayConfig.getAppId())
                    && !callbackAppId.equals(alipayConfig.getAppId())) {
                throw new RuntimeException("支付宝回调应用ID不匹配");
            }

            String totalAmount = params.get("total_amount");
            if (StringUtils.hasText(totalAmount)) {
                BigDecimal callbackAmount = new BigDecimal(totalAmount);
                BigDecimal orderAmount = defaultAmount(vipOrder.getActualPrice());
                if (callbackAmount.compareTo(orderAmount) != 0) {
                    throw new RuntimeException("支付宝回调金额与VIP订单金额不一致");
                }
            }

            return vipOrder;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("支付宝验签失败", e);
        }
    }

    private void activateVipOrder(UserVip vipOrder, String tradeNo) {
        if (vipOrder == null) {
            throw new RuntimeException("VIP订单不存在");
        }
        if (Integer.valueOf(1).equals(vipOrder.getStatus())) {
            return;
        }
        if (!Integer.valueOf(0).equals(vipOrder.getStatus())) {
            throw new RuntimeException("当前VIP订单状态不支持激活");
        }

        VipPackage vipPackage = vipPackageMapper.selectById(vipOrder.getPackageId());
        if (vipPackage == null || vipPackage.getStatus() != 1) {
            throw new RuntimeException("VIP套餐不存在或已下架");
        }

        LocalDateTime now = LocalDateTime.now();
        // 使用 getLatestPaidVip 查找已激活的endTime最晚的VIP（包含未来生效的续费订单），
        // 避免 getCurrentVip 只查 start_time <= now 导致多次续费时天数不叠加的问题
        UserVip latestPaidVip = getLatestPaidVip(vipOrder.getUserId());
        // 排除当前正在激活的订单本身（status=0，不在 latestPaidVip 查询范围内，所以无需排除）
        LocalDateTime startTime = now;
        if (latestPaidVip != null && latestPaidVip.getEndTime() != null && latestPaidVip.getEndTime().isAfter(now)) {
            startTime = latestPaidVip.getEndTime();
        }
        LocalDateTime endTime = startTime.plusDays(vipPackage.getDurationDays());

        vipOrder.setStartTime(startTime);
        vipOrder.setEndTime(endTime);
        vipOrder.setStatus(1);
        vipOrder.setTransactionId(tradeNo);
        userVipMapper.updateById(vipOrder);

        refreshUserVipState(vipOrder.getUserId());
        if (!startTime.isAfter(now)) {
            grantMonthlyCoupons(vipOrder.getUserId());
        }

        log.info("VIP订单支付成功并激活：userId={}, orderNo={}, startTime={}, endTime={}",
                vipOrder.getUserId(), vipOrder.getOrderNo(), startTime, endTime);
    }

    private boolean isAlipayTradeSuccess(String tradeStatus) {
        return "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
    }

    private boolean isAlipayReturnSuccess(Map<String, String> params, UserVip vipOrder) {
        if (isAlipayTradeSuccess(params.get("trade_status"))) {
            return true;
        }
        if (vipOrder != null && Integer.valueOf(1).equals(vipOrder.getStatus())) {
            return true;
        }
        return StringUtils.hasText(params.get("trade_no"));
    }

    private String buildVipResultRedirect(UserVip vipOrder, boolean success, String message) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getVipResultUrl())
                .queryParam("vipPay", success ? "success" : "fail");
        if (vipOrder != null) {
            builder.queryParam("vipOrderNo", vipOrder.getOrderNo());
            builder.queryParam("packageId", vipOrder.getPackageId());
        }
        if (StringUtils.hasText(message)) {
            builder.queryParam("message", message);
        }
        return builder.build().encode().toUriString();
    }

    private String getVipNotifyUrl() {
        return replacePath(alipayConfig.getSanitizedNotifyUrl(), "/api/vip/payment/notify");
    }

    private String getVipReturnUrl() {
        return replacePath(alipayConfig.getSanitizedReturnUrl(), "/api/vip/payment/return");
    }

    private String getVipResultUrl() {
        String resultUrl = alipayConfig.getSanitizedResultUrl();
        if (!StringUtils.hasText(resultUrl)) {
            return "http://localhost:3001/vip";
        }
        try {
            return UriComponentsBuilder.fromUriString(resultUrl)
                    .replacePath("/vip")
                    .build()
                    .toUriString();
        } catch (Exception e) {
            log.warn("构建VIP支付结果跳转地址失败，使用默认地址: {}", resultUrl, e);
            return "http://localhost:3001/vip";
        }
    }

    private String replacePath(String sourceUrl, String targetPath) {
        String sanitizedUrl = alipayConfig.sanitizeUrl(sourceUrl);
        if (!StringUtils.hasText(sanitizedUrl)) {
            return "http://localhost:8080" + targetPath;
        }
        try {
            return UriComponentsBuilder.fromUriString(sanitizedUrl)
                    .replacePath(targetPath)
                    .build()
                    .toUriString();
        } catch (Exception e) {
            log.warn("替换VIP支付回调地址失败，sourceUrl={}, targetPath={}", sanitizedUrl, targetPath, e);
            return "http://localhost:8080" + targetPath;
        }
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        BigDecimal normalized = amount == null ? BigDecimal.ZERO : amount.max(BigDecimal.ZERO);
        return normalized.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizePaymentMethod(String paymentMethod) {
        if (!StringUtils.hasText(paymentMethod)) {
            return "ALIPAY";
        }
        return paymentMethod.trim().toUpperCase(Locale.ROOT);
    }

    private void refreshUserVipState(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }

        UserVip currentVip = userVipMapper.getCurrentVip(userId);
        UserVip latestVip = getLatestPaidVip(userId);
        if (latestVip == null) {
            user.setVipLevel(0);
            user.setVipExpireTime(null);
        } else {
            UserVip effectiveVip = currentVip != null ? currentVip : latestVip;
            user.setVipLevel(effectiveVip.getLevel());
            user.setVipExpireTime(latestVip.getEndTime());
        }
        userMapper.updateById(user);
    }

    @Override
    public Map<String, Object> getUserVipInfo(Long userId) {
        Map<String, Object> result = new HashMap<>();

        UserVip currentVip = userVipMapper.getCurrentVip(userId);
        UserVip latestVip = getLatestPaidVip(userId);
        User user = userMapper.selectById(userId);

        if (latestVip == null || LocalDateTime.now().isAfter(latestVip.getEndTime())) {
            // 非VIP或已过期
            result.put("isVip", false);
            result.put("level", 0);
            result.put("levelName", "普通用户");
            result.put("expireTime", null);
            result.put("daysLeft", 0);
            return result;
        }

        UserVip displayVip = currentVip != null ? currentVip : latestVip;
        VipPackage vipPackage = vipPackageMapper.selectById(displayVip.getPackageId());

        long daysLeft = calculateDaysRemaining(latestVip.getEndTime());
        
        result.put("isVip", true);
        result.put("level", displayVip.getLevel());
        result.put("levelName", getLevelName(displayVip.getLevel()));
        result.put("startTime", displayVip.getStartTime());
        result.put("endTime", latestVip.getEndTime());
        result.put("expireTime", latestVip.getEndTime()); // 前端展示续费后的最晚到期时间
        result.put("daysLeft", daysLeft); // 前端使用daysLeft
        result.put("daysRemaining", daysLeft); // 保留兼容

        if (vipPackage != null) {
            result.put("pointMultiplier", vipPackage.getPointMultiplier());
            result.put("couponCount", vipPackage.getCouponCount());
            result.put("priorityBooking", vipPackage.getPriorityBooking() == 1);
            result.put("exclusiveService", vipPackage.getExclusiveService() == 1);
            // 按等级返回固定折扣，不再依赖数据库字段
            result.put("specialDiscount", LEVEL_DISCOUNT_MAP.get(displayVip.getLevel()));

            // 解析权益列表
            try {
                List<String> features = parseFeatures(vipPackage.getFeatures());
                result.put("features", features);
            } catch (Exception e) {
                log.error("解析VIP权益失败", e);
                result.put("features", new ArrayList<>());
            }
        }

        log.info("返回用户 {} 的VIP信息：level={}, daysLeft={}, expireTime={}",
                userId, displayVip.getLevel(), daysLeft, latestVip.getEndTime());

        return result;
    }

    private UserVip getLatestPaidVip(Long userId) {
        if (userId == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<UserVip> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserVip::getUserId, userId)
                .eq(UserVip::getStatus, 1)
                .gt(UserVip::getEndTime, now)
                .orderByDesc(UserVip::getEndTime)
                .last("LIMIT 1");
        return userVipMapper.selectOne(wrapper);
    }

    @Override
    public boolean isVip(Long userId) {
        UserVip currentVip = userVipMapper.getCurrentVip(userId);
        return currentVip != null && LocalDateTime.now().isBefore(currentVip.getEndTime());
    }

    @Override
    public Integer getUserVipLevel(Long userId) {
        UserVip currentVip = userVipMapper.getCurrentVip(userId);
        if (currentVip == null) {
            return 0;
        }
        return currentVip.getLevel() != null ? currentVip.getLevel() : 0;
    }

    @Override
    public BigDecimal getPointMultiplier(Long userId) {
        UserVip currentVip = userVipMapper.getCurrentVip(userId);
        if (currentVip == null) {
            return BigDecimal.ONE;
        }

        VipPackage vipPackage = vipPackageMapper.selectById(currentVip.getPackageId());
        if (vipPackage == null || vipPackage.getPointMultiplier() == null) {
            return BigDecimal.ONE;
        }

        return vipPackage.getPointMultiplier();
    }

    /**
     * 按会员等级固定折扣：
     * 1-见习侦探 9.5折(0.95)
     * 2-银章侦探 9折(0.90)
     * 3-金章侦探 8.5折(0.85)
     * 4-传奇侦探 8折(0.80)
     */
    private static final Map<Integer, BigDecimal> LEVEL_DISCOUNT_MAP;
    static {
        LEVEL_DISCOUNT_MAP = new HashMap<>();
        LEVEL_DISCOUNT_MAP.put(1, new BigDecimal("0.95"));
        LEVEL_DISCOUNT_MAP.put(2, new BigDecimal("0.90"));
        LEVEL_DISCOUNT_MAP.put(3, new BigDecimal("0.85"));
        LEVEL_DISCOUNT_MAP.put(4, new BigDecimal("0.80"));
    }

    @Override
    public BigDecimal getVipDiscount(Long userId) {
        UserVip currentVip = userVipMapper.getCurrentVip(userId);
        if (currentVip == null || LocalDateTime.now().isAfter(currentVip.getEndTime())) {
            return null;
        }
        return LEVEL_DISCOUNT_MAP.get(currentVip.getLevel());
    }

    @Override
    public boolean hasPriorityBooking(Long userId) {
        UserVip currentVip = userVipMapper.getCurrentVip(userId);
        if (currentVip == null) {
            return false;
        }

        VipPackage vipPackage = vipPackageMapper.selectById(currentVip.getPackageId());
        return vipPackage != null && vipPackage.getPriorityBooking() == 1;
    }

    /**
     * 各VIP等级月度体验券配置：
     * 见习侦探(1)：2张 × 10元
     * 银章侦探(2)：5张 × 20元
     * 金章侦探(3)：10张 × 50元
     * 传奇侦探(4)：15张 × 100元
     */
    private static final Map<Integer, int[]> LEVEL_MONTHLY_COUPON_MAP;
    static {
        LEVEL_MONTHLY_COUPON_MAP = new HashMap<>();
        LEVEL_MONTHLY_COUPON_MAP.put(1, new int[]{2, 10});   // 见习：2张10元
        LEVEL_MONTHLY_COUPON_MAP.put(2, new int[]{5, 20});   // 银章：5张20元
        LEVEL_MONTHLY_COUPON_MAP.put(3, new int[]{10, 50});  // 金章：10张50元
        LEVEL_MONTHLY_COUPON_MAP.put(4, new int[]{15, 100}); // 传奇：15张100元
    }

    @Override
    @Transactional
    public void grantMonthlyCoupons(Long userId) {
        UserVip currentVip = userVipMapper.getCurrentVip(userId);
        if (currentVip == null || LocalDateTime.now().isAfter(currentVip.getEndTime())) {
            log.warn("用户 {} 不是VIP或已过期，无法发放月度体验券", userId);
            return;
        }

        int level = currentVip.getLevel();
        int[] config = LEVEL_MONTHLY_COUPON_MAP.get(level);
        if (config == null) {
            log.warn("未知VIP等级 {}，跳过发放", level);
            return;
        }
        int couponCount = config[0];
        int couponAmount = config[1];

        // 防重复发放：检查本月是否已发放过月度体验券
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime monthEnd = monthStart.plusMonths(1);
        Coupon template = getOrCreateVipCouponTemplate(level, couponAmount, "月度体验券");
        if (template == null) {
            log.error("无法获取VIP月度体验券模板，userId={}, level={}", userId, level);
            return;
        }
        LambdaQueryWrapper<UserCoupon> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(UserCoupon::getUserId, userId)
                    .eq(UserCoupon::getCouponId, template.getId())
                    .ge(UserCoupon::getReceiveTime, monthStart)
                    .lt(UserCoupon::getReceiveTime, monthEnd);
        Long alreadyGranted = userCouponMapper.selectCount(checkWrapper);
        if (alreadyGranted > 0) {
            log.info("用户 {} 本月月度体验券已发放（{}张），跳过重复发放", userId, alreadyGranted);
            return;
        }

        try {
            // 体验券有效期：当月最后一天23:59:59（用完即止，当月有效）
            LocalDateTime expireTime = monthEnd.minusSeconds(1);
            LocalDateTime now = LocalDateTime.now();

            for (int i = 0; i < couponCount; i++) {
                UserCoupon userCoupon = UserCoupon.builder()
                        .userId(userId)
                        .couponId(template.getId())
                        .status(1) // 未使用
                        .receiveTime(now)
                        .expireTime(expireTime)
                        .build();
                userCouponMapper.insert(userCoupon);
            }
            log.info("为VIP用户 {} （{}）成功发放 {} 张月度体验券，面值{}元/张，有效期至{}",
                    userId, getLevelName(level), couponCount, couponAmount, expireTime);

            // 推送站内通知
            if (notificationService != null) {
                String title = "🎁 月度体验券已到账";
                String content = String.format(
                        "您好！作为%s会员，本月 %d 张面值 %d 元的体验券已发放至您的账户，" +
                        "有效期至%d月%d日，请及时使用～",
                        getLevelName(level), couponCount, couponAmount,
                        expireTime.getMonthValue(), expireTime.getDayOfMonth());
                try {
                    notificationService.sendToUsers(title, content, 2, "VIP_MONTHLY_COUPON", null, userId);
                    log.info("已向用户 {} 推送月度体验券到账通知", userId);
                } catch (Exception ne) {
                    // 通知失败不影响主流程
                    log.warn("推送月度体验券通知失败，userId={}", userId, ne);
                }
            }
        } catch (Exception e) {
            log.error("为VIP用户发放月度体验券失败，userId={}", userId, e);
            throw e;
        }
    }

    /**
     * 获取或创建VIP专属优惠券模板
     */
    private Coupon getOrCreateVipCouponTemplate(int level, int amount, String typeName) {
        String couponName = "VIP" + getLevelName(level) + typeName;
        // 查找已有的VIP优惠券模板
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Coupon::getName, couponName).eq(Coupon::getStatus, 1).last("LIMIT 1");
        Coupon coupon = couponMapper.selectOne(wrapper);
        if (coupon != null) return coupon;

        // 不存在则自动创建
        coupon = new Coupon();
        coupon.setName(couponName);
        coupon.setType(1); // 满减券
        coupon.setDiscountValue(new BigDecimal(amount));
        coupon.setMinAmount(new BigDecimal(amount)); // 面额即最低消费
        coupon.setTotalCount(99999);
        coupon.setRemainCount(99999);
        coupon.setStatus(1);
        coupon.setDescription("VIP专属" + typeName + "，面值" + amount + "元，30天内有效");
        coupon.setValidEndTime(LocalDateTime.now().plusYears(1));
        couponMapper.insert(coupon);
        log.info("自动创建VIP优惠券模板：{}", couponName);
        return coupon;
    }

    /**
     * 为生日月VIP用户发放生日专属优惠券
     */
    @Transactional
    public void grantBirthdayCoupon(Long userId) {
        UserVip currentVip = userVipMapper.getCurrentVip(userId);
        if (currentVip == null) return;

        VipPackage vipPackage = vipPackageMapper.selectById(currentVip.getPackageId());
        if (vipPackage == null || vipPackage.getBirthdayGift() == null || vipPackage.getBirthdayGift() != 1) {
            return;
        }

        // 生日券面额：等级越高越大
        int birthdayAmount;
        switch (currentVip.getLevel()) {
            case 1: birthdayAmount = 30; break;
            case 2: birthdayAmount = 80; break;
            case 3: birthdayAmount = 150; break;
            case 4: birthdayAmount = 200; break;
            default: birthdayAmount = 30;
        }

        try {
            Coupon template = getOrCreateVipCouponTemplate(currentVip.getLevel(), birthdayAmount, "生日专享券");
            if (template == null) return;

            LocalDateTime now = LocalDateTime.now();
            // 生日券有效期：当月最后一天
            LocalDateTime expireTime = now.withDayOfMonth(1).plusMonths(1).minusSeconds(1);

            UserCoupon userCoupon = UserCoupon.builder()
                    .userId(userId)
                    .couponId(template.getId())
                    .status(1)
                    .receiveTime(now)
                    .expireTime(expireTime)
                    .build();
            userCouponMapper.insert(userCoupon);
            log.info("为VIP用户 {} 发放生日专享券，面值{}元，有效至{}", userId, birthdayAmount, expireTime);
        } catch (Exception e) {
            log.error("发放生日券失败，userId={}", userId, e);
        }
    }

    @Override
    public Map<String, Object> getMonthlyCouponStatus(Long userId) {
        Map<String, Object> result = new HashMap<>();
        UserVip currentVip = userVipMapper.getCurrentVip(userId);
        if (currentVip == null || LocalDateTime.now().isAfter(currentVip.getEndTime())) {
            result.put("isVip", false);
            result.put("grantedCount", 0);
            result.put("totalCount", 0);
            result.put("couponAmount", 0);
            result.put("nextGrantTime", null);
            return result;
        }

        int level = currentVip.getLevel();
        int[] config = LEVEL_MONTHLY_COUPON_MAP.get(level);
        if (config == null) {
            result.put("isVip", true);
            result.put("grantedCount", 0);
            result.put("totalCount", 0);
            result.put("couponAmount", 0);
            result.put("nextGrantTime", null);
            return result;
        }
        int couponCount = config[0];
        int couponAmount = config[1];

        // 查本月已发张数
        LocalDateTime monthStart = LocalDateTime.now()
                .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime monthEnd = monthStart.plusMonths(1);

        Coupon template = getOrCreateVipCouponTemplate(level, couponAmount, "月度体验券");
        long grantedCount = 0;
        if (template != null) {
            LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserCoupon::getUserId, userId)
                   .eq(UserCoupon::getCouponId, template.getId())
                   .ge(UserCoupon::getReceiveTime, monthStart)
                   .lt(UserCoupon::getReceiveTime, monthEnd);
            grantedCount = userCouponMapper.selectCount(wrapper);
        }

        // 下次发放时间：下月1日凌晨1点
        LocalDateTime nextGrantTime = monthEnd.withHour(1).withMinute(0).withSecond(0).withNano(0);

        result.put("isVip", true);
        result.put("level", level);
        result.put("levelName", getLevelName(level));
        result.put("grantedCount", grantedCount);
        result.put("totalCount", couponCount);
        result.put("couponAmount", couponAmount);
        result.put("alreadyGranted", grantedCount >= couponCount);
        result.put("nextGrantTime", nextGrantTime);
        // 距离下次发放的秒数（前端倒计时用）
        long secondsUntilNext = java.time.Duration.between(LocalDateTime.now(), nextGrantTime).getSeconds();
        result.put("secondsUntilNext", Math.max(0, secondsUntilNext));
        return result;
    }

    @Override
    @Transactional
    public void adminGrantMonthlyCoupons(Long userId, int year, int month, String reason) {
        UserVip currentVip = userVipMapper.getCurrentVip(userId);
        if (currentVip == null) {
            throw new RuntimeException("用户 " + userId + " 当前不是VIP，无法补发");
        }

        int level = currentVip.getLevel();
        int[] config = LEVEL_MONTHLY_COUPON_MAP.get(level);
        if (config == null) {
            throw new RuntimeException("未知VIP等级：" + level);
        }
        int couponCount = config[0];
        int couponAmount = config[1];

        Coupon template = getOrCreateVipCouponTemplate(level, couponAmount, "月度体验券");
        if (template == null) {
            throw new RuntimeException("无法获取体验券模板");
        }

        // 补发的有效期：指定月份最后一天23:59:59
        LocalDateTime specMonthStart = LocalDateTime.of(year, month, 1, 0, 0, 0);
        LocalDateTime expireTime = specMonthStart.plusMonths(1).minusSeconds(1);
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < couponCount; i++) {
            UserCoupon uc = UserCoupon.builder()
                    .userId(userId)
                    .couponId(template.getId())
                    .status(1)
                    .receiveTime(now)
                    .expireTime(expireTime)
                    .build();
            userCouponMapper.insert(uc);
        }

        // 推送补发通知
        if (notificationService != null) {
            String title = "🎁 月度体验券已补发";
            String content = String.format(
                    "您好！管理员已为您补发 %d 月份的 %d 张面值 %d 元月度体验券，" +
                    "有效期至%d年%d月%d日，原因：%s。",
                    month, couponCount, couponAmount,
                    expireTime.getYear(), expireTime.getMonthValue(), expireTime.getDayOfMonth(),
                    reason != null ? reason : "管理员补发");
            try {
                notificationService.sendToUsers(title, content, 2, "VIP_MONTHLY_COUPON_REISSUE", null, userId);
            } catch (Exception e) {
                log.warn("推送补发通知失败，userId={}", userId, e);
            }
        }

        log.info("管理员手动补发月度体验券成功：userId={}, level={}, year={}, month={}, count={}, reason={}",
                userId, getLevelName(level), year, month, couponCount, reason);
    }

    @Override
    public String renewVip(Long userId, Long packageId) {
        return purchaseVip(userId, packageId, "ALIPAY");
    }

    @Override
    @Transactional
    public void checkAndUpdateExpiredVip() {
        LocalDateTime now = LocalDateTime.now();

        // 查询所有过期的VIP记录
        LambdaQueryWrapper<UserVip> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserVip::getStatus, 1)
                .lt(UserVip::getEndTime, now);

        List<UserVip> expiredVips = userVipMapper.selectList(wrapper);

        Set<Long> affectedUserIds = new HashSet<>();
        for (UserVip userVip : expiredVips) {
            // 更新VIP记录状态为已过?
            userVip.setStatus(2);
            userVipMapper.updateById(userVip);
            affectedUserIds.add(userVip.getUserId());

            log.info("用户 {} 的VIP已过期", userVip.getUserId());
        }
        affectedUserIds.forEach(this::refreshUserVipState);
    }

    @Override
    public PageResult<UserVip> getUserVipHistory(Long userId, Integer page, Integer pageSize) {
        Page<UserVip> pageInfo = new Page<>(page, pageSize);

        LambdaQueryWrapper<UserVip> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserVip::getUserId, userId)
                .orderByDesc(UserVip::getCreateTime);

        userVipMapper.selectPage(pageInfo, wrapper);

        return new PageResult<>(pageInfo.getTotal(), pageInfo.getRecords());
    }

    // ========== 管理端功?==========

    @Override
    @Transactional
    public void createPackage(VipPackageDTO dto) {
        VipPackage vipPackage = new VipPackage();
        BeanUtils.copyProperties(dto, vipPackage);

        // 转换Boolean到Integer
        vipPackage.setPriorityBooking(dto.getPriorityBooking() ? 1 : 0);
        vipPackage.setExclusiveService(dto.getExclusiveService() ? 1 : 0);
        vipPackage.setBirthdayGift(dto.getBirthdayGift() ? 1 : 0);
        vipPackage.setExclusiveBadge(dto.getExclusiveBadge() ? 1 : 0);

        // 计算折扣?
        if (dto.getOriginalPrice() != null && dto.getCurrentPrice() != null 
            && dto.getOriginalPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountRate = dto.getCurrentPrice().divide(dto.getOriginalPrice(), 2, RoundingMode.HALF_UP);
            vipPackage.setDiscountRate(discountRate);
        }

        // 转换features列表为JSON字符?
        if (dto.getFeatures() != null && !dto.getFeatures().isEmpty()) {
            try {
                vipPackage.setFeatures(objectMapper.writeValueAsString(dto.getFeatures()));
            } catch (Exception e) {
                log.error("转换features失败", e);
            }
        }

        vipPackageMapper.insert(vipPackage);
        log.info("创建VIP套餐成功：{}", vipPackage.getName());
    }

    @Override
    @Transactional
    public void updatePackage(VipPackageDTO dto) {
        if (dto.getId() == null) {
            throw new RuntimeException("套餐ID不能为空");
        }

        VipPackage vipPackage = vipPackageMapper.selectById(dto.getId());
        if (vipPackage == null) {
            throw new RuntimeException("VIP套餐不存在");
        }

        BeanUtils.copyProperties(dto, vipPackage);

        // 转换Boolean到Integer
        vipPackage.setPriorityBooking(dto.getPriorityBooking() ? 1 : 0);
        vipPackage.setExclusiveService(dto.getExclusiveService() ? 1 : 0);
        vipPackage.setBirthdayGift(dto.getBirthdayGift() ? 1 : 0);
        vipPackage.setExclusiveBadge(dto.getExclusiveBadge() ? 1 : 0);

        // 计算折扣?
        if (dto.getOriginalPrice() != null && dto.getCurrentPrice() != null 
            && dto.getOriginalPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountRate = dto.getCurrentPrice().divide(dto.getOriginalPrice(), 2, RoundingMode.HALF_UP);
            vipPackage.setDiscountRate(discountRate);
        }

        // 转换features列表为JSON字符?
        if (dto.getFeatures() != null && !dto.getFeatures().isEmpty()) {
            try {
                vipPackage.setFeatures(objectMapper.writeValueAsString(dto.getFeatures()));
            } catch (Exception e) {
                log.error("转换features失败", e);
            }
        }

        vipPackageMapper.updateById(vipPackage);
        log.info("更新VIP套餐成功：{}", vipPackage.getName());
    }

    @Override
    @Transactional
    public void deletePackage(Long id) {
        VipPackage vipPackage = vipPackageMapper.selectById(id);
        if (vipPackage == null) {
            throw new RuntimeException("VIP套餐不存在");
        }

        vipPackageMapper.deleteById(id);
        log.info("删除VIP套餐成功：{}", vipPackage.getName());
    }

    @Override
    @Transactional
    public void updatePackageStatus(Long id, Integer status) {
        VipPackage vipPackage = vipPackageMapper.selectById(id);
        if (vipPackage == null) {
            throw new RuntimeException("VIP套餐不存在");
        }

        vipPackage.setStatus(status);
        vipPackageMapper.updateById(vipPackage);
        log.info("更新VIP套餐状态：{}，状态：{}", vipPackage.getName(), status == 1 ? "上架" : "下架");
    }

    @Override
    public PageResult<VipPackageVO> pageQueryPackages(Integer page, Integer pageSize, Integer level, Integer status) {
        Page<VipPackage> pageInfo = new Page<>(page, pageSize);

        LambdaQueryWrapper<VipPackage> wrapper = new LambdaQueryWrapper<>();
        if (level != null) {
            wrapper.eq(VipPackage::getLevel, level);
        }
        if (status != null) {
            wrapper.eq(VipPackage::getStatus, status);
        }
        wrapper.orderByAsc(VipPackage::getSortOrder);

        vipPackageMapper.selectPage(pageInfo, wrapper);

        List<VipPackageVO> voList = pageInfo.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(pageInfo.getTotal(), voList);
    }

    @Override
    @Transactional
    public void grantVip(Long userId, Integer days, Integer level, String reason) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusDays(days);

        // 创建VIP记录
        UserVip userVip = UserVip.builder()
                .userId(userId)
                .packageId(0L) // 赠送的VIP没有套餐ID
                .level(level)
                .startTime(startTime)
                .endTime(endTime)
                .originalPrice(BigDecimal.ZERO)
                .actualPrice(BigDecimal.ZERO)
                .paymentMethod("GRANT")
                .orderNo("GRANT_" + System.currentTimeMillis())
                .status(1)
                .autoRenew(0)
                .build();

        userVipMapper.insert(userVip);

        // 更新用户VIP等级和到期时?
        user.setVipLevel(level);
        user.setVipExpireTime(endTime);
        userMapper.updateById(user);

        log.info("赠送VIP成功：用?{}，{}天，等级 {}，原因：{}", userId, days, level, reason);
    }

    @Override
    @Transactional
    public void extendVip(Long userId, Integer days) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        UserVip currentVip = userVipMapper.getCurrentVip(userId);
        if (currentVip == null) {
            throw new RuntimeException("用户当前不是VIP");
        }

        LocalDateTime newEndTime = currentVip.getEndTime().plusDays(days);
        currentVip.setEndTime(newEndTime);
        userVipMapper.updateById(currentVip);

        user.setVipExpireTime(newEndTime);
        userMapper.updateById(user);

        log.info("延长VIP成功：用户{}，延长{}天", userId, days);
    }

    @Override
    public Map<String, Object> getVipStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 总VIP用户数（当前生效的VIP?
        LambdaQueryWrapper<UserVip> activeWrapper = new LambdaQueryWrapper<>();
        LocalDateTime now = LocalDateTime.now();
        activeWrapper.eq(UserVip::getStatus, 1)
                .and(wrapper -> wrapper.isNull(UserVip::getStartTime).or().le(UserVip::getStartTime, now))
                .gt(UserVip::getEndTime, now);
        Long totalVipUsers = userVipMapper.selectCount(activeWrapper);
        stats.put("totalVipUsers", totalVipUsers);

        // 套餐总数（包括上架和下架的）
        LambdaQueryWrapper<VipPackage> packageWrapper = new LambdaQueryWrapper<>();
        Long totalPackages = vipPackageMapper.selectCount(packageWrapper);
        stats.put("totalPackages", totalPackages);

        // 本月收入（统计本月所有VIP购买的实付金额）
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime nextMonthStart = monthStart.plusMonths(1);
        LocalDateTime lastMonthStart = monthStart.minusMonths(1);
        LambdaQueryWrapper<UserVip> monthRevenueWrapper = new LambdaQueryWrapper<>();
        monthRevenueWrapper.ge(UserVip::getCreateTime, monthStart)
                .lt(UserVip::getCreateTime, nextMonthStart)
                .ne(UserVip::getStatus, 0);
        List<UserVip> monthlyVips = userVipMapper.selectList(monthRevenueWrapper);
        BigDecimal monthlyRevenue = monthlyVips.stream()
                .filter(vip -> vip.getActualPrice() != null)
                .map(UserVip::getActualPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("monthlyRevenue", monthlyRevenue);

        LambdaQueryWrapper<UserVip> lastMonthRevenueWrapper = new LambdaQueryWrapper<>();
        lastMonthRevenueWrapper.ge(UserVip::getCreateTime, lastMonthStart)
                .lt(UserVip::getCreateTime, monthStart)
                .ne(UserVip::getStatus, 0);
        List<UserVip> lastMonthVips = userVipMapper.selectList(lastMonthRevenueWrapper);
        BigDecimal lastMonthRevenue = lastMonthVips.stream()
                .filter(vip -> vip.getActualPrice() != null)
                .map(UserVip::getActualPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("growthRate", calculateVipGrowthRate(monthlyRevenue, lastMonthRevenue));

        // 本月新增VIP用户?
        Long newVipUsers = userVipMapper.selectCount(monthRevenueWrapper);
        stats.put("newVipUsers", newVipUsers);

        // 各等级分?
        Map<Integer, Long> levelDistribution = new HashMap<>();
        for (int level = 1; level <= 4; level++) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getVipLevel, level)
                    .gt(User::getVipExpireTime, LocalDateTime.now());
            Long count = userMapper.selectCount(wrapper);
            levelDistribution.put(level, count);
        }
        stats.put("levelDistribution", levelDistribution);

        log.info("VIP统计数据: totalVipUsers={}, totalPackages={}, monthlyRevenue={}, newVipUsers={}", 
                totalVipUsers, totalPackages, monthlyRevenue, newVipUsers);

        return stats;
    }

    private BigDecimal calculateVipGrowthRate(BigDecimal current, BigDecimal previous) {
        BigDecimal safeCurrent = current != null ? current : BigDecimal.ZERO;
        BigDecimal safePrevious = previous != null ? previous : BigDecimal.ZERO;
        if (safePrevious.compareTo(BigDecimal.ZERO) == 0) {
            return safeCurrent.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return safeCurrent.subtract(safePrevious)
                .divide(safePrevious, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public PageResult<Map<String, Object>> pageQueryVipUsers(Integer page, Integer pageSize, Integer level, Integer status) {
        Page<UserVip> pageInfo = new Page<>(page, pageSize);

        LambdaQueryWrapper<UserVip> wrapper = new LambdaQueryWrapper<>();
        if (level != null) {
            wrapper.eq(UserVip::getLevel, level);
        }
        if (status != null) {
            wrapper.eq(UserVip::getStatus, status);
        }
        wrapper.orderByDesc(UserVip::getCreateTime);

        userVipMapper.selectPage(pageInfo, wrapper);

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (UserVip userVip : pageInfo.getRecords()) {
            try {
                Map<String, Object> map = new HashMap<>();
                map.put("id", userVip.getId());
                map.put("userId", userVip.getUserId());
                map.put("level", userVip.getLevel());
                map.put("levelName", getLevelName(userVip.getLevel()));
                map.put("startTime", userVip.getStartTime());
                map.put("endTime", userVip.getEndTime());
                map.put("status", userVip.getStatus());
                map.put("daysRemaining", calculateDaysRemaining(userVip.getEndTime()));

                User user = userVip.getUserId() != null ? userMapper.selectById(userVip.getUserId()) : null;
                if (user != null) {
                    map.put("username", user.getUsername());
                    map.put("nickname", user.getNickname());
                    map.put("phone", user.getPhone());
                } else {
                    map.put("username", null);
                    map.put("nickname", null);
                    map.put("phone", null);
                }
                resultList.add(map);
            } catch (Exception e) {
                log.error("组装VIP用户列表项失败，已跳过异常数据: userVipId={}", userVip != null ? userVip.getId() : null, e);
            }
        }

        return new PageResult<>(pageInfo.getTotal(), resultList);
    }

    // ========== 私有方法 ==========

    /**
     * 转换为VO
     */
    private VipPackageVO convertToVO(VipPackage vipPackage) {
        VipPackageVO vo = new VipPackageVO();
        BeanUtils.copyProperties(vipPackage, vo);

        // 设置等级名称
        vo.setLevelName(getLevelName(vipPackage.getLevel()));

        // 设置时长描述
        vo.setDurationText(getDurationText(vipPackage.getDurationType()));

        // 转换Integer到Boolean
        vo.setPriorityBooking(vipPackage.getPriorityBooking() == 1);
        vo.setExclusiveService(vipPackage.getExclusiveService() == 1);
        vo.setBirthdayGift(vipPackage.getBirthdayGift() == 1);
        vo.setExclusiveBadge(vipPackage.getExclusiveBadge() == 1);

        // 解析features JSON字符串为列表
        try {
            List<String> features = parseFeatures(vipPackage.getFeatures());
            vo.setFeatures(features);
        } catch (Exception e) {
            log.error("解析features失败", e);
        }

        return vo;
    }

    /**
     * 解析权益列表
     */
    private List<String> parseFeatures(String featuresJson) {
        if (featuresJson == null || featuresJson.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(featuresJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("解析features JSON失败: {}", featuresJson, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取等级名称
     */
    private String getLevelName(Integer level) {
        if (level == null) {
            return "未知等级";
        }
        switch (level) {
            case 1:
                return "见习侦探";
            case 2:
                return "银章侦探";
            case 3:
                return "金章侦探";
            case 4:
                return "传奇侦探";
            default:
                return "未知等级";
        }
    }

    /**
     * 获取时长描述
     */
    private String getDurationText(Integer durationType) {
        switch (durationType) {
            case 1:
                return "月";
            case 2:
                return "季";
            case 3:
                return "半年";
            case 4:
                return "年";
            default:
                return "";
        }
    }

    /**
     * 计算剩余天数
     */
    private long calculateDaysRemaining(LocalDateTime endTime) {
        if (endTime == null) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime)) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(now, endTime);
    }

    /**
     * 生成订单?
     */
    private String generateOrderNo(Long userId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        int randomSuffix = 1000 + new Random().nextInt(9000);
        return "VIP" + timestamp + userId + randomSuffix;
    }
}

