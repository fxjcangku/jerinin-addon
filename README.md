# Jerinin Addon

适用于 Minecraft 1.21.11 与 Meteor Client 的 Fabric 客户端扩展，集中提供自动化、交易辅助与界面汉化功能。

## 功能模块

### 附魔助手

- 自动寻找附近可用的失业村民，并跳过傻子村民
- 自动放置、破坏讲台并循环刷新图书管理员交易
- 按目标附魔、最高等级和绿宝石价格上限筛选附魔书
- 支持自动购买一次以锁定交易，完成后继续处理下一目标
- 支持手动模式与 Baritone 自动寻路模式
- 提供带附魔书图标的目标选择界面

### 圆石出售

- 达到指定数量后自动执行圆石出售流程
- 检测大厅状态并自动返回生存服
- 支持回服后指令、防掉线和状态 HUD

### 传送重锤

- 自动选择目标并切换快捷栏重锤
- 支持分段移动、返回原位和图腾绕过攻击
- 支持好友保护、玩家名单及目标类型筛选

### 界面汉化

- 提供 Meteor Client 常用模块与设置文本的中文显示
- 可在模块列表中独立开启或关闭

## JsMacros 脚本

仓库的 `jsm_scripts` 目录保存独立的 JsMacros 项目：

- `script.txt`：圆石出售脚本参考实现
- `slimefun-helper`：Slimefun Helper 完整项目，包含脚本、配置和工作站配置管理器

## 环境要求

- Minecraft 1.21.11
- Fabric Loader
- Meteor Client
- Java 25
- Baritone（仅附魔助手自动寻路模式需要）

## 安装

1. 安装对应版本的 Fabric Loader、Meteor Client 和 Fabric API 依赖。
2. 从 [Releases](https://github.com/fxjcangku/jerinin-addon/releases) 下载最新 JAR。
3. 将 JAR 放入 Minecraft 的 `mods` 文件夹。

## 本地构建

```powershell
.\gradlew.bat clean build
```

构建产物位于 `build/libs`。

## 开发状态

项目仍在持续开发。不同服务器的菜单布局、移动校验和反作弊规则可能影响自动化功能，请根据实际环境调整配置。

## 许可证

本项目遵循仓库中的 [LICENSE](LICENSE)。
