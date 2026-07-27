// 工作站配置数据验证器
(function () {
  'use strict';

  /**
   * 验证工作站配置数据
   * @param {Object} workstation - 工作站配置对象
   * @returns {Object} { valid: boolean, errors: string[] }
   */
  function validateWorkstation(workstation) {
    const errors = [];

    // 验证基本信息
    if (!workstation.info) {
      errors.push('缺少基本信息(info)');
      return { valid: false, errors };
    }

    // 验证ID（支持大小写，保存时会自动转换为大写）
    if (
      !workstation.info.id ||
      typeof workstation.info.id !== 'string' ||
      !workstation.info.id.trim()
    ) {
      errors.push('工作站ID不能为空');
    } else if (!/^[A-Za-z_]+:[A-Za-z_\-]+$/i.test(workstation.info.id)) {
      errors.push('工作站ID格式不正确，应为"命名空间:ID"格式（如SLIMEFUN:QUANTUM-WORKBENCH）');
    }

    // 验证名称
    if (
      !workstation.info.name ||
      typeof workstation.info.name !== 'string' ||
      !workstation.info.name.trim()
    ) {
      errors.push('工作站名称不能为空');
    }

    // 验证优先级
    if (workstation.info.priority !== undefined) {
      if (typeof workstation.info.priority !== 'number' || workstation.info.priority < 1) {
        errors.push('优先级必须是大于0的数字');
      }
    }

    // 验证supported（可选）
    if (workstation.info.supported !== undefined) {
      if (!Array.isArray(workstation.info.supported)) {
        errors.push('supported必须是数组');
      } else if (workstation.info.supported.some((s) => typeof s !== 'string' || !s.trim())) {
        errors.push('supported数组中包含无效的工作站类型');
      }
    }

    // 验证结构配置
    if (!workstation.structure) {
      errors.push('缺少结构配置(structure)');
      return { valid: false, errors };
    }

    if (!['single', 'multi'].includes(workstation.structure.type)) {
      errors.push('结构类型必须是"single"或"multi"');
    }

    // 验证单方块结构（支持大小写，保存时会自动转换为小写）
    if (workstation.structure.type === 'single') {
      if (!workstation.structure.block || typeof workstation.structure.block !== 'string') {
        errors.push('单方块结构必须指定block属性');
      } else if (!/^[A-Za-z_]+:[A-Za-z0-9_\-]+$/i.test(workstation.structure.block)) {
        errors.push('方块ID格式不正确，应为"命名空间:id"格式（如minecraft:stone）');
      }
    }

    // 验证多方块结构
    if (workstation.structure.type === 'multi') {
      if (!workstation.structure.layers || !Array.isArray(workstation.structure.layers)) {
        errors.push('多方块结构必须包含layers数组');
      } else if (workstation.structure.layers.length === 0) {
        errors.push('多方块结构至少需要一层');
      } else {
        // 验证整个结构至少有一个非空方块
        let hasNonNullBlock = false;
        
        // 验证每一层
        workstation.structure.layers.forEach((layer, layerIndex) => {
          if (!Array.isArray(layer)) {
            errors.push(`第${layerIndex}层不是数组`);
          } else if (layer.length === 0) {
            errors.push(`第${layerIndex}层数组长度不能为0`);
          } else {
            // 验证每个方块ID（支持大小写，保存时会自动转换为小写）
            layer.forEach((block, x) => {
              if (block !== null) {
                hasNonNullBlock = true;
                if (typeof block !== 'string' || !/^[A-Za-z_]+:[A-Za-z0-9_\-]+$/i.test(block)) {
                  errors.push(`第${layerIndex}层位置${x}的方块ID格式不正确`);
                }
              }
            });
          }
        });
        
        // 整个结构必须至少有一个非空方块
        if (!hasNonNullBlock) {
          errors.push('多方块结构至少需要一个非空方块');
        }
      }
    }

    // 验证输入配置
    if (!workstation.input) {
      errors.push('缺少输入配置(input)');
    } else {
      if (!Array.isArray(workstation.input.slots)) {
        errors.push('输入槽位(input.slots)必须是数组');
      } else if (workstation.input.slots.length === 0) {
        errors.push('输入槽位不能为空');
      } else if (workstation.input.slots.some((s) => typeof s !== 'number' || s < 0)) {
        errors.push('输入槽位必须是非负整数');
      }

      if (
        workstation.input.unordered !== undefined &&
        typeof workstation.input.unordered !== 'boolean'
      ) {
        errors.push('input.unordered必须是布尔值');
      }

      // 多方块必须有relativePos
      if (workstation.structure.type === 'multi') {
        if (!workstation.input.relativePos) {
          errors.push('多方块结构必须指定输入容器位置(input.relativePos)');
        } else if (!validateRelativePos(workstation.input.relativePos)) {
          errors.push('输入容器位置格式不正确');
        }
      }
    }

    // 验证输出配置
    if (!workstation.output) {
      errors.push('缺少输出配置(output)');
    } else {
      if (!Array.isArray(workstation.output.slots)) {
        errors.push('输出槽位(output.slots)必须是数组');
      } else if (workstation.output.slots.length === 0) {
        errors.push('输出槽位不能为空');
      } else if (workstation.output.slots.some((s) => typeof s !== 'number' || s < 0)) {
        errors.push('输出槽位必须是非负整数');
      }

      // 多方块可选relativePos
      if (workstation.structure.type === 'multi' && workstation.output.relativePos) {
        if (!validateRelativePos(workstation.output.relativePos)) {
          errors.push('输出容器位置格式不正确');
        }
      }
    }

    // 验证合成配置
    if (!workstation.craft) {
      errors.push('缺少合成配置(craft)');
    } else {
      if (!['auto', 'button', 'interact'].includes(workstation.craft.type)) {
        errors.push('合成类型必须是"auto"、"button"或"interact"');
      }

      // 验证按钮触发配置
      if (workstation.craft.type === 'button') {
        if (!Array.isArray(workstation.craft.buttons)) {
          errors.push('按钮触发方式必须包含buttons数组');
        } else if (workstation.craft.buttons.length === 0) {
          errors.push('按钮触发方式至少需要一个按钮配置');
        } else {
          workstation.craft.buttons.forEach((btn, btnIndex) => {
            if (typeof btn.slot !== 'number' || btn.slot < 0) {
              errors.push(`按钮${btnIndex + 1}的槽位必须是非负整数`);
            }

            if (!Array.isArray(btn.actions)) {
              errors.push(`按钮${btnIndex + 1}必须包含actions数组`);
            } else if (btn.actions.length === 0) {
              errors.push(`按钮${btnIndex + 1}至少需要一个点击动作`);
            } else {
              btn.actions.forEach((action, actionIndex) => {
                if (!['left', 'right', 'shift_left', 'shift_right'].includes(action.type)) {
                  errors.push(`按钮${btnIndex + 1}动作${actionIndex + 1}的类型无效`);
                }
                if (typeof action.craftCount !== 'number' || action.craftCount < 1) {
                  errors.push(`按钮${btnIndex + 1}动作${actionIndex + 1}的合成数量必须是正整数`);
                }
              });
            }
          });
        }
      }

      // 验证交互触发配置
      if (workstation.craft.type === 'interact') {
        if (workstation.structure.type === 'multi') {
          if (!workstation.craft.relativePos) {
            errors.push('交互触发方式必须指定交互方块位置(craft.relativePos)');
          } else if (!validateRelativePos(workstation.craft.relativePos)) {
            errors.push('交互方块位置格式不正确');
          }
        }
      }
    }

    // 验证选择器配置（可选）
    if (workstation.selector) {
      if (workstation.selector.type !== 'switch') {
        errors.push('选择器类型必须是"switch"');
      }
      if (typeof workstation.selector.prev !== 'number' || workstation.selector.prev < 0) {
        errors.push('选择器prev槽位必须是非负整数');
      }
      if (typeof workstation.selector.next !== 'number' || workstation.selector.next < 0) {
        errors.push('选择器next槽位必须是非负整数');
      }
      if (typeof workstation.selector.display !== 'number' || workstation.selector.display < 0) {
        errors.push('选择器display槽位必须是非负整数');
      }
    }

    return {
      valid: errors.length === 0,
      errors,
    };
  }

  /**
   * 验证相对位置对象
   * @param {Object} pos - 相对位置对象
   * @returns {boolean}
   */
  function validateRelativePos(pos) {
    return (
      pos && typeof pos.x === 'number' && typeof pos.y === 'number' && pos.x >= 0 && pos.y >= 0
    );
  }

  /**
   * 验证工作站数组
   * @param {Array} workstations - 工作站配置数组
   * @returns {Object} { valid: boolean, errors: Object[] }
   */
  function validateWorkstations(workstations) {
    if (!Array.isArray(workstations)) {
      return {
        valid: false,
        errors: [{ index: -1, errors: ['数据必须是数组'] }],
      };
    }

    const allErrors = [];
    const ids = new Set();

    workstations.forEach((ws, index) => {
      const result = validateWorkstation(ws);
      if (!result.valid) {
        allErrors.push({
          index,
          id: ws.info?.id || '未知',
          name: ws.info?.name || '未知',
          errors: result.errors,
        });
      }

      // 检查ID重复
      if (ws.info?.id) {
        if (ids.has(ws.info.id)) {
          allErrors.push({
            index,
            id: ws.info.id,
            name: ws.info?.name || '未知',
            errors: [`工作站ID"${ws.info.id}"重复`],
          });
        }
        ids.add(ws.info.id);
      }
    });

    return {
      valid: allErrors.length === 0,
      errors: allErrors,
    };
  }

  /**
   * 格式化验证错误信息
   * @param {Object} validationResult - 验证结果
   * @returns {string}
   */
  function formatValidationErrors(validationResult) {
    if (validationResult.valid) {
      return '';
    }

    let message = '数据验证失败:\n\n';
    validationResult.errors.forEach((item) => {
      if (item.index === -1) {
        message += `全局错误:\n`;
      } else {
        message += `工作站 #${item.index + 1} (${item.name}):\n`;
      }
      item.errors.forEach((err) => {
        message += `  - ${err}\n`;
      });
      message += '\n';
    });

    return message;
  }

  // 导出到全局
  window.WorkstationValidator = {
    validateWorkstation,
    validateWorkstations,
    formatValidationErrors,
  };
})();
