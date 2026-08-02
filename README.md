<div align="center">

```
     ██╗███████╗██████╗  █████╗ ██████╗ ██████╗  ██████╗ ███╗   ██╗
     ██║██╔════╝██╔══██╗██╔══██╗██╔══██╗██╔══██╗██╔═══██╗████╗  ██║
     ██║█████╗  ██████╔╝███████║██║  ██║██║  ██║██║   ██║██╔██╗ ██║
██   ██║██╔══╝  ██╔══██╗██╔══██║██║  ██║██║  ██║██║   ██║██║╚██╗██║
╚█████╔╝███████╗██║  ██║██║  ██║██████╔╝██████╔╝╚██████╔╝██║ ╚████║
 ╚════╝ ╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═════╝ ╚═════╝  ╚═════╝ ╚═╝  ╚═══╝
```

### ⚡ Jerinin 自研 · Meteor Client 私人扩展插件集

*基于 [Meteor Client](https://github.com/MeteorDevelopment/meteor-client) 开发的 Minecraft 1.21.11 Fabric 插件*

<br/>

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen?style=for-the-badge&logo=minecraft&logoColor=white)](https://minecraft.net)
[![Fabric](https://img.shields.io/badge/Fabric_Loader-0.16.5-blue?style=for-the-badge)](https://fabricmc.net)
[![Meteor](https://img.shields.io/badge/Meteor_Client-1.21.11--SNAPSHOT-8a2be2?style=for-the-badge)](https://meteorclient.com)
[![Java](https://img.shields.io/badge/Java-21-f89820?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net)
[![Version](https://img.shields.io/badge/Version-0.1.1-crimson?style=for-the-badge)](https://github.com/fxjcangku/jerinin-addon/releases)
[![License](https://img.shields.io/badge/License-Private-lightgrey?style=for-the-badge)](#)

</div>

---

## 📖 关于本项目

**Jeraddon** 是 **Jerinin** 为 Minecraft 1.21.11 Fabric 平台自研的 Meteor Client 私人扩展插件集。

> 基于 [Meteor Client](https://meteorclient.com) 开发，专注于 **PVP 战斗强化 · 自动化交易 · 智能移动辅助** 三大核心方向。
> 所有模块均经过大量实战测试，持续迭代优化。

**作者仓库：** [github.com/fxjcangku](https://github.com/fxjcangku)

---

## ⚔️ 功能模块

<details open>
<summary><b>🗡️ Jeraddon 通用</b>（战斗 · 移动 · 自动化）</summary>

<br/>

| 模块 | 类型 | 功能说明 |
|------|:----:|---------|
| **传送重锤** | 战斗 | 3D 分段传送高空落锤，支持图腾绕过、目标预测、安全高度扫描 |
| **新TP重锤** | 战斗 | 预测目标未来位置后分步传送，多高度 VClip 连续重锤，攻击后自动归位 |
| **长矛杀戮** | 战斗 | 自动锁定最近目标切换长矛，支持直接攻击/突进两种模式，含目标预测 |
| **破盾重锤** | 战斗 | 目标持盾时自动切斧破盾，破盾后可自动追击，支持触发条件配置 |
| **白名单** | 战斗 | 重锤/长矛攻击目标白名单管理，开启后仅攻击名单内玩家 |
| **垂直上升** | 移动 | 分段垂直 TP 上升，可配置高度、速度和触发方式 |
| **自动落地水** | 移动 | 高处坠落或着火时自动放水，落地后自动收水并恢复原槽位 |
| **附魔交易所** | 自动化 | 全自动附魔刷新：搜索村民 → 清障碍 → 放讲台 → 读取交易 → 购买目标附魔 |
| **圆石出售** | 自动化 | 背包圆石达到阈值自动出售，支持断线回服、HUD 库存显示 |

</details>

<details open>
<summary><b>🔤 Jeraddon 工具汉化</b></summary>

<br/>

| 模块 | 功能说明 |
|------|---------|
| **界面汉化** | 支持 **简体中文 / 繁体中文 / English** 三种语言一键切换，实时生效 |

</details>

---

## 📥 安装方法

> **环境要求：** Minecraft 1.21.11 + Fabric Loader 0.16.5 + Meteor Client 1.21.11-SNAPSHOT

### 安装步骤

```
1. 安装 Fabric Loader 0.16.5
   👉 https://fabricmc.net/use/installer/

2. 下载并安装 Meteor Client
   👉 https://meteorclient.com

3. 下载 Jeraddon.jar（见下方 Releases）

4. 将 Jeraddon.jar 放入 .minecraft/mods/ 文件夹

5. 启动游戏，进入 Meteor Client → 模块列表
   即可看到 「Jeraddon 通用」和「Jeraddon 工具汉化」两个分类
```

### 下载地址

[![Download](https://img.shields.io/badge/⬇️_下载_Jeraddon.jar-Latest_Release-brightgreen?style=for-the-badge)](https://github.com/fxjcangku/jerinin-addon/releases/latest)

---

## 🛠️ 技术栈

| 组件 | 版本 |
|------|------|
| Minecraft | 1.21.11 |
| Fabric Loader | 0.16.5 |
| Fabric Loom | 1.14.10 |
| Yarn Mappings | 1.21.11+build.3 |
| Meteor Client | 1.21.11-SNAPSHOT |
| Java | 21 |

---

## ⚠️ 免责声明

- 本项目为 **Jerinin 个人私用插件**，仅供学习研究参考
- 请勿在明确禁止使用 Mod 的服务器上使用
- 核心模块源代码不开源
- 使用本插件产生的一切后果由使用者自行承担

---

<div align="center">

**Made with ❤️ by [Jerinin](https://github.com/fxjcangku)**

*「比别人多一步，就是优势。」*

<br/>

⭐ 觉得不错的话点个 Star 吧！

</div>
