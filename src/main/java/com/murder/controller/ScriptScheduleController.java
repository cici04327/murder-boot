package com.murder.controller;

import com.murder.common.result.Result;
import com.murder.entity.ScriptSchedule;
import com.murder.service.ScriptScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 剧本排期控制器
 */
@RestController
@RequestMapping("/api/script/schedule")
@Tag(name = "剧本排期接口")
@Slf4j
public class ScriptScheduleController {

    @Autowired
    private ScriptScheduleService scriptScheduleService;

    /**
     * 查询门店某日期的排期列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询排期列表")
    public Result<List<ScriptSchedule>> list(
            @RequestParam Long storeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        log.info("查询排期列表: storeId={}, date={}", storeId, date);
        List<ScriptSchedule> list = scriptScheduleService.listByStoreAndDate(storeId, date);
        return Result.success(list);
    }

    /**
     * 查询门店日期范围内的排期
     */
    @GetMapping("/range")
    @Operation(summary = "查询日期范围排期")
    public Result<List<ScriptSchedule>> listByRange(
            @RequestParam Long storeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        log.info("查询日期范围排期: storeId={}, {} - {}", storeId, startDate, endDate);
        List<ScriptSchedule> list = scriptScheduleService.listByStoreAndDateRange(storeId, startDate, endDate);
        return Result.success(list);
    }

    /**
     * 查询当前 DM 在门店某日期的排期列表
     */
    @GetMapping("/mine")
    @Operation(summary = "查询我的排期列表")
    public Result<List<ScriptSchedule>> listMine(
            @RequestParam Long storeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        log.info("查询我的排期列表: storeId={}, date={}", storeId, date);
        List<ScriptSchedule> list = scriptScheduleService.listMySchedulesByDate(storeId, date);
        return Result.success(list);
    }

    /**
     * 查询当前 DM 在门店日期范围内的排期
     */
    @GetMapping("/mine/range")
    @Operation(summary = "查询我的日期范围排期")
    public Result<List<ScriptSchedule>> listMineByRange(
            @RequestParam Long storeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        log.info("查询我的日期范围排期: storeId={}, {} - {}", storeId, startDate, endDate);
        List<ScriptSchedule> list = scriptScheduleService.listMySchedulesByDateRange(storeId, startDate, endDate);
        return Result.success(list);
    }

    /**
     * 新增排期
     */
    @PostMapping
    @Operation(summary = "新增排期")
    public Result<String> add(@RequestBody ScriptSchedule schedule) {
        log.info("新增排期: {}", schedule);
        scriptScheduleService.add(schedule);
        return Result.success("新增成功");
    }

    /**
     * 批量新增排期
     */
    @PostMapping("/batch")
    @Operation(summary = "批量新增排期")
    public Result<String> batchAdd(@RequestBody List<ScriptSchedule> schedules) {
        log.info("批量新增排期: {}条", schedules.size());
        scriptScheduleService.batchAdd(schedules);
        return Result.success("批量新增成功");
    }

    /**
     * 批量生成排期
     */
    @PostMapping("/generate")
    @Operation(summary = "批量生成排期")
    public Result<String> generate(@RequestBody Map<String, Object> params) {
        Long storeId = Long.valueOf(params.get("storeId").toString());
        Long scriptId = Long.valueOf(params.get("scriptId").toString());
        Long roomId = Long.valueOf(params.get("roomId").toString());
        LocalDate startDate = LocalDate.parse(params.get("startDate").toString());
        LocalDate endDate = LocalDate.parse(params.get("endDate").toString());
        @SuppressWarnings("unchecked")
        List<String> timeSlots = (List<String>) params.get("timeSlots");
        Integer maxPlayers = Integer.valueOf(params.get("maxPlayers").toString());
        Long dmId = params.get("dmId") == null || params.get("dmId").toString().isBlank()
                ? null
                : Long.valueOf(params.get("dmId").toString());

        log.info("批量生成排期: storeId={}, scriptId={}, roomId={}, dmId={}, {} - {}", storeId, scriptId, roomId, dmId, startDate, endDate);
        scriptScheduleService.generateSchedules(storeId, scriptId, roomId, startDate, endDate, timeSlots, maxPlayers, dmId);
        return Result.success("生成成功");
    }

