// 弹窗和提示工具函数
(function () {
  'use strict';

  // Toast提示函数
  function showToast(message, type = 'info', duration = 3000) {
    const container = $('#toastContainer');
    const icons = {
      success:
        '<svg class="toast-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>',
      error:
        '<svg class="toast-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>',
      warning:
        '<svg class="toast-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path></svg>',
      info: '<svg class="toast-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>',
    };

    const toast = $(`
            <div class="toast ${type}">
                ${icons[type]}
                <span class="toast-message">${message}</span>
            </div>
        `);

    container.append(toast);

    setTimeout(() => {
      toast.addClass('removing');
      setTimeout(() => toast.remove(), 300);
    }, duration);
  }

  // 确认对话框函数
  function showConfirm(message, title = '确认操作') {
    return new Promise((resolve) => {
      const modal = $('#confirmModal');
      $('#confirmTitle').text(title);
      $('#confirmMessage').text(message);
      modal.removeClass('hidden');

      const handleOk = () => {
        modal.addClass('hidden');
        cleanup();
        resolve(true);
      };

      const handleCancel = () => {
        modal.addClass('hidden');
        cleanup();
        resolve(false);
      };

      const cleanup = () => {
        $('#confirmOk').off('click', handleOk);
        $('#confirmCancel').off('click', handleCancel);
        modal.off('click', handleClickOutside);
      };

      const handleClickOutside = (e) => {
        if (e.target === modal[0]) {
          handleCancel();
        }
      };

      $('#confirmOk').on('click', handleOk);
      $('#confirmCancel').on('click', handleCancel);
      modal.on('click', handleClickOutside);
    });
  }

  // 输入对话框函数
  function showPrompt(message, defaultValue = '') {
    return new Promise((resolve) => {
      const modal = $('#promptModal');
      $('#promptTitle').text(message);
      $('#promptInput').val(defaultValue);
      modal.removeClass('hidden');

      // 聚焦输入框并选中文本
      setTimeout(() => {
        const input = $('#promptInput')[0];
        input.focus();
        input.select();
      }, 100);

      const handleOk = () => {
        const value = $('#promptInput').val();
        modal.addClass('hidden');
        cleanup();
        resolve(value);
      };

      const handleCancel = () => {
        modal.addClass('hidden');
        cleanup();
        resolve(null);
      };

      const handleEnter = (e) => {
        if (e.key === 'Enter') {
          handleOk();
        } else if (e.key === 'Escape') {
          handleCancel();
        }
      };

      const cleanup = () => {
        $('#promptOk').off('click', handleOk);
        $('#promptCancel').off('click', handleCancel);
        $('#promptInput').off('keydown', handleEnter);
        modal.off('click', handleClickOutside);
      };

      const handleClickOutside = (e) => {
        if (e.target === modal[0]) {
          handleCancel();
        }
      };

      $('#promptOk').on('click', handleOk);
      $('#promptCancel').on('click', handleCancel);
      $('#promptInput').on('keydown', handleEnter);
      modal.on('click', handleClickOutside);
    });
  }

  // 导出到全局
  window.showToast = showToast;
  window.showConfirm = showConfirm;
  window.showPrompt = showPrompt;
})();
