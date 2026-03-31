package com.murder.controller;

import com.murder.common.context.BaseContext;
import com.murder.common.exception.BaseException;
import com.murder.common.result.Result;
import com.murder.dto.AiDmAssignDTO;
import com.murder.dto.AiDmConfirmDTO;
import com.murder.service.AiScheduleService;
import com.murder.vo.AiDmSuggestVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/ai/schedule")
@RequiredArgsConstructor
@Tag(name = "AI智能DM分配", description = "AI驱动的DM智能分配接口")
public class AiScheduleController {

    private final AiScheduleService aiScheduleService;

    @PostMapping("/dm/suggest")
    @Operation(summary = "AI智能推荐DM分配方案")
    public Result<AiDmSuggestVO> suggestDm(@RequestBody AiDmAssignDTO dto) {
        dto.setStoreId(resolveScopedStoreId(dto.getStoreId()));
        log.info("AI DM suggest, scopedStoreId={}, start={}, end={}", dto.getStoreId(), dto.getStartDate(), dto.getEndDate());
        return Result.success(aiScheduleService.suggestDmAssignment(dto));
    }

    @PostMapping("/dm/confirm")
    @Operation(summary = "确认并应用DM分配方案")
    public Result<Void> confirmDm(@RequestBody AiDmConfirmDTO dto) {
        requireAiScheduleAccess();
        log.info("Confirm DM assignment, items={}", dto.getItems() != null ? dto.getItems().size() : 0);
        aiScheduleService.confirmDmAssignment(dto);
        return Result.success();
    }

    @GetMapping("/task/list")
    @Operation(summary = "查询AI分配任务历史")
    public Result<List<?>> listTasks(
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "20") int limit) {
        Long scopedStoreId = resolveScopedStoreId(storeId);
        return Result.success(aiScheduleService.listTaskHistory(scopedStoreId, limit));
    }

    private void requireAiScheduleAccess() {
        resolveScopedStoreId(null);
    }

    private Long resolveScopedStoreId(Long requestedStoreId) {
        String role = BaseContext.getRole();
        if ("SUPER_ADMIN".equals(role)) {
            if (requestedStoreId == null) {
                throw new BaseException("请指定门店");
            }
            return requestedStoreId;
        }
        if ("STORE_ADMIN".equals(role)) {
            Long storeId = BaseContext.getStoreId();
            if (storeId == null) {
                throw new BaseException("当前管理员未绑定门店");
            }
            return storeId;
        }
        throw new BaseException("无权限访问AI排班");
    }
}
