# 兔兔短剧 (bunny-flix) 后端接口文档

> 自动汇总自各微服务 Controller 源码（共 12 个服务、34 个 Controller）。
> 所有接口统一经网关注口 `http://<gateway>:8080`，基础前缀为 `/api`。
> 鉴权：除白名单（登录、注册、搜索、热门推荐等）外，请求头需携带 `Authorization: Bearer <token>`；网关校验后向下游注入 `X-User-Id` / `X-User-Phone` / `X-User-Type` 头。

---

## 目录

| 服务 | 端口 | 路径前缀 | 说明 |
| --- | --- | --- | --- |
| 网关 bunny-flix-gateway | 8080 | （统一入口） | 路由/鉴权/Sentinel |
| 用户服务 service-user | 8100 | /api/user | 普通用户注册登录/资料/关注 |
| 后台管理服务 service-admin | 8150 | /api/admin | 管理员登录/注册/菜单（独立于 user） |
| 短剧服务 service-drama | 8200 | /api/drama | 短剧/剧集/演员/标签/发行方 |
| 视频服务 service-video | 8300 | /api/video | 视频上传/播放/清晰度/进度/历史 |
| 搜索服务 service-search | 8400 | /api/search | 全文搜索/联想/热搜/历史 |
| 互动服务 service-interaction | 8500 | /api/interaction | 点赞/收藏/评论/弹幕/分享 |
| 会员服务 service-member | 8600 | /api/member | 钱包/充值/会员/VIP |
| 推荐服务 service-recommend | 8700 | /api/recommend | 个性化/热门推荐 |
| 数据服务 service-data | 8800 | /api/data | 排行榜/播放统计 |
| 营销服务 service-marketing | 8900 | /api/marketing | 签到/任务/优惠券 |
| 推送服务 service-push | 9000 | /api/push | 站内信/广播 |
| ID生成服务 service-id-generator | 9100 | /api/idgen | 分布式号段ID |

---

## 1. 用户服务 service-user（/api/user）

### UserController — 用户核心
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| POST | /api/user/login/password | 密码登录 | phone, password（Query） |
| POST | /api/user/login/code | 验证码登录（不存在则自动注册） | phone, code（Query） |
| POST | /api/user/register | 手机号+密码注册 | phone, password（Query） |
| POST | /api/user/sms/send | 发送短信验证码 | phone（Query） |
| GET | /api/user/{userId} | 根据ID查用户 | userId（Path） |
| PUT | /api/user/profile | 修改个人资料 | User（Body） |
| PUT | /api/user/password | 修改密码 | oldPassword, newPassword（Query） |
| GET | /api/user/me | 获取当前登录用户 | Header Token |
| PUT | /api/user/vip/status | 更新VIP状态（会员服务调用） | VipStatusDTO（Body） |
| GET | /api/user/{userId}/vip | 查询用户VIP信息 | userId（Path） |
| GET | /api/user/{userId}/exists | 判断用户是否存在 | userId（Path） |

### FollowController — 关注
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| POST | /api/user/follow/{followUserId} | 关注用户 | followUserId（Path） |
| DELETE | /api/user/follow/{followUserId} | 取消关注 | followUserId（Path） |
| GET | /api/user/follow/followers | 我的粉丝列表 | page, size（Query） |
| GET | /api/user/follow/following | 我关注的人 | page, size（Query） |
| GET | /api/user/follow/check/{followUserId} | 是否关注某用户 | followUserId（Path） |

---

## 2. 后台管理服务 service-admin（/api/admin）

### AdminController — 管理员
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| POST | /api/admin/login/password | 管理员密码登录 | phone, password（Query） |
| POST | /api/admin/register | 管理员注册 | phone, password（Query） |
| GET | /api/admin/me | 当前管理员信息 | Header Token |
| GET | /api/admin/menus | 当前管理员分级菜单 | Header Token |

> 后台管理员数据独立存于 `t_admin_user`，与普通用户隔离；JWT `userType=admin`。

---

## 3. 短剧服务 service-drama（/api/drama）

### DramaController — 短剧
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | /api/drama/list | 短剧分页列表 | page, pageSize, keyword, status, payType, tagId（Query） |
| GET | /api/drama/{id} | 短剧详情 | id（Path） |
| GET | /api/drama/{id}/exists | 短剧是否存在 | id（Path） |
| POST | /api/drama | 创建短剧 | Drama（Body） |
| PUT | /api/drama/{id} | 更新短剧 | id（Path）, Drama（Body） |
| DELETE | /api/drama/{id} | 删除短剧 | id（Path） |
| PUT | /api/drama/{id}/status | 更新状态 | id（Path）, status（Query） |
| PUT | /api/drama/{id}/completed | 更新完结状态 | id（Path）, completed（Query） |

