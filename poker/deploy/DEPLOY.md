# Poker 部署说明

## 1. 构建前端

```bash
cd poker
npm install
npm run build        # 产物在 poker/dist
```

## 2. 部署静态文件

将 `poker/dist` 的内容上传到服务器目录（如 `/var/www/poker`）。

## 3. 后端

复用 game-server，使用 prod profile 启动（MySQL/Redis 已由 docker-compose 提供）：

```bash
cd game-server
mvn clean package -DskipTests
# 参考 docker/Dockerfile 构建镜像，或直接
SPRING_PROFILES_ACTIVE=prod nohup java -jar target/game-server-1.0.0-SNAPSHOT.jar > backend.log 2>&1 &
```

首次启动会自动创建 `t_poker_room`、`t_poker_room_player`、`t_poker_hand`、`t_poker_hand_player`、`t_poker_hand_action` 表。

## 4. Nginx

参考 `deploy/nginx-poker.conf` 配置：

- 前端 history 路由 `try_files ... /index.html`
- `/api/` 反代到后端
- `/api/ws/` 反代必须带 `Upgrade` / `Connection` 头（WebSocket），超时设长

```bash
cp deploy/nginx-poker.conf /etc/nginx/conf.d/poker.conf
nginx -t && nginx -s reload
```

## 5. 本地联调（不部署）

- 后端：`cd game-server && mvn spring-boot:run`（dev profile，连远程 MySQL/Redis）
- 前端：`cd poker && npm run dev`，访问 http://localhost:3200
- vite 已配置 `/api` 代理（含 `ws: true`），无需额外配置

## 注意事项

1. **token**：JWT 由后端签发，前后端共用；部署时 `JWT_SECRET` 需配置一致。
2. **房间运行时状态在内存**：后端重启会丢失进行中的房间（DB 中的房间记录仍在，需手动处理或等待清理）。
3. **端口**：生产环境建议 https（Nginx 加 SSL），WS 使用 `wss://`。
