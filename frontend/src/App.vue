<template>
  <router-view />
</template>

<style>
/* ========================================
   服装追溯系统 — 全局设计令牌
   设计理念：工业精准，如光线充足的裁片车间
   ======================================== */

:root {
  /* 色彩 */
  --color-sidebar: #1b1f2d;
  --color-sidebar-hover: #252a3a;
  --color-sidebar-active: #2d3448;
  --color-bg: #f2f3f6;
  --color-surface: #ffffff;
  --color-primary: #3d5980;
  --color-primary-light: #e8edf5;
  --color-text: #1d1f24;
  --color-text-secondary: #6d6f78;
  --color-border: #e5e6ea;
  --color-success: #4d8049;
  --color-warning: #c48138;
  --color-danger: #c45050;
  --color-info: #50789e;

  /* 字体 */
  --font-family: -apple-system, "PingFang SC", "Microsoft YaHei", "Helvetica Neue", sans-serif;
  --font-size-xs: 12px;
  --font-size-sm: 13px;
  --font-size-base: 14px;
  --font-size-lg: 16px;
  --font-size-xl: 18px;
  --font-size-2xl: 22px;

  /* 间距 */
  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;

  /* 圆角 */
  --radius-sm: 2px;
  --radius-md: 4px;
  --radius-lg: 6px;

  /* 阴影（仅用于必要场景） */
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.04);
  --shadow-md: 0 2px 8px rgba(0, 0, 0, 0.06);
}

/* 全局重置 */
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: var(--font-family);
  font-size: var(--font-size-base);
  color: var(--color-text);
  background: var(--color-bg);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* Element Plus 主题覆盖 */
:root {
  --el-color-primary: var(--color-primary);
  --el-color-primary-light-3: #5170a1;
  --el-color-primary-light-5: #6b87b4;
  --el-color-primary-light-7: #92a8c9;
  --el-color-primary-light-8: #a8b9d5;
  --el-color-primary-light-9: #c2cde0;
  --el-color-primary-dark-2: #334a6b;
  --el-color-success: var(--color-success);
  --el-color-warning: var(--color-warning);
  --el-color-danger: var(--color-danger);
  --el-color-info: var(--color-info);
  --el-font-family: var(--font-family);
  --el-border-radius-base: var(--radius-md);
  --el-border-color: var(--color-border);
  --el-bg-color: var(--color-bg);
  --el-bg-color-overlay: var(--color-surface);
  --el-text-color-primary: var(--color-text);
  --el-text-color-regular: var(--color-text-secondary);
  --el-fill-color-blank: var(--color-surface);
  --el-fill-color-light: #f7f8fa;
  --el-box-shadow-light: var(--shadow-md);
  --el-box-shadow: var(--shadow-sm);
}

/* 通用页面容器 */
.page-container {
  padding: 0;
  max-width: 1400px;
}

.page-container h2 {
  margin: 0 0 var(--spacing-lg);
  font-size: var(--font-size-2xl);
  font-weight: 600;
  color: var(--color-text);
  letter-spacing: -0.01em;
}

/* 搜索卡片 */
.search-card {
  border: 1px solid var(--color-border) !important;
  border-radius: var(--radius-lg) !important;
  box-shadow: none !important;
}
.search-card .el-card__body {
  padding: var(--spacing-md) var(--spacing-lg) !important;
}
.search-card .el-form {
  margin-bottom: 0;
}
.search-card .el-form-item {
  margin-bottom: 0;
}

/* 表格卡片 */
.table-card {
  border: 1px solid var(--color-border) !important;
  border-radius: var(--radius-lg) !important;
  box-shadow: none !important;
}
.table-card .el-card__body {
  padding: var(--spacing-lg) !important;
}

/* 工具条 */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-md);
}
.toolbar-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

/* 状态标签覆盖 */
.el-tag {
  font-weight: 500;
  border: none;
}
.el-tag--small {
  padding: 0 6px;
  font-size: var(--font-size-xs);
  line-height: 20px;
  height: 20px;
}

/* 表格样式覆盖 */
.el-table {
  --el-table-border-color: var(--color-border);
  --el-table-header-bg-color: #f8f9fb;
  font-size: var(--font-size-sm);
}
.el-table th.el-table__cell {
  color: var(--color-text-secondary);
  font-weight: 600;
  font-size: var(--font-size-xs);
  letter-spacing: 0.02em;
  text-transform: none;
  border-bottom: 1px solid var(--color-border);
}
.el-table .el-table__row:hover > td {
  background-color: #f8f9fb;
}

/* 对话框样式 */
.el-dialog {
  border-radius: var(--radius-lg);
  --el-dialog-box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}
.el-dialog__header {
  padding: var(--spacing-lg) var(--spacing-lg) var(--spacing-md);
  border-bottom: 1px solid var(--color-border);
  margin: 0;
}
.el-dialog__title {
  font-size: var(--font-size-lg);
  font-weight: 600;
}
.el-dialog__body {
  padding: var(--spacing-lg);
}
.el-dialog__footer {
  padding: var(--spacing-md) var(--spacing-lg);
  border-top: 1px solid var(--color-border);
}

/* 表单标签 */
.el-form-item__label {
  font-weight: 500;
  color: var(--color-text);
}

/* 分页 */
.el-pagination {
  --el-pagination-button-bg-color: var(--color-surface);
}

/* 按钮 */
.el-button--primary {
  --el-button-bg-color: var(--color-primary);
  --el-button-border-color: var(--color-primary);
}

/* 空状态提示 */
.placeholder-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: var(--color-text-secondary);
}
.placeholder-page .icon-box {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-lg);
  background: var(--color-primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--spacing-lg);
}
.placeholder-page h3 {
  font-size: var(--font-size-lg);
  font-weight: 500;
  margin: 0 0 var(--spacing-sm);
  color: var(--color-text);
}
.placeholder-page p {
  margin: 0;
  font-size: var(--font-size-sm);
}
</style>