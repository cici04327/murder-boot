# 门店员工权限模型设计

日期：2026-03-22

## 1. 当前项目现状

结合当前代码，管理端真正接入登录和权限的只有两类身份：

- `SUPER_ADMIN`
- `STORE_ADMIN`

对应代码位置：

- [UserServiceImpl.java](/D:/murder-boot/src/main/java/com/murder/service/impl/UserServiceImpl.java)
- [ReservationController.java](/D:/murder-boot/src/main/java/com/murder/controller/ReservationController.java)
- [ReservationServiceImpl.java](/D:/murder-boot/src/main/java/com/murder/service/impl/ReservationServiceImpl.java)
- [JwtTokenInterceptor.java](/D:/murder-boot/src/main/java/com/murder/interceptor/JwtTokenInterceptor.java)
- [frontend/admin/src/router/index.js](/D:/murder-boot/frontend/admin/src/router/index.js)
- [frontend/admin/src/views/login/index.vue](/D:/murder-boot/frontend/admin/src/views/login/index.vue)

目前虽然已经有门店员工资料表，但它还没有接入认证体系：

- [StoreEmployee.java](/D:/murder-boot/src/main/java/com/murder/entity/StoreEmployee.java)
- [StoreEmployeeVO.java](/D:/murder-boot/src/main/java/com/murder/vo/StoreEmployeeVO.java)
- [StoreEmployeeController.java](/D:/murder-boot/src/main/java/com/murder/controller/StoreEmployeeController.java)

现有 `store_employee.position` 含义：

- `1 店长`
- `2 副店长`
- `3 主持人`
- `4 服务员`

但现在这张表只是“员工资料”，还没有：

- 员工登录账号
- 员工密码
- 员工权限点
- DM 与员工账号的绑定
- 员工级别的菜单可见范围

所以你现在想要的“店长 / 店员 / DM 也能核销预约”，业务上是合理的，但技术上还没有真正的账号和权限支撑。

## 2. 目标

在保留现有：

- `SUPER_ADMIN`
- `STORE_ADMIN`

的前提下，新增“门店员工账号体系”，让：

- 店长
- 店员
- DM

可以登录门店后台，并按门店范围和岗位权限执行操作。

核心目标：

1. 店长、店员、DM 都可以核销预约
2. DM 只能操作分配给自己的场次/预约
3. 店员只能做低风险动作，不给退款、手动支付、跨店查看等高风险权限
4. 店长可以做门店运营动作，但仍不等同于超级管理员
5. 所有员工权限都必须受门店范围约束

## 3. 推荐方案

### 3.1 不建议继续往 `user.role` 里硬塞更多全局角色

当前 `user.role` 更适合放“平台级身份”：

- `USER`
- `SUPER_ADMIN`
- `STORE_ADMIN`

如果继续把：

- `STORE_MANAGER`
- `STORE_CLERK`
- `STORE_DM`

都塞到 `user.role`，会导致普通用户表承担太多后台身份，后续菜单、JWT、数据范围都会越来越乱。

### 3.2 推荐新增“门店员工账号表”

建议新增一张表：`store_employee_account`

用途：

- `store_employee` 继续做员工资料表
- `store_employee_account` 负责登录和权限

建议字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `employee_id` | 关联 `store_employee.id` |
| `store_id` | 冗余门店ID，便于快速做权限范围校验 |
| `login_account` | 登录账号 |
| `login_password` | 登录密码（MD5，先与现有项目保持一致） |
| `staff_role` | 员工角色：`MANAGER / CLERK / DM` |
| `permission_codes` | 权限编码，逗号分隔或 JSON |
| `dm_id` | 仅 DM 账号使用，关联 `dm.id` |
| `status` | 1启用，0禁用 |
| `last_login_time` | 最后登录时间 |
| `is_deleted` | 逻辑删除 |
| `create_time` | 创建时间 |
| `update_time` | 更新时间 |

说明：

- `店长 / 店员 / DM` 建议作为“门店员工角色”，放在 `store_employee_account.staff_role`
- `store_employee.position` 继续保留，用于 HR/人员展示
- 两者可以保持映射，但不要强绑定成完全同一字段

## 4. 角色与权限矩阵

### 4.1 平台级身份

| 身份 | 范围 | 说明 |
| --- | --- | --- |
| `SUPER_ADMIN` | 全平台 | 总部权限 |
| `STORE_ADMIN` | 本门店 | 门店主账号 |
| `STORE_STAFF` | 本门店 | 员工账号，通过 `staff_role + permission_codes` 细分 |

### 4.2 门店员工角色建议

| 员工角色 | 说明 |
| --- | --- |
| `MANAGER` | 店长/副店长 |
| `CLERK` | 店员/前台 |
| `DM` | 主持人 |

### 4.3 权限点建议

建议用权限点，而不是只看角色名：