### EpisodeController — 剧集
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | /api/drama/episode/list/{dramaId} | 剧集列表 | dramaId（Path） |
| GET | /api/drama/episode/{id} | 剧集详情 | id（Path） |
| POST | /api/drama/episode | 创建剧集 | Episode（Body） |
| PUT | /api/drama/episode/{id} | 更新剧集 | id（Path）, Episode（Body） |
| DELETE | /api/drama/episode/{id} | 删除剧集 | id（Path） |
| PUT | /api/drama/episode/{id}/sort | 更新排序 | id（Path）, sortOrder（Query） |
| PUT | /api/drama/episode/{id}/free | 更新免费状态 | id（Path）, free（Query） |

### ActorController — 演员
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | /api/drama/actor/list | 演员分页列表 | page, pageSize, keyword（Query） |
| GET | /api/drama/actor/{id} | 演员详情 | id（Path） |
| POST | /api/drama/actor | 创建演员 | Actor（Body） |
| PUT | /api/drama/actor/{id} | 更新演员 | id（Path）, Actor（Body） |
| DELETE | /api/drama/actor/{id} | 删除演员 | id（Path） |

### PublisherController — 发行方
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | /api/drama/publisher/list | 发行方列表 | page, pageSize, keyword（Query） |
| GET | /api/drama/publisher/{id} | 发行方详情 | id（Path） |
| POST | /api/drama/publisher | 创建发行方 | Publisher（Body） |
| PUT | /api/drama/publisher/{id} | 更新发行方 | id（Path）, Publisher（Body） |
| DELETE | /api/drama/publisher/{id} | 删除发行方 | id（Path） |

### TagController — 标签
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | /api/drama/tag | 标签列表（可传 parentId） | parentId（Query, 可选） |
| GET | /api/drama/tag/all | 全部标签 | - |
| GET | /api/drama/tag/{id} | 标签详情 | id（Path） |
| POST | /api/drama/tag | 创建标签 | Tag（Body） |
| PUT | /api/drama/tag/{id} | 更新标签 | id（Path）, Tag（Body） |
| DELETE | /api/drama/tag/{id} | 删除标签 | id（Path） |

---

## 4. 视频服务 service-video（/api/video）

### VideoController — 视频
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| POST | /api/video/upload | 上传视频 | episodeId（Query）, fileUrl（Query） |
| GET | /api/video/play/{episodeId} | 获取播放地址 | episodeId（Path）, userId（Query, 可选） |
| GET | /api/video/{videoId} | 视频信息 | videoId（Path） |
| GET | /api/video/resolution/{episodeId} | 多清晰度信息 | episodeId（Path） |

### PlayProgressController — 播放进度
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| POST | /api/video/progress | 保存播放进度 | userId, episodeId, dramaId, position, duration（Query） |
| GET | /api/video/progress/{episodeId} | 查询播放进度 | userId（Query）, episodeId（Path） |

### WatchHistoryController — 观看历史
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | /api/video/history/list | 观看历史列表 | userId, page, pageSize（Query） |
| DELETE | /api/video/history/{id} | 删除历史记录 | id（Path） |

---

## 5. 搜索服务 service-search（/api/search）

### SearchController — 搜索
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | /api/search | 全文搜索 | keyword, page, size, sortBy（Query） |
| GET | /api/search/suggest | 搜索联想 | keyword（Query） |
| GET | /api/search/hot | 热门搜索词 | - |
| POST | /api/search/index | 创建ES索引（手动） | - |

### HotWordController — 热搜词
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | /api/search/hot-word | 热搜词列表 | - |
| POST | /api/search/hot-word | 新增热搜词 | HotWord（Body） |
| PUT | /api/search/hot-word/{id} | 更新热搜词 | id（Path）, HotWord（Body） |
| DELETE | /api/search/hot-word/{id} | 删除热搜词 | id（Path） |

### SearchHistoryController — 搜索历史
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| POST | /api/search/history | 记录搜索历史 | keyword（Query） |
| GET | /api/search/history | 查询搜索历史 | Header Token |
| DELETE | /api/search/history | 清空搜索历史 | Header Token |

---

## 6. 互动服务 service-interaction（/api/interaction）

