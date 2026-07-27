'use strict';const filename = file.getName();
const filenameSplit = filename.replace(/\.(js|ts)$/, '').split('V');
const AppConfig = Object.freeze({
    script: {
        name: filenameSplit.shift()?.trim() || '',
        version: filenameSplit.pop()?.trim() || '',
        author: 'Jerinin',
    },
    file: {
        configDir: 'config',
        blacklistFile: '黑名单.json',
        workstationFile: '工作站.json',
        recipeFile: '配方.json',
    },
    timeout: {
        delayTime: 20,
        delayAfterMove: 100,
        delayAfterCompletion: 50,
        timeoutWaitChange: 1000,
        timeoutCheckCompletion: 5000,
    },
    crafting: {
        reservedSlotsForCrafting: 3,
        maxAttempts: 10,
        maxRefillAttempts: 10,
        timeoutCheckStart: 5000,
    },
    calculation: {
        debounceCalculation: 10,
    },
    storage: {
        blockScan: [
            { blockId: 'minecraft:barrel', blockStates: [] },
            { blockId: 'minecraft:chest', blockStates: ['type=left', 'type=single'] },
            { blockId: 'minecraft:honeycomb_block', blockStates: [] },
            { blockId: 'minecraft:note_block', blockStates: ['note=0'] },
        ],
        gridContainerTitles: ['网格', 'Network Grid', '高级网格', '高级 Network Grid'],
    },
    workstation: {},
    guide: {
        needAmount: {
            left: 1,
            right: -1,
            shift_left: 64,
            shift_right: -64,
            drop: 32,
            ctrl_drop: -64,
        },
        recipeDisplayConfig: [
            {
                type: '3x3',
                minSlotCount: 27,
                workSlot: 10,
                materialSlots: [3, 4, 5, 12, 13, 14, 21, 22, 23],
                outputSlot: 16,
                extraValid: {
                    slots: {
                        'OR|0|1': 'SLIMEFUN:_UI_BACK',
                        'AND|2|6|11|15|20|24': 'minecraft:air',
                    },
                },
            },
            {
                type: '6x6',
                minSlotCount: 54,
                workSlot: 8,
                materialSlots: [
                    1, 2, 3, 4, 5, 6, 10, 11, 12, 13, 14, 15, 19, 20, 21, 22, 23, 24, 28, 29, 30, 31, 32, 33,
                    37, 38, 39, 40, 41, 42, 46, 47, 48, 49, 50, 51,
                ],
                outputSlot: 35,
                extraValid: {
                    slots: { '0': 'SLIMEFUN:_UI_BACK' },
                    workIds: ['SLIMEFUN:LOGITECH_BUG_CRAFTER', 'SLIMEFUN:INFINITY_FORGE'],
                    isOutputItemIsTitle: false,
                },
            },
        ],
    },
    ui: {
        topPercent: 35,
        middlePercent: 50,
        bottomPercent: 85,
        margin: 4,
        columnWidth: 100,
        lineHeight: 9,
        leftMargin: 30,
        colors: {
            brand: 0x55ffff,
            title: 0xffd65a,
            content: 0xf2f2f2,
        },
    },
    game: {
        ticksPerSecond: 20,
        maxItemStackSize: 64,
    },
});class Logger {
    constructor() {
        this.prefix = `§9[${AppConfig.script.name}]`;
        this.logPath = 'logs/slimefun-helper.log';
        this.colors = {
            INFO: '§f',
            WARN: '§e',
            ERROR: '§c',
            DEBUG: '§7',
            SUCCESS: '§a',
        };
    }
    output(msg, color = '§f') {
        Chat.log(`${this.prefix} ${color}${msg}`);
    }
    log(msg, level) {
        const color = this.colors[level] || '§f';
        const message = typeof msg === 'object' ? JSON.stringify(msg) : String(msg);
        this.pushLogToFile(`${level}: ${message}`);
        this.output(message, color);
    }
    clearLogFile() {
        try {
            if (FS.exists(this.logPath)) {
                FS.unlink(this.logPath);
            }
        }
        catch (e) {
            Chat.log(`[Logger] 清空日志文件失败: ${e}`);
        }
    }
    pushLogToFile(msg) {
        if (!FS.exists('logs')) {
            FS.makeDir('logs');
        }
        FS.open(this.logPath).append(`[${new Date().toLocaleString()}] ${msg}\n`);
    }
    actionbar(msg) {
        Chat.actionbar(`[${AppConfig.script.name}] ${msg}`);
    }
    logf(msg) {
        Chat.log(msg);
    }
    info(msg) {
        this.log(msg, 'INFO');
    }
    warn(msg) {
        this.log(msg, 'WARN');
    }
    success(msg) {
        this.log(msg, 'SUCCESS');
    }
    error(msg, error) {
        const message = error
            ? `${msg}\n错误详情: ${error instanceof Error ? error.message : String(error)}`
            : msg;
        this.log(message, 'ERROR');
    }
    debug(msg) {
        if (msg.trim()) {
            this.pushLogToFile(`DEBUG: ${msg.trim()}`);
        }
    }
}
var Logger$1 = new Logger();const path = file.getPath();
const keySession = path + ':toggle:session';
const keyStopRequest = path + ':toggle:stopRequests';
const isRenderThread = JavaWrapper.methodToJava((th) => th.getName() === 'Render thread');
const contexts = Java.from(JsMacros.getOpenContexts()).filter((c) => c.getFile()?.getPath() === path && c !== context.getCtx() && c.getMainThread().isAlive());
class Toggle {
    check() {
        return false;
    }
    checkWhileWait(ticks, stopCondition, stopConditionOnSec) {
        while (ticks-- > 0 &&
            this.check() &&
            !stopCondition?.() &&
            !(!(ticks % 20) && stopConditionOnSec?.()))
            Client.waitTick();
        return this.check();
    }
    basicLoop(cb, interval = 1) {
        if (!this.check())
            return;
        let value = undefined;
        interval = Math.ceil(interval);
        if (interval < 1) {
            while (this.check())
                if ((value = cb()) !== undefined)
                    break;
        }
        else
            do {
                if ((value = cb()) !== undefined)
                    break;
            } while (this.checkWhileWait(interval));
        if (value != null)
            Logger$1.info(value);
        try {
            context.getCtx().getBoundThreads().removeIf(isRenderThread);
        }
        catch (error) {
            Logger$1.error('清理渲染线程失败', error);
        }
    }
}
const toggle = new Toggle();
if (contexts.length === 0) {
    GlobalVars.putInt(keyStopRequest, 0);
    const session = GlobalVars.incrementAndGetInt(keySession);
    if (session === null)
        throw new TypeError('会话密钥被其他东西占用了！');
    toggle.check = () => {
        return GlobalVars.getInt(keySession) === session && !GlobalVars.getInt(keyStopRequest);
    };
}
else {
    const stopRequests = GlobalVars.incrementAndGetInt(keyStopRequest);
    if (stopRequests === null)
        throw new TypeError('stopRequests键被其他东西占用了！');
    for (const context of contexts)
        try {
            if (context.getBoundThreads().removeIf(isRenderThread))
                Chat.log(`找到并删除了渲染线程。`);
        }
        catch (error) {
            Logger$1.error('清理上下文渲染线程失败', error);
        }
    if (stopRequests > 1) {
        if (stopRequests === 2) {
            Logger$1.info(`有 ${contexts.length} 个上下文!`);
            Logger$1.info(`再次单击以尝试强制关闭它们。可能会导致意外行为！`);
        }
        else {
            for (const context of contexts)
                context.closeContext();
            Logger$1.info(`强制关闭了 ${contexts.length} 个上下文.`);
            GlobalVars.putInt(keyStopRequest, 1);
        }
    }
}function Ok(data) {
    return { success: true, data };
}
function Err(error) {
    return { success: false, error };
}
var Result;
(function (Result) {
    function isOk(result) {
        return result.success === true;
    }
    Result.isOk = isOk;
    function isErr(result) {
        return result.success === false;
    }
    Result.isErr = isErr;
    function unwrapOr(result, defaultValue) {
        return result.success ? result.data : defaultValue;
    }
    Result.unwrapOr = unwrapOr;
    function unwrap(result) {
        if (result.success) {
            return result.data;
        }
        throw new Error(`Unwrap failed: ${result.error}`);
    }
    Result.unwrap = unwrap;
    function map(result, fn) {
        return result.success ? Ok(fn(result.data)) : result;
    }
    Result.map = map;
    function mapErr(result, fn) {
        return result.success ? result : Err(fn(result.error));
    }
    Result.mapErr = mapErr;
    function andThen(result, fn) {
        return result.success ? fn(result.data) : result;
    }
    Result.andThen = andThen;
    function all(results) {
        const data = [];
        for (const result of results) {
            if (!result.success) {
                return result;
            }
            data.push(result.data);
        }
        return Ok(data);
    }
    Result.all = all;
    async function fromPromise(promise) {
        try {
            const data = await promise;
            return Ok(data);
        }
        catch (error) {
            return Err(error instanceof Error ? error : new Error(String(error)));
        }
    }
    Result.fromPromise = fromPromise;
    function fromThrowable(fn) {
        try {
            return Ok(fn());
        }
        catch (error) {
            return Err(error instanceof Error ? error : new Error(String(error)));
        }
    }
    Result.fromThrowable = fromThrowable;
    function fromNullable(value, error = '值为空或未定义') {
        return value !== null && value !== undefined ? Ok(value) : Err(error);
    }
    Result.fromNullable = fromNullable;
})(Result || (Result = {}));class ConfigLoader {
    constructor(configName, defaultData, options = {}) {
        this.configName = configName;
        this.cache = new Map();
        this.configPath = FS.combine(FS.toRawPath(AppConfig.file.configDir).toString(), configName);
        this.defaultData = defaultData;
        this.options = { autoCreate: true, enableCache: true, prettyFormat: true, ...options };
    }
    read() {
        if (this.options.enableCache) {
            const cached = this.cache.get(this.configPath);
            if (cached)
                return Ok(cached);
        }
        try {
            if (!FS.exists(this.configPath)) {
                if (!this.options.autoCreate)
                    return Err('配置文件不存在');
                const result = this.write(this.defaultData);
                if (result.success)
                    return result;
                return Err(`创建配置文件失败 [${this.configName}]: ${result.error}`);
            }
            const content = this.readFile();
            if (!content.success)
                return content;
            const data = JSON.parse(content.data);
            if (this.options.enableCache)
                this.cache.set(this.configPath, data);
            return Ok(data);
        }
        catch (error) {
            return Err(`读取配置文件失败 [${this.configName}]: ${error instanceof Error ? error.message : String(error)}`);
        }
    }
    write(data) {
        try {
            const jsonString = this.options.prettyFormat
                ? JSON.stringify(data, null, 2)
                : JSON.stringify(data);
            const dirPath = FS.getDir(this.configPath);
            if (!FS.exists(dirPath)) {
                FS.makeDir(dirPath);
            }
            if (!FS.exists(this.configPath)) {
                FS.createFile(dirPath, FS.getName(this.configPath), true);
            }
            const fileHandler = FS.open(this.configPath);
            fileHandler.write(jsonString);
            if (this.options.enableCache) {
                this.cache.set(this.configPath, data);
            }
            return Ok(data);
        }
        catch {
            return Err('写入配置文件失败');
        }
    }
    readFile() {
        try {
            const fileHandler = FS.open(this.configPath);
            const fileContent = fileHandler.read();
            return Ok(fileContent);
        }
        catch {
            return Err(`读取配置文件失败 [${this.configName}]`);
        }
    }
    clearCache() {
        this.cache.clear();
    }
    reload() {
        this.clearCache();
        return Result.unwrapOr(this.read(), this.defaultData);
    }
    getPath() {
        return this.configPath;
    }
    getFilename() {
        return FS.getName(this.configPath);
    }
}class BaseRepository {
    constructor(filename, defaultValue) {
        this.defaultValue = defaultValue;
        this.data = new Map();
        this.dataType = 'unknown';
        this.configLoader = new ConfigLoader(filename, defaultValue);
    }
    initialize() {
        const result = this.configLoader.read();
        if (!result.success)
            throw new Error(result.error);
        const config = result.data;
        this.dataType = Array.isArray(config) ? 'array' : 'object';
        this.loadData(config);
        this.logInitialization();
    }
    loadData(config) {
        this.clear();
        if (Array.isArray(config) && config.length > 0) {
            config.forEach((value) => this.data.set(this.getKey(value), value));
        }
        else {
            Object.entries(config).forEach(([key, value]) => this.data.set(key, value));
        }
    }
    logInitialization() {
        Logger$1.info(`✓ ${this.getRepositoryName()}已加载: ${this.data.size} 项`);
    }
    save() {
        const data = this.dataToFile();
        const result = this.configLoader.write(data);
        if (result.success) {
            return Ok(data);
        }
        return Err('写入配置文件失败');
    }
    dataToFile() {
        switch (this.dataType) {
            case 'array':
                return Array.from(this.data.values());
            case 'object':
                return Object.fromEntries(this.data);
            default:
                throw new Error('数据类型未确定，请先初始化仓库');
        }
    }
    getAll() {
        return this.data;
    }
    size() {
        return this.data.size;
    }
    has(key) {
        return this.data.has(key);
    }
    get(key) {
        return this.data.get(key);
    }
    clear() {
        this.data.clear();
    }
}class BlacklistRepository extends BaseRepository {
    constructor() {
        super(AppConfig.file.blacklistFile, []);
        this.getRepositoryName = () => '黑名单数据';
        this.getKey = (item) => item.id;
    }
    add(id, name) {
        if (!this.isBlacklist(id)) {
            const black = { id: id, name: name };
            this.data.set(id, black);
            const result = this.save();
            if (!result.success) {
                this.data.delete(id);
                return result;
            }
            return Ok(void 0);
        }
        return Err(`添加失败：物品 ${id}(${name}) 已存在于黑名单中`);
    }
    remove(id) {
        const black = this.data.get(id);
        if (this.data.delete(id)) {
            const result = this.save();
            if (!result.success) {
                this.data.set(id, black);
                return result;
            }
            return Ok(void 0);
        }
        return Err(`移除失败：物品 ${id} 不存在于黑名单中`);
    }
    isBlacklist(id) {
        return this.data.has(id);
    }
}
var BlacklistRepository$1 = new BlacklistRepository();class RecipeRepository extends BaseRepository {
    constructor() {
        super(AppConfig.file.recipeFile, []);
        this.recipesByWorkstation = new Map();
        this.getRepositoryName = () => '配方数据';
        this.getKey = (item) => item.id;
        this.getRecipe = (itemId) => {
            return this.data.get(itemId);
        };
    }
    loadData(config) {
        super.loadData(config);
        this.recipesByWorkstation.clear();
        this.addRecipes(config, false);
    }
    logInitialization() {
        Logger$1.info(`✓ ${this.getRepositoryName()}已加载: ${this.data.size} 个配方`);
    }
    addRecipe(recipe, save = true) {
        const previousData = save ? this.dataToFile() : null;
        this.data.set(recipe.id, recipe);
        if (recipe.station) {
            if (!this.recipesByWorkstation.has(recipe.station)) {
                this.recipesByWorkstation.set(recipe.station, []);
            }
            this.recipesByWorkstation.get(recipe.station).push(recipe);
        }
        if (save) {
            const result = this.save();
            if (!result.success) {
                this.loadData(previousData);
                return result;
            }
        }
        return Ok(undefined);
    }
    addRecipes(recipes, save = true) {
        const previousData = save ? this.dataToFile() : null;
        recipes.forEach((recipe) => this.addRecipe(recipe, false));
        if (save) {
            const result = this.save();
            if (!result.success) {
                this.loadData(previousData);
                return result;
            }
        }
        return Ok(undefined);
    }
}
var RecipeRepository$1 = new RecipeRepository();function waitForCondition(condition, timeout, waitTime = AppConfig.timeout.delayTime) {
    let elapsed = 0;
    do {
        const result = condition();
        if (result)
            return result;
        if (elapsed >= timeout)
            return null;
        Time.sleep(waitTime);
        elapsed += waitTime;
    } while (toggle.check());
    return null;
}
const isEmptyEx = (obj) => {
    if (obj === null || obj === undefined)
        return true;
    if (typeof obj === 'string')
        return obj.trim().length === 0;
    if (typeof obj === 'object')
        return Object.keys(obj).length === 0;
    if (Array.isArray(obj))
        return obj.length === 0;
    return false;
};class PositionHelper {
    constructor() {
        this.isPosArray = (pos) => Array.isArray(pos) && pos.length === 3;
        this.isPos3D = (pos) => ['x', 'y', 'z'].every((p) => p in pos);
        this.isBlockPos = (pos) => ['getX', 'getY', 'getZ'].every((p) => p in pos);
        this.P2A = (pos) => {
            if (this.isPosArray(pos))
                return pos;
            if (this.isPos3D(pos))
                return [pos.x, pos.y, pos.z];
            if (this.isBlockPos(pos))
                return [pos.getX(), pos.getY(), pos.getZ()];
            throw new Error('无效的位置类型');
        };
        this.arePositionsEqual = (pos1, pos2) => {
            const p1 = this.P2A(pos1);
            const p2 = this.P2A(pos2);
            return p1[0] === p2[0] && p1[1] === p2[1] && p1[2] === p2[2];
        };
    }
}
var PositionHelper$1 = new PositionHelper();class PlayerHelper {
    player() {
        return (Player.getPlayer() ??
            (() => {
                throw new Error('找不到玩家实体！');
            })());
    }
    interactionManager() {
        return (Player.getInteractionManager() ??
            (() => {
                throw new Error('找不到交互管理器！');
            })());
    }
    pos() {
        return this.player()?.getPos() ?? { x: 0, y: 0, z: 0 };
    }
    eyePos() {
        return this.player()?.getEyePos() ?? { x: 0, y: 0, z: 0 };
    }
    reach() {
        return Player.getReach() ?? 4.5;
    }
    oppositeFacing() {
        const dir = this.player()?.getFacingDirection();
        return dir?.getOpposite()?.getName() ?? 'down';
    }
    rayTraceBlock(distance = this.reach(), fluid = false) {
        return Player.detailedRayTraceBlock(distance, fluid);
    }
    rayTraceEntity(distance = this.reach()) {
        return Player.rayTraceEntity(distance);
    }
    interactBlock(pos, offHand = false) {
        this.interactionManager().interactBlock(...PositionHelper$1.P2A(pos), this.oppositeFacing(), offHand);
        Client.waitTick(2);
        return true;
    }
    interactItem(offHand = false) {
        this.interactionManager().interactItem(offHand);
        Client.waitTick(2);
        return true;
    }
}
var PlayerHelper$1 = new PlayerHelper();class SlimefunHelper {
    constructor() {
        this.PREFIX = 'SLIMEFUN:';
        this.MINECRAFT_PREFIX = 'minecraft:';
        this.normalize = (id = '') => {
            if (id.startsWith(this.MINECRAFT_PREFIX) || id.startsWith(this.PREFIX)) {
                return id.toUpperCase();
            }
            return `${this.PREFIX}${id}`.toUpperCase();
        };
        this.isDenied = (id = '') => {
            return this.DENIED_IDS.includes(id);
        };
        this.matchId = (id = '', ...targetIds) => {
            const normalizedId = this.normalize(id);
            return targetIds.some((targetId) => normalizedId === this.normalize(targetId));
        };
        this.matchStack = (stack, ...targetIds) => {
            if (!stack || stack.isEmpty())
                return false;
            return this.matchId(stack.id, ...targetIds);
        };
        this.isSlimefun = (item) => {
            if (item?.name?.includes?.('工作台'))
                return true;
            const { id = '' } = item ?? {};
            const menuIds = Object.values(this.MENU_IDS);
            return id.startsWith(this.PREFIX) && !menuIds.includes(id);
        };
        this.GUIDE_ID = this.normalize('slimefun_guide');
        this.MENU_IDS = Object.freeze({
            Menu: this.normalize('_UI_MENU'),
            Search: this.normalize('_UI_SEARCH'),
            Back: this.normalize('_UI_BACK'),
            PrevActive: this.normalize('_UI_PREVIOUS_ACTIVE'),
            PrevInactive: this.normalize('_UI_PREVIOUS_INACTIVE'),
            NextActive: this.normalize('_UI_NEXT_ACTIVE'),
            NextInactive: this.normalize('_UI_NEXT_INACTIVE'),
            NoPermission: this.normalize('_UI_NO_PERMISSION'),
            NotResearched: this.normalize('_UI_NOT_RESEARCHED'),
            Barrier: 'minecraft:barrier',
        });
        this.DENIED_IDS = Object.freeze([
            this.MENU_IDS.NoPermission,
            this.MENU_IDS.NotResearched,
            this.MENU_IDS.Barrier,
        ]);
    }
}
var SlimefunHelper$1 = new SlimefunHelper();class InventoryHelper {
    getCurrentSyncId() {
        return Player.openInventory().getCurrentSyncId();
    }
    isContainer() {
        return Player.openInventory().isContainer();
    }
    getContainerTitle() {
        return Player.openInventory().getContainerTitle();
    }
    getMainHandSlot() {
        return this.getSlots('hotbar')[Player.openInventory().getSelectedHotbarSlotIndex()];
    }
    getMainHandItem() {
        return this.getSlot(this.getMainHandSlot());
    }
    getOffHandSlot() {
        return this.getSlots('offhand')[0];
    }
    getOffHandItem() {
        return this.getSlot(this.getOffHandSlot());
    }
    findFreeSlot(...mapIds) {
        return Player.openInventory().findFreeSlot(...mapIds);
    }
    swap(slot1, slot2) {
        return Player.openInventory().swap(slot1, slot2);
    }
    getSlots(...mapIds) {
        return Java.from(Player.openInventory().getSlots(...mapIds));
    }
    getSlot(slot) {
        const inv = Player.openInventory();
        if (inv.getTotalSlots() <= slot) {
            throw new Error(`Invalid slot: ${slot}, total slots: ${inv.getTotalSlots()}`);
        }
        const item = inv.getSlot(slot);
        const id = this.getItemId(item);
        const name = item.getName().getString();
        const count = this.getItemCount(item, id, slot);
        const maxCount = item.getMaxCount();
        const mapId = inv.getLocation(slot);
        const isEmpty = () => id === 'minecraft:air' || item.isEmpty();
        return { mapId, slot, id, name, count, maxCount, raw: item, inv, isEmpty };
    }
    getItemsWithEmpty(...mapIds) {
        return this.getSlots(...mapIds).map((slot) => this.getSlot(slot));
    }
    getItems(...mapIds) {
        return this.getItemsWithEmpty(...mapIds).filter((item) => item && !item.isEmpty());
    }
    openContainerForBlock(pos, offHand = false, timeout = AppConfig.timeout.timeoutWaitChange) {
        if (this.isContainer() &&
            this.lastOpenInvPos &&
            PositionHelper$1.arePositionsEqual(this.lastOpenInvPos, pos))
            return this.lastOpenInv;
        this.lastOpenInv = this.waitForInventoryChange(() => PlayerHelper$1.interactBlock(pos, offHand), timeout);
        return this.lastOpenInv;
    }
    openContainerForItem(offHand = false) {
        return this.waitForInventoryChange(() => PlayerHelper$1.interactItem(offHand));
    }
    closeContainer() {
        const isCloseInv = this.waitForInventoryClose(() => Player.openInventory().closeAndDrop());
        if (isCloseInv)
            this.lastOpenInv = undefined;
        return isCloseInv;
    }
    waitForInventoryChange(fn, timeout = AppConfig.timeout.timeoutWaitChange) {
        const syncId = this.getCurrentSyncId();
        fn?.();
        return (waitForCondition(() => {
            if (this.getCurrentSyncId() !== syncId && this.isContainer()) {
                return Player.openInventory();
            }
            return null;
        }, timeout) ?? undefined);
    }
    waitForInventoryClose(fn, timeout = AppConfig.timeout.timeoutWaitChange) {
        fn?.();
        return (waitForCondition(() => {
            if (this.getCurrentSyncId() === 0 && !this.isContainer()) {
                return true;
            }
            return null;
        }, timeout) !== null);
    }
    waitForItemInSlot(slot, itemId, maxTicks = AppConfig.timeout.timeoutCheckCompletion) {
        return this.waitForSlotCondition(slot, (item) => {
            if (!item || item.isEmpty())
                return false;
            if (!itemId)
                return true;
            return SlimefunHelper$1.matchId(item.id, itemId);
        }, maxTicks);
    }
    waitForSlotCondition(slot, condition, timeout = AppConfig.timeout.timeoutCheckCompletion) {
        try {
            return waitForCondition(() => {
                const item = this.getSlot(slot);
                if (condition(item))
                    return item;
                return null;
            }, timeout);
        }
        catch {
            return null;
        }
    }
    waitForItemInSlots(slots, itemId, count = 1, timeout = AppConfig.timeout.timeoutCheckCompletion) {
        const syncId = this.getCurrentSyncId();
        const result = waitForCondition(() => {
            if (this.getCurrentSyncId() !== syncId)
                return -1;
            const items = slots.map((slot) => this.getSlot(slot));
            const itemCounts = items.reduce((acc, item) => acc.set(item.id, (acc.get(item.id) || 0) + (item.count || 0)), new Map());
            const itemCount = itemCounts.get(itemId) || 0;
            if (itemCount >= count)
                return itemCount;
            return null;
        }, timeout);
        return result ?? -1;
    }
    emptyMainHand() {
        const mainHandItem = this.getMainHandItem();
        if (!mainHandItem || mainHandItem.isEmpty())
            return true;
        const freeSlot = this.findFreeSlot('main', 'hotbar');
        if (freeSlot === -1)
            return false;
        const shouldOpenContainer = !this.isContainer();
        if (shouldOpenContainer)
            Player.openInventory().openGui();
        this.swap(this.getMainHandSlot(), freeSlot);
        if (shouldOpenContainer)
            this.closeContainer();
        return true;
    }
    emptyContainer(slotIndexes) {
        if (!this.isContainer())
            return false;
        return this.getSlots('container').every((slot, i) => {
            if (!slotIndexes?.includes(i) || this.getSlot(slot).isEmpty())
                return true;
            if (this.findFreeSlot('main', 'hotbar') === -1)
                return false;
            this.quickSlot(slot);
            return this.getSlot(slot).isEmpty();
        });
    }
    clickSlot(slot, mouseButton = MouseButton.LEFT) {
        return Player.openInventory().click(slot, mouseButton);
    }
    quickSlot(slot) {
        return Player.openInventory().quick(slot);
    }
    checkMove(slot, targetSlot) {
        const [source, target] = [slot, targetSlot].map((slot) => this.getSlot(slot));
        if (source.isEmpty())
            return false;
        if (!target.isEmpty() && target.id !== source.id)
            return false;
        return true;
    }
    moveCount(slot, targetSlot, count) {
        if (!this.checkMove(slot, targetSlot))
            return 0;
        const source = this.getSlot(slot);
        let sourceCount = source.count;
        let movedTotal = 0;
        let remaining = count;
        let retry = 0;
        const maxAttempts = AppConfig.crafting.maxAttempts;
        while (toggle.check() && remaining > 0 && sourceCount > 0 && retry <= maxAttempts) {
            let moved = 0;
            if (sourceCount <= remaining) {
                moved = this.moveAll(slot, targetSlot);
            }
            else if (remaining >= Math.ceil(sourceCount / 2)) {
                moved = this.moveHalf(slot, targetSlot);
            }
            else {
                moved = this.moveMulti(slot, targetSlot, remaining);
            }
            Time.sleep(AppConfig.timeout.delayAfterMove);
            if (moved <= 0) {
                retry++;
                continue;
            }
            sourceCount -= moved;
            remaining -= moved;
            movedTotal += moved;
            retry = 0;
        }
        if (retry >= maxAttempts)
            Logger$1.error('拿取物品失败，重试次数过多');
        return movedTotal;
    }
    moveMulti(slot, targetSlot, moveNum = 1) {
        if (!this.checkMove(slot, targetSlot))
            return 0;
        const openInv = Player.openInventory();
        openInv.click(slot, MouseButton.RIGHT);
        let movedCount = openInv.getHeld().getCount();
        while (toggle.check() && moveNum-- > 0)
            openInv.click(targetSlot, MouseButton.RIGHT);
        if (!openInv.getHeld().isEmpty()) {
            movedCount -= openInv.getHeld().getCount();
            openInv.click(slot, MouseButton.LEFT);
        }
        return movedCount;
    }
    moveHalf(slot, targetSlot) {
        if (!this.checkMove(slot, targetSlot))
            return 0;
        const openInv = Player.openInventory();
        openInv.click(slot, MouseButton.RIGHT);
        let movedCount = openInv.getHeld().getCount();
        openInv.click(targetSlot, MouseButton.LEFT);
        if (!openInv.getHeld().isEmpty()) {
            movedCount -= openInv.getHeld().getCount();
            openInv.click(slot, MouseButton.LEFT);
        }
        return movedCount;
    }
    moveAll(slot, targetSlot) {
        if (!this.checkMove(slot, targetSlot))
            return 0;
        const openInv = Player.openInventory();
        openInv.click(slot, MouseButton.LEFT);
        let movedCount = openInv.getHeld().getCount();
        openInv.click(targetSlot, MouseButton.LEFT);
        if (!openInv.getHeld().isEmpty()) {
            movedCount -= openInv.getHeld().getCount();
            openInv.click(slot, MouseButton.LEFT);
        }
        return movedCount;
    }
    moveItemToSlot(itemId, targetSlot, count) {
        const items = this.getItemsBySfItemId(['main', 'hotbar'], itemId);
        let movedCount = 0;
        if (items.length === 0)
            return 0;
        for (const item of items) {
            movedCount += this.moveCount(item.slot, targetSlot, count - movedCount);
            if (movedCount >= count)
                break;
        }
        return movedCount;
    }
    waitForItemInInventory(itemId, timeout = AppConfig.timeout.timeoutCheckCompletion) {
        return this.waitForItemInSlots(this.getSlots('container'), itemId, 1, timeout) > 0;
    }
    getItemsBySfItemId(mapIds, ...itemIds) {
        const items = this.getItems(...mapIds);
        return items.filter((item) => itemIds.includes(item.id));
    }
    getNotFullSlot(mapIds, itemId) {
        const items = this.getItemsBySfItemId(mapIds, itemId);
        return items.filter((item) => item.count < item.maxCount);
    }
    getFreeInventorySlot(itemId, mapIds) {
        const items = this.getNotFullSlot(mapIds, itemId);
        if (items.length > 0) {
            return {
                slot: items[0].slot,
                num: items[0].maxCount - items[0].count,
                there: true,
                maxCount: items[0].maxCount,
            };
        }
        const freeSlot = this.findFreeSlot('main', 'hotbar');
        if (freeSlot !== -1) {
            return {
                slot: freeSlot,
                num: AppConfig.game.maxItemStackSize,
                there: false,
                maxCount: AppConfig.game.maxItemStackSize,
            };
        }
        return { slot: -1, num: 0, there: false, maxCount: 0 };
    }
    getItemId(itemStack) {
        if (itemStack.isEmpty())
            return 'minecraft:air';
        const itemNbt = itemStack.getNBT();
        if (!itemNbt || !itemNbt.isCompound())
            return itemStack.getItemId().toString();
        const nbtPaths = [
            'components.minecraft:custom_data.PublicBukkitValues',
            'minecraft:custom_data.PublicBukkitValues',
            'PublicBukkitValues',
        ];
        for (const path of nbtPaths) {
            const firstPath = path.split('.')[0];
            if (itemNbt.has(firstPath)) {
                const nbtTemp = itemNbt.resolve(path)?.shift()?.asCompoundHelper();
                if (nbtTemp) {
                    if (nbtTemp.has('slimefun:slimefun_guide_mode'))
                        return SlimefunHelper$1.GUIDE_ID;
                    const nbtText = nbtTemp.get('slimefun:slimefun_item');
                    if (nbtText)
                        return SlimefunHelper$1.normalize(nbtText.asString());
                }
            }
        }
        return itemStack.getItemId().toString();
    }
    getItemCount(item, id, slot) {
        if (!this.isContainer())
            return item.getCount();
        const containerTitle = this.getContainerTitle();
        const isGridContainer = AppConfig.storage.gridContainerTitles.includes(containerTitle);
        if (!isGridContainer)
            return item.getCount();
        let gridSlots = this.getSlots('container');
        if (!gridSlots.includes(slot))
            return item.getCount();
        if (containerTitle === '网格' || containerTitle === 'Network Grid')
            gridSlots = gridSlots.filter((_, i) => (i + 1) % 9 !== 0);
        const lore = item.getLore();
        if (!lore.isEmpty()) {
            if (SlimefunHelper$1.matchId(id, 'minecraft:light_gray_stained_glass_pane') &&
                lore.every((l) => l.getString().trim() === ''))
                return 0;
            const loreJoin = lore.join('');
            let match = loreJoin.match(/数量: ([\d,]+)"/);
            if (!match)
                match = loreJoin.match(/Amount: ([\d,]+)"/);
            const realCount = match ? parseInt(match[1].replace(/,/g, ''), 10) : null;
            if (typeof realCount === 'number')
                return realCount;
        }
        return item.getCount();
    }
}
var InventoryHelper$1 = new InventoryHelper();
var MouseButton;
(function (MouseButton) {
    MouseButton[MouseButton["LEFT"] = 0] = "LEFT";
    MouseButton[MouseButton["RIGHT"] = 1] = "RIGHT";
    MouseButton[MouseButton["MIDDLE"] = 2] = "MIDDLE";
})(MouseButton || (MouseButton = {}));class WorldHelper {
    scan(blockId, blockState = []) {
        let scanner = World.getWorldScanner();
        if (blockId)
            scanner = scanner.withStringBlockFilter().contains(blockId);
        if (blockState.length)
            scanner = scanner.withStringStateFilter().contains(...blockState);
        return scanner.build();
    }
    scanSphere(blockId, blockState, pos, radius) {
        if (!pos)
            pos = PlayerHelper$1.eyePos();
        if (!radius)
            radius = PlayerHelper$1.reach() + 2;
        return Java.from(this.scan(blockId, blockState).scanSphereArea(...PositionHelper$1.P2A(pos), radius));
    }
}
var WorldHelper$1 = new WorldHelper();class WorkstationRepository extends BaseRepository {
    constructor() {
        super(AppConfig.file.workstationFile, []);
        this.singleWorkstations = new Map();
        this.multiWorkstations = new Map();
        this.scannedWorkstations = [];
        this.occupiedPositions = new Set();
        this.lastScanPos = null;
        this.getRepositoryName = () => '工作站数据';
        this.getKey = (item) => item.info.id;
        this.getWorkstation = (workstationId) => {
            return this.scannedWorkstations
                .sort((a, b) => b.config.info.priority - a.config.info.priority)
                .find((w) => SlimefunHelper$1.matchId(workstationId, ...[w.config.info.id, ...(w.config.info.supported ?? [])]));
        };
        this.getWorkstationByName = (workstationName) => {
            return this.scannedWorkstations.find((w) => w.config.info.name === workstationName);
        };
        this.findMultiBlockPositions = (multi) => {
            const layers = multi.structure.layers;
            if (!layers || layers.length === 0)
                return Err('结构为空');
            const directions = this.getSearchDirections(layers);
            const input = multi.input;
            if (input.relativePos) {
                const offsetX = 0 - input.relativePos.x;
                const offsetY = 0 - input.relativePos.y;
                const inputId = multi.structure.layers[input.relativePos.y][input.relativePos.x];
                const positions = WorldHelper$1.scanSphere(inputId, input.facing ? [`facing=${input.facing}`] : []);
                for (const pos of positions) {
                    for (const direction of directions) {
                        const basePos = this.calculateComponentPosition(PositionHelper$1.P2A(pos), direction, offsetX, offsetY);
                        const structureValidationResult = this.validateFullStructure(basePos, direction, multi);
                        if (structureValidationResult.success) {
                            return Ok(structureValidationResult.data);
                        }
                    }
                }
            }
            return Err('未找到匹配结构');
        };
    }
    loadData(config) {
        super.clear();
        this.singleWorkstations.clear();
        this.multiWorkstations.clear();
        config.forEach((w) => this.addWorkstation(w));
    }
    logInitialization() {
        Logger$1.info(`✓ ${this.getRepositoryName()}已加载: 单方块 ${this.singleWorkstations.size} 个, 多方块 ${this.multiWorkstations.size} 个`);
    }
    addWorkstation(workstation) {
        this.data.set(workstation.info.id, workstation);
        if (this.isSingleBlockConfig(workstation)) {
            this.singleWorkstations.set(workstation.info.id, workstation);
        }
        else if (this.isMultiBlockConfig(workstation)) {
            this.multiWorkstations.set(workstation.info.id, workstation);
        }
    }
    isSingleBlockConfig(config) {
        return config.structure.type === 'single';
    }
    isMultiBlockConfig(config) {
        return config.structure.type === 'multi';
    }
    scanWorkstationAround() {
        if (this.lastScanPos) {
            const isSamePos = PositionHelper$1.arePositionsEqual(this.lastScanPos, PlayerHelper$1.eyePos());
            if (isSamePos) {
                return;
            }
        }
        this.scannedWorkstations = [];
        this.occupiedPositions.clear();
        this.initialize();
        if (this.multiWorkstations.size === 0 && this.singleWorkstations.size === 0) {
            Logger$1.warn('未配置工作站结构, 请在配置文件中添加配置');
            return;
        }
        Logger$1.info('开始扫描工作站结构...');
        for (const multi of this.multiWorkstations.values()) {
            const multiBlockResult = this.findMultiBlockPositions(multi);
            if (multiBlockResult.success) {
                this.scannedWorkstations.push(multiBlockResult.data);
                Array.from(multiBlockResult.data.occupied ?? []).map((v) => {
                    this.occupiedPositions.add(v);
                });
                Logger$1.info(`识别成功: §6${multi.info.name}§r`);
            }
            continue;
        }
        for (const single of this.singleWorkstations.values()) {
            const positions = WorldHelper$1.scanSphere(single.structure.block);
            if (positions.length > 10) {
                Logger$1.warn(`尝试识别 ${positions.length} 个 ${single.info.name}(${single.structure.block})`);
                Logger$1.warn('已跳过该工作站识别，请检查周围方块，拆除不属于该工作站的方块后重试。');
                continue;
            }
            for (const pos of positions) {
                if (!toggle.check())
                    return;
                if (this.occupiedPositions.has(PositionHelper$1.P2A(pos).join(',')))
                    continue;
                if (InventoryHelper$1.openContainerForBlock(pos)) {
                    if (InventoryHelper$1.getContainerTitle() === single.info.name) {
                        this.scannedWorkstations.push({
                            config: single,
                            mainPos: PositionHelper$1.P2A(pos),
                            inputPos: PositionHelper$1.P2A(pos),
                            outputPos: PositionHelper$1.P2A(pos),
                            interactPos: PositionHelper$1.P2A(pos),
                            occupied: new Set().add(PositionHelper$1.P2A(pos).join(',')),
                        });
                        this.occupiedPositions.add(PositionHelper$1.P2A(pos).join(','));
                        Logger$1.info(`识别成功: §5${single.info.name}§r`);
                        break;
                    }
                }
            }
        }
        this.lastScanPos = PositionHelper$1.P2A(PlayerHelper$1.eyePos());
        Logger$1.info(`✓ 已扫描到 ${this.scannedWorkstations.length} 个工作站`);
    }
    validateBlock(worldPos, expectedBlockId) {
        if (!expectedBlockId)
            return true;
        const worldBlock = World.getBlock(...worldPos);
        if (!worldBlock)
            return false;
        const foundId = worldBlock.getId();
        return (SlimefunHelper$1.matchId(expectedBlockId, foundId) ||
            (SlimefunHelper$1.matchId(expectedBlockId, 'minecraft:flint_and_steel') &&
                SlimefunHelper$1.matchId(foundId, 'minecraft:fire')));
    }
    validateFullStructure(basePos, direction, config) {
        if (config.structure.type !== 'multi') {
            return Err(`工作站 ${config.info.id} 不是多方块工作站`);
        }
        if (!config.structure.layers) {
            return Err(`工作站 ${config.info.id} 没有识别结构`);
        }
        const data = {
            config,
            mainPos: [0, 0, 0],
            inputPos: [0, 0, 0],
            outputPos: [0, 0, 0],
            interactPos: [0, 0, 0],
            occupied: new Set(),
        };
        const structure = config.structure.layers;
        const valid = structure.every((layer, layerY) => layer.every((blockId, layerX) => {
            if (isEmptyEx(blockId))
                return true;
            const worldPos = this.calculateComponentPosition(basePos, direction, layerX, layerY);
            if (!this.validateBlock(worldPos, blockId))
                return false;
            if (config.input.relativePos?.x === layerX && config.input.relativePos?.y === layerY) {
                data.mainPos = PositionHelper$1.P2A(worldPos);
                data.inputPos = PositionHelper$1.P2A(worldPos);
            }
            if (config.output.relativePos?.x === layerX && config.output.relativePos?.y === layerY) {
                data.outputPos = PositionHelper$1.P2A(worldPos);
            }
            if (config.craft.type === 'interact' &&
                config.craft.relativePos.x === layerX &&
                config.craft.relativePos.y === layerY) {
                data.interactPos = PositionHelper$1.P2A(worldPos);
            }
            data.occupied.add(worldPos.join(','));
            return true;
        }));
        if (!valid)
            return Err('结构验证失败');
        return Ok(data);
    }
    calculateComponentPosition(initialPos, direction, offsetX, offsetY) {
        return PositionHelper$1.P2A(PositionCommon.createBlockPos(...initialPos)
            .offset(direction, offsetX)
            .down(offsetY));
    }
    getSearchDirections(layers) {
        const maxColumns = Math.max(...layers.map((layer) => layer.length));
        if (maxColumns === 1)
            return ['north'];
        const hasSymmetricStructure = layers.every((layer) => {
            const halfColumns = Math.floor(maxColumns / 2);
            for (let i = 0; i < halfColumns; i++) {
                if (layer[i] !== layer[maxColumns - i - 1])
                    return false;
            }
            return true;
        });
        return hasSymmetricStructure ? ['north', 'east'] : ['north', 'south', 'east', 'west'];
    }
}
var WorkstationRepository$1 = new WorkstationRepository();class EventHelper {
    constructor() {
        this.registry = new Map();
    }
    on(event, cb) {
        const eventListener = JsMacros.on(event, JavaWrapper.methodToJava(cb));
        const listeners = this.registry.get(event) ?? [];
        this.registry.set(event, [...listeners, eventListener]);
        return eventListener;
    }
    once(event, listener) {
        return JsMacros.once(event, JavaWrapper.methodToJavaAsync(listener));
    }
    off(event) {
        if (event) {
            const listeners = this.registry.get(event);
            listeners?.forEach((listener) => JsMacros.off(listener));
            this.registry.delete(event);
        }
        else {
            for (const [, listeners] of this.registry) {
                listeners.forEach((listener) => JsMacros.off(listener));
            }
            this.registry.clear();
        }
    }
}
var EventHelper$1 = new EventHelper();class ItemContainer {
    constructor() {
        this.items = [];
        this.indexMap = new Map();
    }
    rebuildIndex(startIndex) {
        for (const [id, idx] of this.indexMap) {
            if (idx >= startIndex) {
                this.indexMap.delete(id);
            }
        }
        for (let i = startIndex; i < this.items.length; i++) {
            const item = this.items[i];
            if (item.count > 0) {
                this.indexMap.set(item.id, i);
            }
        }
    }
    add(id, name, count, extra) {
        if (count <= 0)
            return this;
        const index = this.indexMap.get(id);
        if (index !== undefined) {
            this.items[index].count += count;
            if (extra !== undefined) {
                this.items[index].extra = extra;
            }
            return this;
        }
        this.items.push({ id, name, count, extra });
        this.indexMap.set(id, this.items.length - 1);
        return this;
    }
    removeAt(index) {
        const entry = this.items[index];
        this.items.splice(index, 1);
        this.indexMap.delete(entry.id);
        this.rebuildIndex(index);
    }
    removeCount(index, count) {
        const entry = this.items[index];
        entry.count -= count;
        if (entry.count <= 0) {
            this.removeAt(index);
        }
    }
    remove(id, count) {
        if (count <= 0)
            return this;
        const index = this.indexMap.get(id);
        if (index === undefined)
            return this;
        this.removeCount(index, count);
        return this;
    }
    *[Symbol.iterator]() {
        for (const entry of this.items) {
            yield entry;
        }
    }
    *reverse() {
        for (let i = this.items.length - 1; i >= 0; i--) {
            yield this.items[i];
        }
    }
    merge(other, subtract = false) {
        if (other instanceof ItemContainer) {
            for (const entry of other.items) {
                if (subtract) {
                    this.remove(entry.id, entry.count);
                }
                else {
                    this.add(entry.id, entry.name, entry.count, entry.extra);
                }
            }
        }
        return this;
    }
    get(id) {
        const index = this.indexMap.get(id);
        return index !== undefined ? this.items[index] : undefined;
    }
    getCount(id) {
        return this.get(id)?.count ?? 0;
    }
    has(id) {
        return this.indexMap.has(id);
    }
    get isEmpty() {
        return this.items.length === 0;
    }
    get length() {
        return this.items.filter((item) => item.count > 0).length;
    }
    size() {
        return this.length;
    }
    isEmptyOrZero() {
        return this.items.every((item) => item.count <= 0);
    }
    clear() {
        this.items = [];
        this.indexMap.clear();
    }
    all() {
        return Array.from(this);
    }
    toString() {
        return Array.from(this)
            .map((entry) => `${entry.name} x${entry.count}`)
            .join('，');
    }
    clone() {
        const newContainer = new ItemContainer();
        for (const entry of this.items) {
            newContainer.add(entry.id, entry.name, entry.count, entry.extra ? JSON.parse(JSON.stringify(entry.extra)) : undefined);
        }
        return newContainer;
    }
}var CraftingState;
(function (CraftingState) {
    CraftingState["IDLE"] = "IDLE";
    CraftingState["CALCULATING"] = "CALCULATING";
    CraftingState["CRAFTING"] = "CRAFTING";
    CraftingState["REFILLING"] = "REFILLING";
})(CraftingState || (CraftingState = {}));
class CraftingStateManager {
    constructor() {
        this.state = CraftingState.IDLE;
        this.listeners = new Set();
    }
    getState() {
        return this.state;
    }
    setState(newState) {
        if (this.state !== newState) {
            const oldState = this.state;
            this.state = newState;
            this.notifyListeners(oldState, newState);
        }
    }
    isIdle() {
        return this.state === CraftingState.IDLE;
    }
    isBusy() {
        return this.state !== CraftingState.IDLE;
    }
    isCalculating() {
        return this.state === CraftingState.CALCULATING;
    }
    isCrafting() {
        return this.state === CraftingState.CRAFTING;
    }
    isRefilling() {
        return this.state === CraftingState.REFILLING;
    }
    reset() {
        this.setState(CraftingState.IDLE);
    }
    addListener(listener) {
        this.listeners.add(listener);
    }
    removeListener(listener) {
        this.listeners.delete(listener);
    }
    clearListeners() {
        this.listeners.clear();
    }
    notifyListeners(oldState, newState) {
        this.listeners.forEach((listener) => {
            try {
                listener(oldState, newState);
            }
            catch (error) {
                Logger$1.error('状态监听器执行失败:', error);
            }
        });
    }
    getStateDescription() {
        switch (this.state) {
            case CraftingState.IDLE:
                return '空闲';
            case CraftingState.CALCULATING:
                return '计算中';
            case CraftingState.CRAFTING:
                return '合成中';
            case CraftingState.REFILLING:
                return '补货中';
            default:
                return '未知状态';
        }
    }
}
var CraftingStateManager$1 = new CraftingStateManager();class GuideButtonManager {
    constructor() {
        this._event = null;
        this._elements = new Map();
        this._processing = false;
        this.BUTTON_WIDTH = 60;
        this.BUTTON_HEIGHT = 20;
        this.startX = 0;
        this.startY = 0;
        this.initButtonPosition = (screen, inventory) => {
            if (inventory.is('3 Row Chest')) {
                this.startY = Math.floor(screen.getHeight() / 2) - 83;
            }
            else {
                this.startY = Math.floor(screen.getHeight() / 2) - 110;
            }
            this.startX = Math.floor(screen.getWidth() / 2) + 92;
        };
        this.buildButton = (screen, buttons) => {
            let offsetY = 0;
            buttons.sort((a, b) => (a.weight || 0) - (b.weight || 0));
            for (const button of buttons) {
                if (!button.showCondition || button.showCondition(screen)) {
                    screen
                        .buttonBuilder()
                        .message(button.text)
                        .pos(this.startX, this.startY + (this.BUTTON_HEIGHT + 5) * offsetY)
                        .size(this.BUTTON_WIDTH, this.BUTTON_HEIGHT)
                        .action(JavaWrapper.methodToJavaAsync(button.callback))
                        .build();
                    offsetY++;
                }
            }
        };
        this.addButtonsToScreen = (screenClassName, ...buttons) => {
            const currentButtons = this._elements.get(screenClassName) || [];
            currentButtons.push(...buttons);
            this._elements.set(screenClassName, currentButtons);
        };
    }
    initialize() {
        this._event = EventHelper$1.on('ContainerUpdate', (event) => {
            if (CraftingStateManager$1.isBusy())
                return;
            if (!event.screen || this._processing)
                return;
            if (this._elements.size === 0)
                return;
            this._processing = true;
            this.initButtonPosition(event.screen, event.inventory);
            const buttons = this._elements.get(event.screen.getClass().getSimpleName());
            if (buttons && buttons.length > 0)
                this.buildButton(event.screen, buttons);
            this._processing = false;
        });
    }
    clear() {
        this._event?.off();
        this._elements.clear();
    }
}
var GuideButtonManager$1 = new GuideButtonManager();class MaterialCalculator {
    constructor() {
        this.existingItems = new ItemContainer();
        this.extraItems = new ItemContainer();
        this.dependencyCache = new LRUCache(50);
        this.processing = new Set();
        this.visited = new Set();
    }
    calculateWithDP(needItems) {
        if (needItems.isEmptyOrZero()) {
            return Err('制作列表为空');
        }
        this.resetCalculationState();
        this.existingItems = this.getExistingItems();
        Logger$1.debug(`当前库存: ${this.existingItems.toString()}`);
        return this.performOptimizedCalculation(needItems);
    }
    performOptimizedCalculation(needItems) {
        try {
            this.resetCycleDetection();
            const cacheKey = this.generateDependencyCacheKey(needItems);
            let cachedDependency = this.dependencyCache.get(cacheKey);
            if (!cachedDependency) {
                const dependencyGraph = this.buildGlobalDependencyGraph(needItems);
                const synthesisOrder = this.topologicalSort(dependencyGraph);
                cachedDependency = { dependencyGraph, synthesisOrder };
                this.dependencyCache.put(cacheKey, cachedDependency);
            }
            const { synthesisOrder } = cachedDependency;
            const { requirements: materialRequirements, craftingNeeds } = this.calculateMaterialRequirements(synthesisOrder, needItems);
            Logger$1.debug(`材料需求: ${JSON.stringify(Array.from(materialRequirements.entries()))}`);
            Logger$1.debug(`合成需求: ${JSON.stringify(Array.from(craftingNeeds.entries()))}`);
            const optimizedMaterials = this.optimizeInventoryAllocation(materialRequirements);
            Logger$1.debug(`优化后的材料需求: ${optimizedMaterials.toString()}`);
            const synthesisQueue = this.generateOptimizedQueue(synthesisOrder, craftingNeeds);
            Logger$1.debug(`合成队列: ${synthesisQueue.toString()}`);
            return Ok({
                queue: synthesisQueue,
                missing: optimizedMaterials,
            });
        }
        catch (error) {
            const errorMsg = `材料计算失败: ${error instanceof Error ? error.message : String(error)}`;
            Logger$1.error(errorMsg);
            return Err(errorMsg);
        }
    }
    buildGlobalDependencyGraph(needItems) {
        const graph = new DependencyGraph();
        for (const { id } of needItems) {
            this.buildDependencyRecursive(id, graph, new Set());
        }
        return graph;
    }
    buildDependencyRecursive(itemId, graph, visited) {
        if (this.visited.has(itemId)) {
            return;
        }
        if (this.processing.has(itemId)) {
            const error = `检测到循环依赖: ${itemId}`;
            Logger$1.error(error);
            throw new Error(error);
        }
        this.visited.add(itemId);
        this.processing.add(itemId);
        const recipe = RecipeRepository$1.getRecipe(itemId);
        if (!recipe || BlacklistRepository$1.isBlacklist(itemId)) {
            graph.addNode(itemId, [], recipe?.name);
            this.processing.delete(itemId);
            return;
        }
        Logger$1.debug(`正在构建依赖关系: ${itemId}`);
        Logger$1.debug(`配方: ${JSON.stringify(recipe)}`);
        const dependencies = recipe.ingredients
            .map((ing) => ing.id)
            .filter((depId) => RecipeRepository$1.getRecipe(depId));
        const uniqueDependencies = Array.from(new Set(dependencies));
        graph.addNode(itemId, uniqueDependencies, recipe.name);
        for (const dep of dependencies) {
            this.buildDependencyRecursive(dep, graph, visited);
        }
        this.processing.delete(itemId);
    }
    topologicalSort(graph) {
        const inDegree = new Map();
        const reverseDeps = new Map();
        const queue = [];
        const result = [];
        for (const [node, deps] of graph.nodes) {
            inDegree.set(node, deps.length);
            if (!reverseDeps.has(node)) {
                reverseDeps.set(node, new Set());
            }
            for (const dep of deps) {
                if (!reverseDeps.has(dep)) {
                    reverseDeps.set(dep, new Set());
                }
                reverseDeps.get(dep).add(node);
            }
            if (deps.length === 0) {
                queue.push(node);
            }
        }
        while (queue.length > 0) {
            const current = queue.shift();
            result.push(current);
            const dependents = reverseDeps.get(current);
            if (dependents) {
                for (const dependent of dependents) {
                    const newDegree = inDegree.get(dependent) - 1;
                    inDegree.set(dependent, newDegree);
                    if (newDegree === 0) {
                        queue.push(dependent);
                    }
                }
            }
        }
        if (result.length !== graph.nodes.size) {
            const unprocessed = Array.from(graph.nodes.keys()).filter((node) => !result.includes(node));
            Logger$1.error(`检测到循环依赖: ${unprocessed.join(', ')}`);
            throw new Error(`检测到循环依赖，无法进行拓扑排序。未处理节点: ${unprocessed.join(', ')}`);
        }
        return result;
    }
    calculateMaterialRequirements(synthesisOrder, needItems) {
        const requirements = new Map();
        const craftingNeeds = new Map();
        const userRequestedItems = new Set(Array.from(needItems).map((item) => item.id));
        for (let i = synthesisOrder.length - 1; i >= 0; i--) {
            const itemId = synthesisOrder[i];
            const needed = needItems.getCount(itemId) || requirements.get(itemId)?.count || 0;
            if (needed <= 0)
                continue;
            const recipe = RecipeRepository$1.getRecipe(itemId);
            if (!recipe || BlacklistRepository$1.isBlacklist(recipe.id)) {
                this.processBasicMaterial(itemId, needed, requirements);
                continue;
            }
            const existing = this.getAvailableCount(itemId);
            const actualNeed = Math.max(0, needed - existing);
            const isUserRequested = userRequestedItems.has(itemId);
            if (actualNeed <= 0 && !isUserRequested) {
                continue;
            }
            const craftCount = isUserRequested ? needed : actualNeed;
            if (craftCount > 0) {
                craftingNeeds.set(itemId, craftCount);
                requirements.set(itemId, {
                    id: itemId,
                    name: recipe.name,
                    count: craftCount,
                    isCraftable: true,
                });
                const craftTimes = Math.ceil(craftCount / recipe.output);
                const totalOutput = craftTimes * recipe.output;
                const extraCount = totalOutput - craftCount;
                if (extraCount > 0) {
                    this.extraItems.add(recipe.id, recipe.name, extraCount);
                }
                for (const ingredient of recipe.ingredients) {
                    const ingredientNeed = (ingredient.count || 1) * craftTimes;
                    const existingReq = requirements.get(ingredient.id);
                    const newCount = (existingReq?.count || 0) + ingredientNeed;
                    requirements.set(ingredient.id, {
                        id: ingredient.id,
                        name: ingredient.name,
                        count: newCount,
                        isCraftable: !!RecipeRepository$1.getRecipe(ingredient.id),
                    });
                }
            }
        }
        return { requirements, craftingNeeds };
    }
    optimizeInventoryAllocation(requirements) {
        const missing = new ItemContainer();
        for (const [itemId, requirement] of requirements) {
            const recipe = RecipeRepository$1.getRecipe(itemId);
            const { name: itemName, count: required } = requirement;
            const isBasicMaterial = !recipe || BlacklistRepository$1.isBlacklist(recipe.id);
            const available = this.getAvailableCount(itemId);
            const actualMissing = Math.max(0, required - available);
            if (isBasicMaterial && actualMissing > 0) {
                missing.add(itemId, itemName, actualMissing);
            }
            const usedFromExisting = Math.min(available, required);
            this.existingItems.remove(itemId, usedFromExisting);
            this.extraItems.remove(itemId, Math.max(0, required - available));
        }
        return missing;
    }
    generateOptimizedQueue(synthesisOrder, craftingNeeds) {
        const queue = new ItemContainer();
        for (const itemId of synthesisOrder) {
            const recipe = RecipeRepository$1.getRecipe(itemId);
            if (!recipe || BlacklistRepository$1.isBlacklist(recipe.id)) {
                continue;
            }
            const needCount = craftingNeeds.get(itemId);
            if (needCount && needCount > 0) {
                queue.add(itemId, recipe.name, needCount, recipe);
            }
        }
        return queue;
    }
    getAvailableCount(itemId) {
        return this.existingItems.getCount(itemId) + this.extraItems.getCount(itemId);
    }
    addMaterialRequirement(requirements, itemId, itemName, count) {
        const existing = requirements.get(itemId);
        requirements.set(itemId, {
            id: itemId,
            name: itemName,
            count: (existing?.count || 0) + count,
            isCraftable: !!RecipeRepository$1.getRecipe(itemId),
        });
    }
    processBasicMaterial(itemId, needed, requirements) {
        const existing = this.getAvailableCount(itemId);
        const actualNeed = Math.max(0, needed - existing);
        if (actualNeed > 0) {
            const recipe = RecipeRepository$1.getRecipe(itemId);
            const itemName = recipe?.name || BlacklistRepository$1.get(itemId)?.name || itemId;
            this.addMaterialRequirement(requirements, itemId, itemName, actualNeed);
            requirements.get(itemId).isCraftable = false;
        }
    }
    resetCalculationState() {
        this.existingItems.clear();
        this.extraItems.clear();
    }
    resetCycleDetection() {
        this.processing.clear();
        this.visited.clear();
    }
    generateDependencyCacheKey(needItems) {
        const itemIds = Array.from(needItems)
            .map((item) => item.id)
            .sort()
            .join('|');
        return itemIds;
    }
    getExistingItems() {
        return InventoryHelper$1.getItems('main', 'hotbar').reduce((c, i) => c.add(i.id, i.name, i.count), new ItemContainer());
    }
    calculateRecipeMaterials(recipe, count) {
        const missingItems = new ItemContainer();
        const existingItems = this.getExistingItems();
        const craftTimes = Math.ceil(count / recipe.output);
        for (const { id: materialId, name: materialName, count: requirementCount, } of recipe.ingredients) {
            const needCount = (requirementCount ?? 1) * craftTimes;
            const existCount = existingItems.getCount(materialId);
            if (needCount > existCount) {
                missingItems.add(materialId, materialName, needCount - existCount);
            }
        }
        return missingItems;
    }
    clearCache() {
        this.dependencyCache.clear();
    }
}
class DependencyGraph {
    constructor() {
        this.nodes = new Map();
        this.names = new Map();
    }
    addNode(node, dependencies, name) {
        this.nodes.set(node, dependencies);
        if (name) {
            this.names.set(node, name);
        }
    }
    getDependencies(node) {
        return this.nodes.get(node) || [];
    }
    hasNode(node) {
        return this.nodes.has(node);
    }
}
class LRUCache {
    constructor(maxSize) {
        this.cache = new Map();
        this.maxSize = maxSize;
    }
    get(key) {
        const value = this.cache.get(key);
        if (value !== undefined) {
            this.cache.delete(key);
            this.cache.set(key, value);
        }
        return value;
    }
    put(key, value) {
        if (this.cache.has(key)) {
            this.cache.delete(key);
        }
        else if (this.cache.size >= this.maxSize) {
            const firstKey = this.cache.keys().next().value;
            if (firstKey !== undefined) {
                this.cache.delete(firstKey);
            }
        }
        this.cache.set(key, value);
    }
    clear() {
        this.cache.clear();
    }
    size() {
        return this.cache.size;
    }
}
var MaterialCalculator$1 = new MaterialCalculator();class SlimefunGuideManager {
    constructor() {
        this.slimefunGuideClassName = '';
        this.checkHoldSfGuide = (offHand) => {
            const item = offHand ? InventoryHelper$1.getOffHandItem() : InventoryHelper$1.getMainHandItem();
            return item && SlimefunHelper$1.matchStack(item, SlimefunHelper$1.GUIDE_ID);
        };
        this.Enter = {
            clickSlot: (slot, quick = false) => {
                const fn = quick ? InventoryHelper$1.quickSlot : InventoryHelper$1.clickSlot;
                return InventoryHelper$1.waitForInventoryChange(() => fn(slot));
            },
            gotoHomePage: () => this.Enter.clickSlotById(SlimefunHelper$1.MENU_IDS.Back, false, true),
            gotoPage: (page = 1) => {
                if (page < 1)
                    page = 1;
                this.Enter.clickSlotById(SlimefunHelper$1.MENU_IDS.PreviousActive, true);
                while (toggle.check() && --page > 0) {
                    this.Enter.clickSlotById(SlimefunHelper$1.MENU_IDS.NextActive);
                }
            },
            clickSlotById: (clickItemId, repeat = false, quick = false) => {
                Client.waitTick(2);
                const items = InventoryHelper$1.getItemsBySfItemId(['container'], clickItemId);
                if (items.length > 0) {
                    if (!this.Enter.clickSlot(items[0].slot, quick))
                        return false;
                    if (repeat)
                        return this.Enter.clickSlotById(clickItemId, repeat, quick);
                    return true;
                }
                return repeat;
            },
        };
        this.addButtons = () => {
            const buttons = [];
            buttons.push(this.readBtn());
            buttons.push(this.readCurrentBtn());
            buttons.push(this.blacklistBtn('add', '添加到黑名单'));
            buttons.push(this.blacklistBtn('remove', '从黑名单移除'));
            GuideButtonManager$1.addButtonsToScreen(this.slimefunGuideClassName, ...buttons);
        };
        this.check = () => {
            if (!this.isSlimefunGuideScreen()) {
                Logger$1.error('获取粘液配方失败, 请在Slimefun指南GUI中运行!');
                return false;
            }
            return true;
        };
        this.readBtn = () => {
            const text = '读取配方';
            const showCondition = () => this.isRecipeListScreen();
            const callback = (btn) => {
                if (!this.check())
                    return;
                btn.setActive(false);
                Logger$1.info('开始读取粘液配方...');
                const result = RecipeRepository$1.addRecipes(this.readItemsPage());
                if (result.success) {
                    MaterialCalculator$1.clearCache();
                    Logger$1.info('粘液配方获取成功!');
                }
                else {
                    Logger$1.error(`粘液配方保存失败: ${result.error}`);
                }
                btn.setActive(true);
            };
            return { text, showCondition, callback };
        };
        this.readCurrentBtn = () => {
            const text = '读取当前配方';
            const showCondition = () => {
                const res = this.getRecipeDetail();
                if (!res)
                    return false;
                return !BlacklistRepository$1.isBlacklist(res.outputItem.id);
            };
            const callback = (btn) => {
                if (!this.check())
                    return;
                btn.setActive(false);
                const newRecipe = this.readCraftingRecipe();
                if (newRecipe) {
                    const result = RecipeRepository$1.addRecipe(newRecipe);
                    if (result.success) {
                        Logger$1.info(`读取成功: ${newRecipe.name}`);
                        MaterialCalculator$1.clearCache();
                    }
                    else {
                        Logger$1.error(`当前配方保存失败: ${result.error}`);
                    }
                }
                else {
                    Logger$1.error('当前配方读取失败!');
                }
                btn.setActive(true);
            };
            return { text, showCondition, callback };
        };
        this.blacklistBtn = (operation, text) => {
            const showCondition = () => {
                const res = this.getRecipeDetail();
                if (!res)
                    return false;
                if (operation === 'add')
                    return !BlacklistRepository$1.isBlacklist(res.outputItem.id);
                else if (operation === 'remove')
                    return BlacklistRepository$1.isBlacklist(res.outputItem.id);
                else
                    return false;
            };
            const callback = (btn) => {
                if (!this.check())
                    return;
                if (!Hud.isContainer())
                    return;
                const res = this.getRecipeDetail();
                if (!res)
                    return;
                let result;
                if (operation === 'add')
                    result = BlacklistRepository$1.add(res.outputItem.id, res.outputItem.name);
                else if (operation === 'remove')
                    result = BlacklistRepository$1.remove(res.outputItem.id);
                if (!result?.success) {
                    Logger$1.error(`${text}: ${result?.error ?? '未知操作'}`);
                    return;
                }
                MaterialCalculator$1.clearCache();
                Logger$1.info(`${text}: 成功!`);
                btn.setActive(false);
            };
            return { text, showCondition, callback };
        };
        this.readCraftingRecipe = () => {
            const recipeDetail = this.getRecipeDetail();
            if (!recipeDetail)
                return;
            if (recipeDetail.workItem.isEmpty() || recipeDetail.outputItem.isEmpty())
                return;
            if (recipeDetail.workItem.name === '多方块结构' ||
                SlimefunHelper$1.matchStack(recipeDetail.workItem, 'MULTIBLOCK_ICON')) {
                Logger$1.warn(`[跳过]多方块结构: ${recipeDetail.outputItem.name}`);
                return;
            }
            const materials = [];
            for (let index = 0; index < recipeDetail.materialItems.length; index++) {
                const materialItem = recipeDetail.materialItems[index];
                if (materialItem.isEmpty())
                    continue;
                const materialId = materialItem.id;
                const materialName = materialItem.name;
                if (SlimefunHelper$1.matchStack(materialItem, SlimefunHelper$1.MENU_IDS.Barrier)) {
                    Logger$1.warn(`[跳过]材料未解锁: ${recipeDetail.outputItem.name} > ${materialName}`);
                    return;
                }
                materials.push({
                    id: materialId,
                    name: materialName,
                    count: materialItem.count,
                    slot: index,
                });
            }
            const craftingRecipe = {
                id: recipeDetail.outputItem.id,
                name: recipeDetail.outputItem.name,
                station: recipeDetail.workItem.id,
                stationName: recipeDetail.workItem.name,
                ingredients: materials,
                output: recipeDetail.outputItem.count,
            };
            return craftingRecipe;
        };
        this.readItemsPage = () => {
            const recipes = [];
            const itemSlots = InventoryHelper$1.getSlots('container').slice(9, -9);
            for (const slot of itemSlots) {
                Client.waitTick(2);
                const item = InventoryHelper$1.getSlot(slot);
                if (item.isEmpty())
                    continue;
                if (SlimefunHelper$1.matchId(item.id, SlimefunHelper$1.MENU_IDS.Barrier)) {
                    Logger$1.warn('[跳过]配方未解锁: ' + item.name);
                    continue;
                }
                if (BlacklistRepository$1.isBlacklist(item.id)) {
                    Logger$1.warn('[跳过]黑名单: ' + item.name);
                    continue;
                }
                this.Enter.clickSlot(slot);
                const craftingRecipe = this.readCraftingRecipe();
                if (craftingRecipe) {
                    recipes.push(craftingRecipe);
                    Logger$1.info(`读取成功: ${craftingRecipe.name}`);
                }
                this.Enter.clickSlotById(SlimefunHelper$1.MENU_IDS.Back);
            }
            if (this.Enter.clickSlotById(SlimefunHelper$1.MENU_IDS.NextActive)) {
                recipes.push(...this.readItemsPage());
            }
            return recipes;
        };
    }
    isSlimefunGuideScreen() {
        if (!this.slimefunGuideClassName)
            throw new Error('未初始化<粘液科技指南>界面, 请先调用init()初始化!');
        if (Hud.getOpenScreen()?.getClass().getSimpleName() !== this.slimefunGuideClassName)
            return false;
        if (!Hud.isContainer())
            return false;
        const index = InventoryHelper$1.getItems('container').findIndex((item) => {
            return SlimefunHelper$1.matchStack(item, ...[
                SlimefunHelper$1.MENU_IDS.Back,
                SlimefunHelper$1.MENU_IDS.Menu,
                SlimefunHelper$1.MENU_IDS.Search,
            ]);
        });
        if (index === -1)
            return false;
        return true;
    }
    isRecipeListScreen() {
        if (this.isSlimefunGuideScreen()) {
            const items = InventoryHelper$1.getItemsWithEmpty('container');
            if (items.length < 27)
                return false;
            if (SlimefunHelper$1.matchStack(items[1], SlimefunHelper$1.MENU_IDS.Back)) {
                if (SlimefunHelper$1.matchStack(items[7], SlimefunHelper$1.MENU_IDS.Search)) {
                    const title = InventoryHelper$1.getContainerTitle();
                    return title !== '逻辑工艺';
                }
                return items[7].id.endsWith('_ICON');
            }
        }
        return false;
    }
    getRecipeDetail() {
        if (!this.isSlimefunGuideScreen())
            return null;
        const title = InventoryHelper$1.getContainerTitle() || '';
        const items = InventoryHelper$1.getItemsWithEmpty('container');
        if (items.length === 0)
            return null;
        const config = AppConfig.guide.recipeDisplayConfig
            .sort((a, b) => b.minSlotCount - a.minSlotCount)
            .find((c) => {
            if (items.length < c.minSlotCount)
                return false;
            if (!items[c.workSlot] || items[c.workSlot].isEmpty())
                return false;
            if (!items[c.outputSlot] ||
                items[c.outputSlot].isEmpty() ||
                !SlimefunHelper$1.isSlimefun(items[c.outputSlot]))
                return false;
            if (c.extraValid?.slots) {
                const extraValidSlots = Object.entries(c.extraValid.slots).every(([slotStr, expectedId]) => {
                    if (slotStr.startsWith('OR')) {
                        const slots = slotStr.split('|').slice(1).map(Number);
                        return slots.some((slot) => SlimefunHelper$1.matchId(items[slot].id, expectedId));
                    }
                    else if (slotStr.startsWith('AND')) {
                        const slots = slotStr.split('|').slice(1).map(Number);
                        return slots.every((slot) => SlimefunHelper$1.matchId(items[slot].id, expectedId));
                    }
                    else {
                        const slot = Number(slotStr);
                        return SlimefunHelper$1.matchId(items[slot].id, expectedId);
                    }
                });
                if (!extraValidSlots)
                    return false;
            }
            if (c.extraValid?.workIds) {
                const validWorkstation = c.extraValid.workIds.some((id) => SlimefunHelper$1.matchStack(items[c.workSlot], id));
                if (!validWorkstation)
                    return false;
            }
            const outputItem = items[c.outputSlot];
            if (c.extraValid?.isOutputItemIsTitle) {
                if (title !== outputItem.name) {
                    return false;
                }
            }
            return true;
        });
        if (!config)
            return null;
        const workItem = items[config.workSlot];
        const outputItem = items[config.outputSlot];
        const materialItems = config.materialSlots.map((slot) => items[slot]);
        if (title === '配方展示') {
            return {
                type: config.type,
                workItem,
                materialItems,
                outputItem,
                workSlot: config.workSlot,
                materialSlots: config.materialSlots,
                outputSlot: config.outputSlot,
            };
        }
        if (!workItem || workItem.isEmpty())
            return null;
        if (!outputItem || outputItem.isEmpty())
            return null;
        if (!SlimefunHelper$1.isSlimefun(outputItem))
            return null;
        return {
            type: config.type,
            workItem,
            materialItems,
            outputItem,
            workSlot: config.workSlot,
            materialSlots: config.materialSlots,
            outputSlot: config.outputSlot,
        };
    }
    initialize(offHand = false) {
        if (!this.checkHoldSfGuide(offHand)) {
            throw new Error(`需要${offHand ? '副手' : '主手'}持有<粘液科技指南>`);
        }
        if (!InventoryHelper$1.openContainerForItem(offHand)) {
            throw new Error('打开<粘液科技指南>界面失败, 等待超时或其他原因!');
        }
        const screen = Hud.getOpenScreen();
        if (!screen) {
            throw new Error('初始化<粘液科技指南>失败, 未能自动打开界面!');
        }
        this.slimefunGuideClassName = screen.getClass().getSimpleName();
        InventoryHelper$1.closeContainer();
        return true;
    }
}
var SlimefunGuideManager$1 = new SlimefunGuideManager();class HudManager {
    constructor() {
        this.register = () => {
            this._d2d.setOnInit(JavaWrapper.methodToJava(this.initHud)).register();
        };
        this.unregister = () => {
            this._d2d.unregister();
        };
        this.initHud = () => {
            const height = this._d2d.getHeight();
            const [top, middle, bottom] = [
                AppConfig.ui.topPercent,
                AppConfig.ui.middlePercent,
                AppConfig.ui.bottomPercent,
            ].map((n) => Math.round(height * (n / 100)));
            const [margin, colWidth, lineHeight] = [
                AppConfig.ui.margin,
                AppConfig.ui.columnWidth,
                AppConfig.ui.lineHeight,
            ];
            this.drawText(`脚本名称：${AppConfig.script.name} ${AppConfig.script.version}`, AppConfig.ui.leftMargin, top - lineHeight - margin, AppConfig.ui.colors.brand);
            this.drawText(`作者：${AppConfig.script.author}`, AppConfig.ui.leftMargin, top - lineHeight * 2 - margin, AppConfig.ui.colors.brand);
            this.draw(this._make, '制作列表', top, middle - margin, AppConfig.ui.leftMargin, lineHeight);
            this.draw(this._missing, '材料列表', middle, bottom - margin, AppConfig.ui.leftMargin, lineHeight);
            this.draw(this._queue, '合成队列: (0)', top, bottom - margin, margin + colWidth + AppConfig.ui.leftMargin, lineHeight);
        };
        this.draw = (o, title, startY, endY, startX, lineHeight) => {
            for (let y = startY; y <= endY; y += lineHeight) {
                if (y === startY) {
                    o.title = this.drawText(title, startX, y, AppConfig.ui.colors.title);
                }
                else {
                    const text = this.drawText('', startX, y, AppConfig.ui.colors.content);
                    if (text)
                        o.list.push(text);
                }
            }
        };
        this.updateMakeHud = (list) => {
            EventHelper$1.once('Tick', () => {
                this._make.list.forEach((t) => t.setText(''));
                if (list.isEmptyOrZero()) {
                    return;
                }
                let i = 0;
                const len = this._make.list.length;
                for (const item of list) {
                    this._make.list[i].setText(`§l${item.name} => ${item.count}`);
                    if (++i === len)
                        break;
                }
            });
        };
        this.updateMissingHud = (list) => {
            EventHelper$1.once('Tick', () => {
                this._missing.list.forEach((v) => v.setText(''));
                if (list.isEmptyOrZero()) {
                    return;
                }
                let i = 0;
                const len = this._missing.list.length;
                for (const { name, count } of list) {
                    if (count < AppConfig.game.maxItemStackSize) {
                        this._missing.list[i].setText(`§l${name} => ${count}`);
                    }
                    else {
                        let numStr = `${Math.floor(count / AppConfig.game.maxItemStackSize)} * ${AppConfig.game.maxItemStackSize}`;
                        if (count % AppConfig.game.maxItemStackSize !== 0)
                            numStr += ` + ${count % AppConfig.game.maxItemStackSize}`;
                        this._missing.list[i].setText(`§l${name} => ${numStr}`);
                    }
                    if (++i === len - 1 && list.length > len) {
                        const remainNum = list.length - len + 1;
                        this._missing.list[i].setText(`§l... ... => ${remainNum}`);
                        break;
                    }
                }
            });
        };
        this.updateQueueHud = (list) => {
            EventHelper$1.once('Tick', () => {
                this._queue.list.forEach((v) => v.setText(''));
                this._queue.title.setText(`§l合成队列: (${list.length})`);
                if (list.isEmptyOrZero()) {
                    return;
                }
                let i = 0;
                const len = this._queue.list.length;
                for (const { name, count } of list) {
                    this._queue.list[i].setText(`§l${name} => ${count}`);
                    if (++i === len - 1 && list.length > len) {
                        const remainNum = list.length - len + 1;
                        this._queue.list[i].setText(`§l... ... => ${remainNum}`);
                        break;
                    }
                }
            });
        };
        this._d2d = Hud.createDraw2D();
        this._make = { title: undefined, list: [] };
        this._missing = { title: undefined, list: [] };
        this._queue = { title: undefined, list: [] };
    }
    drawText(label, x, y, color = AppConfig.ui.colors.content) {
        return this._d2d?.addText(`§l${label}`, x, y, color, true);
    }
}
var HudManager$1 = new HudManager();class Debounce {
    constructor(defaultWaitTicks) {
        this.pendingFn = null;
        this.waitTicksRemaining = 0;
        this.defaultWaitTicks = defaultWaitTicks;
    }
    isEmpty() {
        return this.pendingFn === null;
    }
    debounce(fn, waitTicks = this.defaultWaitTicks) {
        this.pendingFn = fn;
        this.waitTicksRemaining = waitTicks;
    }
    processTick() {
        if (this.pendingFn && this.waitTicksRemaining > 0) {
            this.waitTicksRemaining--;
            if (this.waitTicksRemaining === 0) {
                this.pendingFn();
                this.pendingFn = null;
            }
        }
    }
    flush() {
        if (this.pendingFn) {
            this.pendingFn();
            this.pendingFn = null;
            this.waitTicksRemaining = 0;
        }
    }
    clear() {
        this.pendingFn = null;
        this.waitTicksRemaining = 0;
    }
}class CraftingExecutor {
    constructor() {
        this.maxStackSizeCache = new Map();
    }
    emptyMainHand() {
        if (InventoryHelper$1.emptyMainHand())
            return true;
        Logger$1.error('[中断]背包空间不足');
        return false;
    }
    craftRecipe(recipe, count) {
        let successCount = 0;
        this.maxStackSizeCache.clear();
        const workstation = this.getWorkstation(recipe);
        if (!workstation)
            return successCount;
        if (!this.openWorkstation(workstation))
            return successCount;
        while (toggle.check()) {
            if (!this.emptyMainHand())
                break;
            if (count <= 0)
                break;
            const craftedCount = this.craftItem(recipe, workstation, count);
            if (!craftedCount.success) {
                Logger$1.error(`[中断]合成失败: ${craftedCount.error}`);
                break;
            }
            if (craftedCount.data <= 0) {
                Logger$1.error('[中断]本轮合成没有产出，已停止以避免重复空转');
                break;
            }
            successCount += craftedCount.data;
            count -= craftedCount.data;
            Time.sleep(AppConfig.timeout.delayAfterCompletion);
        }
        return successCount;
    }
    getWorkstation(recipe) {
        let workstation = WorkstationRepository$1.getWorkstation(recipe.station);
        if (workstation)
            return workstation;
        workstation = WorkstationRepository$1.getWorkstationByName(recipe.stationName);
        if (workstation)
            return workstation;
        Logger$1.error(`[中断]未找到工作站: ${recipe.stationName}`);
        Logger$1.error(`超出交互距离/未正确搭建/暂不支持`);
        return null;
    }
    openWorkstation(workstation) {
        if (!this.emptyMainHand())
            return false;
        const pos = workstation.mainPos;
        if (!InventoryHelper$1.openContainerForBlock(pos)) {
            Logger$1.error(`[中断]工作站未正确打开: ${workstation.config.info.name}, 位置: ${pos.join(',')}`);
            return false;
        }
        const slots = [];
        slots.push(...workstation.config.input.slots, ...workstation.config.output.slots);
        if (!InventoryHelper$1.emptyContainer(slots)) {
            Logger$1.error('[中断]无法清空工作站，请检查背包空间');
            return false;
        }
        return true;
    }
    craftItem(recipe, workstation, count) {
        const slots = workstation.config.input.slots;
        const putResult = this.putInputItems(recipe, workstation, count);
        if (!putResult.success) {
            if (!InventoryHelper$1.emptyContainer(slots))
                return Err(`${putResult.error}；且无法清空工作站`);
            return Err(putResult.error);
        }
        const productSelectionResult = this.handleProductSelection(recipe, workstation);
        if (!productSelectionResult.success) {
            if (!InventoryHelper$1.emptyContainer(slots))
                return Err(`${productSelectionResult.error}；且无法清空工作站`);
            return Err(productSelectionResult.error);
        }
        const triggerResult = this.triggerCrafting(workstation, recipe, putResult.data);
        if (!triggerResult.success) {
            return Err(triggerResult.error);
        }
        return Ok(triggerResult.data);
    }
    putInputItems(recipe, workstation, count) {
        const isAllowUnordered = !!workstation.config.input.unordered;
        const craftCount = Math.ceil(count / (recipe.output ?? 1));
        if (isAllowUnordered) {
            return this.putInputItemsUnordered(recipe, workstation, craftCount);
        }
        else {
            return this.putInputItemsOrdered(recipe, workstation, craftCount);
        }
    }
    putInputItemsUnordered(recipe, workstation, targetCraftCount) {
        const slots = workstation.config.input.slots;
        const materials = this.collectMaterialsInfo(recipe, targetCraftCount);
        const baseSlotStates = this.getBaseSlotStates(slots);
        let actualCraftCount = targetCraftCount;
        for (const material of materials) {
            const slotCapacities = this.calculateSlotCapacitiesFromBase(baseSlotStates, material.id);
            const sortedSlots = this.sortSlotsByStackPriority(slotCapacities);
            const placedCount = this.fillMaterialToSlots(material, sortedSlots);
            const possibleCrafts = Math.floor(placedCount / material.requirementCount);
            actualCraftCount = Math.min(actualCraftCount, possibleCrafts);
        }
        if (actualCraftCount <= 0)
            return Err('材料不足，未能放入一组完整配方');
        return Ok(actualCraftCount);
    }
    putInputItemsOrdered(recipe, workstation, targetCraftCount) {
        const slots = workstation.config.input.slots;
        const inputs = recipe.ingredients;
        const uniqueMaterialCount = new Set(inputs.map((m) => m.slot)).size;
        const availableSlots = slots.length;
        if (uniqueMaterialCount >= availableSlots) {
            return this.putInputItemsSingle(recipe, workstation);
        }
        const maxBatchCount = this.calculateMaxBatchCount(recipe, slots, targetCraftCount);
        let actualBatchCount = maxBatchCount;
        for (const input of inputs) {
            const slotIndex = input.slot;
            const targetSlot = slots[slotIndex];
            const requirementPerCraft = input.count ?? 1;
            const totalToPlace = requirementPerCraft * maxBatchCount;
            const placed = this.placeMaterialToSlot(input.id, targetSlot, totalToPlace);
            const possibleBatches = Math.floor(placed / requirementPerCraft);
            actualBatchCount = Math.min(actualBatchCount, possibleBatches);
        }
        if (actualBatchCount <= 0)
            return Err('材料不足，未能放入一组完整配方');
        return Ok(actualBatchCount);
    }
    putInputItemsSingle(recipe, workstation) {
        const slots = workstation.config.input.slots;
        for (const input of recipe.ingredients) {
            const targetSlot = slots[input.slot];
            const required = input.count ?? 1;
            const moved = InventoryHelper$1.moveItemToSlot(input.id, targetSlot, required);
            if (moved < required)
                return Err(`材料不足，无法放入 ${input.name ?? input.id}`);
        }
        return Ok(1);
    }
    collectMaterialsInfo(recipe, count) {
        const materialMap = new Map();
        for (const input of recipe.ingredients) {
            const requirementCount = input.count ?? 1;
            const totalNeeded = requirementCount * count;
            if (materialMap.has(input.id)) {
                const existing = materialMap.get(input.id);
                existing.requirementCount += requirementCount;
                existing.totalNeeded += totalNeeded;
            }
            else {
                materialMap.set(input.id, {
                    id: input.id,
                    name: input.name,
                    requirementCount: requirementCount,
                    totalNeeded: totalNeeded,
                    availableInInventory: this.countItemInInventory(input.id),
                });
            }
        }
        return Array.from(materialMap.values());
    }
    getBaseSlotStates(slots) {
        return slots.map((slot) => ({
            slot,
            item: InventoryHelper$1.getSlot(slot),
        }));
    }
    calculateSlotCapacitiesFromBase(baseStates, materialId) {
        return baseStates.map(({ slot, item }) => {
            if (item.isEmpty()) {
                const maxCount = this.getItemMaxStackSize(materialId);
                return {
                    slot,
                    isEmpty: true,
                    itemId: null,
                    currentCount: 0,
                    maxCount,
                    availableSpace: maxCount,
                };
            }
            else {
                return {
                    slot,
                    isEmpty: false,
                    itemId: item.id,
                    currentCount: item.count,
                    maxCount: item.maxCount,
                    availableSpace: item.maxCount - item.count,
                };
            }
        });
    }
    sortSlotsByStackPriority(capacities) {
        return capacities.sort((a, b) => {
            if (a.itemId && b.itemId && a.itemId === b.itemId) {
                return b.currentCount - a.currentCount;
            }
            if (!a.isEmpty && b.isEmpty)
                return -1;
            if (a.isEmpty && !b.isEmpty)
                return 1;
            return 0;
        });
    }
    fillMaterialToSlots(material, sortedSlots) {
        let remainingCount = material.totalNeeded;
        let totalPlaced = 0;
        for (const slotInfo of sortedSlots) {
            if (remainingCount <= 0)
                break;
            if (slotInfo.isEmpty || slotInfo.itemId !== material.id)
                continue;
            const canPlace = Math.min(remainingCount, slotInfo.availableSpace, material.availableInInventory - totalPlaced);
            if (canPlace > 0) {
                const moved = InventoryHelper$1.moveItemToSlot(material.id, slotInfo.slot, canPlace);
                totalPlaced += moved;
                remainingCount -= moved;
                slotInfo.currentCount += moved;
                slotInfo.availableSpace -= moved;
            }
        }
        Client.waitTick(2);
        if (remainingCount > 0) {
            for (const slotInfo of sortedSlots) {
                if (remainingCount <= 0)
                    break;
                if (!slotInfo.isEmpty)
                    continue;
                const canPlace = Math.min(remainingCount, slotInfo.availableSpace, material.availableInInventory - totalPlaced);
                if (canPlace > 0) {
                    const moved = InventoryHelper$1.moveItemToSlot(material.id, slotInfo.slot, canPlace);
                    totalPlaced += moved;
                    remainingCount -= moved;
                    slotInfo.isEmpty = false;
                    slotInfo.itemId = material.id;
                    slotInfo.currentCount = moved;
                    slotInfo.availableSpace -= moved;
                }
            }
        }
        return totalPlaced;
    }
    getItemMaxStackSize(itemId) {
        if (this.maxStackSizeCache.has(itemId)) {
            return this.maxStackSizeCache.get(itemId);
        }
        const items = InventoryHelper$1.getItems('main', 'hotbar');
        const foundItem = items.find((item) => item.id === itemId);
        const maxCount = foundItem ? foundItem.maxCount : 64;
        this.maxStackSizeCache.set(itemId, maxCount);
        return maxCount;
    }
    calculateMaxBatchCount(recipe, slots, targetCount) {
        let maxBatch = targetCount;
        for (const input of recipe.ingredients) {
            const slotIndex = input.slot;
            const targetSlot = slots[slotIndex];
            const item = InventoryHelper$1.getSlot(targetSlot);
            const slotCapacity = item.isEmpty() ? this.getItemMaxStackSize(input.id) : item.maxCount;
            const requirementPerCraft = input.count ?? 1;
            const maxTimesForThisSlot = Math.floor(slotCapacity / requirementPerCraft);
            const availableCount = this.countItemInInventory(input.id);
            const maxTimesFromInventory = Math.floor(availableCount / requirementPerCraft);
            maxBatch = Math.min(maxBatch, maxTimesForThisSlot, maxTimesFromInventory);
        }
        return Math.max(1, maxBatch);
    }
    placeMaterialToSlot(materialId, targetSlot, count) {
        const item = InventoryHelper$1.getSlot(targetSlot);
        const maxCanPlace = item.isEmpty()
            ? Math.min(count, 64)
            : Math.min(count, item.maxCount - item.count);
        if (maxCanPlace <= 0)
            return 0;
        const moved = InventoryHelper$1.moveItemToSlot(materialId, targetSlot, maxCanPlace);
        return moved;
    }
    countItemInInventory(itemId) {
        const items = InventoryHelper$1.getItemsBySfItemId(['main', 'hotbar'], itemId);
        return items.reduce((sum, item) => sum + item.count, 0);
    }
    handleProductSelection(recipe, workstation) {
        if (!workstation.config.selector) {
            return Ok(undefined);
        }
        switch (workstation.config.selector.type) {
            case 'switch':
                return this.handleRecipeSwitch(recipe, workstation.config.selector);
            case 'page':
                return this.handlePaging(recipe, workstation.config.selector);
            default:
                return Err('未知的产物选择类型');
        }
    }
    handleRecipeSwitch(recipe, recipeSwitchConfig) {
        if (!recipeSwitchConfig) {
            return Err('产物选择配置错误');
        }
        let displayItem = InventoryHelper$1.getSlot(recipeSwitchConfig.display);
        let nextItem = InventoryHelper$1.getSlot(recipeSwitchConfig.next);
        let previousItem = InventoryHelper$1.getSlot(recipeSwitchConfig.prev);
        let switchCount = 0;
        const maxSwitchCount = AppConfig.crafting.maxAttempts * 10;
        while (toggle.check() && switchCount < maxSwitchCount) {
            if (!InventoryHelper$1.isContainer())
                return Err('容器不存在');
            if (!displayItem || !nextItem || !previousItem)
                return Err('产物选择槽位不存在');
            if (displayItem.id === 'minecraft:red_stained_glass_pane') {
                InventoryHelper$1.waitForSlotCondition(recipeSwitchConfig.display, (item) => item.id !== 'minecraft:red_stained_glass_pane');
            }
            displayItem = InventoryHelper$1.getSlot(recipeSwitchConfig.display);
            nextItem = InventoryHelper$1.getSlot(recipeSwitchConfig.next);
            previousItem = InventoryHelper$1.getSlot(recipeSwitchConfig.prev);
            if (SlimefunHelper$1.matchId(displayItem.id, recipe.id))
                return Ok(undefined);
            InventoryHelper$1.waitForInventoryChange(() => InventoryHelper$1.clickSlot(SlimefunHelper$1.matchId(previousItem.id, recipe.id) ? previousItem.slot : nextItem.slot));
            switchCount++;
        }
        return Err(`切换产物超出上限，工作站可能不支持 ${recipe.name ?? recipe.id}`);
    }
    handlePaging(recipe, pagingConfig) {
        if (!pagingConfig) {
            return Err('产物选择配置错误');
        }
        let currItem = InventoryHelper$1.getSlot(pagingConfig.display);
        const slots = pagingConfig.recipeSlots;
        let attemptCount = 0;
        while (toggle.check() &&
            InventoryHelper$1.isContainer() &&
            !SlimefunHelper$1.matchId(currItem.id, recipe.id) &&
            attemptCount < AppConfig.crafting.maxAttempts) {
            const item = slots
                .map((slot) => InventoryHelper$1.getSlot(slot))
                .find((slot) => SlimefunHelper$1.matchId(slot.id, recipe.id));
            if (!item)
                return Err(`当前页面未找到产物 ${recipe.name ?? recipe.id}`);
            InventoryHelper$1.waitForInventoryChange(() => InventoryHelper$1.clickSlot(item.slot));
            currItem = InventoryHelper$1.getSlot(pagingConfig.display);
            attemptCount++;
        }
        if (!SlimefunHelper$1.matchId(currItem.id, recipe.id))
            return Err(`选择产物失败 ${recipe.name ?? recipe.id}`);
        return Ok(undefined);
    }
    triggerCrafting(workstation, recipe, craftCount) {
        let craftedCount = 0;
        const { input, output } = workstation.config;
        const isNotUnordered = !input.unordered;
        const isEqualPos = input.relativePos?.x === output.relativePos?.x &&
            input.relativePos?.y === output.relativePos?.y;
        const isSameSlot = input.slots.some((slot) => output.slots.includes(slot));
        if (workstation.config.craft.type === 'interact') {
            let num = craftCount;
            while (toggle.check() && num > 0) {
                if (isNotUnordered && isEqualPos && isSameSlot) {
                    const item = InventoryHelper$1.getItems('container').find((f) => f.id === recipe.id);
                    if (item && item.count > 0) {
                        craftedCount += item.count;
                        InventoryHelper$1.quickSlot(item.slot);
                    }
                }
                PlayerHelper$1.interactBlock(workstation.interactPos, false);
                num--;
            }
        }
        else if (workstation.config.craft.type === 'button') {
            const btns = workstation.config.craft.buttons;
            if (!btns)
                return Err('按钮配置错误');
            let selectedAction;
            for (const btn of btns) {
                for (const act of btn.actions) {
                    if (act.craftCount >= craftCount &&
                        (!selectedAction || act.craftCount < selectedAction.craftCount)) {
                        selectedAction = { slot: btn.slot, type: act.type, craftCount: act.craftCount };
                    }
                }
            }
            if (!selectedAction)
                return Err('按钮配置错误');
            const clickType = selectedAction.type === 'left' ? MouseButton.LEFT : MouseButton.RIGHT;
            InventoryHelper$1.clickSlot(selectedAction.slot, clickType);
        }
        if (isEqualPos) {
            const itemCount = InventoryHelper$1.waitForItemInSlots(output.slots, recipe.id, craftCount * recipe.output - craftedCount);
            if (itemCount < 0)
                return Err('未等待到合成物品');
            if (!InventoryHelper$1.emptyContainer([...input.slots, ...output.slots]))
                return Err('无法收取合成物品，请检查背包空间');
        }
        else {
            if (!InventoryHelper$1.openContainerForBlock(workstation.outputPos))
                return Err('打开输出容器失败');
            const itemCount = InventoryHelper$1.waitForItemInSlots(output.slots, recipe.id, craftCount * recipe.output - craftedCount);
            if (itemCount < 0)
                return Err('未等待到合成物品');
            if (!InventoryHelper$1.emptyContainer([...output.slots]))
                return Err('无法收取合成物品，请检查背包空间');
        }
        return Ok(craftCount * recipe.output);
    }
}
var CraftingExecutor$1 = new CraftingExecutor();class StorageManager {
    constructor() {
        this.cache = new Map();
        this.lastScanPos = null;
        this.gridMove = {
            takeStack: (slot, targetSlot) => {
                const openInv = Player.openInventory();
                openInv.click(slot, MouseButton.RIGHT);
                Client.waitTick(2);
                const heldNum = openInv.getHeld().getCount();
                openInv.click(targetSlot, MouseButton.LEFT);
                Client.waitTick(2);
                return heldNum;
            },
            takeNum: (slot, targetSlot, num) => {
                if (num <= 0)
                    return 0;
                const openInv = Player.openInventory();
                openInv.click(slot);
                while (toggle.check() && --num > 0) {
                    openInv.click(slot, MouseButton.LEFT);
                    Client.waitTick(2);
                }
                const heldNum = openInv.getHeld().getCount();
                openInv.click(targetSlot, MouseButton.LEFT);
                Client.waitTick(2);
                return heldNum;
            },
        };
    }
    scanStorageAround() {
        if (this.lastScanPos) {
            const isSamePos = PositionHelper$1.arePositionsEqual(this.lastScanPos, PlayerHelper$1.eyePos());
            if (isSamePos) {
                return;
            }
        }
        this.cache.clear();
        const positions = this.findStoragePositions();
        this.buildContainerCache(positions);
        Logger$1.info(`查看库存 ==> 检测到${this.cache.size}个容器`);
        this.lastScanPos = PositionHelper$1.P2A(PlayerHelper$1.eyePos());
    }
    findStoragePositions() {
        return AppConfig.storage.blockScan
            .flatMap(({ blockId, blockStates }) => WorldHelper$1.scanSphere(blockId, blockStates))
            .map(PositionHelper$1.P2A);
    }
    buildContainerCache(posList) {
        for (const pos of posList) {
            Client.waitTick(2);
            const inv = InventoryHelper$1.openContainerForBlock(pos, false, 100);
            if (!inv)
                continue;
            const items = InventoryHelper$1.getItems('container');
            const list = new ItemContainer();
            for (const item of items) {
                const slotInfo = {
                    slot: item.slot,
                    count: item.count,
                    maxCount: item.maxCount,
                };
                const existingItem = list.get(item.id);
                if (existingItem) {
                    existingItem.count += item.count;
                    existingItem.extra.push(slotInfo);
                }
                else {
                    list.add(item.id, item.name, item.count, [slotInfo]);
                }
            }
            if (!list.isEmptyOrZero()) {
                this.cache.set(pos, list);
                Logger$1.debug(`缓存容器 ${pos} 内物品: ${list.toString()}`);
            }
        }
        InventoryHelper$1.closeContainer();
    }
    takeFromContainer(itemId, needCount) {
        let sumMovedCount = 0;
        for (const [pos, list] of this.cache) {
            if (needCount <= 0)
                break;
            const item = list.get(itemId);
            if (!item)
                continue;
            const slotInfos = item.extra;
            if (!slotInfos || slotInfos.length === 0)
                continue;
            const inv = InventoryHelper$1.openContainerForBlock(pos);
            Logger$1.debug(`打开容器 ${pos} 内物品: ${list.toString()}`);
            if (!inv)
                continue;
            const containerTitle = inv.getContainerTitle();
            const movedCount = this.takeItem(slotInfos, needCount, containerTitle, itemId);
            Logger$1.debug(`从容器 ${pos} 内拿取 ${movedCount} x ${itemId} 需要 ${needCount}`);
            if (movedCount > 0) {
                needCount -= movedCount;
                sumMovedCount += movedCount;
                list.remove(itemId, movedCount);
            }
        }
        return sumMovedCount;
    }
    takeItem(slotInfos, needCount, containerTitle, itemId) {
        let totalMoved = 0;
        for (let i = 0; i < slotInfos.length; i++) {
            let retry = 0;
            const maxRetryNum = AppConfig.crafting.maxAttempts;
            while (toggle.check() && slotInfos[i].count > 0 && needCount > 0 && retry <= maxRetryNum) {
                const currentItem = InventoryHelper$1.getSlot(slotInfos[i].slot);
                if (currentItem.isEmpty() || currentItem.id !== itemId) {
                    slotInfos[i].count = 0;
                    break;
                }
                slotInfos[i].count = currentItem.count;
                slotInfos[i].maxCount = currentItem.maxCount;
                const free = InventoryHelper$1.getFreeInventorySlot(itemId, ['main', 'hotbar']);
                if (free.slot === -1)
                    return totalMoved;
                const num = Math.min(currentItem.count, free.num, needCount);
                let movedTotal = 0;
                if (AppConfig.storage.gridContainerTitles.includes(containerTitle)) {
                    if (!free.there && needCount >= slotInfos[i].maxCount) {
                        movedTotal = this.gridMove.takeStack(slotInfos[i].slot, free.slot);
                    }
                    else {
                        movedTotal = this.gridMove.takeNum(slotInfos[i].slot, free.slot, num);
                    }
                }
                else {
                    movedTotal = InventoryHelper$1.moveCount(slotInfos[i].slot, free.slot, num);
                }
                Time.sleep(AppConfig.timeout.delayAfterMove);
                if (movedTotal > 0) {
                    needCount -= movedTotal;
                    slotInfos[i].count -= movedTotal;
                    totalMoved += movedTotal;
                    retry = 0;
                }
                else {
                    retry++;
                }
            }
            if (retry >= maxRetryNum) {
                Logger$1.error(`拿取物品${itemId}失败，重试次数过多`);
                break;
            }
        }
        return totalMoved;
    }
}
var StorageManager$1 = new StorageManager();class InventorySpaceChecker {
    checkSpace(items, reservedSlots = 3) {
        const stackableItems = this.getStackableItems();
        let emptySlots = Math.max(0, this.countEmptySlots() - reservedSlots);
        for (const item of items) {
            let remainingCount = item.count;
            remainingCount -= this.calculateStackableSpace(stackableItems, item.id);
            remainingCount = Math.max(0, remainingCount);
            const slotsNeeded = Math.ceil(remainingCount / AppConfig.game.maxItemStackSize);
            if (slotsNeeded > emptySlots) {
                return Err(`背包空间不足，无法放下${item.count}个${item.name}`);
            }
            emptySlots -= slotsNeeded;
        }
        return Ok(undefined);
    }
    getStackableItems() {
        const stackableItems = new Map();
        InventoryHelper$1.getItemsWithEmpty('main', 'hotbar').forEach((item) => {
            if (!item.isEmpty() && item.count < item.maxCount) {
                const slots = stackableItems.get(item.id) || [];
                slots.push({
                    slot: item.slot,
                    count: item.count,
                    maxCount: item.maxCount,
                });
                stackableItems.set(item.id, slots);
            }
        });
        return stackableItems;
    }
    countEmptySlots() {
        return InventoryHelper$1.getItemsWithEmpty('main', 'hotbar').filter((item) => item.isEmpty())
            .length;
    }
    calculateStackableSpace(stackableItems, itemId) {
        const slots = stackableItems.get(itemId);
        if (!slots)
            return 0;
        return slots.reduce((total, slot) => total + (slot.maxCount - slot.count), 0);
    }
}
var InventorySpaceChecker$1 = new InventorySpaceChecker();class AutoCrafterManager {
    constructor() {
        this.makeList = new ItemContainer();
        this.queueList = new ItemContainer();
        this.missingList = new ItemContainer();
        this.calcDebounce = new Debounce(AppConfig.calculation.debounceCalculation);
        this.registerEvents = () => {
            EventHelper$1.on('ClickSlot', (event) => {
                this.handleClickSlot(event);
            });
            let tickCount = 0;
            EventHelper$1.on('Tick', () => {
                if (tickCount++ % 20 === 0) {
                    if (CraftingStateManager$1.isIdle() && !this.makeList.isEmptyOrZero()) {
                        this.calcDebounce.debounce(() => this.calculate());
                    }
                }
            });
        };
        this.unregisterEvents = () => {
            EventHelper$1.off('ClickSlot');
            EventHelper$1.off('Tick');
        };
        this.modifyMakeTable = (itemId, itemName, countDelta) => {
            if (countDelta > 0) {
                const recipe = RecipeRepository$1.getRecipe(itemId);
                if (!recipe)
                    return Logger$1.error(`[中断]未找到合成配方: ${itemName}`);
                if (BlacklistRepository$1.isBlacklist(itemId))
                    return Logger$1.warn(`[警告]物品 ${itemName} 在黑名单中，不可制作`);
                this.makeList.add(itemId, recipe.name, countDelta);
            }
            else {
                this.makeList.remove(itemId, Math.abs(countDelta));
            }
            this.redrawHud(true, false, false);
            this.calcDebounce.debounce(() => this.calculate());
        };
    }
    handleClickSlot(event) {
        if (event.slot === -999)
            return;
        if (event.mode === 6)
            return;
        if (event.mode === 5 && ![2, 6, 10].includes(event.button))
            return;
        const validSlots = AppConfig.guide.recipeDisplayConfig
            .map((config) => [config.workSlot, config.outputSlot])
            .flat();
        if (!validSlots.includes(event.slot))
            return;
        if (SlimefunGuideManager$1.isRecipeListScreen())
            return;
        const item = InventoryHelper$1.waitForItemInSlot(event.slot, undefined, 500);
        if (!item)
            return;
        const res = SlimefunGuideManager$1.getRecipeDetail();
        if (!res)
            return;
        switch (event.slot) {
            case res.workSlot:
                if (event.button === 0 || event.button === 1) {
                    if (InventoryHelper$1.waitForInventoryClose(undefined, AppConfig.crafting.timeoutCheckStart)) {
                        this.startCraft();
                    }
                }
                break;
            case res.outputSlot:
                if (item && !item.isEmpty()) {
                    const itemCountDelta = this.calculateItemCountDelta(event);
                    if (itemCountDelta !== 0) {
                        this.modifyMakeTable(item.id, item.name, itemCountDelta);
                    }
                }
                break;
        }
    }
    calculateItemCountDelta(event) {
        if (event.button === 0 && event.mode === 0)
            return AppConfig.guide.needAmount.left;
        if (event.button === 1 && event.mode === 0)
            return AppConfig.guide.needAmount.right;
        if (event.button === 0 && event.mode === 1)
            return AppConfig.guide.needAmount.shift_left;
        if (event.button === 1 && event.mode === 1)
            return AppConfig.guide.needAmount.shift_right;
        if (event.button === 0 && event.mode === 4)
            return AppConfig.guide.needAmount.drop;
        if (event.button === 1 && event.mode === 4)
            return AppConfig.guide.needAmount.ctrl_drop;
        return 0;
    }
    calculate() {
        const previousState = CraftingStateManager$1.getState();
        CraftingStateManager$1.setState(CraftingState.CALCULATING);
        this.missingList.clear();
        this.queueList.clear();
        if (this.makeList.isEmptyOrZero()) {
            this.redrawHud(true, true, true);
            CraftingStateManager$1.reset();
            return;
        }
        const result = MaterialCalculator$1.calculateWithDP(this.makeList);
        if (!result.success) {
            Logger$1.error(`[中断]材料计算失败: ${result.error}`);
            CraftingStateManager$1.reset();
            return;
        }
        this.queueList = result.data.queue.clone();
        this.missingList = result.data.missing.clone();
        this.redrawHud(false, true, true);
        if (previousState === CraftingState.CALCULATING) {
            CraftingStateManager$1.reset();
        }
        else {
            CraftingStateManager$1.setState(previousState);
        }
    }
    startCraft() {
        try {
            CraftingStateManager$1.setState(CraftingState.CRAFTING);
            if (!this.initializeCrafting()) {
                return;
            }
            let refillAttempts = 0;
            const MAX_REFILL_ATTEMPTS = AppConfig.crafting.maxRefillAttempts;
            while (toggle.check() && !this.queueList.isEmptyOrZero()) {
                const needRecalculate = this.tryRefillPhase();
                if (needRecalculate) {
                    refillAttempts++;
                    if (refillAttempts >= MAX_REFILL_ATTEMPTS) {
                        Logger$1.warn(`[警告]补货次数已达上限 ${MAX_REFILL_ATTEMPTS}，停止补货`);
                        break;
                    }
                    Logger$1.debug(`补货成功，重新计算材料 (第 ${refillAttempts} 次)`);
                    this.calculate();
                    if (this.queueList.isEmptyOrZero()) {
                        break;
                    }
                    continue;
                }
                const craftingSuccess = this.executeCraftingBatch();
                if (!craftingSuccess) {
                    break;
                }
                refillAttempts = 0;
            }
        }
        catch (error) {
            Logger$1.error(`[中断]自动合成失败: ${error instanceof Error ? error.message : error}`);
        }
        finally {
            InventoryHelper$1.closeContainer();
            CraftingStateManager$1.reset();
        }
    }
    initializeCrafting() {
        this.calculate();
        if (this.queueList.isEmptyOrZero()) {
            Logger$1.info('[完成]合成队列为空，无需合成');
            CraftingStateManager$1.reset();
            return false;
        }
        if (!CraftingExecutor$1.emptyMainHand()) {
            Logger$1.error('[中断]无法空出主手，请清理主手物品');
            CraftingStateManager$1.reset();
            return false;
        }
        WorkstationRepository$1.scanWorkstationAround();
        return true;
    }
    tryRefillPhase() {
        if (this.missingList.isEmptyOrZero()) {
            return false;
        }
        CraftingStateManager$1.setState(CraftingState.REFILLING);
        StorageManager$1.scanStorageAround();
        const refilled = this.refillQueue(this.queueList, this.makeList);
        CraftingStateManager$1.setState(CraftingState.CRAFTING);
        return refilled;
    }
    executeCraftingBatch() {
        const entries = this.queueList.all().reverse();
        let anyCrafted = false;
        while (toggle.check()) {
            const entry = entries.pop();
            if (!entry || entry.count <= 0) {
                break;
            }
            const recipe = entry.extra;
            if (!this.checkAndRefillMaterials(recipe, entry.count)) {
                entries.push(entry);
                break;
            }
            this.redrawHud(false, false, true);
            const craftedCount = CraftingExecutor$1.craftRecipe(recipe, entry.count);
            if (craftedCount > 0) {
                anyCrafted = true;
                entry.count -= craftedCount;
                this.queueList.remove(entry.id, craftedCount);
                if (this.makeList.remove(entry.id, craftedCount)) {
                    Logger$1.info(`[成功]合成 ${craftedCount} 个 ${entry.name}`);
                }
            }
            if (entry.count > 0) {
                entries.push(entry);
                this.redrawHud(true, true, true);
                break;
            }
            this.redrawHud(true, true, true);
        }
        return anyCrafted;
    }
    checkAndRefillMaterials(recipe, count) {
        let missingItems = this.missingList.clone();
        if (missingItems.isEmptyOrZero()) {
            return true;
        }
        let spaceCheckResult = InventorySpaceChecker$1.checkSpace(missingItems, AppConfig.crafting.reservedSlotsForCrafting);
        if (!spaceCheckResult.success) {
            missingItems = MaterialCalculator$1.calculateRecipeMaterials(recipe, count);
            if (missingItems.isEmptyOrZero()) {
                return true;
            }
            spaceCheckResult = InventorySpaceChecker$1.checkSpace(missingItems, AppConfig.crafting.reservedSlotsForCrafting);
            if (!spaceCheckResult.success) {
                Logger$1.error(`[中断]缺少材料: ${spaceCheckResult.error}`);
                return false;
            }
        }
        const refilledItems = this.refillMissing(missingItems);
        missingItems.merge(refilledItems, true);
        if (!missingItems.isEmptyOrZero()) {
            Logger$1.error(`[中断]缺少材料: ${missingItems.toString()}`);
            return false;
        }
        this.missingList.merge(refilledItems, true);
        return true;
    }
    refillQueue(queueList, makeList) {
        if (queueList.isEmptyOrZero())
            return false;
        for (const queueItem of queueList.reverse()) {
            if (!makeList.has(queueItem.id)) {
                const refilledCount = StorageManager$1.takeFromContainer(queueItem.id, queueItem.count);
                Logger$1.debug(`补充合成队列材料，成功补充 ${refilledCount} x ${queueItem.id} 需要 ${queueItem.count}`);
                if (refilledCount > 0) {
                    return true;
                }
            }
        }
        return false;
    }
    refillMissing(missingItems) {
        const refilledItems = new ItemContainer();
        if (missingItems.isEmptyOrZero())
            return refilledItems;
        for (const item of missingItems) {
            const refilledCount = StorageManager$1.takeFromContainer(item.id, item.count);
            Logger$1.debug(`补充缺失材料，成功补充 ${refilledCount} x ${item.id} 需要 ${item.count}`);
            if (refilledCount > 0) {
                refilledItems.add(item.id, item.name, refilledCount);
            }
        }
        return refilledItems;
    }
    redrawHud(updateMake, updateQueue, updateMissing) {
        if (updateMissing)
            HudManager$1.updateMissingHud(this.missingList);
        if (updateQueue)
            HudManager$1.updateQueueHud(this.queueList);
        if (updateMake)
            HudManager$1.updateMakeHud(this.makeList);
    }
}
var AutoCrafterManager$1 = new AutoCrafterManager();class Application {
    constructor() {
        this.isInitialized = false;
        this._isRunning = false;
    }
    get isRunning() {
        return this._isRunning;
    }
    async initialize() {
        if (this.isInitialized) {
            return true;
        }
        Logger$1.logf('');
        Logger$1.info('脚本初始化...');
        try {
            BlacklistRepository$1.initialize();
            RecipeRepository$1.initialize();
            WorkstationRepository$1.initialize();
            await SlimefunGuideManager$1.initialize(true);
            HudManager$1.register();
            SlimefunGuideManager$1.addButtons();
            AutoCrafterManager$1.registerEvents();
            GuideButtonManager$1.initialize();
            this.isInitialized = true;
            return true;
        }
        catch (error) {
            Logger$1.error('脚本初始化失败', error);
            return false;
        }
    }
    start() {
        if (!this.isInitialized) {
            Logger$1.error('应用未初始化,无法启动');
            return;
        }
        if (this._isRunning) {
            Logger$1.warn('应用已在运行');
            return;
        }
        this._isRunning = true;
        Logger$1.info('脚本已启动');
        toggle.basicLoop(() => AutoCrafterManager$1.calcDebounce.processTick(), 1);
    }
    stop() {
        if (!this._isRunning) {
            return;
        }
        Logger$1.info('脚本结束');
        this._isRunning = false;
    }
    cleanup() {
        try {
            GuideButtonManager$1.clear();
            AutoCrafterManager$1.unregisterEvents();
            InventoryHelper$1.closeContainer();
            HudManager$1.unregister();
            EventHelper$1.off();
            this.isInitialized = false;
        }
        catch (error) {
            Logger$1.error('清理资源时发生错误', error);
        }
    }
}
var Application$1 = new Application();async function main() {
    if (!toggle.check()) {
        return;
    }
    try {
        Logger$1.clearLogFile();
        if (!(await Application$1.initialize())) {
            throw new Error('应用初始化失败');
        }
        Application$1.start();
    }
    catch (error) {
        Logger$1.error('脚本执行失败', error);
    }
    finally {
        if (Application$1.isRunning) {
            Application$1.stop();
        }
        Application$1.cleanup();
    }
}
main();
Hud.clearDraw2Ds();
JsMacros.disableScriptListeners();