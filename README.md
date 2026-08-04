<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:5b3a29,35:8f5f3d,70:6f8f3d,100:2f5d3a&height=190&section=header&text=JERADDON&fontSize=58&fontColor=fff3c4&fontAlignY=38&desc=一个慢慢长大的%20Minecraft%20Meteor%20扩展&descAlignY=62&descColor=f6d98b&animation=fadeIn" width="100%"/>

<img src="https://readme-typing-svg.demolab.com?font=ZCOOL+KuaiLe&size=22&duration=3200&pause=900&color=6F8F3D&center=true&vCenter=true&width=720&height=45&lines=今天也来打理一下农场吧~;收获、探索、战斗，还有一点自动化;为日常游戏写的小工具合集" alt="Jeraddon"/>

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-6f8f3d?style=flat-square)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.16.5-8f5f3d?style=flat-square)](https://fabricmc.net/)
[![Meteor](https://img.shields.io/badge/Meteor_Client-SNAPSHOT-6b4f3a?style=flat-square)](https://meteorclient.com/)
[![Java](https://img.shields.io/badge/Java-21-bb7a3e?style=flat-square)](https://adoptium.net/)
[![Latest](https://img.shields.io/github/v/release/fxjcangku/jerinin-addon?label=最新版本&color=6f8f3d&style=flat-square)](https://github.com/fxjcangku/jerinin-addon/releases/latest)

**[下载最新版本](https://github.com/fxjcangku/jerinin-addon/releases/latest)**　·　**[查看更新](https://github.com/fxjcangku/jerinin-addon/releases)**　·　**[提交问题](https://github.com/fxjcangku/jerinin-addon/issues)**

</div>

---

## 🌱 这是 Jeraddon

Jeraddon 是一个面向 Minecraft 日常游戏的 Meteor Client 扩展。它没有复杂的包装，主要就是把自己想用的小功能慢慢整理起来：像一块刚开垦的田地，今天种下一个功能，明天再把它打磨得顺手一点。

它适合喜欢探索、种田、整理物品、跑服务器流程，也偶尔需要战斗辅助的玩家。

## 🧺 功能一览

| 分类 | 内容 |
| --- | --- |
| 农业自动化 | 作物选择、成熟识别、路线规划、收获、补种、库存与容器辅助 |
| 服务器辅助 | 资源包处理、自动注册、自动登录、进服指令、断线重连 |
| 附魔交易 | 自动寻找村民、刷新讲台、识别目标附魔、购买交易 |
| 日常工具 | 物品信息查看、中文界面、圆石出售、HUD 提示 |
| 移动与战斗 | 垂直上升、自动落地水、长矛、重锤、破盾和白名单 |

## 🍓 星露谷农业助手

农业功能是 Jeraddon 里单独的一块田地，支持服务器资源包中的自定义作物和部分原版作物。

- 支持 26 种自定义作物识别
- 支持成熟状态扫描与作物筛选
- 支持最近邻与路线规划
- 支持可选收获、补种、返程和库存判断
- 支持种子识别、容器存取和补给辅助
- 提供中文的[作物资料与种子表](资料/农业/作物分类与种子表.csv)

第一次使用时，建议先只开启扫描和预览，确认识别结果后再开启收获、补种和移动动作。

## 🔑 离线服务器自动登入

自动登入助手主要给需要 `/login`、`/register` 的离线模式服务器使用，也兼容正版账号进入离线模式服务器的情况。它不会改变 Minecraft 账号登录方式，只是在进入服务器后，根据服务器提示发送你已经配置好的命令。

- 自动识别注册或登录提示
- 支持按服务器保存登录信息
- 支持离线昵称匹配
- 支持登录后自动执行服务器指令
- 支持资源包处理和断线重连
- 账号密码只保存在本地游戏配置中，不提交到仓库

启用前请确认服务器允许相关操作，并仔细检查自己的本地配置。不要把密码写进截图、Issue 或公开文件。

## 🧙 附魔交易需要 Baritone

附魔交易所使用 Baritone 进行寻路，所以只有使用附魔交易功能时才需要额外安装 Baritone。

安装顺序：

1. 安装 Minecraft 1.21.11、Fabric Loader、Meteor Client 和 Java 21。
2. 从 [v1.2 Release](https://github.com/fxjcangku/jerinin-addon/releases/tag/v1.2) 下载 `Jeraddon-1.2.jar`。
3. 同一发布页下载 `bariton1.21.11.jar`，和 Jeraddon 一起放进 `.minecraft/mods/`。
4. 启动游戏，在 Meteor 菜单中打开对应功能。

Baritone 只作为附魔寻路功能的前置，不使用附魔交易时可以不安装。

## 📦 下载安装

| 版本 | 适合人群 | 页面 |
| --- | --- | --- |
| v1.2 | 当前推荐，包含农业自动化和离线登入更新 | [下载 v1.2](https://github.com/fxjcangku/jerinin-addon/releases/tag/v1.2) |
| v1.1 | 经典稳定版本 | [下载 v1.1](https://github.com/fxjcangku/jerinin-addon/releases/tag/v1.1) |
| v0.1.1 | 初始版本归档 | [下载 v0.1.1](https://github.com/fxjcangku/jerinin-addon/releases/tag/v0.1.1) |

下载后只需要把 Jeraddon JAR 放入 `mods` 文件夹。需要附魔寻路时，再把同一版本发布页中的 Baritone JAR 一起放入。

## 📚 资料

- [农业使用说明](资料/农业/使用说明.md)
- [作物分类与种子表](资料/农业/作物分类与种子表.csv)
- [成熟状态与种子数据](资料/农业/成熟状态与种子数据.json)
- [模块说明](docs/MODULES.md)
- [常见问题](docs/FAQ.md)

## 🟢 相关项目

[Slimefun Helper](https://github.com/fxjcangku/slimefun-helper) 是独立的粘液助手项目，拥有自己的版本线和下载页面。Jeraddon 不包含它的脚本文件。

## 📌 说明

本仓库是公开发布页，核心实现暂不公开；这里主要提供下载文件、使用说明和独立资料。Jeraddon 完全免费，作者只是有空就写一点自己想用的东西。

请遵守服务器规则，不要在明确禁止相关功能的服务器使用。使用前请先在安全环境中测试。

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:2f5d3a,45:6f8f3d,75:8f5f3d,100:5b3a29&height=110&section=footer&animation=fadeIn" width="100%"/>

`愿你的背包有空位，农场有收成。`　🌾

</div>