- `reservation:view`
- `reservation:confirm`
- `reservation:checkin`
- `reservation:complete`
- `reservation:manual_pay`
- `reservation:assign_dm`
- `reservation:reschedule`
- `reservation:refund_process`
- `schedule:view`
- `schedule:update`
- `dm:view`
- `dm:assign`
- `store:profile:view`
- `statistics:store`

### 4.4 默认权限分配

#### 超级管理员

- 全部权限
- 全部门店范围

#### 门店管理员

- 本店全部运营权限
- 仍建议保留：
  - 退款处理
  - 手动置已支付
  - DM 分配
  - 排期维护

#### 店长 `MANAGER`

建议拥有：

- `reservation:view`
- `reservation:confirm`
- `reservation:checkin`
- `reservation:complete`
- `reservation:assign_dm`
- `schedule:view`
- `schedule:update`
- `dm:view`
- `statistics:store`
- `store:profile:view`

默认不建议给：

- `reservation:refund_process`
- `reservation:manual_pay`

除非你业务上明确允许店长处理资金动作。

#### 店员 `CLERK`

建议拥有：

- `reservation:view`
- `reservation:checkin`

可选：

- `store:profile:view`

不建议给：

- 确认预约
- 完成预约
- 手动支付
- 退款处理
- 分配 DM
- 排期修改

#### DM

建议拥有：

- `reservation:view`
- `reservation:checkin`
- `reservation:complete`
- `schedule:view`

但 DM 的权限必须附带“仅本人场次”约束：

- 只能看自己被分配的预约
- 只能核销自己场次对应的预约
- 只能完成自己场次对应的预约

## 5. DM 账号和 DM 数据如何关联

当前项目中：

- `store_employee` 表里有“主持人”职位
- 预约/排期又是通过 `dm.id` 来指向 DM

见：

- [Dm.java](/D:/murder-boot/src/main/java/com/murder/entity/Dm.java)
- [ReservationServiceImpl.java](/D:/murder-boot/src/main/java/com/murder/service/impl/ReservationServiceImpl.java)

所以如果要让 DM 登录后只操作自己的预约，必须补一层账号绑定关系。

推荐方案：

- 在 `store_employee_account.dm_id` 存对应 `dm.id`
- 仅当 `staff_role = DM` 时允许有 `dm_id`
- 登录后把 `dmId` 放进 JWT

这样在校验时可以直接判断：

- `reservation.dm_id == BaseContext.dmId`
- 或 `schedule.dm_id == BaseContext.dmId`

## 6. JWT 与上下文建议

当前 JWT 只放：

- `userId`
- `username`
- `phone`
- `role`
- `storeId`

见：

- [JwtClaimsConstant.java](/D:/murder-boot/src/main/java/com/murder/common/constant/JwtClaimsConstant.java)
- [BaseContext.java](/D:/murder-boot/src/main/java/com/murder/common/context/BaseContext.java)

建议扩展为：

- `role`
- `storeId`
- `employeeId`
- `staffRole`
- `dmId`
- `permissionCodes`

需要新增：

- `JwtClaimsConstant.EMPLOYEE_ID`
- `JwtClaimsConstant.STAFF_ROLE`
- `JwtClaimsConstant.DM_ID`
- `JwtClaimsConstant.PERMISSION_CODES`

`BaseContext` 也建议扩展为：

- `employeeId`
- `staffRole`
- `dmId`
- `permissionCodes`

## 7. 后端接口拦截与服务层改造

### 7.1 登录入口

建议保留现有两条：

- `/api/user/admin/login`：总部管理员
- `/api/store/login`：门店管理员主账号

新增：

- `/api/store/employee/login`：门店员工账号登录

返回统一结构：

- `token`
- `role`
- `storeId`
- `staffRole`
- `permissionCodes`
- `employeeId`
- `dmId`

### 7.2 鉴权拦截

当前 `JwtTokenInterceptor` 主要识别：

- `/api/admin/**`
- 少数管理动作接口

建议改造成两层判断：

1. 是否是管理端 JWT
2. 是否具备具体权限点

不要再只靠：

- `SUPER_ADMIN`
- `STORE_ADMIN`

做硬编码判断。

### 7.3 预约相关权限建议

当前这些接口都只认管理员：

- `confirm`
- `check-in`
- `complete`
- `pay`
- `assign-dm`

建议改成：

| 接口 | SUPER_ADMIN | STORE_ADMIN | MANAGER | CLERK | DM |
| --- | --- | --- | --- | --- | --- |
| `confirm` | 是 | 是 | 是 | 否 | 否 |
| `check-in` | 是 | 是 | 是 | 是 | 是（仅本人场次） |
| `complete` | 是 | 是 | 是 | 否 | 是（仅本人场次） |
| `pay` | 是 | 是 | 否 | 否 | 否 |
| `assign-dm` | 是 | 是 | 是 | 否 | 否 |
| `refund_process` | 是 | 是 | 可选 | 否 | 否 |

### 7.4 服务层校验建议

当前 [ReservationServiceImpl.java](/D:/murder-boot/src/main/java/com/murder/service/impl/ReservationServiceImpl.java) 里是：