### InteractionController — 聚合接口
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| POST | /api/interaction/like | 点赞 | dramaId（Body） |
| DELETE | /api/interaction/like/{dramaId} | 取消点赞 | dramaId（Path） |
| GET | /api/interaction/like/check/{dramaId} | 是否点赞 | dramaId（Path） |
| GET | /api/interaction/like/count/{dramaId} | 点赞数 | dramaId（Path） |
| POST | /api/interaction/collect | 收藏 | dramaId（Body） |
| DELETE | /api/interaction/collect/{dramaId} | 取消收藏 | dramaId（Path） |
| GET | /api/interaction/collect/check/{dramaId} | 是否收藏 | dramaId（Path） |
| GET | /api/interaction/collect/count/{dramaId} | 收藏数 | dramaId（Path） |
| POST | /api/interaction/comment | 发表评论 | dramaId, episodeId, parentId, replyToUserId, content（Body） |
| PUT | /api/interaction/comment/{id} | 更新评论 | id（Path）, content（Body） |
| DELETE | /api/interaction/comment/{id} | 删除评论 | id（Path） |
| PUT | /api/interaction/comment/{id}/top | 评论置顶 | id（Path）, top（Query） |
| GET | /api/interaction/comment/list | 评论列表 | dramaId, episodeId, parentId, page, size, sortBy（Query） |
| GET | /api/interaction/comment/replies/{commentId} | 评论回复 | commentId（Path）, page, size（Query） |
| POST | /api/interaction/comment/like | 评论点赞 | commentId（Body） |
| DELETE | /api/interaction/comment/like/{commentId} | 取消评论点赞 | commentId（Path） |
| POST | /api/interaction/danmaku | 发送弹幕 | dramaId, episodeId, content, playTime, color, font（Body） |
| GET | /api/interaction/danmaku/list | 弹幕列表 | episodeId, playTime（Query） |
| POST | /api/interaction/share | 分享 | dramaId, platform（Body） |

### LikeController — 点赞（独立）
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| POST | /api/interaction/like | 点赞 | dramaId（Body） |
| DELETE | /api/interaction/like/{dramaId} | 取消点赞 | dramaId（Path） |
| GET | /api/interaction/like/check/{dramaId} | 点赞状态 | dramaId（Path） |
| GET | /api/interaction/like/list | 我的点赞列表 | page, size（Query） |

### CollectController — 收藏（独立）
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| POST | /api/interaction/collect | 收藏 | dramaId（Body） |
| DELETE | /api/interaction/collect/{dramaId} | 取消收藏 | dramaId（Path） |
| GET | /api/interaction/collect/check/{dramaId} | 收藏状态 | dramaId（Path） |
| GET | /api/interaction/collect/list | 我的收藏列表 | page, size（Query） |

### CommentController — 评论（独立）
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| POST | /api/interaction/comment | 发表评论 | dramaId, episodeId, parentId, replyToUserId, content（Body） |
| PUT | /api/interaction/comment/{id} | 更新评论 | id（Path）, content（Body） |
| DELETE | /api/interaction/comment/{id} | 删除评论 | id（Path） |
| PUT | /api/interaction/comment/{id}/top | 评论置顶 | id（Path） |
| GET | /api/interaction/comment/list | 评论列表 | dramaId, episodeId, parentId, page, size, sortBy（Query） |

### DanmakuController — 弹幕（独立）
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| POST | /api/interaction/danmaku | 发送弹幕 | dramaId, episodeId, content, playTime, color, font（Body） |
| GET | /api/interaction/danmaku/list | 弹幕列表 | episodeId, playTime（Query, 可选） |

### ShareController — 分享（独立）
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| POST | /api/interaction/share | 分享记录 | dramaId, platform（Body） |

> 注：`InteractionController` 与下方独立 Controller 存在功能重叠（点赞/收藏/评论/弹幕），属历史遗留重复路由，调用任选其一即可。

---

## 7. 会员服务 service-member（/api/member）

### WalletController — 钱包
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | /api/member/wallet | 钱包信息 | Header Token |
| GET | /api/member/wallet/balance | 余额 | Header Token |

### RechargeController — 充值
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| POST | /api/member/recharge/order | 创建充值订单 | amount（Body） |
| POST | /api/member/recharge/callback | 支付回调 | orderNo, success（Body） |
| GET | /api/member/recharge/orders | 充值订单列表 | page, size（Query） |

### VipController — 会员VIP
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | /api/member/vip/status | 用户VIP状态 | Header Token |
| GET | /api/member/vip/plans | VIP套餐列表 | - |
| POST | /api/member/vip/purchase | 购买VIP | planId（Body） |

### ConsumeController — 消费记录
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | /api/member/consume/records | 消费记录 | page, size, type（Query, type可选） |

---

## 8. 营销服务 service-marketing（/api/marketing）

### MarketingController — 聚合接口
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| POST | /api/marketing/checkin | 签到 | Header Token |
| GET | /api/marketing/checkin/calendar | 签到日历 | year, month（Query） |
| GET | /api/marketing/task/list | 任务列表 | - |
| GET | /api/marketing/task/my | 我的任务 | Header Token |
| POST | /api/marketing/task/claim/{taskId} | 领取任务奖励 | taskId（Path） |
| POST | /api/marketing/task | 创建任务 | Task（Body） |
| POST | /api/marketing/coupon/receive/{templateId} | 领取优惠券 | templateId（Path） |
| GET | /api/marketing/coupon/list | 我的优惠券 | status（Query, 可选） |
| POST | /api/marketing/coupon/use/{couponId} | 使用优惠券 | couponId（Path）, orderAmount（Body） |
| GET | /api/marketing/coupon/template/list | 券模板列表 | page, size（Query） |
| POST | /api/marketing/coupon/template | 创建券模板 | CouponTemplate（Body） |
| PUT | /api/marketing/coupon/template/{id} | 更新券模板 | id（Path）, CouponTemplate（Body） |
| DELETE | /api/marketing/coupon/template/{id} | 删除券模板 | id（Path） |

