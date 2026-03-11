package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.murder.entity.GroupMember;
import com.murder.entity.GroupOrder;
import com.murder.entity.Reservation;
import com.murder.entity.Script;
import com.murder.entity.ScriptCategory;
import com.murder.entity.User;
import com.murder.mapper.GroupMemberMapper;
import com.murder.mapper.GroupOrderMapper;
import com.murder.mapper.ReservationMapper;
import com.murder.mapper.ScriptCategoryMapper;
import com.murder.mapper.ScriptMapper;
import com.murder.mapper.UserMapper;
import com.murder.service.CouponService;
import com.murder.service.GroupOrderService;
import com.murder.service.NotificationService;
import com.murder.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupOrderServiceImpl extends ServiceImpl<GroupOrderMapper, GroupOrder> implements GroupOrderService {

    private static final int GROUP_TIMEOUT_HOURS = 24;

    private final GroupMemberMapper groupMemberMapper;
    private final UserMapper userMapper;
    private final ReservationMapper reservationMapper;
    private final ScriptMapper scriptMapper;
    private final ScriptCategoryMapper scriptCategoryMapper;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final CouponService couponService;

    @Override
    public Page<Map<String, Object>> pageQuery(Integer page, Integer pageSize, Long scriptId, Long categoryId, Integer playerCount, Integer status) {
        Page<GroupOrder> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<GroupOrder> wrapper = new LambdaQueryWrapper<>();

        if (scriptId != null) {
            wrapper.eq(GroupOrder::getScriptId, scriptId);
        }
        if (categoryId != null) {
            LambdaQueryWrapper<Script> scriptWrapper = new LambdaQueryWrapper<>();
            scriptWrapper.eq(Script::getCategoryId, categoryId)
                    .eq(Script::getIsDeleted, 0);
            List<Script> scripts = scriptMapper.selectList(scriptWrapper);
            if (scripts.isEmpty()) {
                Page<Map<String, Object>> emptyPage = new Page<>(page, pageSize, 0);
                emptyPage.setRecords(new ArrayList<>());
                return emptyPage;
            }
            List<Long> scriptIds = scripts.stream().map(Script::getId).toList();
            wrapper.in(GroupOrder::getScriptId, scriptIds);
        }
        if (playerCount != null) {
            wrapper.eq(GroupOrder::getPlayerCount, playerCount);
        }
        if (status != null) {
            wrapper.eq(GroupOrder::getStatus, status);
        } else {
            wrapper.in(GroupOrder::getStatus, Arrays.asList(1, 2));
        }

        wrapper.orderByDesc(GroupOrder::getCreateTime);

        Page<GroupOrder> result = this.page(pageParam, wrapper);
        Page<Map<String, Object>> mapPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<Map<String, Object>> records = new ArrayList<>();
        for (GroupOrder group : result.getRecords()) {
            records.add(convertToMap(group));
        }
        mapPage.setRecords(records);
        return mapPage;
    }

    @Override
    public List<Map<String, Object>> getHotGroups(Integer limit) {
        try {
            int safeLimit = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);
            LambdaQueryWrapper<GroupOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(GroupOrder::getStatus, 1)
                    .orderByDesc(GroupOrder::getCreateTime)
                    .last("LIMIT " + safeLimit);

            List<GroupOrder> groups = this.list(wrapper);
            if (groups == null || groups.isEmpty()) {
                return new ArrayList<>();
            }

            // 批量查询剧本，避免 N+1 查询导致的性能问题和偶发异常
            List<Long> scriptIds = groups.stream()
                    .filter(g -> g.getScriptId() != null)
                    .map(GroupOrder::getScriptId)
                    .distinct()
                    .toList();

            Map<Long, Script> scriptMap = new java.util.HashMap<>();
            Map<Long, ScriptCategory> categoryMap = new java.util.HashMap<>();

            if (!scriptIds.isEmpty()) {
                try {
                    LambdaQueryWrapper<Script> scriptWrapper = new LambdaQueryWrapper<>();
                    scriptWrapper.in(Script::getId, scriptIds).eq(Script::getIsDeleted, 0);
                    List<Script> scripts = scriptMapper.selectList(scriptWrapper);
                    scripts.forEach(s -> scriptMap.put(s.getId(), s));

                    // 批量查分类
                    List<Long> categoryIds = scripts.stream()
                            .filter(s -> s.getCategoryId() != null)
                            .map(Script::getCategoryId)
                            .distinct()
                            .toList();
                    if (!categoryIds.isEmpty()) {
                        LambdaQueryWrapper<ScriptCategory> catWrapper = new LambdaQueryWrapper<>();
                        catWrapper.in(ScriptCategory::getId, categoryIds);
                        scriptCategoryMapper.selectList(catWrapper)
                                .forEach(c -> categoryMap.put(c.getId(), c));
                    }
                } catch (Exception e) {
                    log.warn("批量查询剧本信息失败，将使用拼单快照数据: {}", e.getMessage());
                }
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (GroupOrder group : groups) {
                try {
                    Script script = group.getScriptId() != null ? scriptMap.get(group.getScriptId()) : null;
                    ScriptCategory category = (script != null && script.getCategoryId() != null)
                            ? categoryMap.get(script.getCategoryId()) : null;

                    Map<String, Object> map = new HashMap<>();
                    map.put("id", group.getId());
                    map.put("creatorName", "神秘车主");
                    map.put("creatorAvatar", null);
                    map.put("scriptId", group.getScriptId());
                    map.put("scriptName", script != null && StringUtils.hasText(script.getName())
                            ? script.getName() : group.getScriptName());
                    map.put("categoryId", script != null ? script.getCategoryId() : null);
                    map.put("categoryName", category != null ? category.getName() : null);
                    map.put("storeId", group.getStoreId());
                    map.put("storeName", group.getStoreName());
                    map.put("playTime", group.getPlayTime());
                    map.put("currentCount", group.getCurrentCount());
                    map.put("needCount", group.getNeedCount());
                    map.put("playerCount", group.getPlayerCount());
                    map.put("price", group.getPrice());
                    map.put("genderRequirement", group.getGenderRequirement());
                    map.put("newbieWelcome", group.getNewbieWelcome());
                    map.put("description", group.getDescription());
                    map.put("status", group.getStatus());
                    map.put("createTime", group.getCreateTime());
                    result.add(map);
                } catch (Exception e) {
                    log.warn("处理拼单数据失败，跳过 groupId={}: {}", group.getId(), e.getMessage());
                }
            }
            return result;
        } catch (Exception e) {
            log.error("获取热门拼单失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Object> getDetailWithMembers(Long id) {
        GroupOrder group = this.getById(id);
        if (group == null) {
            return null;
        }

        Map<String, Object> result = convertToMapAnonymous(group);

        List<GroupMember> members = getGroupMembers(id);
        List<Map<String, Object>> anonymousMembers = new ArrayList<>();
        String[] roleNames = {
                "神秘侦探", "暗夜行者", "迷雾旅人", "幽灵猎手", "黑袍法师",
                "白衣天使", "午夜骑士", "影子刺客", "星辉守望者", "命运编织者",
                "时空旅者", "月光猎人", "风暴使者", "深渊行者", "光明守护"
        };

        int index = 0;
        for (GroupMember member : members) {
            Map<String, Object> anonymousMember = new HashMap<>();
            anonymousMember.put("id", member.getId());
            anonymousMember.put("isCreator", member.getIsCreator());
            anonymousMember.put("joinCount", member.getJoinCount());
            anonymousMember.put("createTime", member.getCreateTime());

            String roleName = roleNames[index % roleNames.length];
            anonymousMember.put(
                    "anonymousName",
                    member.getIsCreator() == 1 ? "🎭 " + roleName + " (发起人)" : "🎭 " + roleName
            );
            anonymousMember.put("avatar", null);
            anonymousMember.put("nickname", roleName);
            anonymousMembers.add(anonymousMember);
            index++;
        }
        result.put("members", anonymousMembers);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupOrder createGroup(GroupOrder groupOrder, Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        populateScriptSnapshot(groupOrder);

        groupOrder.setCreatorId(userId);
        groupOrder.setCreatorName(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        groupOrder.setCreatorAvatar(user.getAvatar());
        groupOrder.setStatus(1);
        groupOrder.setCreateTime(LocalDateTime.now());
        groupOrder.setUpdateTime(LocalDateTime.now());
        if (groupOrder.getCurrentCount() == null || groupOrder.getCurrentCount() < 1) {
            groupOrder.setCurrentCount(1);
        }

        this.save(groupOrder);

        GroupMember creator = new GroupMember();
        creator.setGroupId(groupOrder.getId());
        creator.setUserId(userId);
        creator.setNickname(groupOrder.getCreatorName());
        creator.setAvatar(groupOrder.getCreatorAvatar());
        creator.setIsCreator(1);
        creator.setJoinCount(groupOrder.getCurrentCount());
        creator.setStatus(1);
        creator.setCreateTime(LocalDateTime.now());
        groupMemberMapper.insert(creator);

        bindCreatorReservation(groupOrder);
        checkAndUpdateGroupStatus(groupOrder);
        return groupOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean joinGroup(Long groupId, Long userId, Integer joinCount) {
        GroupOrder group = this.getById(groupId);
        if (group == null) {
            throw new RuntimeException("拼单不存在");
        }
        if (group.getStatus() != 1) {
            throw new RuntimeException("该拼单已结束或已取消");
        }

        LambdaQueryWrapper<GroupMember> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId)
                .eq(GroupMember::getStatus, 1);
        if (groupMemberMapper.selectCount(checkWrapper) > 0) {
            throw new RuntimeException("您已经参加了该拼单");
        }

        int actualJoinCount = (joinCount == null || joinCount < 1) ? 1 : joinCount;
        if (group.getCurrentCount() + actualJoinCount > group.getNeedCount()) {
            throw new RuntimeException("剩余名额不足");
        }

        User user = userMapper.selectById(userId);
        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setNickname(user != null
                ? (StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername())
                : "用户" + userId);
        member.setAvatar(user != null ? user.getAvatar() : null);
        member.setIsCreator(0);
        member.setJoinCount(actualJoinCount);
        member.setStatus(1);
        member.setCreateTime(LocalDateTime.now());
        groupMemberMapper.insert(member);

        group.setCurrentCount(group.getCurrentCount() + actualJoinCount);
        group.setUpdateTime(LocalDateTime.now());
        this.updateById(group);

        checkAndUpdateGroupStatus(group);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean leaveGroup(Long groupId, Long userId) {
        GroupOrder group = this.getById(groupId);
        if (group == null) {
            throw new RuntimeException("拼单不存在");
        }
        if (group.getStatus() != 1) {
            throw new RuntimeException("拼单已成团或已结束，无法退出");
        }
        if (group.getCreatorId().equals(userId)) {
            throw new RuntimeException("发起人不能退出，如需关闭请取消拼单");
        }

        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId)
                .eq(GroupMember::getStatus, 1);
        GroupMember member = groupMemberMapper.selectOne(wrapper);
        if (member == null) {
            throw new RuntimeException("您未参加该拼单");
        }

        member.setStatus(0);
        groupMemberMapper.updateById(member);

        group.setCurrentCount(group.getCurrentCount() - member.getJoinCount());
        group.setUpdateTime(LocalDateTime.now());
        this.updateById(group);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelGroup(Long groupId, Long userId) {
        GroupOrder group = this.getById(groupId);
        if (group == null) {
            throw new RuntimeException("拼单不存在");
        }
        if (!group.getCreatorId().equals(userId)) {
            throw new RuntimeException("只有发起人可以取消拼单");
        }
        if (group.getStatus() != 1) {
            throw new RuntimeException("只能取消拼团中的拼单");
        }

        group.setStatus(0);
        group.setUpdateTime(LocalDateTime.now());
        this.updateById(group);
        handleGroupFailed(group, "发起人已取消拼单");
        return true;
    }

    @Override
    public Page<Map<String, Object>> getMyGroups(Long userId, Integer page, Integer pageSize, Integer type) {
        Page<GroupOrder> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<GroupOrder> wrapper = new LambdaQueryWrapper<>();

        if (type == null || type == 0) {
            wrapper.eq(GroupOrder::getCreatorId, userId);
        } else {
            LambdaQueryWrapper<GroupMember> memberWrapper = new LambdaQueryWrapper<>();
            memberWrapper.eq(GroupMember::getUserId, userId)
                    .eq(GroupMember::getStatus, 1)
                    .eq(GroupMember::getIsCreator, 0);
            List<GroupMember> members = groupMemberMapper.selectList(memberWrapper);
            if (members.isEmpty()) {
                Page<Map<String, Object>> emptyPage = new Page<>(page, pageSize, 0);
                emptyPage.setRecords(new ArrayList<>());
                return emptyPage;
            }
            List<Long> groupIds = new ArrayList<>();
            for (GroupMember member : members) {
                groupIds.add(member.getGroupId());
            }
            wrapper.in(GroupOrder::getId, groupIds);
        }

        wrapper.orderByDesc(GroupOrder::getCreateTime);
        Page<GroupOrder> result = this.page(pageParam, wrapper);

        Page<Map<String, Object>> mapPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<Map<String, Object>> records = new ArrayList<>();
        for (GroupOrder group : result.getRecords()) {
            records.add(convertToMap(group));
        }
        mapPage.setRecords(records);
        return mapPage;
    }

    @Override
    public List<GroupMember> getGroupMembers(Long groupId) {
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getStatus, 1)
                .orderByAsc(GroupMember::getCreateTime);
        return groupMemberMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processExpiredGroups() {
        LocalDateTime expireTime = LocalDateTime.now().minusHours(GROUP_TIMEOUT_HOURS);
        LambdaQueryWrapper<GroupOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupOrder::getStatus, 1)
                .le(GroupOrder::getCreateTime, expireTime);

        List<GroupOrder> expiredGroups = this.list(wrapper);
        for (GroupOrder group : expiredGroups) {
            try {
                group.setStatus(0);
                group.setUpdateTime(LocalDateTime.now());
                this.updateById(group);
                handleGroupFailed(group, "拼单24小时内未成团，系统自动关闭");
            } catch (Exception e) {
                log.error("处理超时拼单失败: groupId={}", group.getId(), e);
            }
        }
    }

    private void checkAndUpdateGroupStatus(GroupOrder group) {
        if (group.getCurrentCount() < group.getNeedCount() || group.getStatus() != 1) {
            return;
        }

        group.setStatus(2);
        group.setUpdateTime(LocalDateTime.now());
        this.updateById(group);
        finalizeGroupSuccess(group);
    }

    private void finalizeGroupSuccess(GroupOrder group) {
        Reservation creatorReservation = getLinkedReservation(group);
        List<GroupMember> activeMembers = getGroupMembers(group.getId());

        if (creatorReservation != null) {
            ensureCreatorReservationBound(group, creatorReservation);
            createParticipantReservations(group, creatorReservation, activeMembers);
        }

        sendGroupSuccessNotifications(group, creatorReservation, activeMembers);
    }

    private void createParticipantReservations(GroupOrder group, Reservation creatorReservation, List<GroupMember> activeMembers) {
        for (GroupMember member : activeMembers) {
            if (Integer.valueOf(1).equals(member.getIsCreator())) {
                continue;
            }

            LambdaQueryWrapper<Reservation> existingWrapper = new LambdaQueryWrapper<>();
            existingWrapper.eq(Reservation::getGroupId, group.getId())
                    .eq(Reservation::getUserId, member.getUserId())
                    .eq(Reservation::getIsDeleted, 0);
            if (reservationMapper.selectCount(existingWrapper) > 0) {
                continue;
            }

            User user = userMapper.selectById(member.getUserId());
            Reservation reservation = new Reservation();
            reservation.setOrderNo(generateReservationNo());
            reservation.setUserId(member.getUserId());
            reservation.setStoreId(creatorReservation.getStoreId());
            reservation.setRoomId(creatorReservation.getRoomId());
            reservation.setScriptId(creatorReservation.getScriptId());
            reservation.setGroupId(group.getId());
            reservation.setPlayerCount(member.getJoinCount());
            reservation.setReservationTime(creatorReservation.getReservationTime());
            reservation.setDuration(creatorReservation.getDuration());
            BigDecimal totalAmount = defaultAmount(group.getPrice()).multiply(BigDecimal.valueOf(member.getJoinCount()));
            reservation.setTotalPrice(totalAmount);
            reservation.setDiscountAmount(BigDecimal.ZERO);
            reservation.setActualAmount(totalAmount);
            reservation.setContactName(buildContactName(user));
            reservation.setContactPhone(user != null ? user.getPhone() : null);
            reservation.setRemark("拼单成团后自动生成的正式订单");
            reservation.setStatus(1);
            reservation.setPayStatus(0);
            reservation.setRefundStatus(0);
            reservation.setCheckInStatus(0);
            reservation.setCheckInCode(generateCheckInCode());
            reservationMapper.insert(reservation);
        }
    }

    private void handleGroupFailed(GroupOrder group, String reason) {
        Reservation creatorReservation = getLinkedReservation(group);
        if (creatorReservation != null) {
            try {
                if (Integer.valueOf(1).equals(creatorReservation.getPayStatus())) {
                    paymentService.autoRefund(creatorReservation.getId(), reason);
                } else if (!Integer.valueOf(4).equals(creatorReservation.getStatus())) {
                    cancelReservationBySystem(creatorReservation, reason);
                }
            } catch (Exception e) {
                log.error("处理拼单发起人预约失败: groupId={}, reservationId={}", group.getId(), creatorReservation.getId(), e);
            }
        }

        LambdaQueryWrapper<Reservation> reservationWrapper = new LambdaQueryWrapper<>();
        reservationWrapper.eq(Reservation::getGroupId, group.getId())
                .eq(Reservation::getIsDeleted, 0);
        List<Reservation> linkedReservations = reservationMapper.selectList(reservationWrapper);
        for (Reservation reservation : linkedReservations) {
            if (creatorReservation != null && creatorReservation.getId().equals(reservation.getId())) {
                continue;
            }
            try {
                if (Integer.valueOf(1).equals(reservation.getPayStatus())) {
                    paymentService.autoRefund(reservation.getId(), reason);
                } else if (!Integer.valueOf(4).equals(reservation.getStatus())) {
                    cancelReservationBySystem(reservation, reason);
                }
            } catch (Exception e) {
                log.error("处理拼单成员预约失败: groupId={}, reservationId={}", group.getId(), reservation.getId(), e);
            }
        }

        sendGroupFailedNotifications(group, reason);
    }

    private void sendGroupSuccessNotifications(GroupOrder group, Reservation creatorReservation, List<GroupMember> activeMembers) {
        LocalDateTime playTime = creatorReservation != null ? creatorReservation.getReservationTime() : group.getPlayTime();
        String playTimeText = formatPlayTime(playTime);

        List<Long> creatorIds = new ArrayList<>();
        List<Long> participantIds = new ArrayList<>();
        for (GroupMember member : activeMembers) {
            if (member.getUserId() == null) {
                continue;
            }
            if (Integer.valueOf(1).equals(member.getIsCreator())) {
                creatorIds.add(member.getUserId());
            } else {
                participantIds.add(member.getUserId());
            }
        }

        if (!creatorIds.isEmpty()) {
            notificationService.sendToUsers(
                    "拼单已成团通知",
                    String.format("您的拼单已成团，剧本《%s》已锁定场次，时间：%s。", group.getScriptName(), playTimeText),
                    2,
                    "group",
                    group.getId(),
                    creatorIds.toArray(new Long[0])
            );
        }

        if (!participantIds.isEmpty()) {
            String participantContent = creatorReservation != null
                    ? String.format("您参与的拼单《%s》已成团，系统已生成正式预约订单，请在2小时内完成支付。场次时间：%s。", group.getScriptName(), playTimeText)
                    : String.format("您参与的拼单《%s》已成团，场次时间：%s，请尽快联系门店确认正式预约。", group.getScriptName(), playTimeText);
            notificationService.sendToUsers(
                    "拼单已成团通知",
                    participantContent,
                    2,
                    "group",
                    group.getId(),
                    participantIds.toArray(new Long[0])
            );
        }
    }

    private void sendGroupFailedNotifications(GroupOrder group, String reason) {
        List<GroupMember> activeMembers = getGroupMembers(group.getId());
        List<Long> userIds = new ArrayList<>();
        for (GroupMember member : activeMembers) {
            if (member.getUserId() != null) {
                userIds.add(member.getUserId());
            }
        }
        if (userIds.isEmpty()) {
            return;
        }

        notificationService.sendToUsers(
                "拼单未成团通知",
                String.format("拼单《%s》未能在24小时内成团，系统已自动关闭。%s", group.getScriptName(), reason),
                2,
                "group",
                group.getId(),
                userIds.toArray(new Long[0])
        );
    }

    private void bindCreatorReservation(GroupOrder groupOrder) {
        if (groupOrder.getReservationId() == null) {
            return;
        }

        Reservation reservation = reservationMapper.selectById(groupOrder.getReservationId());
        if (reservation == null) {
            return;
        }

        Reservation update = new Reservation();
        update.setId(reservation.getId());
        update.setGroupId(groupOrder.getId());
        reservationMapper.updateById(update);
    }

    private void ensureCreatorReservationBound(GroupOrder group, Reservation reservation) {
        if (reservation.getGroupId() != null) {
            return;
        }

        Reservation update = new Reservation();
        update.setId(reservation.getId());
        update.setGroupId(group.getId());
        reservationMapper.updateById(update);
        reservation.setGroupId(group.getId());
    }

    private Reservation getLinkedReservation(GroupOrder group) {
        if (group.getReservationId() == null) {
            return null;
        }
        return reservationMapper.selectById(group.getReservationId());
    }

    private String buildContactName(User user) {
        if (user == null) {
            return "拼单玩家";
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername();
        }
        return "拼单玩家";
    }

    private String formatPlayTime(LocalDateTime playTime) {
        if (playTime == null) {
            return "待门店确认";
        }
        return playTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String generateReservationNo() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "R" + datePart + random;
    }

    private String generateCheckInCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private void cancelReservationBySystem(Reservation reservation, String reason) {
        Reservation update = new Reservation();
        update.setId(reservation.getId());
        update.setStatus(4);
        update.setRemark(reason);
        reservationMapper.updateById(update);
        refundCouponQuietly(reservation.getId(), reservation.getCouponId());
    }

    private void refundCouponQuietly(Long reservationId, Long couponId) {
        if (couponId == null) {
            return;
        }
        try {
            couponService.refundCoupon(reservationId);
        } catch (Exception e) {
            log.warn("拼单失败返还优惠券失败: reservationId={}", reservationId, e);
        }
    }

    private Map<String, Object> convertToMap(GroupOrder group) {
        Script script = resolveScript(group);
        ScriptCategory category = resolveCategory(script);
        Map<String, Object> map = new HashMap<>();
        map.put("id", group.getId());
        map.put("creatorName", "神秘车主");
        map.put("creatorAvatar", null);
        map.put("scriptId", group.getScriptId());
        map.put("scriptName", script != null && StringUtils.hasText(script.getName()) ? script.getName() : group.getScriptName());
        map.put("categoryId", script != null ? script.getCategoryId() : null);
        map.put("categoryName", category != null ? category.getName() : null);
        map.put("storeId", group.getStoreId());
        map.put("storeName", group.getStoreName());
        map.put("playTime", group.getPlayTime());
        map.put("currentCount", group.getCurrentCount());
        map.put("needCount", group.getNeedCount());
        map.put("playerCount", group.getPlayerCount());
        map.put("price", group.getPrice());
        map.put("genderRequirement", group.getGenderRequirement());
        map.put("newbieWelcome", group.getNewbieWelcome());
        map.put("description", group.getDescription());
        map.put("status", group.getStatus());
        map.put("createTime", group.getCreateTime());
        return map;
    }

    private Map<String, Object> convertToMapAnonymous(GroupOrder group) {
        Script script = resolveScript(group);
        ScriptCategory category = resolveCategory(script);
        Map<String, Object> map = new HashMap<>();
        map.put("id", group.getId());
        map.put("creatorName", "神秘车主");
        map.put("creatorAvatar", null);
        map.put("scriptId", group.getScriptId());
        map.put("scriptName", script != null && StringUtils.hasText(script.getName()) ? script.getName() : group.getScriptName());
        map.put("categoryId", script != null ? script.getCategoryId() : null);
        map.put("categoryName", category != null ? category.getName() : null);
        map.put("storeId", group.getStoreId());
        map.put("storeName", group.getStoreName());
        map.put("playTime", group.getPlayTime());
        map.put("currentCount", group.getCurrentCount());
        map.put("needCount", group.getNeedCount());
        map.put("playerCount", group.getPlayerCount());
        map.put("price", group.getPrice());
        map.put("genderRequirement", group.getGenderRequirement());
        map.put("newbieWelcome", group.getNewbieWelcome());
        map.put("description", group.getDescription());
        map.put("difficulty", 2);
        map.put("status", group.getStatus());
        map.put("createTime", group.getCreateTime());
        return map;
    }

    private void populateScriptSnapshot(GroupOrder groupOrder) {
        Script script = resolveScript(groupOrder);
        if (script == null) {
            return;
        }
        groupOrder.setScriptName(script.getName());
        if (script.getPlayerCount() != null) {
            groupOrder.setPlayerCount(script.getPlayerCount());
        }
    }

    private Script resolveScript(GroupOrder group) {
        if (group == null || group.getScriptId() == null) {
            return null;
        }
        return scriptMapper.selectById(group.getScriptId());
    }

    private ScriptCategory resolveCategory(Script script) {
        if (script == null || script.getCategoryId() == null) {
            return null;
        }
        return scriptCategoryMapper.selectById(script.getCategoryId());
    }
}
