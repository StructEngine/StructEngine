# StructEngine (StructEngine Voxel Builder)

一个多平台（[Fabric](https://fabricmc.net/) / NeoForge / Paper）的 Minecraft 服务端组件，可通过游戏内命令从远程 JSON 数据源拉取体素模型数据，并在世界中自动批量构建方块模型。核心业务逻辑与平台 API 通过 SPI 完全解耦，一套逻辑三端复用。

## 功能特性

- **远程数据构建**：通过 HTTP 下载 JSON 格式的体素模型数据，无需内置模型文件
- **异步下载**：使用 `HttpClient` 异步拉取数据，不阻塞游戏主线程
- **地址校验**：仅接受合法的 HTTP(S) 地址，非法地址直接报错并终止本次构建
- **大小限制**：下载响应体上限 16MB，超限视为获取失败，避免超大响应撑爆内存
- **分批放置**：每个游戏 tick 最多放置 1000 个方块（`BuildTickHandler.MAX_BLOCKS_PER_TICK`），避免瞬间放置大量方块导致卡顿
- **任务队列**：支持多个构建任务排队执行，按 FIFO 顺序依次完成
- **离线续建**：任务一旦入队，即使发起玩家离线也会继续走完队列
- **异常隔离**：构建过程中出现异常时中断当前模型（不回滚已放置方块），不影响队列中的其他任务
- **构建反馈**：下载、开始构建、完成均会以彩色消息反馈给执行命令的玩家
- **Platform 远程构建**：通过 WebSocket 长连接响应 `QUERY_Y` 高度探测，接收 `BUILD` 后按 Platform 下发的绝对坐标构建；带心跳、断线重连与主线程安全调度
- **核心逻辑零改动**：远程构建只新增网络层与任务装配桥接，原有 JSON 解析、分批放置、回滚/恢复机制完全保留

## 环境要求

| 依赖 | 版本 |
| --- | --- |
| Java | 21+ |
| Minecraft | 1.21.11 |
| Fabric Loader / API | >= 0.19.5 / 0.141.6+1.21.11 |
| NeoForge | 21.11.x |
| Paper | 1.21.11 |

> 三个平台共用同一套核心逻辑（`structengine-common`），均为**纯服务端**组件：Fabric 侧 `fabric.mod.json` 的 `environment` 为 `server`，无需客户端安装。

## 编译

### 前置条件

- **JDK 21**：默认使用系统 `JAVA_HOME`（需指向 JDK 21）；若 `JAVA_HOME` 未指向 JDK 21，可取消 `gradle.properties` 中 `org.gradle.java.home` 的注释并填入本机路径，或在命令行追加 `-Dorg.gradle.java.home=<JDK21 路径>`
- **Gradle**：无需手动安装，项目通过 Gradle Wrapper 自动下载 `9.5.1`；请勿使用发行版自带的旧版 Gradle（如 4.4.1），Fabric Loom 1.17 要求 Gradle 9.5 及以上

### 编译命令

在项目根目录执行（Windows 用 `gradlew.bat`，Linux / macOS 用 `./gradlew`）：

```bash
# 构建全部平台（需可访问 fabric / neoforged / papermc 仓库）
./gradlew build

# 只构建单个平台（限定网络环境下建议加 --configure-on-demand，避免配置其他平台模块）
./gradlew :structengine-common:build
./gradlew :structengine-fabric:build
./gradlew :structengine-neoforge:build
./gradlew :structengine-paper:build
```

首次构建会自动下载 Gradle 发行版、Minecraft 1.21.11 及各平台依赖，耗时较长，请耐心等待。
NeoForge 模块需要能访问 `maven.neoforged.net`，Paper 模块需要 `repo.papermc.io`。

### 产物位置

各模块产物位于自己的 `build/libs/`：

| 产物 | 用途 |
| --- | --- |
| `structengine-fabric/build/libs/structengine-fabric-1.0.0.jar` | Fabric 模组（放入 `mods/`，已通过 Jar-in-Jar 内置 common） |
| `structengine-neoforge/build/libs/structengine-neoforge-1.0.0.jar` | NeoForge 模组（放入 `mods/`，已内含 common 核心类） |
| `structengine-paper/build/libs/structengine-paper-1.0.0.jar` | Paper 插件（放入 `plugins/`，已内含 common 逻辑类） |
| `structengine-common/build/libs/structengine-common-1.0.0.jar` | 核心逻辑库（纯 Java，无需单独安装） |

### 常用命令

| 命令 | 说明 |
| --- | --- |
| `./gradlew build` | 完整构建（全部模块编译 + 打包） |
| `./gradlew clean build` | 清理后重新构建 |
| `./gradlew :structengine-fabric:remapJar` | 仅重新打包 Fabric 模组 jar |

## 安装

**Fabric**

1. 安装 [Fabric Loader](https://fabricmc.net/use/)（1.21.11 对应版本，如 `fabric-server-mc.1.21.11-loader.0.19.5-launcher.1.1.2.jar`）并启动一次
2. 将 [Fabric API](https://modrinth.com/mod/fabric-api)（`0.141.6+1.21.11`）放入 `mods/` 目录
3. 将 `structengine-fabric-1.0.0.jar` 放入 `mods/` 目录
4. 启动服务端/客户端

**NeoForge**

1. 安装 NeoForge 21.11.x 服务端
2. 将 `structengine-neoforge-1.0.0.jar` 放入 `mods/` 目录
3. 启动服务端

**Paper**

1. 准备 Paper 1.21.11 服务端
2. 将 `structengine-paper-1.0.0.jar` 放入 `plugins/` 目录
3. 启动服务端

## 使用方法

在游戏中以玩家身份执行命令：

```
/buildmodel <url>
```

- `<url>`：指向 JSON 体素模型数据的完整 HTTP(S) 地址，参数之间可含空格
- **任何玩家均可执行，无需管理员权限**
- 该指令只能由玩家执行，服务端控制台无法执行

执行后模组会以执行者所在位置为原点构建模型：

- 原点偏移：`x + 1`、`y + 0`、`z + 1`（相对玩家方块坐标）

## 远程 WebSocket 构建（Platform 联动）

游戏服启动后可以主动连接 `StructEnginePlatform`，由 Web 地图选点触发构建。网络层位于 `structengine-common` 的 `remote` 包，三个平台共用。

### 配置

通过环境变量（或同名 JVM 系统属性）配置：

| 变量 | 含义 | 必填 |
|------|------|------|
| `STRUCTENGINE_WS_URL` | Platform WebSocket 地址，如 `ws://platform:8080/ws/game` | 否（不配置则仅保留原命令功能） |
| `STRUCTENGINE_SERVER_ID` | 本服唯一标识，多服部署时必填，默认 `default` | 否 |
| `STRUCTENGINE_SERVER_NAME` | Web 管理台展示名，默认等于 serverId | 否 |
| `STRUCTENGINE_WS_TOKEN` | 握手令牌，对应 Platform 的 `STRUCTENGINE_GAME_TOKEN` | 生产建议 |
| `STRUCTENGINE_BLUEMAP_URL` | 本服 BlueMap 定制选点页地址 | 是（Web 地图） |
| `STRUCTENGINE_BLUEMAP_MAP_ID` | BlueMap 地图 ID，默认 `world` | 否 |
| `STRUCTENGINE_REMOTE_ENABLED` | 设为 `false` 可关闭远程网络层 | 否 |

示例（Paper）：

```bash
STRUCTENGINE_WS_URL=ws://platform.example.com:8080/ws/game \
STRUCTENGINE_SERVER_ID=survival \
STRUCTENGINE_SERVER_NAME="生存服" \
STRUCTENGINE_WS_TOKEN=change-me \
STRUCTENGINE_BLUEMAP_URL=http://map.example.com:8100/structengine-map.html \
java -jar paper-1.21.11.jar
```

> Fabric / NeoForge 在服务端启动完成后才建立连接，停服时自动关闭；Paper 在 `onEnable` / `onDisable` 中管理。

### 链路与线程模型

```
连接：HELLO(serverId, serverName, blueMapUrl, mapId, protocol=2)
      + PLAYER_LIST（连接/玩家事件/每10秒，full=true）
Platform QUERY_Y(taskId,x,z)
   → WS 网络线程解析
   → IServerScheduler.runTask 切到服务端主线程
   → IWorldAdapter.highestBlockYAt(world,x,z)
   → 主线程回传 RESPONSE_Y(taskId,y)
   → Platform 计算中心/旋转/原点并下发 BUILD(taskId,playerUuid,playerName,x,y,z,modelUrl,rotation)
   → 主线程解析玩家/世界，异步 HttpClient 下载 JSON
   → AbsoluteBuildService 生成绝对坐标任务
   → 每玩家独立队列轮转，压入原有 BuildTaskQueue
   → BUILD_STATUS(DOWNLOADING/BUILDING/COMPLETED) 回传 Platform
```

- **每玩家独立队列**：不同玩家可并行推进，共享每个 tick 的 1000 方块预算。
- **历史/回滚**：插件回传 `HISTORY_RESPONSE`，支持 Web 按 `historyId` 回滚/恢复；历史仍为内存态，MCS 重启后清空。
- **玩家列表**：连接、加入/离开/换世界时立即上报；每 10 秒全量刷新。

- **不再计算高度**：`BUILD` 已带完整 `(x,y,z)`，插件只做 `origin + 模型相对偏移`。
- **主线程安全**：QUERY_Y 与任务装配均通过 `IServerScheduler` 投递，WS 线程不直接触碰 Minecraft API。
- **玩家离线**：仍会入队并完成构建，仅跳过聊天反馈；回滚历史以玩家 UUID 记录。
- **心跳/重连**：每 20 秒 `PING`，60 秒无下行消息判定假死；断线按 1s→30s 指数退避重连。
- **鉴权**：配置令牌后，握手请求携带 `X-StructEngine-Token` 请求头。

### 远程构建与原有命令的关系

- `/buildmodel <url>` 保持原行为：以执行玩家位置为原点，偏移 `x+1 / y+0 / z+1`。
- 远程 `BUILD` 使用 Platform 的绝对原点，不读取玩家位置，也不会覆盖原有坐标映射代码。
- 两者共用同一 `BuildTaskQueue`、`BuildTickHandler`、`RegionSnapshot` 与 `BuildHistoryManager`，因此 `/rollback`、`/redo` 对远程任务同样生效。

### JSON 数据格式

请求的 URL 需返回一个 JSON 数组，每个元素描述一个方块（紧凑数组，元素顺序为 `id, x, y, z`）。后端（structengine-platform）产出的即为**紧凑单行**二维数组：

```json
[["minecraft:stone",0,0,0],["minecraft:cobblestone",1,0,0],["minecraft:glass",0,1,0]]
```

元素说明：

| 数组下标 | 类型 | 说明 |
| --- | --- | --- |
| 0 | string | 方块注册 ID，如 `minecraft:stone` |
| 1 / 2 / 3 | int | 相对构建原点的偏移坐标 x / y / z |

注意事项：

- 每个元素必须是 4 个字段的数组，格式错误会被判为解析失败
- 紧凑格式为规范格式；解析器会忽略元素之间的空白符，但请以上述紧凑单行为准
- `id` 必须为游戏内已注册的方块 ID，无法解析的方块（如 ID 非法、方块不存在、为空）会被跳过
- 数据为空或 JSON 解析失败时会在聊天栏提示错误
- HTTP 非 200 状态码、连接超时（10 秒）、请求超时（30 秒）均视为获取失败
- **地址校验**：URL 必须为非空、可解析的 HTTP(S) 地址且包含主机名，否则报错且不会开始构建
- **大小限制**：响应体超过 **16MB** 视为获取失败
- **异常处理**：构建过程中出现异常会中断当前模型并提示错误，已放置的方块不会回滚
- **离线续建**：任务入队后，即使发起玩家离线，队列仍会继续完成该构建
- **远程构建**：`/buildmodel` 以玩家位置为原点；Platform 下发的 `BUILD` 以消息中的 `(x,y,z)` 为原点，JSON 内坐标作为相对偏移叠加

## 项目结构

```
StructEngine (Root)
├── structengine-common        # 核心逻辑层：纯 Java 21，零 MC API 依赖
│   └── com/structengine/buildmodel/
│       ├── StructEngineCore.java      # 组合根：装配适配器与核心业务
│       ├── spi/                       # 平台抽象（依赖倒置）
│       │   ├── IWorldAdapter.java     # 方块读写 / 玩家位置 / 高度查询 / 消息发送
│       │   ├── IServerScheduler.java  # 主线程投递 / tick 回调
│       │   ├── PlayerRef.java          # 玩家句柄
│       │   ├── WorldRef.java           # 世界句柄
│       │   └── BlockStateSnapshot.java # 方块状态快照句柄
│       ├── model/                     # VoxelBlock / BlockPosition / BlockPlacement / RegionSnapshot
│       ├── queue/                     # BuildTaskQueue / ModelBuildJob / RollbackJob / RedoJob / BuildTickHandler
│       ├── service/                   # ModelDownloader（HTTP+JSON）/ ModelBuildService / BuildHistoryManager
│       └── remote/                    # WS 客户端 / 协议 DTO / CoreCommandBridge / AbsoluteBuildService
│           ├── GameServerWsClient.java    # 连接、心跳、重连、消息分发
│           ├── CoreCommandBridge.java     # 网络指令 → 主线程
│           ├── AbsoluteBuildService.java  # 异步下载 → 绝对坐标任务入队
│           └── AbsoluteModelBuildJob.java # 复用 BuildJob 接口的绝对坐标任务
├── structengine-fabric        # Fabric 适配层（fabric-loom + fabric-api + common）
│   └── com/structengine/fabric/
│       ├── StructEngineFabric.java    # ModInitializer 入口
│       ├── adapter/                   # FabricWorldAdapter / FabricScheduler / *Ref
│       └── command/                   # /buildmodel、/rollback、/redo（Brigadier）
├── structengine-neoforge      # NeoForge 适配层（NeoGradle userdev + neoforge + common）
│   └── com/structengine/neoforge/
│       ├── StructEngineNeoForge.java  # @Mod 入口
│       ├── adapter/                   # NeoForgeWorldAdapter / NeoForgeScheduler / *Ref
│       └── command/                   # Brigadier 命令注册
└── structengine-paper         # Paper 插件适配层（paper-api + common）
    └── com/structengine/paper/
        ├── StructEnginePaperPlugin.java  # JavaPlugin 入口
        ├── adapter/                      # PaperWorldAdapter / PaperScheduler / *Ref
        └── command/                      # Bukkit CommandExecutor
```

### 架构说明

- **依赖方向**：`fabric / neoforge / paper` → `common`，`common` 不依赖任何平台代码。
- **依赖倒置**：核心业务只依赖 `IWorldAdapter`、`IServerScheduler` 两个接口；平台差异全部收敛在适配层的 `*Ref` 与 `*Adapter` 实现中。
- **组合根**：各平台入口创建唯一的 `StructEngineCore`，在其中装配适配器、任务队列与历史管理器，并把 service 交给命令层。
- **网络层**：`remote` 包只依赖 JDK 原生 `HttpClient` 与 Gson（服务端运行时自带），通过 `GameServerContext` 解析玩家/世界，通过 `CoreCommandBridge` 回到主线程。

## License

[MIT](https://opensource.org/licenses/MIT)