- `assertAdminStoreScope`
- `assertOwnerOrAdminScope`

建议拆成：

- `assertReservationPermission(reservation, permissionCode)`
- `assertReservationAssignedDmScope(reservation)`
- `assertOwnerOrReservationPermission(reservation, permissionCode)`

判断顺序建议：

1. `SUPER_ADMIN` 直接通过
2. `STORE_ADMIN` 校验 `storeId`
3. `STORE_STAFF`
   - 校验 `storeId`
   - 校验 `permissionCodes`
   - 如果是 DM，再校验 `dmId`

### 7.5 统计权限建议

店长可以看本店统计，店员和 DM 默认不看。

建议：

- `operation-board`
- `store-daily-report`

只允许：

- `SUPER_ADMIN`
- `STORE_ADMIN`
- `MANAGER`

## 8. 管理端前端菜单建议

当前前端路由只分：

- `admin`
- `store`

见：

- [frontend/admin/src/router/index.js](/D:/murder-boot/frontend/admin/src/router/index.js)
- [frontend/admin/src/layout/index.vue](/D:/murder-boot/frontend/admin/src/layout/index.vue)

建议改成：

- `super-admin`
- `store-admin`
- `store-manager`
- `store-clerk`
- `store-dm`

或者更推荐：

- 保留登录类型
- 菜单按 `permissionCodes` 过滤

### 8.1 菜单建议

#### 店长

- 工作台
- 预约列表
- 预约详情
- 排期管理
- DM 管理
- 评价管理
- 门店信息
- 经营看板

#### 店员

- 工作台
- 预约列表
- 预约详情

#### DM

- 工作台
- 我的场次
- 我的预约

这里“我的场次 / 我的预约”可以先不新建页面，第一版直接复用现有页面并在请求里加：

- `onlyMine=true`
- 后端按 `dmId` 过滤

## 9. 推荐实施顺序

### 第一阶段：最小可用版本

目标：先让店长 / 店员 / DM 真正能核销预约

做这些就够：

1. 新增 `store_employee_account`
2. 新增员工登录接口 `/api/store/employee/login`
3. JWT 增加：
   - `employeeId`
   - `staffRole`
   - `dmId`
   - `permissionCodes`
4. 后端把 `check-in` 权限放给：
   - `STORE_ADMIN`
   - `MANAGER`
   - `CLERK`
   - `DM(assigned)`
5. 管理端登录页新增“员工登录”
6. 管理端菜单先只开放：
   - 工作台
   - 预约列表
   - 预约详情

### 第二阶段：补齐 DM / 店长能力

1. `complete` 权限开放给：
   - `MANAGER`
   - `DM(assigned)`
2. `confirm` 和 `assign-dm` 开放给：
   - `MANAGER`
3. 新增“我的场次 / 我的预约”

### 第三阶段：完整员工后台

1. 员工账号管理页
2. 权限点可配置
3. 店长 / 店员 / DM 菜单精细化
4. 统计、门店信息、DM 管理等扩展权限

## 10. 对当前项目最适合的落地结论

对于你这个项目，我建议最终采用下面这套：

1. `SUPER_ADMIN` 保持不变
2. `STORE_ADMIN` 保持为门店主账号
3. 新增 `STORE_STAFF` 员工账号体系
4. 员工账号按：
   - `MANAGER`
   - `CLERK`
   - `DM`
   做细分
5. 第一批先开放：
   - 店长核销
   - 店员核销
   - DM 核销本人场次

这是和你当前代码结构最兼容、改造成本最低、业务也最合理的一条路。

## 11. 如果下一步直接开始开发，建议先改哪些文件

后端优先：

- [JwtClaimsConstant.java](/D:/murder-boot/src/main/java/com/murder/common/constant/JwtClaimsConstant.java)
- [BaseContext.java](/D:/murder-boot/src/main/java/com/murder/common/context/BaseContext.java)
- [JwtTokenInterceptor.java](/D:/murder-boot/src/main/java/com/murder/interceptor/JwtTokenInterceptor.java)
- [ReservationController.java](/D:/murder-boot/src/main/java/com/murder/controller/ReservationController.java)
- [ReservationServiceImpl.java](/D:/murder-boot/src/main/java/com/murder/service/impl/ReservationServiceImpl.java)
- [StoreEmployeeController.java](/D:/murder-boot/src/main/java/com/murder/controller/StoreEmployeeController.java)

前端优先：

- [frontend/admin/src/views/login/index.vue](/D:/murder-boot/frontend/admin/src/views/login/index.vue)
- [frontend/admin/src/router/index.js](/D:/murder-boot/frontend/admin/src/router/index.js)
- [frontend/admin/src/layout/index.vue](/D:/murder-boot/frontend/admin/src/layout/index.vue)
- [frontend/admin/src/utils/request.js](/D:/murder-boot/frontend/admin/src/utils/request.js)

数据库优先：

- 新建 `store_employee_account`
- 给 DM 账号补 `dm_id` 关联

