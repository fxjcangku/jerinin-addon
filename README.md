<div align="center">

<img src="https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Minecraft%20dark%20cinematic%20banner%2C%20glowing%20red%20meteor%20PVP%20combat%20addon%2C%20fantasy%20game%20plugin%20cover%2C%20professional%20dark%20theme&image_size=landscape_16_9" width="100%" />

<br/><br/>

```
     ██╗███████╗██████╗  █████╗ ██████╗ ██████╗  ██████╗ ███╗   ██╗
     ██║██╔════╝██╔══██╗██╔══██╗██╔══██╗██╔══██╗██╔═══██╗████╗  ██║
     ██║█████╗  ██████╔╝███████║██║  ██║██║  ██║██║   ██║██╔██╗ ██║
██   ██║██╔══╝  ██╔══██╗██╔══██║██║  ██║██║  ██║██║   ██║██║╚██╗██║
╚█████╔╝███████╗██║  ██║██║  ██║██████╔╝██████╔╝╚██████╔╝██║ ╚████║
 ╚════╝ ╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═════╝ ╚═════╝  ╚═════╝ ╚═╝  ╚═══╝
```

### ⚡ Jerinin 自研 · Meteor Client 战斗辅助插件

*基于 [Meteor Client](https://meteorclient.com) 开发 · Minecraft 1.21.11 Fabric*

<br/>

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen?style=for-the-badge&logo=minecraft&logoColor=white)](https://minecraft.net)
[![Fabric](https://img.shields.io/badge/Fabric-0.16.5-1976D2?style=for-the-badge)](https://fabricmc.net)
[![Meteor](https://img.shields.io/badge/Meteor_Client-SNAPSHOT-8A2BE2?style=for-the-badge)](https://meteorclient.com)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net)
[![Version](https://img.shields.io/badge/Version-0.1.1-DC143C?style=for-the-badge)](https://github.com/fxjcangku/jerinin-addon/releases)
[![Discord](https://img.shields.io/badge/Discord-加入-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/vwrRCtET)

<br/>

> **「每一行代码，都是实战中打磨出来的。」**

</div>

---

## 📖 关于 Jeraddon

**Jeraddon** 是 **[Jerinin](https://github.com/fxjcangku)** 自研的 Meteor Client 私人扩展插件，专注 PVP 战斗、自动化与本地化四大方向，历经实战打磨。

---

## ⚔️ 功能模块

| 模块 | 分类 | 说明 |
|------|:----:|------|
| 🔨 **传送重锤** | 战斗 | 高空TP落锤，图腾绕过 + 目标预测 + 安全扫描，自动归位 |
| 🔨 **新TP重锤** | 战斗 | 预测目标未来坐标后精准TP，多高度VClip连续重锤 |
| 🗡️ **长矛杀戮** | 战斗 | 自动锁定最近玩家切换长矛，直接攻击/突进双模式 |
| 🛡️ **破盾重锤** | 战斗 | 实时检测目标举盾，自动切斧破盾追击 |
| 📋 **白名单** | 战斗 | 攻击目标精准管理，准星操作一键添加/移除 |
| ⬆️ **垂直上升** | 移动 | 分段垂直发包上升，精确控制高度与速度 |
| 💧 **自动落地水** | 移动 | 自由落体/着火自动放水救命，落地后静默收回 |
| 📚 **附魔交易所** | 自动化 | 全自动刷附魔：搜索村民→清障碍→放讲台→读交易→购买，需 Baritone |
| 💎 **圆石出售** | 自动化 | 达阈值自动出售圆石，含防掉线心跳与 HUD |
| 🌐 **界面汉化** | 工具 | **简体中文 / 繁体中文 / 日本語 / 한국어 / English** 五语言实时切换 |

---

## 🌍 多语言支持

界面汉化模块支持以下语言，覆盖所有 Jeraddon 模块名称与设置项：

| 语言 | 状态 |
|:----:|:----:|
| 简体中文 | ✅ 完整 |
| 繁體中文 | ✅ 完整 |
| 日本語 | ✅ 完整 |
| 한국어 | ✅ 完整 |
| English | ✅ 原文 |

> Meteor Client 原版模块在日文/韩文模式下显示英文原名，Jeraddon 自研模块完整翻译。

---

## 📥 安装指南

> **前置要求：** Minecraft `1.21.11` · Fabric Loader `≥ 0.16.5` · Meteor Client `1.21.11-SNAPSHOT`

```
① 安装 Fabric Loader → https://fabricmc.net/use/installer/
② 安装 Meteor Client → https://meteorclient.com
③ 下载 Jeraddon.jar 放入 .minecraft/mods/
④ 如需附魔交易所自动寻路，同时放入 Baritone-1.21.11.jar
⑤ 启动游戏，Right Shift 打开 Meteor → 找到「Jeraddon 通用」分类
```

[![Download](https://img.shields.io/badge/⬇️%20下载%20Jeraddon.jar-Releases-brightgreen?style=for-the-badge)](https://github.com/fxjcangku/jerinin-addon/releases/latest)

---

## 🗺️ 前置插件：Baritone

**附魔交易所**的自动寻路功能需要安装 Baritone。

原作者：[cabaletta](https://github.com/cabaletta/baritone) · 本发布包提供 1.21.11 移植版，可在 Releases 页面一并下载。

---

## ⚠️ 免责声明

核心源码不开源。请勿在禁用 Mod 的服务器使用，由此产生的后果由使用者自行承担。

---

## 📬 联系作者

<div align="center">

[![GitHub](https://img.shields.io/badge/GitHub-fxjcangku-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/fxjcangku)
[![Discord](https://img.shields.io/badge/Discord-加入服务器-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/vwrRCtET)

<br/>

*Made with ❤️ by Jerinin · Powered by [Meteor Client](https://meteorclient.com)*

⭐ 觉得不错的话点个 Star！

</div>
