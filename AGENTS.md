# GameGroup 开发指南

## 项目概述

GameGroup 是一个微信小程序 + SpringBoot 后端的小游戏匹配平台。用户通过微信登录后，可在首页浏览游戏列表，点击游戏后输入验证号进行匹配。

## 构建命令

### 后端 (game-server)

```bash
# 进入后端目录
cd game-server

# 启动开发环境
./mvnw spring-boot:run -Dspring.profiles.active=dev

# 打包
./mvnw clean package -DskipTests

# 运行测试
./mvnw test

# 运行单个测试
./mvnw test -Dtest=ClassName#methodName

# 代码格式化
./mvnw spotless:apply

# 静态代码检查
./mvnw checkstyle:check

# 生成 API 文档
./mvnw spring-boot:run (访问 http://localhost:8080/swagger-ui.html)
```

### 前端 (game-miniapp)

```bash
# 使用微信开发者工具打开 game-miniapp 目录
# 或使用以下命令进行预览（需要安装微信开发者工具 CLI）

# 代码检查
npm run lint

# 代码格式化
npm run format
```

## 代码风格指南

### Java 后端代码规范

#### 1. 包命名规范
- `com.gamegroup.controller` - REST 控制器
- `com.gamegroup.service` - 业务逻辑层
- `com.gamegroup.repository` - 数据访问层
- `com.gamegroup.entity` - 数据库实体
- `com.gamegroup.dto` - 请求/响应数据传输对象
- `com.gamegroup.common` - 通用工具、异常、常量

#### 2. 类命名规范
- Controller: `XxxController`
- Service: `XxxService`
- Repository: `XxxRepository`
- Entity: `Xxx`
- DTO: `XxxRequest`/`XxxResponse`/`XxxDTO`

#### 3. 代码结构规范
```java
// 1. 包声明
package com.gamegroup.service;

// 2. 导入语句 (按字母顺序，java.* 在前，javax.* 在后，org.* 在最后)
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 3. 类注解
@Slf4j
@Service
@RequiredArgsConstructor
public class XxxService {
    
    // 4. 依赖注入 (使用 final 字段 + 构造器注入)
    private final XxxRepository xxxRepository;
    
    // 5. 常量
    private static final int TIMEOUT = 30;
    
    // 6. 公共方法
    @Transactional
    public XxxDTO createXxx(CreateXxxRequest request) {
        // 业务逻辑
    }
    
    // 7. 私有方法
    private String generateCode() {
        // 辅助逻辑
    }
}
```

#### 4. 导入规范
- 使用 `jakarta.validation.*` 而非 `javax.validation.*`
- 优先使用 Lombok 注解减少样板代码
- 避免通配符导入 `import xxx.*;`

#### 5. 异常处理规范
```java
// 使用自定义业务异常
throw new BusinessException(ErrorCode.GAME_NOT_FOUND);

// Service 层使用 @Transactional 确保事务一致性
@Transactional
public void updateGame(Long gameId, UpdateGameRequest request) {
    Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));
    // 更新逻辑
}
```

#### 6. API 响应规范
```java
// 统一使用 ApiResponse 包装返回值
@Operation(summary = "获取游戏列表")
@GetMapping("/games")
public ApiResponse<List<GameDTO>> getGames() {
    List<GameDTO> games = gameService.getActiveGames();
    return ApiResponse.success(games);
}
```

### 微信小程序前端规范

#### 1. 文件命名规范
- 页面文件: `pages/xxx/xxx.js`, `pages/xxx/xxx.wxml`, `pages/xxx/xxx.wxss`
- 组件文件: `components/xxx/xxx.js`, `components/xxx/xxx.wxml`
- 工具文件: `utils/xxx.js`

#### 2. 代码结构规范
```javascript
// pages/xxx/xxx.js
const { apiMethod } = require('../../utils/api');

Page({
  data: {
    // 页面数据
    loading: false,
    list: []
  },

  // 生命周期函数
  onLoad(options) {
    this.loadData();
  },

  // 事件处理函数
  onButtonClick() {
    this.setData({ loading: true });
    // 处理逻辑
  },

  // 网络请求函数
  async loadData() {
    try {
      const result = await apiMethod();
      this.setData({ list: result });
    } catch (err) {
      console.error('加载数据失败', err);
      wx.showToast({
        title: err.message || '加载失败',
        icon: 'none'
      });
    }
  }
});
```

#### 3. 网络请求规范
```javascript
// 使用统一的 request 工具
const { get, post } = require('../../utils/request');

// GET 请求
const games = await get('/games');

// POST 请求
const result = await post('/match/create', { gameId: 1 });
```

#### 4. 错误处理规范
- 网络请求统一使用 try-catch 包装
- 用户友好的错误提示使用 `wx.showToast`
- 开发调试信息使用 `console.error`

#### 5. 样式规范
- 使用 rpx 响应式单位
- 遵循 BEM 命名规范
- 避免内联样式，使用 class 选择器

## 数据库规范

### 1. 表命名规范
- 统一使用小写字母和下划线
- 表名前缀: `t_` (如: `t_user`, `t_game`)

### 2. 字段命名规范
- 使用小写字母和下划线
- 主键: `id`
- 时间字段: `create_time`, `update_time`
- 状态字段: `status`

### 3. JPA 实体规范
```java
@Entity
@Table(name = "t_game")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }
}
```

## 测试规范

### 1. 单元测试
- 使用 JUnit 5 + Mockito
- 测试类命名: `XxxServiceTest`
- 测试方法命名: `should_xxx_when_xxx`

### 2. 集成测试
- 使用 `@SpringBootTest`
- 测试完整的 API 流程

### 3. 测试数据
- 使用 `@TestConfiguration` 准备测试数据
- 测试后清理数据

## Git 提交规范

### 提交信息格式
```
type(scope): description

[optional body]

[optional footer]
```

### 类型说明
- `feat`: 新功能
- `fix`: 修复 bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建工具或辅助工具的变动

### 示例
```
feat(auth): 添加微信登录功能

- 实现 wx-login 接口
- 添加 JWT token 生成
- 更新用户信息获取逻辑

Closes #123
```

## 环境配置

### 开发环境变量
```bash
# 数据库配置
DB_URL=jdbc:mysql://localhost:3306/gamegroup
DB_USERNAME=root
DB_PASSWORD=your_password

# Redis 配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# 微信小程序配置
WX_APPID=your_appid
WX_SECRET=your_secret

# JWT 配置
JWT_SECRET=your_jwt_secret
JWT_EXPIRATION=86400000
```

## 注意事项

1. **敏感信息**: 不要将 appid/secret、数据库密码等敏感信息提交到代码库
2. **HTTPS**: 生产环境必须使用 HTTPS
3. **API 版本**: 使用路径版本控制 `/api/v1/xxx`
4. **错误码**: 遵循项目统一的错误码规范
5. **日志规范**: 使用 `@Slf4j` 注解，合理使用日志级别
6. **代码注释**: 业务逻辑复杂处添加中文注释
7. **性能优化**: 数据库查询使用合适的索引，避免 N+1 问题

## 常见问题

### Q: 如何添加新的游戏类型？
A: 1. 在 `t_game` 表添加新记录 2. 创建对应的 Controller 和 Service 3. 更新小程序页面

### Q: 如何处理并发匹配？
A: 使用 Redis 分布式锁，确保房间创建和加入的原子性

### Q: 如何扩展到多人游戏？
A: 修改 `t_match_room` 表的 `max_players` 字段，更新匹配逻辑支持多人加入