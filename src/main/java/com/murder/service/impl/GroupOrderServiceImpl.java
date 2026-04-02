package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.murder.entity.GroupMember;
import com.murder.entity.GroupOrder;
import com.murder.entity.Reservation;
import com.murder.entity.Script;
import com.murder.entity.ScriptCategory;
import com.murder.entity.ScriptSchedule;
import com.murder.entity.User;
import com.murder.mapper.GroupMemberMapper;
import com.murder.mapper.GroupOrderMapper;
import com.murder.mapper.ReservationMapper;
import com.murder.mapper.ScriptCategoryMapper;
import com.murder.mapper.ScriptMapper;
import com.murder.mapper.StoreMapper;
import com.murder.mapper.UserMapper;
import com.murder.service.CouponService;
import com.murder.service.GroupOrderService;
import com.murder.service.NotificationService;
import com.murder.service.PaymentService;
import com.murder.service.ScriptScheduleService;
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
    private static final int GROUP_FORMATION_LEAD_HOURS = 2;

    private final GroupMemberMapper groupMemberMapper;
    private final UserMapper userMapper;
    private final ReservationMapper reservationMapper;
    private final ScriptMapper scriptMapper;
    private final ScriptCategoryMapper scriptCategoryMapper;
    private final StoreMapper storeMapper;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final CouponService couponService;
    private final ScriptScheduleService scriptScheduleService;

    @Override
    public Page<Map<String, Object>> pageQuery(Integer page, Integer pageSize, Long scriptId, Long categoryId, Integer playerCount, Integer status) {
        Page<GroupOrder> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<GroupOrder> wrapper = new LambdaQueryWrapper<>();
        LocalDateTime now = LocalDateTime.now();

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
            if (status == 1) {
                applyActiveStatusFilter(wrapper, now);
            } else {
                wrapper.eq(GroupOrder::getStatus, status);
            }
        } else {
            wrapper.and(query -> query
                    .eq(GroupOrder::getStatus, 2)
                    .or(active -> active
                            .eq(GroupOrder::getStatus, 1)
                            .gt(GroupOrder::getCreateTime, now.minusHours(GROUP_TIMEOUT_HOURS))
                            .and(play -> play
                                    .isNull(GroupOrder::getPlayTime)
                                    .or()
                                    .gt(GroupOrder::getPlayTime, now.plusHours(GROUP_FORMATION_LEAD_HOURS)))));
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
            applyActiveStatusFilter(wrapper, LocalDateTime.now());
            wrapper.orderByDesc(GroupOrder::getCreateTime)
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
                    map.put("needCount", resolveTargetPlayerCount(group));
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
    public Map<String, Object> getDetailWithMembers(Long id, Long userId) {
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

        boolean hasJoined = false;
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

            // 判断当前用户是否已加入
            if (userId != null && userId.equals(member.getUserId())) {
                hasJoined = true;
            }
            index++;
        }
        result.put("members", anonymousMembers);
        result.put("hasJoined", hasJoined);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupOrder createGroup(GroupOrder groupOrder, Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        validateGroupTimeline(groupOrder, now);
        populateScriptSnapshot(groupOrder);

        groupOrder.setCreatorId(userId);
        groupOrder.setCreatorName(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        groupOrder.setCreatorAvatar(user.getAvatar());
        groupOrder.setStatus(1);
        groupOrder.setCreateTime(now);
        groupOrder.setUpdateTime(now);
        if (groupOrder.getCurrentCount() == null || groupOrder.getCurrentCount() < 1) {
            groupOrder.setCurrentCount(1);
        }
        groupOrder.setNeedCount(resolveTargetPlayerCount(groupOrder));

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
    public GroupOrder createOrAttachAutoGroup(GroupOrder groupOrder, Reservation reservation, Long userId) {
        if (groupOrder == null || reservation == null) {
            return null;
        }

        GroupOrder reusableGroup = findReusableAutoGroup(groupOrder);
        if (reusableGroup == null) {
            int currentCount = resolveJoinCount(reservation.getPlayerCount());
            int needCount = resolveAutoGroupNeedCount(null, groupOrder, reservation, currentCount);
            groupOrder.setCurrentCount(currentCount);
            groupOrder.setNeedCount(needCount);
            groupOrder.setDescription(buildAutoGroupDescription(currentCount, needCount));
            return createGroup(groupOrder, userId);
        }

        attachReservationToExistingGroup(reusableGroup, reservation, userId, groupOrder);
        return this.getById(reusableGroup.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupOrder ensureAutoGroupForPaidReservation(Reservation reservation) {
        if (reservation == null || reservation.getId() == null || reservation.getUserId() == null) {
            return null;
        }
        if (!Integer.valueOf(1).equals(reservation.getPayStatus())) {
            return null;
        }
        if (reservation.getGroupId() != null) {
            refreshGroupStatus(reservation.getGroupId());
            return this.getById(reservation.getGroupId());
        }

        GroupOrder groupDraft = buildAutoGroupDraft(reservation);
        if (groupDraft == null) {
            return null;
        }

        GroupOrder reusableGroup = findReusableAutoGroup(groupDraft);
        if (reusableGroup != null) {
            attachReservationToExistingGroup(reusableGroup, reservation, reservation.getUserId(), groupDraft);
            return this.getById(reusableGroup.getId());
        }

        int requiredPlayers = resolveTargetPlayerCount(groupDraft);
        int paidPlayers = countPaidPlayersForSession(reservation);
        if (paidPlayers >= requiredPlayers) {
            return null;
        }

        int currentCount = resolveJoinCount(reservation.getPlayerCount());
        groupDraft.setCurrentCount(currentCount);
        groupDraft.setNeedCount(Math.max(requiredPlayers, currentCount));
        groupDraft.setDescription(buildAutoGroupDescription(currentCount, groupDraft.getNeedCount()));
        return createGroup(groupDraft, reservation.getUserId());
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
        if (expireGroupIfNeeded(group, LocalDateTime.now())) {
            throw new RuntimeException(buildExpiredActionMessage(group));
        }

        LambdaQueryWrapper<GroupMember> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId)
                .eq(GroupMember::getStatus, 1);
        if (groupMemberMapper.selectCount(checkWrapper) > 0) {
            throw new RuntimeException("您已经参加了该拼单");
        }

        int actualJoinCount = (joinCount == null || joinCount < 1) ? 1 : joinCount;
        int targetPlayerCount = resolveTargetPlayerCount(group);
        if (group.getCurrentCount() + actualJoinCount > targetPlayerCount) {
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

        // 通知已有成员：又来了一位新玩家
        notifyExistingMembersOnJoin(group, userId, actualJoinCount);

        return true;
    }

    /**
     * 新成员加入时通知已有的其他成员
     */
    private void notifyExistingMembersOnJoin(GroupOrder group, Long newUserId, int joinCount) {
        List<GroupMember> allMembers = getGroupMembers(group.getId());
        List<Long> existingUserIds = new ArrayList<>();
        for (GroupMember m : allMembers) {
            if (m.getUserId() != null && !m.getUserId().equals(newUserId)) {
                existingUserIds.add(m.getUserId());
            }
        }
        if (existingUserIds.isEmpty()) {
            return;
        }

        String playTimeText = formatPlayTime(group.getPlayTime());
        int stillNeed = resolveTargetPlayerCount(group) - group.getCurrentCount();
        String content = stillNeed <= 0
                ? String.format("🎉 拼单《%s》人已到齐，马上成团！场次时间：%s，请留意成团通知。",
                        group.getScriptName(), playTimeText)
                : String.format("🎭 又来了 %d 位神秘玩家！拼单《%s》现在已有 %d 人，还差 %d 人就能成团，场次时间：%s。",
                        joinCount, group.getScriptName(), group.getCurrentCount(), stillNeed, playTimeText);

        try {
            notificationService.sendToUsers(
                    "拼单来新伙伴啦",
                    content,
                    2,
                    "group",
                    group.getId(),
                    existingUserIds.toArray(new Long[0])
            );
        } catch (Exception e) {
            log.warn("发送新成员加入通知失败: groupId={}", group.getId(), e);
        }
    }

    /**
     * 发送拼团即将截止提醒（开局前3小时）
     * 由定时任务调用
     */
    @Override
    public void sendDeadlineReminders() {
        LocalDateTime now = LocalDateTime.now();
        // 查找 status=1，且 playTime 在 now+2h 到 now+3h 之间的拼单（即将截止但还没到2小时关闭线）
        LocalDateTime deadlineStart = now.plusHours(GROUP_FORMATION_LEAD_HOURS);       // 2小时后
        LocalDateTime deadlineEnd   = now.plusHours(GROUP_FORMATION_LEAD_HOURS + 1);  // 3小时后

        LambdaQueryWrapper<GroupOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupOrder::getStatus, 1)
                .isNotNull(GroupOrder::getPlayTime)
                .ge(GroupOrder::getPlayTime, deadlineStart)
                .lt(GroupOrder::getPlayTime, deadlineEnd);

        List<GroupOrder> groups = this.list(wrapper);
        for (GroupOrder group : groups) {
            try {
                sendDeadlineReminderForGroup(group, now);
            } catch (Exception e) {
                log.error("发送拼单截止提醒失败: groupId={}", group.getId(), e);
            }
        }
        if (!groups.isEmpty()) {
            log.info("已发送拼单截止提醒: 共 {} 个拼单", groups.size());
        }
    }

    private void sendDeadlineReminderForGroup(GroupOrder group, LocalDateTime now) {
        List<GroupMember> members = getGroupMembers(group.getId());
        List<Long> userIds = members.stream()
                .filter(m -> m.getUserId() != null)
                .map(GroupMember::getUserId)
                .toList();
        if (userIds.isEmpty()) {
            return;
        }

        int stillNeed = resolveTargetPlayerCount(group) - group.getCurrentCount();
        String playTimeText = formatPlayTime(group.getPlayTime());
        String content = String.format(
                "⚠️ 紧急提醒！拼单《%s》距开局仅剩约3小时，还差 %d 人成团。" +
                "若1小时内仍未成团（即距开局前2小时），系统将自动退款。场次时间：%s。",
                group.getScriptName(), stillNeed, playTimeText
        );

        notificationService.sendToUsers(
                "拼单即将截止，快来组队！",
                content,
                2,
                "group",
                group.getId(),
                userIds.toArray(new Long[0])
        );
        log.info("已发送截止提醒: groupId={}, memberCount={}, stillNeed={}", group.getId(), userIds.size(), stillNeed);
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

        // 退出拼团成员
        member.setStatus(0);
        groupMemberMapper.updateById(member);

        group.setCurrentCount(group.getCurrentCount() - member.getJoinCount());
        group.setUpdateTime(LocalDateTime.now());
        this.updateById(group);

        // 对退出者关联的预约进行退款或取消
        handleMemberLeaveRefund(group, userId);

        // 通知剩余成员有人退出，继续等待拼团
        notifyRemainingMembersOnLeave(group, userId);

        return true;
    }

    /**
     * 处理成员退出拼团时的退款逻辑：
     * - 已支付 → 自动退款
     * - 未支付 → 系统取消
     */
    private void handleMemberLeaveRefund(GroupOrder group, Long userId) {
        // 查找该成员关联到此拼单的预约
        LambdaQueryWrapper<Reservation> resWrapper = new LambdaQueryWrapper<>();
        resWrapper.eq(Reservation::getGroupId, group.getId())
                .eq(Reservation::getUserId, userId)
                .eq(Reservation::getIsDeleted, 0)
                .ne(Reservation::getStatus, 4);
        List<Reservation> userReservations = reservationMapper.selectList(resWrapper);

        String leaveReason = "用户主动退出拼单";
        for (Reservation reservation : userReservations) {
            try {
                if (Integer.valueOf(1).equals(reservation.getPayStatus())) {
                    // 已支付 → 退款
                    paymentService.autoRefund(reservation.getId(), leaveReason);
                    log.info("拼团成员退出，已发起退款: groupId={}, userId={}, reservationId={}",
                            group.getId(), userId, reservation.getId());
                } else if (!Integer.valueOf(4).equals(reservation.getStatus())) {
                    // 未支付 → 取消预约
                    cancelReservationBySystem(reservation, leaveReason);
                    log.info("拼团成员退出，已取消未支付预约: groupId={}, userId={}, reservationId={}",
                            group.getId(), userId, reservation.getId());
                }
            } catch (Exception e) {
                log.error("处理退出拼团成员预约失败: groupId={}, userId={}, reservationId={}",
                        group.getId(), userId, reservation.getId(), e);
            }
        }
    }

    /**
     * 通知拼单剩余成员：有人退出，拼团继续等待中
     * 若距开局前2小时内仍未成团，系统将自动退款
     */
    private void notifyRemainingMembersOnLeave(GroupOrder group, Long leavingUserId) {
        List<GroupMember> remainingMembers = getGroupMembers(group.getId());
        List<Long> remainingUserIds = new ArrayList<>();
        for (GroupMember m : remainingMembers) {
            if (m.getUserId() != null && !m.getUserId().equals(leavingUserId)) {
                remainingUserIds.add(m.getUserId());
            }
        }
        if (remainingUserIds.isEmpty()) {
            return;
        }

        String playTimeText = formatPlayTime(group.getPlayTime());
        int stillNeed = resolveTargetPlayerCount(group) - group.getCurrentCount();
        String content = String.format(
                "拼单《%s》有成员退出，当前已有 %d 人，还差 %d 人成团，场次时间：%s。" +
                "请继续等待新成员加入，若距开局前2小时仍未成团，系统将自动退款。",
                group.getScriptName(),
                group.getCurrentCount(),
                stillNeed,
                playTimeText
        );

        try {
            notificationService.sendToUsers(
                    "拼单有人退出，继续等待中",
                    content,
                    2,
                    "group",
                    group.getId(),
                    remainingUserIds.toArray(new Long[0])
            );
        } catch (Exception e) {
            log.warn("发送拼单成员退出通知失败: groupId={}", group.getId(), e);
        }
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
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timeoutTime = now.minusHours(GROUP_TIMEOUT_HOURS);
        LocalDateTime playTimeDeadline = now.plusHours(GROUP_FORMATION_LEAD_HOURS);
        LambdaQueryWrapper<GroupOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupOrder::getStatus, 1)
                .and(query -> query
                        .le(GroupOrder::getCreateTime, timeoutTime)
                        .or(play -> play
                                .isNotNull(GroupOrder::getPlayTime)
                                .le(GroupOrder::getPlayTime, playTimeDeadline)));

        List<GroupOrder> expiredGroups = this.list(wrapper);
        for (GroupOrder group : expiredGroups) {
            try {
                expireGroupIfNeeded(group, now);
            } catch (Exception e) {
                log.error("处理超时拼单失败: groupId={}", group.getId(), e);
            }
        }
    }

    private void checkAndUpdateGroupStatus(GroupOrder group) {
        int targetPlayerCount = resolveTargetPlayerCount(group);
        if (group.getCurrentCount() < targetPlayerCount || group.getStatus() != 1) {
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

        generateCheckInCodesAfterGroupSuccess(group, creatorReservation, activeMembers);
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
            reservation.setScheduleId(creatorReservation.getScheduleId());
            reservation.setGroupId(group.getId());
            reservation.setDmId(creatorReservation.getDmId());
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
            reservationMapper.insert(reservation);

            if (creatorReservation.getScheduleId() != null) {
                try {
                    scriptScheduleService.incrementCurrentPlayers(creatorReservation.getScheduleId(), member.getJoinCount());
                } catch (Exception e) {
                    log.warn("拼团成员正式预约同步排期人数失败: scheduleId={}, reservationId={}",
                            creatorReservation.getScheduleId(), reservation.getId(), e);
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshGroupStatus(Long groupId) {
        if (groupId == null) {
            return;
        }
        GroupOrder group = this.getById(groupId);
        if (group == null || !Integer.valueOf(1).equals(group.getStatus())) {
            return;
        }
        if (!expireGroupIfNeeded(group, LocalDateTime.now())) {
            checkAndUpdateGroupStatus(group);
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

        syncFailedGroupScheduleState(group, creatorReservation, linkedReservations, reason);
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

        LambdaQueryWrapper<Reservation> memberResWrapper = new LambdaQueryWrapper<>();
        memberResWrapper.eq(Reservation::getGroupId, group.getId())
                .eq(Reservation::getIsDeleted, 0)
                .ne(Reservation::getStatus, 4);
        List<Reservation> memberReservations = reservationMapper.selectList(memberResWrapper);
        java.util.Map<Long, Reservation> userReservationMap = new java.util.HashMap<>();
        for (Reservation r : memberReservations) {
            if (r.getUserId() != null && r.getId() != null) {
                userReservationMap.putIfAbsent(r.getUserId(), r);
            }
        }
        if (creatorReservation != null && creatorReservation.getUserId() != null) {
            userReservationMap.putIfAbsent(creatorReservation.getUserId(), creatorReservation);
        }

        if (!creatorIds.isEmpty()) {
            String creatorTitle = "拼单已成团";
            String creatorContent = buildGroupSuccessContent(group, playTimeText, userReservationMap.get(creatorIds.get(0)), true);
            notificationService.sendToUsers(
                    creatorTitle,
                    creatorContent,
                    2,
                    "group",
                    group.getId(),
                    creatorIds.toArray(new Long[0])
            );
        }

        for (Long participantId : participantIds) {
            Reservation participantReservation = userReservationMap.get(participantId);
            Long memberReservationId = participantReservation != null && participantReservation.getId() != null
                    ? participantReservation.getId()
                    : group.getId();
            String title = isPaidReservation(participantReservation) ? "拼单已成团" : "拼单成团，请前往支付";
            String participantContent = buildGroupSuccessContent(group, playTimeText, participantReservation, false);
            try {
                notificationService.sendToUsers(
                        title,
                        participantContent,
                        2,
                        "reservation",
                        memberReservationId,
                        participantId
                );
            } catch (Exception e) {
                log.error("发送拼单成团通知失败: participantId={}", participantId, e);
            }
        }
    }

    private void generateCheckInCodesAfterGroupSuccess(GroupOrder group, Reservation creatorReservation, List<GroupMember> activeMembers) {
        List<Reservation> reservations = new ArrayList<>();
        if (creatorReservation != null) {
            reservations.add(creatorReservation);
        }

        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getGroupId, group.getId())
                .eq(Reservation::getIsDeleted, 0)
                .ne(Reservation::getStatus, 4);
        List<Reservation> linkedReservations = reservationMapper.selectList(wrapper);
        for (Reservation reservation : linkedReservations) {
            if (reservation == null || reservation.getId() == null) {
                continue;
            }
            boolean alreadyAdded = reservations.stream().anyMatch(item -> item.getId().equals(reservation.getId()));
            if (!alreadyAdded) {
                reservations.add(reservation);
            }
        }

        for (Reservation reservation : reservations) {
            if (!isPaidReservation(reservation) || StringUtils.hasText(reservation.getCheckInCode())) {
                continue;
            }
            Reservation update = new Reservation();
            update.setId(reservation.getId());
            update.setCheckInCode(generateCheckInCode());
            reservationMapper.updateById(update);
            reservation.setCheckInCode(update.getCheckInCode());
        }
    }

    private String buildGroupSuccessContent(GroupOrder group, String playTimeText, Reservation reservation, boolean creator) {
        boolean paid = isPaidReservation(reservation);
        boolean hasCheckInCode = reservation != null && StringUtils.hasText(reservation.getCheckInCode());
        int totalMembers = group.getCurrentCount() != null ? group.getCurrentCount() : 0;
        String teamInfo = totalMembers > 0 ? String.format("（共 %d 人）", totalMembers) : "";

        if (creator) {
            if (paid) {
                if (hasCheckInCode) {
                    return String.format("🎉 好消息！您发起的拼单《%s》已成团%s，场次时间：%s。系统已为您生成核销码：%s，请到店后出示使用。",
                            group.getScriptName(), teamInfo, playTimeText, reservation.getCheckInCode());
                }
                return String.format("🎉 好消息！您发起的拼单《%s》已成团%s，场次时间：%s。订单已支付成功，请前往「我的预约」查看最新状态。",
                        group.getScriptName(), teamInfo, playTimeText);
            }
            return String.format("🎉 好消息！您发起的拼单《%s》已成团%s，场次时间：%s。请尽快前往「我的预约」完成支付（30分钟内），逾期将自动取消。",
                    group.getScriptName(), teamInfo, playTimeText);
        }

        if (paid) {
            if (hasCheckInCode) {
                return String.format("您参与的拼单《%s》已成团%s，场次时间：%s。订单已支付成功，系统已为您生成核销码：%s，请前往「我的预约」查看。",
                        group.getScriptName(), teamInfo, playTimeText, reservation.getCheckInCode());
            }
            return String.format("您参与的拼单《%s》已成团%s，场次时间：%s。订单已支付成功，请前往「我的预约」查看最新状态。",
                    group.getScriptName(), teamInfo, playTimeText);
        }
        if (reservation != null) {
            return String.format("您参与的拼单《%s》已成团%s！系统已为您生成正式预约，请尽快前往「我的预约」完成支付（30分钟内）。场次时间：%s。",
                    group.getScriptName(), teamInfo, playTimeText);
        }
        return String.format("您参与的拼单《%s》已成团%s，场次时间：%s，请尽快联系门店确认正式预约。",
                group.getScriptName(), teamInfo, playTimeText);
    }

    private boolean isPaidReservation(Reservation reservation) {
        return reservation != null && Integer.valueOf(1).equals(reservation.getPayStatus());
    }

    private String generateCheckInCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
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
                String.format("拼单《%s》未能在截止时间前成团，系统已自动关闭。%s", group.getScriptName(), reason),
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

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private void cancelReservationBySystem(Reservation reservation, String reason) {
        Reservation update = new Reservation();
        update.setId(reservation.getId());
        update.setStatus(4);
        update.setRemark(reason);
        reservationMapper.updateById(update);
        rollbackSchedulePlayersQuietly(reservation);
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

    private void rollbackSchedulePlayersQuietly(Reservation reservation) {
        if (reservation == null || reservation.getScheduleId() == null) {
            return;
        }
        try {
            scriptScheduleService.decrementCurrentPlayers(
                    reservation.getScheduleId(),
                    reservation.getPlayerCount() == null || reservation.getPlayerCount() <= 0
                            ? 1
                            : reservation.getPlayerCount());
        } catch (Exception e) {
            log.warn("拼团失败回退排期人数失败: reservationId={}, scheduleId={}",
                    reservation.getId(), reservation.getScheduleId(), e);
        }
    }

    private void syncFailedGroupScheduleState(GroupOrder group,
                                              Reservation creatorReservation,
                                              List<Reservation> linkedReservations,
                                              String reason) {
        if (group == null || scriptScheduleService == null) {
            return;
        }

        Long scheduleId = resolveGroupScheduleId(creatorReservation, linkedReservations);
        if (scheduleId == null) {
            return;
        }

        try {
            ScriptSchedule schedule = scriptScheduleService.getById(scheduleId);
            if (schedule == null) {
                return;
            }

            int activePlayers = countActivePlayersForSchedule(scheduleId);
            boolean hasActiveGroup = hasOtherActiveGroupForSameSession(group);
            int requiredPlayers = resolveRequiredPlayers(group, schedule);
            int maxPlayers = schedule.getMaxPlayers() != null && schedule.getMaxPlayers() > 0
                    ? schedule.getMaxPlayers()
                    : requiredPlayers;

            ScriptSchedule update = new ScriptSchedule();
            update.setId(scheduleId);
            update.setCurrentPlayers(activePlayers);

            if (maxPlayers > 0 && activePlayers >= maxPlayers) {
                update.setStatus(0);
            } else if (!hasActiveGroup && activePlayers < requiredPlayers) {
                update.setStatus(2);
                update.setRemark(reason);
            } else {
                update.setStatus(1);
            }

            scriptScheduleService.update(update);
            log.info("拼团失败后已同步排期状态: groupId={}, scheduleId={}, activePlayers={}, requiredPlayers={}, hasActiveGroup={}",
                    group.getId(), scheduleId, activePlayers, requiredPlayers, hasActiveGroup);
        } catch (Exception e) {
            log.warn("拼团失败后同步排期状态失败: groupId={}, scheduleId={}", group.getId(), scheduleId, e);
        }
    }

    private Long resolveGroupScheduleId(Reservation creatorReservation, List<Reservation> linkedReservations) {
        if (creatorReservation != null && creatorReservation.getScheduleId() != null) {
            return creatorReservation.getScheduleId();
        }
        if (linkedReservations == null || linkedReservations.isEmpty()) {
            return null;
        }
        return linkedReservations.stream()
                .map(Reservation::getScheduleId)
                .filter(id -> id != null)
                .findFirst()
                .orElse(null);
    }

    private int countActivePlayersForSchedule(Long scheduleId) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getScheduleId, scheduleId)
                .eq(Reservation::getIsDeleted, 0)
                .ne(Reservation::getStatus, 4);
        List<Reservation> reservations = reservationMapper.selectList(wrapper);
        return reservations.stream()
                .mapToInt(reservation -> reservation.getPlayerCount() != null && reservation.getPlayerCount() > 0
                        ? reservation.getPlayerCount()
                        : 1)
                .sum();
    }

    private boolean hasOtherActiveGroupForSameSession(GroupOrder group) {
        if (group == null || group.getScriptId() == null || group.getStoreId() == null || group.getPlayTime() == null) {
            return false;
        }

        LambdaQueryWrapper<GroupOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupOrder::getStatus, 1)
                .eq(GroupOrder::getScriptId, group.getScriptId())
                .eq(GroupOrder::getStoreId, group.getStoreId())
                .eq(GroupOrder::getPlayTime, group.getPlayTime());
        if (group.getId() != null) {
            wrapper.ne(GroupOrder::getId, group.getId());
        }

        List<GroupOrder> groups = this.list(wrapper);
        LocalDateTime now = LocalDateTime.now();
        return groups.stream().anyMatch(candidate -> !isGroupExpired(candidate, now));
    }

    private GroupOrder findReusableAutoGroup(GroupOrder groupOrder) {
        if (groupOrder == null || groupOrder.getScriptId() == null
                || groupOrder.getStoreId() == null || groupOrder.getPlayTime() == null) {
            return null;
        }

        LambdaQueryWrapper<GroupOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupOrder::getStatus, 1)
                .eq(GroupOrder::getScriptId, groupOrder.getScriptId())
                .eq(GroupOrder::getStoreId, groupOrder.getStoreId())
                .eq(GroupOrder::getPlayTime, groupOrder.getPlayTime())
                .orderByAsc(GroupOrder::getCreateTime);

        LocalDateTime now = LocalDateTime.now();
        List<GroupOrder> groups = this.list(wrapper);
        for (GroupOrder candidate : groups) {
            if (!isGroupExpired(candidate, now)) {
                return candidate;
            }
        }
        return null;
    }

    private void attachReservationToExistingGroup(GroupOrder group,
                                                  Reservation reservation,
                                                  Long userId,
                                                  GroupOrder groupDraft) {
        LocalDateTime now = LocalDateTime.now();
        int reservationCount = resolveJoinCount(reservation.getPlayerCount());

        upsertAutoGroupMember(group, reservation, userId, reservationCount, now);
        bindReservationToGroup(group.getId(), reservation);

        int currentCount = resolveJoinCount(group.getCurrentCount()) + reservationCount;
        int needCount = resolveAutoGroupNeedCount(group, groupDraft, reservation, currentCount);

        GroupOrder update = new GroupOrder();
        update.setId(group.getId());
        update.setCurrentCount(currentCount);
        update.setNeedCount(needCount);
        update.setDescription(buildAutoGroupDescription(currentCount, needCount));
        update.setUpdateTime(now);
        this.updateById(update);

        group.setCurrentCount(currentCount);
        group.setNeedCount(needCount);
        group.setDescription(update.getDescription());
        group.setUpdateTime(now);
        checkAndUpdateGroupStatus(group);
    }

    private void upsertAutoGroupMember(GroupOrder group,
                                       Reservation reservation,
                                       Long userId,
                                       int reservationCount,
                                       LocalDateTime now) {
        LambdaQueryWrapper<GroupMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(GroupMember::getGroupId, group.getId())
                .eq(GroupMember::getUserId, userId)
                .eq(GroupMember::getStatus, 1);
        GroupMember existingMember = groupMemberMapper.selectOne(memberWrapper);
        if (existingMember != null) {
            existingMember.setJoinCount(resolveJoinCount(existingMember.getJoinCount()) + reservationCount);
            groupMemberMapper.updateById(existingMember);
            return;
        }

        User user = userMapper.selectById(userId);
        GroupMember member = new GroupMember();
        member.setGroupId(group.getId());
        member.setUserId(userId);
        member.setNickname(user != null
                ? (StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername())
                : "用户" + userId);
        member.setAvatar(user != null ? user.getAvatar() : null);
        member.setIsCreator(0);
        member.setJoinCount(reservationCount);
        member.setStatus(1);
        member.setCreateTime(now);
        groupMemberMapper.insert(member);
    }

    private void bindReservationToGroup(Long groupId, Reservation reservation) {
        if (groupId == null || reservation == null || reservation.getId() == null) {
            return;
        }
        Reservation update = new Reservation();
        update.setId(reservation.getId());
        update.setGroupId(groupId);
        reservationMapper.updateById(update);
        reservation.setGroupId(groupId);
    }

    private int resolveAutoGroupNeedCount(GroupOrder existingGroup,
                                          GroupOrder groupDraft,
                                          Reservation reservation,
                                          int currentCount) {
        int requiredPlayers = 0;
        if (groupDraft != null && groupDraft.getPlayerCount() != null && groupDraft.getPlayerCount() > 0) {
            requiredPlayers = groupDraft.getPlayerCount();
        } else if (existingGroup != null && existingGroup.getPlayerCount() != null && existingGroup.getPlayerCount() > 0) {
            requiredPlayers = existingGroup.getPlayerCount();
        }

        if (requiredPlayers <= 0) {
            int fallbackNeedCount = existingGroup != null && existingGroup.getNeedCount() != null && existingGroup.getNeedCount() > 0
                    ? existingGroup.getNeedCount()
                    : currentCount;
            return Math.max(currentCount, fallbackNeedCount);
        }

        return Math.max(currentCount, requiredPlayers);
    }

    private int countActivePlayersOutsideGroup(Long scheduleId, Long groupId) {
        if (scheduleId == null) {
            return 0;
        }

        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getScheduleId, scheduleId)
                .eq(Reservation::getIsDeleted, 0)
                .ne(Reservation::getStatus, 4);

        List<Reservation> reservations = reservationMapper.selectList(wrapper);
        int count = 0;
        for (Reservation item : reservations) {
            if (groupId != null && groupId.equals(item.getGroupId())) {
                continue;
            }
            count += resolveJoinCount(item.getPlayerCount());
        }
        return count;
    }

    private GroupOrder buildAutoGroupDraft(Reservation reservation) {
        if (reservation == null || reservation.getUserId() == null || reservation.getScriptId() == null
                || reservation.getStoreId() == null || reservation.getReservationTime() == null) {
            return null;
        }

        Script script = scriptMapper.selectById(reservation.getScriptId());
        if (script == null || script.getPlayerCount() == null || reservation.getPlayerCount() == null) {
            return null;
        }
        if (reservation.getPlayerCount() >= script.getPlayerCount()) {
            return null;
        }

        GroupOrder groupOrder = new GroupOrder();
        groupOrder.setScriptId(reservation.getScriptId());
        groupOrder.setScriptName(script.getName());
        groupOrder.setStoreId(reservation.getStoreId());
        groupOrder.setStoreName(resolveStoreName(reservation.getStoreId()));
        groupOrder.setPlayTime(reservation.getReservationTime());
        groupOrder.setCurrentCount(resolveJoinCount(reservation.getPlayerCount()));
        groupOrder.setNeedCount(script.getPlayerCount());
        groupOrder.setPlayerCount(script.getPlayerCount());
        groupOrder.setPrice(script.getPrice());
        groupOrder.setNewbieWelcome(1);
        groupOrder.setReservationId(reservation.getId());
        return groupOrder;
    }

    private int countPaidPlayersForSession(Reservation reservation) {
        if (reservation == null) {
            return 0;
        }

        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getIsDeleted, 0)
                .eq(Reservation::getPayStatus, 1)
                .ne(Reservation::getStatus, 4)
                .eq(Reservation::getScriptId, reservation.getScriptId())
                .eq(Reservation::getStoreId, reservation.getStoreId())
                .eq(Reservation::getReservationTime, reservation.getReservationTime());
        if (reservation.getScheduleId() != null) {
            wrapper.eq(Reservation::getScheduleId, reservation.getScheduleId());
        }

        List<Reservation> reservations = reservationMapper.selectList(wrapper);
        return reservations.stream()
                .mapToInt(item -> resolveJoinCount(item.getPlayerCount()))
                .sum();
    }

    private String resolveStoreName(Long storeId) {
        if (storeId == null || storeMapper == null) {
            return "";
        }
        com.murder.entity.Store store = storeMapper.selectById(storeId);
        return store != null && StringUtils.hasText(store.getName()) ? store.getName() : "";
    }

    private String buildAutoGroupDescription(int currentCount, int needCount) {
        int remaining = Math.max(0, needCount - currentCount);
        return remaining > 0
                ? "预约自动发起的拼团，还差" + remaining + "位玩家"
                : "预约自动发起的拼团，已满足成团人数";
    }

    private int resolveJoinCount(Integer count) {
        return count != null && count > 0 ? count : 1;
    }

    private int resolveTargetPlayerCount(GroupOrder group) {
        if (group == null) {
            return 1;
        }
        if (group.getPlayerCount() != null && group.getPlayerCount() > 0) {
            return Math.max(resolveJoinCount(group.getCurrentCount()), group.getPlayerCount());
        }
        if (group.getNeedCount() != null && group.getNeedCount() > 0) {
            return Math.max(resolveJoinCount(group.getCurrentCount()), group.getNeedCount());
        }
        return Math.max(1, resolveJoinCount(group.getCurrentCount()));
    }

    private int resolveRequiredPlayers(GroupOrder group, ScriptSchedule schedule) {
        if (group != null && group.getPlayerCount() != null && group.getPlayerCount() > 0) {
            return group.getPlayerCount();
        }
        if (schedule != null && schedule.getPlayerCount() != null && schedule.getPlayerCount() > 0) {
            return schedule.getPlayerCount();
        }
        if (schedule != null && schedule.getMaxPlayers() != null && schedule.getMaxPlayers() > 0) {
            return schedule.getMaxPlayers();
        }
        return 1;
    }

    private Map<String, Object> convertToMap(GroupOrder group) {
        Script script = resolveScript(group);
        ScriptCategory category = resolveCategory(script);
        LocalDateTime expireTime = resolveGroupExpireTime(group);
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
        map.put("needCount", resolveTargetPlayerCount(group));
        map.put("playerCount", group.getPlayerCount());
        map.put("price", group.getPrice());
        map.put("genderRequirement", group.getGenderRequirement());
        map.put("newbieWelcome", group.getNewbieWelcome());
        map.put("description", group.getDescription());
        map.put("status", resolveDisplayStatus(group));
        map.put("expireTime", expireTime);
        map.put("createTime", group.getCreateTime());
        return map;
    }

    private Map<String, Object> convertToMapAnonymous(GroupOrder group) {
        Script script = resolveScript(group);
        ScriptCategory category = resolveCategory(script);
        LocalDateTime expireTime = resolveGroupExpireTime(group);
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
        map.put("needCount", resolveTargetPlayerCount(group));
        map.put("playerCount", group.getPlayerCount());
        map.put("price", group.getPrice());
        map.put("genderRequirement", group.getGenderRequirement());
        map.put("newbieWelcome", group.getNewbieWelcome());
        map.put("description", group.getDescription());
        map.put("difficulty", 2);
        map.put("status", resolveDisplayStatus(group));
        map.put("expireTime", expireTime);
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

    private void applyActiveStatusFilter(LambdaQueryWrapper<GroupOrder> wrapper, LocalDateTime now) {
        wrapper.eq(GroupOrder::getStatus, 1)
                .gt(GroupOrder::getCreateTime, now.minusHours(GROUP_TIMEOUT_HOURS))
                .and(query -> query
                        .isNull(GroupOrder::getPlayTime)
                        .or(active -> active
                                .isNull(GroupOrder::getReservationId)
                                .gt(GroupOrder::getPlayTime, now.plusHours(GROUP_FORMATION_LEAD_HOURS)))
                        .or(active -> active
                                .isNotNull(GroupOrder::getReservationId)
                                .gt(GroupOrder::getPlayTime, now)));
    }

    private void validateGroupTimeline(GroupOrder groupOrder, LocalDateTime now) {
        if (groupOrder.getPlayTime() == null) {
            throw new RuntimeException("请选择开车时间");
        }
        if (!groupOrder.getPlayTime().isAfter(now)) {
            throw new RuntimeException("开车时间必须晚于当前时间");
        }

        if (isAutoReservationGroup(groupOrder)) {
            return;
        }

        LocalDateTime expireTime = resolveGroupExpireTime(now, groupOrder.getPlayTime(), false);
        if (expireTime == null || !expireTime.isAfter(now)) {
            throw new RuntimeException(String.format("该场次距离开场不足%d小时，暂时不能发起拼团", GROUP_FORMATION_LEAD_HOURS));
        }
    }

    private boolean expireGroupIfNeeded(GroupOrder group, LocalDateTime now) {
        if (group == null || group.getStatus() == null || group.getStatus() != 1) {
            return false;
        }
        if (!isGroupExpired(group, now)) {
            return false;
        }

        group.setStatus(0);
        group.setUpdateTime(now);
        this.updateById(group);
        handleGroupFailed(group, buildExpiredReason(group));
        return true;
    }

    private boolean isAutoReservationGroup(GroupOrder group) {
        return group != null && group.getReservationId() != null;
    }

    private boolean isGroupExpired(GroupOrder group, LocalDateTime now) {
        LocalDateTime expireTime = resolveGroupExpireTime(group);
        return expireTime != null && !expireTime.isAfter(now);
    }

    private int resolveDisplayStatus(GroupOrder group) {
        if (group == null || group.getStatus() == null) {
            return 0;
        }
        if (group.getStatus() == 1 && isGroupExpired(group, LocalDateTime.now())) {
            return 0;
        }
        return group.getStatus();
    }

    private LocalDateTime resolveGroupExpireTime(GroupOrder group) {
        if (group == null) {
            return null;
        }
        return resolveGroupExpireTime(group.getCreateTime(), group.getPlayTime(), isAutoReservationGroup(group));
    }

    private LocalDateTime resolveGroupExpireTime(LocalDateTime createTime, LocalDateTime playTime, boolean autoReservationGroup) {
        if (createTime == null) {
            return null;
        }

        LocalDateTime timeoutExpireTime = createTime.plusHours(GROUP_TIMEOUT_HOURS);
        if (playTime == null) {
            return timeoutExpireTime;
        }

        LocalDateTime playTimeDeadline = autoReservationGroup
                ? playTime
                : playTime.minusHours(GROUP_FORMATION_LEAD_HOURS);
        return playTimeDeadline.isBefore(timeoutExpireTime) ? playTimeDeadline : timeoutExpireTime;
    }

    private String buildExpiredReason(GroupOrder group) {
        LocalDateTime createTime = group != null ? group.getCreateTime() : null;
        LocalDateTime playTime = group != null ? group.getPlayTime() : null;
        if (createTime == null) {
            return "拼单已超过成团时限，系统自动关闭";
        }

        LocalDateTime timeoutExpireTime = createTime.plusHours(GROUP_TIMEOUT_HOURS);
        if (playTime != null) {
            LocalDateTime playTimeDeadline = isAutoReservationGroup(group)
                    ? playTime
                    : playTime.minusHours(GROUP_FORMATION_LEAD_HOURS);
            if (!playTimeDeadline.isAfter(timeoutExpireTime)) {
                return isAutoReservationGroup(group)
                        ? "拼单在开场前仍未成团，系统自动关闭"
                        : String.format("距离开场不足%d小时仍未成团，系统自动关闭", GROUP_FORMATION_LEAD_HOURS);
            }
        }
        return String.format("拼单%d小时内未成团，系统自动关闭", GROUP_TIMEOUT_HOURS);
    }

    private String buildExpiredActionMessage(GroupOrder group) {
        LocalDateTime playTime = group != null ? group.getPlayTime() : null;
        if (playTime != null) {
            LocalDateTime deadline = isAutoReservationGroup(group)
                    ? playTime
                    : playTime.minusHours(GROUP_FORMATION_LEAD_HOURS);
            if (!deadline.isAfter(LocalDateTime.now())) {
                return isAutoReservationGroup(group)
                        ? "该拼单已到开场时间仍未成团，已自动关闭"
                        : String.format("该拼单距离开场不足%d小时，已自动关闭", GROUP_FORMATION_LEAD_HOURS);
            }
        }
        return "该拼单已超过成团时限，已自动关闭";
    }
}
