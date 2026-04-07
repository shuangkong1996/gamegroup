# GameGroup 项目开发指南

## 项目概述

一个微信小程序 + SpringBoot 后端的小游戏匹配平台。用户通过微信登录后，可在首页浏览游戏列表，点击游戏后输入验证号进行匹配。

## 技术栈

### 后端 (game-server)
- **框架**: Spring Boot 3.x
- **语言**: Java 17+
- **数据库**: MySQL 8.0
- **缓存**: Redis (用于会话管理和匹配队列)
- **API文档**: SpringDoc OpenAPI

### 前端 (game-miniapp)
- **框架**: 微信小程序原生开发
- **UI组件**: WeUI / Vant Weapp (可选)
- **状态管理**: 小程序原生 globalData 或 mobx-miniprogram

## 项目结构

```
gameGroup/
├── game-server/                 # SpringBoot 后端
│   ├── src/main/java/com/gamegroup/
│   │   ├── GameGroupApplication.java
│   │   ├── config/              # 配置 (CORS, Security, Redis等)
│   │   ├── controller/          # REST控制器
│   │   ├── service/             # 业务逻辑层
│   │   ├── repository/          # JPA数据访问层
│   │   ├── entity/              # 数据库实体
│   │   ├── dto/                 # 请求/响应DTO
│   │   └── common/              # 通用工具、异常、常量
│   └── src/main/resources/
│       ├── application.yml
│       └── application-dev.yml
│
├── game-miniapp/                # 微信小程序
│   ├── pages/
│   │   ├── index/               # 首页 - 游戏列表
│   │   ├── login/               # 登录页
│   │   └── match/               # 匹配页
│   ├── components/              # 公共组件
│   ├── utils/
│   │   ├── request.js           # 网络请求封装
│   │   └── auth.js              # 认证工具
│   ├── app.js
│   ├── app.json
│   └── app.wxss
│
└── CLAUDE.md
```

## 核心业务模块

### 1. 用户认证模块

**微信登录流程:**
1. 小程序调用 `wx.login()` 获取 code
2. 后端接收 code，调用微信接口换取 openid/session_key
3. 生成自定义登录态(JWT)返回给小程序
4. 小程序后续请求携带 token

**后端接口:**
- `POST /api/auth/wx-login` - 微信登录
- `GET /api/auth/user-info` - 获取当前用户信息

**数据表:** `t_user`
- id, openid, union_id, nickname, avatar_url, phone, create_time, update_time

### 2. 游戏管理模块

**后端接口:**
- `GET /api/games` - 获取游戏列表
- `GET /api/games/{id}` - 获取游戏详情

**数据表:** `t_game`
- id, name, icon, description, rules, status, sort_order, create_time

### 3. 游戏匹配模块

**匹配流程:**
1. 用户在游戏详情页输入验证号(房间号)
2. 后端验证该验证号是否有效
3. 验证通过则将用户加入匹配房间
4. 双方匹配成功后通知开始游戏

**后端接口:**
- `POST /api/match/create` - 创建匹配房间，生成验证号
- `POST /api/match/join` - 通过验证号加入匹配
- `GET /api/match/status/{matchId}` - 查询匹配状态
- `DELETE /api/match/{matchId}` - 取消匹配

**数据表:** `t_match_room`
- id, game_id, verify_code, creator_id, player_ids, status, create_time, expire_time

## API 规范

### 统一响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 错误码定义
| 错误码 | 说明 |
|-------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未登录或token失效 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 1001 | 微信登录失败 |
| 2001 | 游戏不存在 |
| 3001 | 验证号无效 |
| 3002 | 房间已满 |
| 3003 | 房间已过期 |

## 开发规范

### 后端规范
- 遵循 RESTful API 设计
- Controller 只做参数校验和调用 Service
- Service 处理业务逻辑，事务控制
- Repository 只做数据访问
- 使用 @Valid 进行参数校验
- 统一异常处理 (@ControllerAdvice)

### 小程序规范
- 页面文件放在 pages/ 目录，按功能命名
- 公共组件放在 components/ 目录
- 网络请求统一使用 utils/request.js 封装
- 使用 Promise 处理异步操作

### Git 规范
- main: 生产分支
- dev: 开发分支
- feature/*: 功能分支
- fix/*: 修复分支

提交信息格式: `type(scope): description`
- feat: 新功能
- fix: 修复bug
- docs: 文档更新
- refactor: 重构
- test: 测试

## 环境配置

### 后端环境变量
```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/gamegroup
    username: root
    password: ${DB_PASSWORD}
  redis:
    host: localhost
    port: 6379

wx:
  miniapp:
    appid: ${WX_APPID}
    secret: ${WX_SECRET}

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000
```

### 小程序配置
```javascript
// utils/config.js
const config = {
  baseUrl: 'https://api.yourdomain.com',  // 生产环境
  devBaseUrl: 'http://localhost:8080',     // 开发环境
};
```

## 待实现功能清单

### Phase 1: 基础框架 (当前)
- [ ] 初始化 SpringBoot 项目结构
- [ ] 初始化微信小程序项目结构
- [ ] 数据库表设计与创建
- [ ] 微信登录功能

### Phase 2: 核心功能
- [ ] 游戏列表展示
- [ ] 游戏详情页
- [ ] 验证号匹配功能
- [ ] 匹配状态查询

### Phase 3: 增强功能
- [ ] 用户个人中心
- [ ] 匹配历史记录
- [ ] 消息通知(WebSocket)
- [ ] 管理后台

## 常用命令

### 后端
```bash
# 启动开发环境
cd game-server && ./mvnw spring-boot:run -Dspring.profiles.active=dev

# 打包
./mvnw clean package -DskipTests

# 运行测试
./mvnw test
```

### 小程序
```bash
# 使用微信开发者工具打开 game-miniapp 目录
# 或使用 npm 脚本 (如果配置了)
```

## 注意事项

1. **微信小程序配置**: 需要在微信公众平台配置服务器域名白名单
2. **HTTPS**: 生产环境必须使用 HTTPS
3. **敏感信息**: 不要将 appid/secret 等敏感信息提交到代码库
4. **验证号有效期**: 建议设置 5-10 分钟过期时间，使用 Redis TTL 管理
