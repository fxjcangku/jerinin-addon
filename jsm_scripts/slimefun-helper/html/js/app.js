// 工作站配置管理器应用
(function () {
  'use strict';

  // 配置常量
  const CONFIG = {
    GRID: {
      MIN_SIZE: 1,
      MAX_SIZE: 9,
      DEFAULT_WIDTH: 3,
      DEFAULT_HEIGHT: 1,
    },
    SELECTORS: {
      GRID_CELL: '.grid-cell',
      SPECIAL_INPUT: '.special-input',
      SPECIAL_OUTPUT: '.special-output',
      SPECIAL_CRAFT: '.special-craft',
      CELL_BADGE: '.cell-badge',
      CELL_TEXT: '.cell-text',
    },
    BADGE_TYPES: {
      input: { class: 'special-input', badge: 'input-badge', text: '输入' },
      output: { class: 'special-output', badge: 'output-badge', text: '输出' },
      craft: { class: 'special-craft', badge: 'craft-badge', text: '交互' },
    },
    MESSAGES: {
      EMPTY_BLOCK: '空',
    },
  };

  // 应用状态
  function escapeHtml(value) {
    return String(value ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  const state = {
    workstations: [],
    filteredWorkstations: [],
    currentEditingIndex: -1,
    searchTerm: '',
    typeFilter: '',
    sortBy: 'priority',
  };

  // DOM元素
  const elements = {
    workstationList: $('#workstationList'),
    searchInput: $('#searchInput'),
    typeFilter: $('#typeFilter'),
    sortBy: $('#sortBy'),
    addBtn: $('#addBtn'),
    importBtn: $('#importBtn'),
    exportBtn: $('#exportBtn'),
    editModal: $('#editModal'),
    closeModal: $('#closeModal'),
    modalTitle: $('#modalTitle'),
    modalContent: $('#modalContent'),
    jsonModal: $('#jsonModal'),
    closeJsonModal: $('#closeJsonModal'),
    jsonPreview: $('#jsonPreview'),
    downloadJson: $('#downloadJson'),
    copyJson: $('#copyJson'),
    fileInput: $('#fileInput'),
  };

  // 初始化应用
  function init() {
    bindEvents();
    loadInitialData();
  }

  // 绑定事件
  function bindEvents() {
    elements.searchInput.on('input', handleSearch);
    elements.typeFilter.on('change', handleTypeFilter);
    elements.sortBy.on('change', handleSort);
    elements.addBtn.on('click', handleAdd);
    elements.importBtn.on('click', handleImport);
    elements.exportBtn.on('click', handleExport);
    elements.closeModal.on('click', closeEditModal);
    elements.closeJsonModal.on('click', closeJsonModal);
    elements.downloadJson.on('click', handleDownloadJson);
    elements.copyJson.on('click', handleCopyJson);
    elements.fileInput.on('change', handleFileSelect);

    // JSON预览模态框可以点击外部关闭（只读，不会丢失数据）
    elements.jsonModal.on('click', function (e) {
      if (e.target === this) closeJsonModal();
    });
  }

  // 加载初始数据
  function loadInitialData() {
    // 优先从localStorage加载数据
    const savedData = loadFromLocalStorage();
    if (savedData && savedData.length > 0) {
      state.workstations = savedData;
      console.log('已从localStorage加载配置数据');
    } else {
      state.workstations = [];
      console.log('提示: 没有保存的配置。请使用"导入配置"按钮加载工作站配置。');
    }
    applyFiltersAndSort();
  }

  // 从localStorage加载数据
  function loadFromLocalStorage() {
    try {
      const data = localStorage.getItem('workstations');
      if (data) {
        return JSON.parse(data);
      }
    } catch (error) {
      console.error('从localStorage加载数据失败:', error);
    }
    return null;
  }

  // 保存数据到localStorage
  function saveToLocalStorage() {
    try {
      // 验证数据
      const validationResult = WorkstationValidator.validateWorkstations(state.workstations);
      if (!validationResult.valid) {
        const errorMsg = WorkstationValidator.formatValidationErrors(validationResult);
        console.error('数据验证失败:', errorMsg);
        showToast('数据验证失败，请检查配置', 'error');
        // 显示详细错误信息
        showConfirm(errorMsg, '数据验证失败');
        return false;
      }

      localStorage.setItem('workstations', JSON.stringify(state.workstations));
      console.log('已保存到localStorage');
      return true;
    } catch (error) {
      console.error('保存到localStorage失败:', error);
      showToast('保存失败: ' + error.message, 'error');
      return false;
    }
  }

  // 搜索处理
  function handleSearch(e) {
    state.searchTerm = e.target.value.toLowerCase();
    applyFiltersAndSort();
  }

  // 类型过滤处理
  function handleTypeFilter(e) {
    state.typeFilter = e.target.value;
    applyFiltersAndSort();
  }

  // 排序处理
  function handleSort(e) {
    state.sortBy = e.target.value;
    applyFiltersAndSort();
  }

  // 应用过滤和排序
  function applyFiltersAndSort() {
    let filtered = state.workstations.filter((ws) => {
      const matchesSearch =
        !state.searchTerm ||
        ws.info.id.toLowerCase().includes(state.searchTerm) ||
        ws.info.name.toLowerCase().includes(state.searchTerm);

      const matchesType = !state.typeFilter || ws.structure.type === state.typeFilter;

      return matchesSearch && matchesType;
    });

    // 排序
    filtered.sort((a, b) => {
      switch (state.sortBy) {
        case 'priority':
          return (b.info.priority || 0) - (a.info.priority || 0);
        case 'id':
          return a.info.id.localeCompare(b.info.id);
        case 'name':
          return a.info.name.localeCompare(b.info.name);
        default:
          return 0;
      }
    });

    state.filteredWorkstations = filtered;
    renderWorkstations();
  }

  // 渲染工作站列表
  function renderWorkstations() {
    if (state.filteredWorkstations.length === 0) {
      elements.workstationList.html(`
                <div class="col-span-full empty-state">
                    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                              d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"></path>
                    </svg>
                    <p class="text-lg font-medium">没有找到工作站</p>
                    <p class="text-sm mt-2">尝试调整搜索条件或添加新的工作站</p>
                </div>
            `);
      return;
    }

    const html = state.filteredWorkstations
      .map((ws, index) => {
        const originalIndex = state.workstations.indexOf(ws);
        return createWorkstationCard(ws, originalIndex);
      })
      .join('');

    elements.workstationList.html(html);
    bindCardEvents();
  }

  // 创建工作站卡片
  function createWorkstationCard(ws, index) {
    const typeText = ws.structure.type === 'single' ? '单方块' : '多方块';
    const typeClass =
      ws.structure.type === 'single' ? 'workstation-type-single' : 'workstation-type-multi';
    const blockDisplay =
      ws.structure.type === 'single' ? escapeHtml(ws.structure.block) : `${ws.structure.layers.length}层结构`;
    const workstationName = escapeHtml(ws.info.name);
    const workstationId = escapeHtml(ws.info.id);

    return `
            <div class="workstation-card" data-index="${index}">
                <div class="workstation-card-header">
                    <div>
                        <span class="workstation-type-badge ${typeClass}">${typeText}</span>
                    </div>
                    <div class="workstation-priority">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"></path>
                        </svg>
                        ${ws.info.priority || 0}
                    </div>
                </div>
                <h3 class="text-lg font-semibold text-gray-900 mb-1">${workstationName}</h3>
                <p class="text-sm text-gray-600 mb-2">${workstationId}</p>
                <div class="text-sm text-gray-500">
                    <p>方块: ${blockDisplay}</p>
                    <p>输入槽: ${ws.input.slots.length}个</p>
                    <p>输出槽: ${ws.output.slots.length}个</p>
                </div>
                <div class="workstation-actions">
                    <button class="btn-action btn-edit" data-action="edit" data-index="${index}">编辑</button>
                    <button class="btn-action btn-view-json" data-action="json" data-index="${index}">JSON</button>
                    <button class="btn-action btn-delete" data-action="delete" data-index="${index}">删除</button>
                </div>
            </div>
        `;
  }

  // 绑定卡片事件（使用事件委托）
  function bindCardEvents() {
    // 移除旧的事件监听器
    elements.workstationList.off('click.cardActions');

    // 使用事件委托统一处理所有卡片操作
    elements.workstationList.on('click.cardActions', '.btn-action', function (e) {
      e.stopPropagation();
      const $btn = $(this);
      const action = $btn.data('action');
      const index = parseInt($btn.data('index'));

      switch (action) {
        case 'edit':
          handleEdit(index);
          break;
        case 'json':
          handleViewJson(index);
          break;
        case 'delete':
          handleDelete(index);
          break;
      }
    });
  }

  // 添加工作站
  function handleAdd() {
    state.currentEditingIndex = -1;
    elements.modalTitle.text('添加工作站');
    showEditForm(createEmptyWorkstation());
    openEditModal();
  }

  // 编辑工作站
  function handleEdit(index) {
    state.currentEditingIndex = index;
    elements.modalTitle.text('编辑工作站');
    showEditForm(JSON.parse(JSON.stringify(state.workstations[index])));
    openEditModal();
  }

  // 删除工作站
  async function handleDelete(index) {
    const confirmed = await showConfirm(
      `确定要删除工作站"${state.workstations[index].info.name}"吗?`,
      '删除确认'
    );
    if (confirmed) {
      state.workstations.splice(index, 1);
      saveToLocalStorage();
      applyFiltersAndSort();
      showToast('删除成功', 'success');
    }
  }

  // 查看JSON
  function handleViewJson(index) {
    const ws = state.workstations[index];
    elements.jsonPreview.text(JSON.stringify(ws, null, 2));
    openJsonModal();
  }

  // 导入配置
  function handleImport() {
    elements.fileInput.click();
  }

  // 文件选择处理
  function handleFileSelect(e) {
    const file = e.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = function (event) {
      try {
        const data = JSON.parse(event.target.result);
        if (Array.isArray(data)) {
          // 先验证数据
          const validationResult = WorkstationValidator.validateWorkstations(data);
          if (!validationResult.valid) {
            const errorMsg = WorkstationValidator.formatValidationErrors(validationResult);
            console.error('导入数据验证失败:', errorMsg);
            showToast('导入的数据验证失败', 'error');
            showConfirm(errorMsg, '数据验证失败');
            return;
          }

          state.workstations = data;
          if (saveToLocalStorage()) {
            applyFiltersAndSort();
            showToast('导入成功!', 'success');
          }
        } else {
          showToast('无效的JSON格式,需要数组类型', 'error');
        }
      } catch (error) {
        showToast('JSON解析失败: ' + error.message, 'error');
      }
    };
    reader.readAsText(file);

    // 重置input以允许重复选择同一文件
    e.target.value = '';
  }

  // 导出配置
  function handleExport() {
    elements.jsonPreview.text(JSON.stringify(state.workstations, null, 2));
    openJsonModal();
  }

  // 下载JSON
  function handleDownloadJson() {
    const dataStr = elements.jsonPreview.text();
    const blob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = '工作站.json';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  // 复制JSON
  function handleCopyJson() {
    const text = elements.jsonPreview.text();
    navigator.clipboard
      .writeText(text)
      .then(() => {
        showToast('已复制到剪贴板!', 'success');
      })
      .catch((err) => {
        showToast('复制失败: ' + err, 'error');
      });
  }

  // 显示编辑表单
  function showEditForm(workstation) {
    const formHtml = `
            <div class="tabs">
                <div class="tab active" data-tab="basic">基本信息</div>
                <div class="tab" data-tab="structure">结构配置</div>
                <div class="tab" data-tab="slots">槽位配置</div>
                <div class="tab" data-tab="craft">合成配置</div>
            </div>

            <div class="tab-content active" data-content="basic">
                ${renderBasicInfoForm(workstation)}
            </div>

            <div class="tab-content" data-content="structure">
                ${renderStructureForm(workstation)}
            </div>

            <div class="tab-content" data-content="slots">
                ${renderSlotsForm(workstation)}
            </div>

            <div class="tab-content" data-content="craft">
                ${renderCraftForm(workstation)}
            </div>

            <div class="btn-group">
                <button class="btn-primary" id="saveBtn">保存</button>
                <button class="btn-secondary" id="cancelBtn">取消</button>
            </div>
        `;

    elements.modalContent.html(formHtml);
    bindFormEvents(workstation);
  }

  // 渲染基本信息表单
  function renderBasicInfoForm(ws) {
    const workstationId = escapeHtml(ws.info.id);
    const workstationName = escapeHtml(ws.info.name);
    const supported = escapeHtml((ws.info.supported || []).join('\n'));
    return `
            <div class="form-group">
                <label class="form-label">工作站ID *</label>
                <input type="text" class="form-input" id="wsId" value="${workstationId}" placeholder="例如: SLIMEFUN:EXAMPLE">
            </div>
            <div class="form-group">
                <label class="form-label">工作站名称 *</label>
                <input type="text" class="form-input" id="wsName" value="${workstationName}" placeholder="例如: 示例工作台">
            </div>
            <div class="form-group">
                <label class="form-label">优先级</label>
                <input type="number" class="form-input" id="wsPriority" value="${ws.info.priority || 1}" min="1">
                <p class="text-xs text-gray-500 mt-1">数值越大优先级越高</p>
            </div>
            <div class="form-group">
                <label class="form-label">支持的工作站类型</label>
                <textarea class="form-textarea" id="wsSupported" rows="3" placeholder="一行一个,例如:\nENHANCED_CRAFTING_TABLE\nMAGIC_WORKBENCH">${supported}</textarea>
                <p class="text-xs text-gray-500 mt-1">可选,用于快捷综合工作台</p>
            </div>
        `;
  }

  // 渲染结构表单
  function renderStructureForm(ws) {
    const isSingle = ws.structure.type === 'single';
    return `
            <div class="form-group">
                <label class="form-label">结构类型 *</label>
                <select class="form-select" id="structureType">
                    <option value="single" ${isSingle ? 'selected' : ''}>单方块</option>
                    <option value="multi" ${!isSingle ? 'selected' : ''}>多方块</option>
                </select>
            </div>
            <div id="structureConfig">
                ${isSingle ? renderSingleBlockForm(ws) : renderMultiBlockForm(ws)}
            </div>
        `;
  }

  // 渲染单方块表单
  function renderSingleBlockForm(ws) {
    const blockId = escapeHtml(ws.structure.block);
    return `
            <div class="form-group">
                <label class="form-label">方块ID *</label>
                <input type="text" class="form-input" id="blockId" value="${blockId}" placeholder="例如: minecraft:crafting_table">
            </div>
        `;
  }

  // 渲染多方块表单
  function renderMultiBlockForm(ws) {
    const layers = ws.structure.layers || [[null, null, null]];

    // 计算当前网格尺寸
    const currentHeight = layers.length;
    const currentWidth = layers[0] ? layers[0].length : CONFIG.GRID.DEFAULT_WIDTH;

    // 获取特殊方块位置
    const inputPos = ws.input.relativePos || { x: -1, y: -1 };
    // 如果没有设置输出位置，默认使用输入位置
    const outputPos = ws.output.relativePos || inputPos;
    const craftPos = ws.craft.relativePos || { x: -1, y: -1 };

    let html = '<div class="structure-editor">';
    html +=
      '<div class="info-box">左键点击编辑方块,右键点击设置特殊功能(输入/输出容器、交互方块)</div>';

    // 网格尺寸配置
    html += `
            <div class="form-row mb-4">
                <div class="form-group">
                    <label class="form-label">宽度 (X轴)</label>
                    <input type="number" class="form-input" id="gridWidth" value="${currentWidth}" min="1" max="9">
                </div>
                <div class="form-group">
                    <label class="form-label">高度 (Y轴/层数)</label>
                    <input type="number" class="form-input" id="gridHeight" value="${currentHeight}" min="1" max="9">
                </div>
                <div class="form-group flex items-end">
                    <button class="btn-primary" id="resizeGridBtn">应用尺寸</button>
                </div>
            </div>
        `;

    // 所有层合并到一个layer-editor中显示
    html += '<div class="layer-editor">';

    // 按原始顺序渲染层,但显示Y值时反向计算(Y=0在最下面)
    layers.forEach((layer, layerIndex) => {
      // 显示的Y值:最后一层是Y=0,第一层是Y=layers.length-1
      const displayY = layers.length - 1 - layerIndex;
      html += `
                <div class="mb-4">
                    <div class="text-sm font-medium text-gray-700 mb-2">Y=${displayY}</div>
                    <div class="grid-3x3" style="grid-template-columns: repeat(${currentWidth}, 1fr);">
                        ${renderLayerGrid(layer, layerIndex, inputPos, outputPos, craftPos)}
                    </div>
                </div>
            `;
    });

    html += '</div></div>';

    return html;
  }

  // 工具对象：网格管理器
  const GridManager = {
    // 验证网格尺寸
    validateSize(width, height) {
      return width >= CONFIG.GRID.MIN_SIZE && 
             width <= CONFIG.GRID.MAX_SIZE && 
             height >= CONFIG.GRID.MIN_SIZE && 
             height <= CONFIG.GRID.MAX_SIZE;
    },

    // 提取单元格文本
    extractCellText($cell) {
      const $cellText = $cell.find(CONFIG.SELECTORS.CELL_TEXT);
      return $cellText.length ? $cellText.text().trim() : $cell.text().trim();
    },

    // 获取单元格方块ID
    getCellBlock($cell) {
      const text = this.extractCellText($cell);
      return text === CONFIG.MESSAGES.EMPTY_BLOCK ? null : text ? text.toLowerCase() : null;
    },

    // 从网格提取layers
    extractLayers(width, height) {
      const layers = [];
      for (let y = 0; y < height; y++) {
        const layer = [];
        for (let x = 0; x < width; x++) {
          const $cell = $(`${CONFIG.SELECTORS.GRID_CELL}[data-layer="${y}"][data-x="${x}"]`);
          layer.push(this.getCellBlock($cell));
        }
        layers.push(layer);
      }
      return layers;
    },

    // 验证网格至少有一个非空方块
    hasNonEmptyBlock($cells) {
      return $cells.toArray().some((cell) => {
        const text = this.extractCellText($(cell));
        return text !== CONFIG.MESSAGES.EMPTY_BLOCK && text !== '';
      });
    }
  };

  // 工具对象：UI层即时验证器
  // 职责：提供快速、友好的用户输入验证反馈
  // 用途：表单输入时的即时验证，提供即时用户反馈
  // 注意：这里是轻量级验证，完整验证在保存时使用 WorkstationValidator
  const Validators = {
    // 验证网格尺寸
    gridSize(width, height) {
      if (!GridManager.validateSize(width, height)) {
        return {
          valid: false,
          message: `宽度和高度必须在${CONFIG.GRID.MIN_SIZE}-${CONFIG.GRID.MAX_SIZE}之间`
        };
      }
      return { valid: true };
    },

    // 验证工作站ID
    workstationId(id) {
      if (!id || !id.trim()) {
        return { valid: false, message: '工作站ID不能为空' };
      }
      if (!/^[A-Za-z_]+:[A-Za-z_\-]+$/i.test(id)) {
        return { valid: false, message: 'ID格式不正确，应为"命名空间:ID"格式' };
      }
      return { valid: true };
    },

    // 验证方块ID
    blockId(id) {
      if (!id || !id.trim()) {
        return { valid: false, message: '方块ID不能为空' };
      }
      if (!/^[a-z_]+:[a-z0-9_\-]+$/i.test(id)) {
        return { valid: false, message: '方块ID格式不正确' };
      }
      return { valid: true };
    },

    // 验证槽位表达式
    slotExpression(expr) {
      if (!expr || !expr.trim()) {
        return { valid: false, message: '槽位表达式不能为空' };
      }
      const slots = parseSlotExpression(expr);
      if (slots.length === 0) {
        return { valid: false, message: '槽位表达式无效' };
      }
      return { valid: true, slots };
    },

    // 验证特殊方块位置
    specialBlockPos($cell, blockType, layers) {
      if (!$cell.length) return { valid: true, pos: null };

      const x = parseInt($cell.data('x'));
      const y = parseInt($cell.data('layer'));

      const block = layers[y]?.[x];
      if (!block) {
        return { valid: false, message: `${blockType}位置必须填写方块ID` };
      }

      return { valid: true, pos: { x, y } };
    }
  };

  // 处理网格尺寸调整
  function handleGridResize(workstation) {
    const width = parseInt($('#gridWidth').val()) || CONFIG.GRID.DEFAULT_WIDTH;
    const height = parseInt($('#gridHeight').val()) || CONFIG.GRID.DEFAULT_HEIGHT;

    const validation = Validators.gridSize(width, height);
    if (!validation.valid) {
      showToast(validation.message, 'warning');
      return;
    }

    const specialBlocks = extractSpecialBlockPositions();
    const newLayers = createResizedLayers(workstation, width, height);
    const warnings = updateSpecialBlockPositions(workstation, specialBlocks, width, height);

    workstation.structure.layers = newLayers;
    $('#structureConfig').html(renderMultiBlockForm(workstation));
    bindFormEvents(workstation);

    if (warnings.length > 0) {
      showToast(warnings.join('；'), 'warning');
    } else {
      showToast('网格尺寸已调整', 'success');
    }
  }

  // 工具函数：从DOM提取特殊方块位置
  function extractSpecialBlockPositions() {
    const specialBlocks = { input: null, output: null, craft: null };
    
    const types = [
      { key: 'input', selector: CONFIG.SELECTORS.SPECIAL_INPUT },
      { key: 'output', selector: CONFIG.SELECTORS.SPECIAL_OUTPUT },
      { key: 'craft', selector: CONFIG.SELECTORS.SPECIAL_CRAFT }
    ];
    
    types.forEach(({ key, selector }) => {
      $(selector).each(function () {
        const $cell = $(this);
        specialBlocks[key] = {
          x: parseInt($cell.data('x')),
          y: parseInt($cell.data('layer'))
        };
      });
    });
    
    return specialBlocks;
  }

  // 工具函数：创建调整尺寸后的layers
  function createResizedLayers(workstation, width, height) {
    const newLayers = [];
    for (let y = 0; y < height; y++) {
      const newLayer = [];
      for (let x = 0; x < width; x++) {
        const oldLayer = workstation.structure.layers?.[y];
        newLayer[x] = (oldLayer && oldLayer[x]) || null;
      }
      newLayers.push(newLayer);
    }
    return newLayers;
  }

  // 工具函数：更新特殊方块位置并返回警告
  function updateSpecialBlockPositions(workstation, specialBlocks, width, height) {
    const warnings = [];
    
    // 验证并更新输入位置
    if (specialBlocks.input) {
      if (specialBlocks.input.x < width && specialBlocks.input.y < height) {
        workstation.input.relativePos = specialBlocks.input;
      } else {
        delete workstation.input.relativePos;
        warnings.push('输入容器位置超出新范围，已清除');
      }
    }
    
    // 验证并更新输出位置
    if (specialBlocks.output) {
      if (specialBlocks.output.x < width && specialBlocks.output.y < height) {
        workstation.output.relativePos = specialBlocks.output;
      } else {
        delete workstation.output.relativePos;
        warnings.push('输出容器位置超出新范围，已清除');
      }
    }
    
    // 验证并更新交互方块位置
    if (specialBlocks.craft) {
      if (specialBlocks.craft.x < width && specialBlocks.craft.y < height) {
        if (!workstation.craft.relativePos) {
          workstation.craft.relativePos = {};
        }
        workstation.craft.relativePos = specialBlocks.craft;
      } else {
        delete workstation.craft.relativePos;
        warnings.push('交互方块位置超出新范围，已清除');
      }
    }
    
    return warnings;
  }

  // 生成方块的badge HTML
  function generateCellBadges(hasInput, hasOutput, hasCraft) {
    const badges = [];
    
    if (hasInput && hasOutput) {
      // 输入输出重叠时，显示组合标记
      badges.push('<span class="cell-badge input-badge">输入/输出</span>');
    } else {
      // 分别显示各自的标记
      if (hasInput) {
        badges.push('<span class="cell-badge input-badge">输入</span>');
      }
      if (hasOutput) {
        badges.push('<span class="cell-badge output-badge">输出</span>');
      }
    }
    if (hasCraft) {
      badges.push('<span class="cell-badge craft-badge">交互</span>');
    }
    
    return badges.join('');
  }

  // 渲染层网格
  function renderLayerGrid(layer, layerIndex, inputPos, outputPos, craftPos) {
    let html = '';
    // layer是一维数组,包含若干个方块
    const width = layer.length;
    for (let x = 0; x < width; x++) {
      const block = layer[x] || null;
      let displayText = block || CONFIG.MESSAGES.EMPTY_BLOCK;
      let cellClass = block ? '' : 'empty';

      // 检查是否是特殊方块
      const isInput = inputPos.x === x && inputPos.y === layerIndex;
      const isOutput = outputPos && outputPos.x === x && outputPos.y === layerIndex;
      const isCraft = craftPos.x === x && craftPos.y === layerIndex;

      // 添加特殊标记class
      if (isInput) {
        cellClass += ' special-input';
      }
      if (isOutput) {
        cellClass += ' special-output';
      }
      if (isCraft) {
        cellClass += ' special-craft';
      }
      
      // 生成badge HTML
      const badge = generateCellBadges(isInput, isOutput, isCraft);

      html += `
                <div class="grid-cell ${cellClass}"
                     data-layer="${layerIndex}"
                     data-x="${x}"
                     onclick="editCell(${layerIndex}, ${x})"
                     oncontextmenu="showCellMenu(event, ${layerIndex}, ${x}); return false;">
                    ${badge}
                    <div class="cell-text">${escapeHtml(displayText)}</div>
                </div>
            `;
    }
    return html;
  }

  // 渲染槽位表单
  function renderSlotsForm(ws) {
    const inputSlotsStr = formatSlotExpression(ws.input.slots);
    const outputSlotsStr = formatSlotExpression(ws.output.slots);

    let html = `
            <div class="info-box">
                输入槽位编号,支持范围表达式。例如: 1-5,7 表示槽位1,2,3,4,5,7<br>
                槽位编号从0开始,按行排列(0-8为第一行,9-17为第二行,以此类推)
            </div>
            
            <div class="form-group">
                <label class="form-label">输入槽位 *</label>
                <input type="text" class="form-input" id="inputSlotsText" value="${inputSlotsStr}"
                       placeholder="例如: 0-8,10,15-20">
                <p class="text-xs text-gray-500 mt-1">支持单个数字、范围(1-5)和逗号分隔的组合</p>
                <label class="form-checkbox mt-2">
                    <input type="checkbox" id="inputUnordered" ${ws.input.unordered ? 'checked' : ''}>
                    <span class="ml-2">无序输入</span>
                </label>
            </div>

            <div class="form-group">
                <label class="form-label">输出槽位 *</label>
                <input type="text" class="form-input" id="outputSlotsText" value="${outputSlotsStr}"
                       placeholder="例如: 27-35,40">
                <p class="text-xs text-gray-500 mt-1">支持单个数字、范围(1-5)和逗号分隔的组合</p>
            </div>
        `;

    if (ws.structure.type === 'multi') {
      html += `
                <div class="warning-box">
                    多方块结构的输入/输出容器和交互方块位置请在"结构配置"标签页中通过右键菜单设置
                </div>
            `;
    }

    return html;
  }

  // 解析槽位表达式 (例如: "1-5,7,10-12" -> [1,2,3,4,5,7,10,11,12])
  function parseSlotExpression(expr) {
    const slots = [];
    const parts = expr
      .split(',')
      .map((p) => p.trim())
      .filter((p) => p);

    for (const part of parts) {
      if (part.includes('-')) {
        // 范围表达式
        const [start, end] = part.split('-').map((n) => parseInt(n.trim()));
        if (!isNaN(start) && !isNaN(end) && start <= end) {
          for (let i = start; i <= end; i++) {
            if (!slots.includes(i)) {
              slots.push(i);
            }
          }
        }
      } else {
        // 单个数字
        const num = parseInt(part);
        if (!isNaN(num) && !slots.includes(num)) {
          slots.push(num);
        }
      }
    }

    return slots.sort((a, b) => a - b);
  }

  // 将槽位数组转换为范围表达式 (例如: [1,2,3,4,5,7,10,11,12] -> "1-5,7,10-12")
  function formatSlotExpression(slots) {
    if (!slots || slots.length === 0) return '';

    const sorted = [...slots].sort((a, b) => a - b);
    const ranges = [];
    let start = sorted[0];
    let end = sorted[0];

    // 辅助函数：添加范围到结果数组
    const addRange = (s, e) => {
      if (s === e) {
        ranges.push(s.toString());
      } else if (e === s + 1) {
        ranges.push(s.toString(), e.toString());
      } else {
        ranges.push(`${s}-${e}`);
      }
    };

    for (let i = 1; i < sorted.length; i++) {
      if (sorted[i] === end + 1) {
        end = sorted[i];
      } else {
        addRange(start, end);
        start = end = sorted[i];
      }
    }

    addRange(start, end);
    return ranges.join(', ');
  }

  // 渲染合成配置表单
  function renderCraftForm(ws) {
    const isSingle = ws.structure.type === 'single';
    const craftType = ws.craft.type || (isSingle ? 'button' : 'interact');
    const isButton = craftType === 'button';

    let html = `
            <div class="form-group">
                <label class="form-label">合成触发方式 *</label>
                <select class="form-select" id="craftType">
                    <option value="auto">自动合成</option>
        `;

    // 单方块支持按钮触发,多方块支持交互触发
    if (isSingle) {
      html += `<option value="button" ${isButton ? 'selected' : ''}>按钮触发 (默认)</option>`;
    } else {
      html += `<option value="interact" ${!isButton ? 'selected' : ''}>交互触发 (默认)</option>`;
    }

    html += `
                </select>
                <p class="text-xs text-gray-500 mt-1">
                    ${isSingle ? '单方块工作站支持自动合成和按钮触发' : '多方块工作站支持自动合成和交互触发'}
                </p>
            </div>
        `;

    if (craftType === 'auto') {
      html += renderAutoCraftForm(ws);
    } else if (isButton) {
      html += renderButtonCraftForm(ws);
    } else {
      html += renderInteractCraftForm(ws);
    }

    return html;
  }

  // 渲染自动合成表单
  function renderAutoCraftForm(ws) {
    return `
            <div class="info-box">
                自动合成模式:当输入槽位的物品满足配方要求时,自动进行合成并将结果放入输出槽位
            </div>
        `;
  }

  // 渲染按钮合成表单
  function renderButtonCraftForm(ws) {
    const buttons = ws.craft.buttons || [];
    let html = '<div id="buttonsList">';

    buttons.forEach((btn, index) => {
      html += `
                <div class="layer-editor" data-button="${index}">
                    <div class="layer-header">
                        <h4 class="font-semibold">按钮 ${index + 1}</h4>
                        <button class="btn-danger btn-sm" onclick="removeButton(${index})">删除按钮</button>
                    </div>
                    <div class="form-row">
                        <div class="form-group">
                            <label class="form-label">槽位</label>
                            <input type="number" class="form-input button-slot" value="${btn.slot}" min="0">
                        </div>
                    </div>
                    <div class="mb-2">
                        <label class="form-label">点击动作</label>
                    </div>
                    ${renderButtonActions(btn.actions, index)}
                    <button class="btn-secondary btn-sm mt-2" onclick="addAction(${index})">添加点击类型</button>
                </div>
            `;
    });

    html += '</div>';
    html += '<button class="btn-primary mt-4" id="addButtonBtn">添加按钮</button>';

    return html;
  }

  // 渲染按钮动作
  function renderButtonActions(actions, buttonIndex) {
    let html = '<div class="actions-list" data-button-actions="${buttonIndex}">';

    actions.forEach((action, actionIndex) => {
      html += `
                <div class="form-row action-row" data-action="${actionIndex}">
                    <div class="form-group">
                        <label class="form-label">点击类型</label>
                        <select class="form-select action-type">
                            <option value="left" ${action.type === 'left' ? 'selected' : ''}>左键</option>
                            <option value="right" ${action.type === 'right' ? 'selected' : ''}>右键</option>
                            <option value="shift_left" ${action.type === 'shift_left' ? 'selected' : ''}>Shift+左键</option>
                            <option value="shift_right" ${action.type === 'shift_right' ? 'selected' : ''}>Shift+右键</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label class="form-label">合成数量</label>
                        <input type="number" class="form-input action-count" value="${action.craftCount}" min="1">
                    </div>
                    <div class="form-group flex items-end">
                        <button class="btn-danger btn-sm" onclick="removeAction(${buttonIndex}, ${actionIndex})">删除</button>
                    </div>
                </div>
            `;
    });

    html += '</div>';
    return html;
  }

  // 渲染交互合成表单
  function renderInteractCraftForm(ws) {
    return `
            <div class="warning-box">
                交互触发方式的交互方块位置请在"结构配置"标签页中通过右键菜单设置
            </div>
        `;
  }

  // 绑定表单事件
  function bindFormEvents(workstation) {
    // 使用事件委托，避免重复绑定
    const modalContent = elements.modalContent;

    // 移除旧的事件监听器
    modalContent.off('click.formEvents change.formEvents');

    // 使用事件委托统一处理点击和变化事件
    modalContent.on('click.formEvents', function (e) {
      const $target = $(e.target);

      // 标签页切换
      if ($target.hasClass('tab')) {
        const tab = $target.data('tab');
        $('.tab').removeClass('active');
        $('.tab-content').removeClass('active');
        $target.addClass('active');
        $(`[data-content="${tab}"]`).addClass('active');
      }
      // 保存按钮
      else if ($target.attr('id') === 'saveBtn') {
        if (saveWorkstation(workstation)) {
          closeEditModal();
          applyFiltersAndSort();
        }
      }
      // 取消按钮
      else if ($target.attr('id') === 'cancelBtn') {
        closeEditModal();
      }
      // 调整网格尺寸按钮
      else if ($target.attr('id') === 'resizeGridBtn') {
        handleGridResize(workstation);
      }
      // 添加按钮
      else if ($target.attr('id') === 'addButtonBtn') {
        if (!workstation.craft.buttons) {
          workstation.craft.buttons = [];
        }
        workstation.craft.buttons.push({
          slot: 0,
          actions: [{ type: 'left', craftCount: 1 }],
        });
        $('[data-content="craft"]').html(renderCraftForm(workstation));
        bindFormEvents(workstation);
      }
    });

    // 处理选择框变化
    modalContent.on('change.formEvents', function (e) {
      const $target = $(e.target);

      // 结构类型切换
      if ($target.attr('id') === 'structureType') {
        const type = $target.val();
        workstation.structure.type = type;
        
        // 根据新的结构类型自动更新合成触发类型
        const newCraftType = type === 'single' ? 'button' : 'interact';
        const oldCraftType = workstation.craft.type;
        
        // 如果旧的触发类型与结构不匹配，自动切换
        if ((type === 'single' && oldCraftType === 'interact') || 
            (type === 'multi' && oldCraftType === 'button')) {
          workstation.craft.type = newCraftType;
          
          // 根据类型初始化craft对象
          if (newCraftType === 'button') {
            workstation.craft = {
              type: 'button',
              buttons: [],
            };
          } else {
            workstation.craft = {
              type: 'interact',
              relativePos: { x: 0, y: 0 },
            };
          }
          
          // 重新渲染合成配置区域
          $('[data-content="craft"]').html(renderCraftForm(workstation));
        }
        
        // 重新渲染结构配置区域
        $('#structureConfig').html(
          type === 'single' ? renderSingleBlockForm(workstation) : renderMultiBlockForm(workstation)
        );
        bindFormEvents(workstation);
      }
      // 合成类型切换
      else if ($target.attr('id') === 'craftType') {
        const type = $target.val();
        workstation.craft.type = type;

        // 根据类型初始化craft对象
        if (type === 'auto') {
          workstation.craft = { type: 'auto' };
        } else if (type === 'button') {
          workstation.craft = {
            type: 'button',
            buttons: workstation.craft.buttons || [],
          };
        } else {
          workstation.craft = {
            type: 'interact',
            relativePos: workstation.craft.relativePos || { x: 0, y: 0 },
          };
        }

        // 重新渲染合成配置部分
        $('[data-content="craft"]').html(renderCraftForm(workstation));
        bindFormEvents(workstation);
      }
    });
  }

  // 全局函数:添加点击动作
  window.addAction = function (buttonIndex) {
    const buttonDiv = $(`.layer-editor[data-button="${buttonIndex}"]`);
    const actionsList = buttonDiv.find('.actions-list');
    const actionCount = actionsList.find('.action-row').length;

    const newActionHtml = `
            <div class="form-row action-row" data-action="${actionCount}">
                <div class="form-group">
                    <label class="form-label">点击类型</label>
                    <select class="form-select action-type">
                        <option value="left">左键</option>
                        <option value="right">右键</option>
                        <option value="shift_left">Shift+左键</option>
                        <option value="shift_right">Shift+右键</option>
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label">合成数量</label>
                    <input type="number" class="form-input action-count" value="1" min="1">
                </div>
                <div class="form-group flex items-end">
                    <button class="btn-danger btn-sm" onclick="removeAction(${buttonIndex}, ${actionCount})">删除</button>
                </div>
            </div>
        `;

    actionsList.append(newActionHtml);
  };

  // 全局函数:删除点击动作
  window.removeAction = async function (buttonIndex, actionIndex) {
    const buttonDiv = $(`.layer-editor[data-button="${buttonIndex}"]`);
    const actionRows = buttonDiv.find('.action-row');

    if (actionRows.length <= 1) {
      showToast('每个按钮至少需要一个点击动作', 'warning');
      return;
    }

    const confirmed = await showConfirm('确定要删除这个点击动作吗?', '删除确认');
    if (confirmed) {
      buttonDiv.find(`.action-row[data-action="${actionIndex}"]`).remove();
    }
  };

  // 保存工作站
  function saveWorkstation(workstation) {
    // 收集基本信息
    const id = $('#wsId').val().trim().toUpperCase(); // 自动转换为大写
    const name = $('#wsName').val().trim();
    const priority = parseInt($('#wsPriority').val()) || 1;
    const supported = $('#wsSupported')
      .val()
      .trim()
      .split('\n')
      .filter((s) => s.trim())
      .map((s) => s.trim().toUpperCase()); // 支持的工作站类型也转换为大写

    if (!id || !name) {
      showToast('请填写工作站ID和名称', 'warning');
      return false;
    }

    workstation.info = {
      id,
      name,
      priority,
      ...(supported.length > 0 && { supported }),
    };

    // 收集结构信息
    if (workstation.structure.type === 'single') {
      const blockId = $('#blockId').val().trim().toLowerCase(); // 方块ID转换为小写
      if (!blockId) {
        showToast('请填写方块ID', 'warning');
        return false;
      }
      workstation.structure.block = blockId;
    } else {
      // 多方块结构：直接保存完整网格布局（保留空白行列）
      
      const cells = $('.grid-cell').toArray();
      if (cells.length === 0) {
        showToast('多方块结构不能为空', 'warning');
        return false;
      }

      // 获取网格尺寸
      const gridWidth = parseInt($('#gridWidth').val()) || CONFIG.GRID.DEFAULT_WIDTH;
      const gridHeight = parseInt($('#gridHeight').val()) || CONFIG.GRID.DEFAULT_HEIGHT;

      // 验证至少有一个非空方块
      if (!GridManager.hasNonEmptyBlock(cells)) {
        showToast('多方块结构至少需要一个方块', 'warning');
        return false;
      }

      // 按层收集所有数据（包括空白）
      workstation.structure.layers = GridManager.extractLayers(gridWidth, gridHeight);
    }

    // 收集槽位信息
    const inputSlotsText = $('#inputSlotsText').val().trim();
    const outputSlotsText = $('#outputSlotsText').val().trim();

    const inputValidation = Validators.slotExpression(inputSlotsText);
    if (!inputValidation.valid) {
      showToast('输入' + inputValidation.message, 'warning');
      return false;
    }

    const outputValidation = Validators.slotExpression(outputSlotsText);
    if (!outputValidation.valid) {
      showToast('输出' + outputValidation.message, 'warning');
      return false;
    }

    const inputSlots = inputValidation.slots;
    const outputSlots = outputValidation.slots;

    workstation.input = {
      slots: inputSlots,
      unordered: $('#inputUnordered').is(':checked'),
    };

    workstation.output = {
      slots: outputSlots,
    };

    // 多方块相对位置
    if (workstation.structure.type === 'multi') {
      // 处理输入容器
      const inputValidation = Validators.specialBlockPos(
        $(CONFIG.SELECTORS.SPECIAL_INPUT), 
        '输入容器', 
        workstation.structure.layers
      );
      if (!inputValidation.valid) {
        showToast(inputValidation.message, 'warning');
        return false;
      }
      if (!inputValidation.pos) {
        showToast('多方块工作站必须设置输入容器位置', 'warning');
        return false;
      }
      workstation.input.relativePos = inputValidation.pos;

      // 处理输出容器（可选，默认使用输入容器位置）
      const outputValidation = Validators.specialBlockPos(
        $(CONFIG.SELECTORS.SPECIAL_OUTPUT), 
        '输出容器', 
        workstation.structure.layers
      );
      if (!outputValidation.valid) {
        showToast(outputValidation.message, 'warning');
        return false;
      }
      workstation.output.relativePos = outputValidation.pos || inputValidation.pos;

      // 处理交互方块（仅交互触发方式需要）
      if (workstation.craft.type === 'interact') {
        const craftValidation = Validators.specialBlockPos(
          $(CONFIG.SELECTORS.SPECIAL_CRAFT), 
          '交互方块', 
          workstation.structure.layers
        );
        if (!craftValidation.valid) {
          showToast(craftValidation.message, 'warning');
          return false;
        }
        if (!craftValidation.pos) {
          showToast('交互触发方式必须设置交互方块位置', 'warning');
          return false;
        }
        workstation.craft.relativePos = craftValidation.pos;
      }
    }

    // 收集合成配置
    const craftType = $('#craftType').val();
    if (craftType === 'auto') {
      workstation.craft = { type: 'auto' };
    } else if (craftType === 'button') {
      const buttons = [];
      $('.layer-editor[data-button]').each(function () {
        const slot = parseInt($(this).find('.button-slot').val());
        const actions = [];
        $(this)
          .find('.action-row')
          .each(function () {
            const type = $(this).find('.action-type').val();
            const craftCount = parseInt($(this).find('.action-count').val());
            if (type && craftCount) {
              actions.push({ type, craftCount });
            }
          });
        if (actions.length > 0) {
          buttons.push({ slot, actions });
        }
      });

      if (buttons.length === 0) {
        showToast('按钮触发方式至少需要一个按钮配置', 'warning');
        return false;
      }

      workstation.craft = { type: 'button', buttons };
    } else {
      // 交互类型的relativePos已在多方块部分处理
      workstation.craft = {
        type: 'interact',
        relativePos: workstation.craft.relativePos || { x: 0, y: 0 },
      };
    }

    // ============================================
    // 最终验证：使用 WorkstationValidator 进行完整的数据验证
    // 这是保存前的最后一道防线，确保数据完整性
    // ============================================
    const finalValidation = WorkstationValidator.validateWorkstation(workstation);
    if (!finalValidation.valid) {
      console.error('工作站最终验证失败:', finalValidation.errors);
      
      // 格式化错误信息以便展示
      const errorMsg = '数据验证失败:\n\n' + finalValidation.errors.map((err, i) => `${i + 1}. ${err}`).join('\n');
      
      showToast('配置验证失败，请检查所有字段', 'error');
      showConfirm(errorMsg, '数据验证失败');
      return false;
    }

    // 保存到状态
    if (state.currentEditingIndex === -1) {
      state.workstations.push(workstation);
    } else {
      state.workstations[state.currentEditingIndex] = workstation;
    }

    // 保存到localStorage（包含数组级别的验证）
    if (!saveToLocalStorage()) {
      // 如果保存失败，回滚更改
      if (state.currentEditingIndex === -1) {
        state.workstations.pop();
      } else {
        // 保存失败时的回滚逻辑
        console.warn('保存失败，但数据已经被修改');
      }
      return false;
    }

    showToast('保存成功', 'success');
    return true;
  }

  // 创建空工作站
  function createEmptyWorkstation() {
    return {
      info: {
        id: '',
        name: '',
        priority: 1,
      },
      structure: {
        type: 'single',
        block: '',
      },
      input: {
        slots: [],
        unordered: false,
      },
      output: {
        slots: [],
      },
      craft: {
        type: 'button',
        buttons: [],
      },
    };
  }

  // 全局函数供HTML调用
  window.editCell = function (layerIndex, x) {
    const cell = $(`[data-layer="${layerIndex}"][data-x="${x}"]`);
    const cellText = cell.find(CONFIG.SELECTORS.CELL_TEXT);
    const currentText = cellText.length ? cellText.text().trim() : cell.text().trim();
    const currentBlock = currentText === CONFIG.MESSAGES.EMPTY_BLOCK ? '' : currentText;

    showPrompt('输入完整方块ID (例如: minecraft:stone,留空表示删除):', currentBlock).then(
      (newBlock) => {
        if (newBlock !== null) {
          if (newBlock.trim()) {
            if (cellText.length) {
              cellText.text(newBlock.trim());
            } else {
              cell.text(newBlock.trim());
            }
            cell.removeClass('empty');
          } else {
            if (cellText.length) {
              cellText.text(CONFIG.MESSAGES.EMPTY_BLOCK);
            } else {
              cell.text(CONFIG.MESSAGES.EMPTY_BLOCK);
            }
            cell.addClass('empty');
          }
        }
      }
    );
  };

  window.showCellMenu = function (event, layerIndex, x) {
    event.preventDefault();

    // 移除已存在的菜单
    $('.context-menu').remove();

    const menu = $(`
            <div class="context-menu" style="position: fixed; left: ${event.clientX}px; top: ${event.clientY}px; z-index: 9999;">
                <div class="context-menu-item" data-action="input">设为输入容器</div>
                <div class="context-menu-item" data-action="output">设为输出容器</div>
                <div class="context-menu-item" data-action="craft">设为交互方块</div>
                <div class="context-menu-item" data-action="clear">清除特殊标记</div>
            </div>
        `);

    $('body').append(menu);

    // 点击菜单项 - 使用one()确保只触发一次
    menu.find('.context-menu-item').one('click', function () {
      const action = $(this).data('action');
      handleCellAction(action, layerIndex, x);
      menu.remove();
    });

    // 点击其他地方关闭菜单
    setTimeout(() => {
      $(document).one('click', () => menu.remove());
    }, 100);
  };

  function handleCellAction(action, layerIndex, x) {
    const cell = $(`[data-layer="${layerIndex}"][data-x="${x}"]`);

    // 使用配置中的特殊标记类型
    const markConfig = CONFIG.BADGE_TYPES;

    if (action === 'clear') {
      // 清除所有特殊标记
      cell.removeClass('special-input special-output special-craft').find('.cell-badge').remove();
      
      // 如果清除的是输出标记，且有输入标记，则输出应该自动跟随输入
      const hasInput = $(CONFIG.SELECTORS.SPECIAL_INPUT).length > 0;
      const hasOutput = $(CONFIG.SELECTORS.SPECIAL_OUTPUT).length > 0;
      
      if (!hasOutput && hasInput) {
        // 没有显式的输出标记时，输出自动跟随输入
        $(CONFIG.SELECTORS.SPECIAL_INPUT).addClass('special-output');
      }
    } else if (markConfig[action]) {
      const config = markConfig[action];
      
      // 移除所有同类标记的class
      $(`.${config.class}`).removeClass(config.class);
      
      // 清除当前方块的所有标记class
      cell.removeClass('special-input special-output special-craft');
      
      // 添加新标记class
      cell.addClass(config.class);
      
      // 处理输入输出的默认跟随逻辑
      const hasInput = $(CONFIG.SELECTORS.SPECIAL_INPUT).length > 0;
      const hasOutput = $(CONFIG.SELECTORS.SPECIAL_OUTPUT).length > 0;
      
      if (action === 'output') {
        // 设置了显式的输出位置，需要移除输入位置上的输出标记（如果之前是默认跟随）
        // 不需要额外处理，因为上面已经移除了所有同类标记
      } else if (action === 'input') {
        // 设置了输入，如果没有显式的输出，则输出自动跟随输入
        if (!hasOutput) {
          cell.addClass('special-output');
        }
      }
    }
    
    // 统一重新渲染所有方块的badge
    $(CONFIG.SELECTORS.GRID_CELL).each(function() {
      const $cell = $(this);
      const hasInput = $cell.hasClass('special-input');
      const hasOutput = $cell.hasClass('special-output');
      const hasCraft = $cell.hasClass('special-craft');
      
      // 清除所有badge
      $cell.find(CONFIG.SELECTORS.CELL_BADGE).remove();
      
      // 生成并添加badge
      const badgeHtml = generateCellBadges(hasInput, hasOutput, hasCraft);
      if (badgeHtml) {
        $cell.prepend(badgeHtml);
      }
    });
  }

  window.removeButton = async function (buttonIndex) {
    const totalButtons = $('.layer-editor[data-button]').length;

    if (totalButtons <= 1) {
      showToast('按钮触发方式至少需要一个按钮配置', 'warning');
      return;
    }

    const confirmed = await showConfirm('确定要删除这个按钮吗?', '删除确认');
    if (confirmed) {
      $(`.layer-editor[data-button="${buttonIndex}"]`).remove();
    }
  };

  // 模态框控制（统一处理）
  const modalControl = {
    open: (modal) => elements[modal].removeClass('hidden'),
    close: (modal) => elements[modal].addClass('hidden'),
  };

  const openEditModal = () => modalControl.open('editModal');
  const closeEditModal = () => modalControl.close('editModal');
  const openJsonModal = () => modalControl.open('jsonModal');
  const closeJsonModal = () => modalControl.close('jsonModal');

  // 启动应用
  $(document).ready(init);
})();
