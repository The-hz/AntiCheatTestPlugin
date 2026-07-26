# ACTestPlugin

一个专为 Minecraft Paper 服务器设计的反作弊测试插件。

## 功能特性

### 1. 假人系统
使用 Citizens API 实现四种不同类型的测试假人：

| 类型 | 特性 |
|------|------|
| **站桩假人 (stationary)** | 无敌、无击退、不会死亡、保持位置不动 |
| **PVP假人 (pvp)** | 主动攻击10格范围内的玩家、可死亡、有击退、玩家离开范围返回出生点 |
| **被动假人 (passive)** | 受击不还手、可死亡、有击退 |
| **反击假人 (counter)** | 被攻击时反击一下、无敌、无击退 |

### 2. 搭路测试区
- 定时自动清理区域内的方块
- 智能延迟：清理时如果区域内有人，自动延后60秒
- 支持多区域管理

### 3. 计分板
实时显示玩家状态信息：
- **Ping** - 网络延迟
- **CPS L** - 左键每秒点击次数
- **CPS R** - 右键每秒点击次数
- **BPS** - 每秒移动方块数
- **Sprint** - 是否疾跑
- **Ground** - 是否在地面上
- **Sneak** - 是否潜行

### 4. 环境控制
- 禁用除玩家外的所有生物生成
- 纯净的PVP测试环境

## 依赖

- **Paper 1.20.6+**
- **[Citizens](https://wiki.citizensnpcs.co/)** - 假人框架

## 安装

1. 下载插件 JAR 文件
2. 将 JAR 文件放入服务器的 `plugins` 文件夹
3. 安装 Citizens 插件
4. 重启服务器

## 命令

### 主命令
```
/actest help          - 显示帮助信息
/actest reload        - 重载配置
/actest status        - 查看插件状态
```

### 假人管理
```
/spawnbot <type> [name]    - 生成假人
/removebot [name]          - 移除假人（不填名字则移除最近的）
```

### 搭路区管理
```
/buildzone pos1                 - 设置第一个角落
/buildzone pos2 <name>          - 设置第二个角落并创建区域
/buildzone remove <name>        - 删除区域
/buildzone clear [name]         - 立即清理区域
/buildzone list                 - 列出所有区域
```

## 权限

- `actest.admin` - 插件管理权限（默认：OP）

## 配置

插件配置文件位于 `plugins/ACTestPlugin/config.yml`：

```yaml
# 假人配置
bots:
  testbot:
    type: stationary
    location:
      world: world
      x: 0
      y: 100
      z: 0
      yaw: 0
      pitch: 0

# 搭路区配置
buildzones:
  testzone:
    world: world
    corner1:
      x: 0
      y: 100
      z: 0
    corner2:
      x: 50
      y: 120
      z: 50
    clearInterval: 300
```

## 开发

### 构建
```bash
./gradlew build
```

### 运行测试服务器
```bash
./gradlew runServer
```

## 适用场景

- 反作弊测试
- PVP 机制测试
- 客户端验证

## 开源协议

详见 [LICENSE](LICENSE.MD) 文件。

## 作者

Copyright The-hz (https://github.com/The-hz)