    /**
     * 批量生成预检查
     */
    @PostMapping("/generate/precheck")
    @Operation(summary = "批量生成排期预检查")
    public Result<Map<String, Object>> precheckGenerate(@RequestBody Map<String, Object> params) {
        Long storeId = Long.valueOf(params.get("storeId").toString());
        Long scriptId = Long.valueOf(params.get("scriptId").toString());
        Long roomId = Long.valueOf(params.get("roomId").toString());
        LocalDate startDate = LocalDate.parse(params.get("startDate").toString());
        LocalDate endDate = LocalDate.parse(params.get("endDate").toString());
        @SuppressWarnings("unchecked")
        List<String> timeSlots = (List<String>) params.get("timeSlots");
        Integer maxPlayers = Integer.valueOf(params.get("maxPlayers").toString());
        Long dmId = params.get("dmId") == null || params.get("dmId").toString().isBlank()
                ? null
                : Long.valueOf(params.get("dmId").toString());

        return Result.success(scriptScheduleService.precheckGenerateSchedules(
                storeId, scriptId, roomId, startDate, endDate, timeSlots, maxPlayers, dmId));
    }

    /**
     * 更新排期
     */
    @PutMapping
    @Operation(summary = "更新排期")
    public Result<String> update(@RequestBody ScriptSchedule schedule) {
        log.info("更新排期: {}", schedule);
        scriptScheduleService.update(schedule);
        return Result.success("更新成功");
    }

    /**
     * 更新排期状态
     */
    @PutMapping("/status")
    @Operation(summary = "更新排期状态")
    public Result<String> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        log.info("更新排期状态: id={}, status={}", id, status);
        scriptScheduleService.updateStatus(id, status);
        return Result.success("状态更新成功");
    }

    /**
     * 删除排期
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除排期")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除排期: id={}", id);
        scriptScheduleService.delete(id);
        return Result.success("删除成功");
    }

    /**
     * 查询排期详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询排期详情")
    public Result<ScriptSchedule> getById(@PathVariable Long id) {
        log.info("查询排期详情: id={}", id);
        ScriptSchedule schedule = scriptScheduleService.getById(id);
        return Result.success(schedule);
    }

    /**
     * 实时冲突检测（前端新增/编辑排期表单填写完时间后调用）
     * 返回 conflict=true/false，冲突时附带冲突场次信息
     */
    @GetMapping("/check-conflict")
    @Operation(summary = "排期时段冲突检测", description = "新增传excludeId=null，编辑传自身ID，检测同门店同房间同日期时段是否冲突")
    public Result<Map<String, Object>> checkConflict(
            @RequestParam Long storeId,
            @RequestParam Long roomId,
            @RequestParam String scheduleDate,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) Long excludeId) {
        log.info("冲突检测: storeId={}, roomId={}, date={}, {}-{}, excludeId={}",
                storeId, roomId, scheduleDate, startTime, endTime, excludeId);
        Map<String, Object> result = scriptScheduleService.checkConflict(
                storeId, roomId, scheduleDate, startTime, endTime, excludeId);
        return Result.success(result);
    }

    /**
     * 查询可约场次（用户端）- 含余量展示
     * 返回指定剧本/门店未来N天内 status=1 且未满员的场次
     */
    @GetMapping("/available")
    @Operation(summary = "查询可约场次（含余量）", description = "供用户端展示剧本详情页/门店详情页的可约场次和剩余名额")
    public Result<List<ScriptSchedule>> getAvailable(
            @RequestParam(required = false) Long scriptId,
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "7") Integer days) {
        log.info("查询可约场次: scriptId={}, storeId={}, days={}", scriptId, storeId, days);
        List<ScriptSchedule> list = scriptScheduleService.getAvailableSchedules(scriptId, storeId, days);
        return Result.success(list);
    }
}
