# GameGroup

微信小程序游戏匹配平台

## 项目结构

```
gameGroup/
├── game-server/        # SpringBoot 后端服务
├── game-miniapp/       # 微信小程序 (待创建)
└── CLAUDE.md           # 开发指南
```

## 快速开始

### 后端启动

1. 确保已安装 Java 17+ 和 Maven
2. 创建 MySQL 数据库并执行 `game-server/src/main/resources/schema.sql`
3. 配置环境变量或修改 `application-dev.yml`
4. 启动服务：

```bash
cd game-server
./mvnw spring-boot:run
```

5. 访问 API 文档: http://localhost:8080/api/swagger-ui.html

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| DB_PASSWORD | 数据库密码 | root |
| WX_APPID | 微信小程序 AppID | - |
| WX_SECRET | 微信小程序 Secret | - |
| JWT_SECRET | JWT 签名密钥 | - |

## API 接口

- `POST /api/auth/wx-login` - 微信登录
- `GET /api/games` - 游戏列表
- `GET /api/games/{id}` - 游戏详情
- `POST /api/match/create` - 创建房间
- `POST /api/match/join` - 加入房间
- `GET /api/match/status/{roomId}` - 房间状态
