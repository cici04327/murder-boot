import request from '@/utils/request'

// 获取文章列表（分页）
export const getArticleList = (params) => {
  return request({
    url: '/article/page',
    method: 'get',
    params
  })
}

// 获取文章详情
export const getArticleDetail = (id) => {
  return request({
    url: `/article/${id}`,
    method: 'get'
  })
}

// 获取热门文章
export const getHotArticles = (limit = 10) => {
  return request({
    url: '/article/hot',
    method: 'get',
    params: { limit }
  })
}

// 获取推荐文章
export const getRecommendedArticles = (limit = 10) => {
  return request({
    url: '/article/recommended',
    method: 'get',
    params: { limit }
  })
}

// 点赞文章
export const likeArticle = (id) => {
  return request({
    url: `/article/${id}/like`,
    method: 'post'
  })
}

// 取消点赞文章
export const unlikeArticle = (id) => {
  return request({
    url: `/article/${id}/like`,
    method: 'delete'
  })
}

// 收藏文章
export const favoriteArticle = (id) => {
  return request({
    url: `/article/${id}/favorite`,
    method: 'post'
  })
}

// 取消收藏文章
export const unfavoriteArticle = (id) => {
  return request({
    url: `/article/${id}/favorite`,
    method: 'delete'
  })
}

// 检查收藏状态
export const checkFavoriteStatus = (id) => {
  return request({
    url: `/article/${id}/favorite/status`,
    method: 'get'
  })
}

// 获取文章评论列表
export const getArticleComments = (id) => {
  return request({
    url: `/article/${id}/comments`,
    method: 'get'
  })
}

// 添加评论
export const addArticleComment = (id, data) => {
  return request({
    url: `/article/${id}/comments`,
    method: 'post',
    data
  })
}

// 删除评论
export const deleteArticleComment = (commentId) => {
  return request({
    url: `/article/comments/${commentId}`,
    method: 'delete'
  })
}

// 点赞评论
export const likeArticleComment = (commentId) => {
  return request({
    url: `/article/comments/${commentId}/like`,
    method: 'post'
  })
}

// 取消点赞评论
export const unlikeArticleComment = (commentId) => {
  return request({
    url: `/article/comments/${commentId}/like`,
    method: 'delete'
  })
}

// 添加文章
export const addArticle = (data) => {
  return request({
    url: '/article',
    method: 'post',
    data
  })
}

// 更新文章
export const updateArticle = (data) => {
  return request({
    url: '/article',
    method: 'put',
    data
  })
}

// 删除文章
export const deleteArticle = (id) => {
  return request({
    url: `/article/${id}`,
    method: 'delete'
  })
}
