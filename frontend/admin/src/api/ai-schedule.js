import request from "../utils/request";

const BASE = "/ai/schedule";

// DM智能分配建议（AI调用，60s超时）
export const suggestDmAssignment = (data) =>
  request({
    url: `${BASE}/dm/suggest`,
    method: "post",
    data,
    timeout: 60000,
  });

// 确认DM分配方案
export const confirmDmAssignment = (data) =>
  request.post(`${BASE}/dm/confirm`, data);

// 查询AI分配任务历史
export const listScheduleTasks = (storeId, limit = 20) =>
  request.get(`${BASE}/task/list`, { params: { storeId, limit } });