### CheckinController — 签到（独立）
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| POST | /api/marketing/checkin | 签到 | userId（Query） |
| GET | /api/marketing/checkin/calendar | 签到日历 | userId, month（Query） |
| POST | /api/marketing/checkin/makeup | 补签 | userId, date（Query） |

### CouponController — 优惠券（独立）
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| POST | /api/marketing/coupon/receive/{templateId} | 领取优惠券 | userId（Query）, templateId（Path） |
| GET | /api/marketing/coupon | 用户优惠券列表 | userId, status（Query, status可选） |
| POST | /api/marketing/coupon/use/{couponId} | 使用优惠券 | userId, couponId（Path）, orderAmount（Query） |

### CouponTemplateController — 券模板（独立）
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| POST | /api/marketing/coupon/template | 创建模板 | CouponTemplate（Body） |
| PUT | /api/marketing/coupon/template/{id} | 更新模板 | id（Path）, CouponTemplate（Body） |
| DELETE | /api/marketing/coupon/template/{id} | 删除模板 | id（Path） |
| GET | /api/marketing/coupon/template/{id} | 模板详情 | id（Path） |
| GET | /api/marketing/coupon/template | 模板分页列表 | page, size（Query） |
| PUT | /api/marketing/coupon/template/{id}/enable | 启用模板 | id（Path） |
| PUT | /api/marketing/coupon/template/{id}/disable | 停用模板 | id（Path） |
| GET | /api/marketing/coupon/template/available | 可用模板列表 | page, size（Query） |

### TaskController — 任务（独立）
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | /api/marketing/task | 用户任务 | userId（Query） |
| POST | /api/marketing/task/claim/{taskId} | 领取奖励 | userId（Query）, taskId（Path） |
| POST | /api/marketing/task | 创建任务 | Task（Body） |

> 注：MarketingController 与下方独立 Controller 存在功能重叠，部分接口用 Header Token 取 userId，部分用 Query userId，属于历史遗留重复路由。

---

## 9. 推送服务 service-push（/api/push）

### PushController — 推送聚合
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | /api/push/notification | 站内信列表 | page, size（Query） |
| GET | /api/push/notification/unread-count | 未读数量 | - |
| PUT | /api/push/notification/read/{id} | 标记已读 | id（Path） |
| POST | /api/push/send | 发送通知（管理端） | userId, title, content, type（Body） |
| POST | /api/push/broadcast | 广播通知 | title, content（Body） |

### NotificationController — 通知（独立）
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | /api/push/notification | 通知列表 | userId, page, size（Query） |
| GET | /api/push/notification/unread-count | 未读数量 | userId（Query） |
| PUT | /api/push/notification/read/{id} | 标记已读 | id（Path） |

---

## 10. 推荐服务 service-recommend（/api/recommend）

### RecommendController — 推荐
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | /api/recommend | 个性化推荐 | page, size（Query） |
| GET | /api/recommend/hot | 热门推荐 | page, size（Query） |

---

## 11. 数据服务 service-data（/api/data）

### DataController — 数据统计
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | /api/data/ranking/hot | 热门排行榜 | page, size（Query） |
| GET | /api/data/ranking/new | 最新排行榜 | page, size（Query） |
| GET | /api/data/ranking/category | 分类排行榜 | tagId, page, size（Query） |
| GET | /api/data/stats/{dramaId} | 播放统计(日/周/月/总) | dramaId（Path） |

---

## 12. ID生成服务 service-id-generator（/api/idgen）

### IdGeneratorController — 号段ID
| 方法 | 路径 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | /api/idgen/next | 获取下一个ID | bizType（BizTypeEnum, Query） |
| POST | /api/idgen/batch | 批量获取ID | bizType（Query）, count（Query） |

> bizType 枚举：user, drama, episode, actor, tag, drama_actor, drama_tag, publisher, comment, order, video, admin

---

## 网关白名单（无需登录）

`/api/user/login`、`/api/user/sms`、`/api/user/register`、`/api/admin/login`、`/api/admin/register`、`/api/search`、`/api/recommend/hot`

---

*文档由代码静态分析自动生成，参数以源码注解为准；部分 Body 参数为 `Map<String,Object>` 动态结构，具体字段见对应 Service 实现。*
