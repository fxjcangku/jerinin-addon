package com.example.addon.librarian;

public enum LibrarianState {
    IDLE,             // 空闲
    SCANNING,         // 扫描无业村民
    WALKING,          // Baritone 自动走向村民
    PLACING_LECTERN,  // 放置讲台（首次或重刷）
    WAITING_REFRESH,  // 等待村民成为图书管理员
    WAITING_TRADE,    // 等待交易界面打开
    CHECKING_TRADES,  // 检查交易内容
    CONFIRMING_PURCHASE, // 等待服务器确认自动交易成功
    BREAKING_LECTERN, // 破坏讲台（重刷）
    WAITING_JOBLESS,  // 等待村民失去职业
    COOLDOWN          // 找到后冷却，换下一个村民
}
