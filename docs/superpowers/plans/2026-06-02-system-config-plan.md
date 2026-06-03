# 系统配置管理功能 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为管理后台添加系统配置管理功能，支持按分类管理业务参数（基本设置、SEO、邮件、订单、上传等）

**Architecture:** 在 auth-service 中新增 SystemConfig 实体和 CRUD 控制器，使用 MyBatis-Plus 操作 `system_config` 表，Redis 缓存配置值。前端 admin-frontend 新增配置管理页面，按 Tab 分类展示配置项。

**Tech Stack:** Spring Boot 3.2.5, MyBatis-Plus, Redis, Vue 3 + Element Plus

---

### Task 1: 数据库迁移 — system_config 建表和预置数据

**Files:**
- Modify: `deploy/mysql/init.sql`

- [ ] **Step 1: 追加建表 SQL 和预置数据到 init.sql**

在 `deploy/mysql/init.sql` 末尾追加：

```sql
-- ============================================================
-- 系统配置表
-- ============================================================
USE `mall_auth`;

CREATE TABLE IF NOT EXISTS `system_config` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(50) NOT NULL COMMENT '分类: basic/seo/email/order/upload/payment 等',
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    description VARCHAR(255) COMMENT '配置说明',
    data_type VARCHAR(20) NOT NULL DEFAULT 'string' COMMENT '值类型: string/number/boolean/image/password/textarea',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    created_time DATETIME NOT NULL,
    updated_time DATETIME NOT NULL,
    INDEX idx_category (category),
    INDEX idx_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 预置数据
INSERT INTO `system_config` (category, config_key, config_value, description, data_type, sort_order, created_time, updated_time) VALUES
('basic', 'site_name', 'Warmart', '网站名称', 'string', 1, NOW(), NOW()),
('basic', 'site_logo', '/logo.png', '网站 Logo', 'image', 2, NOW(), NOW()),
('basic', 'site_icp', '京ICP备xxx号', 'ICP 备案号', 'string', 3, NOW(), NOW()),
('basic', 'site_copyright', '©2026 Warmart', '版权信息', 'string', 4, NOW(), NOW()),
('basic', 'customer_phone', '400-xxx-xxxx', '客服电话', 'string', 5, NOW(), NOW()),
('basic', 'customer_email', 'support@xxx.com', '客服邮箱', 'string', 6, NOW(), NOW()),
('basic', 'customer_address', '北京市朝阳区xxx', '公司地址', 'string', 7, NOW(), NOW()),
('seo', 'seo_title', 'Warmart - 温暖你的生活', '首页标题', 'string', 1, NOW(), NOW()),
('seo', 'seo_keywords', '电商,购物,温暖', '首页关键词', 'string', 2, NOW(), NOW()),
('seo', 'seo_description', '精选全球好物', '首页描述', 'textarea', 3, NOW(), NOW()),
('seo', 'seo_analytics', '', '统计代码（百度/GA）', 'textarea', 4, NOW(), NOW()),
('email', 'dm_access_key_id', '', '阿里云 AccessKey ID', 'password', 1, NOW(), NOW()),
('email', 'dm_access_key_secret', '', '阿里云 AccessKey Secret', 'password', 2, NOW(), NOW()),
('email', 'dm_account_name', '', '发信地址', 'string', 3, NOW(), NOW()),
('order', 'auto_confirm_days', '7', '自动确认收货天数', 'number', 1, NOW(), NOW()),
('order', 'auto_review_days', '15', '自动好评天数', 'number', 2, NOW(), NOW()),
('order', 'refund_timeout_hours', '72', '退款超时时间（小时）', 'number', 3, NOW(), NOW()),
('upload', 'max_file_size', '10', '最大文件上传（MB）', 'number', 1, NOW(), NOW()),
('upload', 'allowed_image_types', 'jpg,png,gif,webp', '允许图片格式', 'string', 2, NOW(), NOW());
```

先用 Read 读取文件末尾，然后用 Edit 追加。

---

### Task 2: 创建 SystemConfig 实体

**Files:**
- Create: `backend/auth-service/src/main/java/com/mall/auth/entity/SystemConfig.java`

- [ ] **Step 1: 创建 SystemConfig.java**

```java
package com.mall.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("system_config")
public class SystemConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String category;
    private String configKey;
    private String configValue;
    private String description;
    private String dataType;
    private Integer sortOrder;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
```

- [ ] **Step 2: 验证编译**

```bash
cd d:/project/webwork/backend && mvn compile -pl auth-service -am -q
```
Expected: BUILD SUCCESS

---

### Task 3: 创建 SystemConfigMapper

**Files:**
- Create: `backend/auth-service/src/main/java/com/mall/auth/mapper/SystemConfigMapper.java`

- [ ] **Step 1: 创建 SystemConfigMapper.java**

```java
package com.mall.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.auth.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {
}
```

- [ ] **Step 2: 验证编译**

```bash
cd d:/project/webwork/backend && mvn compile -pl auth-service -am -q
```
Expected: BUILD SUCCESS

---

### Task 4: 创建 SystemConfigService

**Files:**
- Create: `backend/auth-service/src/main/java/com/mall/auth/service/SystemConfigService.java`

- [ ] **Step 1: 创建 SystemConfigService.java**

```java
package com.mall.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.auth.entity.SystemConfig;
import com.mall.auth.mapper.SystemConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigMapper configMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取所有配置，按 category 和 sort_order 排序
     */
    public List<SystemConfig> listAll() {
        return configMapper.selectList(
                new LambdaQueryWrapper<SystemConfig>()
                        .orderByAsc(SystemConfig::getCategory)
                        .orderByAsc(SystemConfig::getSortOrder));
    }

    /**
     * 按分类获取配置
     */
    public List<SystemConfig> getByCategory(String category) {
        return configMapper.selectList(
                new LambdaQueryWrapper<SystemConfig>()
                        .eq(SystemConfig::getCategory, category)
                        .orderByAsc(SystemConfig::getSortOrder));
    }

    /**
     * 获取所有分类列表
     */
    public List<String> getCategories() {
        return configMapper.selectList(
                        new LambdaQueryWrapper<SystemConfig>()
                                .select(SystemConfig::getCategory)
                                .groupBy(SystemConfig::getCategory)
                                .orderByAsc(SystemConfig::getCategory))
                .stream()
                .map(SystemConfig::getCategory)
                .collect(Collectors.toList());
    }

    /**
     * 批量更新配置
     */
    @Transactional
    public void batchUpdate(List<SystemConfig> configs) {
        for (SystemConfig config : configs) {
            config.setUpdatedTime(LocalDateTime.now());
            configMapper.updateById(config);
            // 清除缓存
            redisTemplate.delete("auth:config:" + config.getConfigKey());
        }
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
cd d:/project/webwork/backend && mvn compile -pl auth-service -am -q
```
Expected: BUILD SUCCESS

---

### Task 5: 创建 ConfigHolder 工具类

**Files:**
- Create: `backend/auth-service/src/main/java/com/mall/auth/service/ConfigHolder.java`

- [ ] **Step 1: 创建 ConfigHolder.java**

```java
package com.mall.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.auth.entity.SystemConfig;
import com.mall.auth.mapper.SystemConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 配置读取工具类。
 * 业务代码通过此类读取系统配置，优先从 Redis 缓存获取。
 *
 * 使用示例：
 *   ConfigHolder configHolder;
 *   String siteName = configHolder.get("site_name", "Warmart");
 *   int confirmDays = configHolder.getInt("auto_confirm_days", 7);
 */
@Component
public class ConfigHolder {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SystemConfigMapper configMapper;

    public String get(String key) {
        String cacheKey = "auth:config:" + key;
        String val = (String) redisTemplate.opsForValue().get(cacheKey);
        if (val != null) return val;

        SystemConfig config = configMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>()
                        .eq(SystemConfig::getConfigKey, key));
        if (config == null) return null;

        redisTemplate.opsForValue().set(cacheKey, config.getConfigValue());
        return config.getConfigValue();
    }

    public String get(String key, String defaultValue) {
        String val = get(key);
        return val != null ? val : defaultValue;
    }

    public Integer getInt(String key, Integer defaultValue) {
        String val = get(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        String val = get(key);
        return val != null ? Boolean.parseBoolean(val) : defaultValue;
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
cd d:/project/webwork/backend && mvn compile -pl auth-service -am -q
```
Expected: BUILD SUCCESS

---

### Task 6: 创建 SystemConfigController

**Files:**
- Create: `backend/auth-service/src/main/java/com/mall/auth/controller/SystemConfigController.java`

- [ ] **Step 1: 创建 SystemConfigController.java**

```java
package com.mall.auth.controller;

import com.mall.auth.common.Result;
import com.mall.auth.entity.SystemConfig;
import com.mall.auth.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth/admin/config")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService configService;

    @GetMapping("/list")
    public Result<List<SystemConfig>> listAll() {
        return Result.success(configService.listAll());
    }

    @GetMapping("/category/{category}")
    public Result<List<SystemConfig>> getByCategory(@PathVariable String category) {
        return Result.success(configService.getByCategory(category));
    }

    @GetMapping("/categories")
    public Result<List<String>> getCategories() {
        return Result.success(configService.getCategories());
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody List<SystemConfig> configs) {
        configService.batchUpdate(configs);
        return Result.success(null);
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
cd d:/project/webwork/backend && mvn compile -pl auth-service -am -q
```
Expected: BUILD SUCCESS

---

### Task 7: 前端 API — system.ts

**Files:**
- Create: `admin-frontend/src/api/system.ts`

- [ ] **Step 1: 创建 system.ts**

```typescript
import request from './request'

export interface SystemConfig {
  id: number
  category: string
  configKey: string
  configValue: string
  description: string
  dataType: string
  sortOrder: number
}

export function getConfigList() {
  return request.get<any, SystemConfig[]>('/auth/admin/config/list')
}

export function getConfigByCategory(category: string) {
  return request.get<any, SystemConfig[]>(`/auth/admin/config/category/${category}`)
}

export function getConfigCategories() {
  return request.get<any, string[]>('/auth/admin/config/categories')
}

export function updateConfigs(configs: SystemConfig[]) {
  return request.put<void>('/auth/admin/config/update', configs)
}
```

---

### Task 8: 前端页面 — SystemSettingsView.vue

**Files:**
- Create: `admin-frontend/src/views/SystemSettingsView.vue`

- [ ] **Step 1: 创建 SystemSettingsView.vue**

```vue
<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getConfigList, updateConfigs, type SystemConfig } from '@/api/system'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const configs = ref<SystemConfig[]>([])
const loading = ref(false)
const saving = ref(false)
const activeTab = ref('basic')

const categories = computed(() => {
  const cats = new Map<string, SystemConfig[]>()
  for (const cfg of configs.value) {
    if (!cats.has(cfg.category)) cats.set(cfg.category, [])
    cats.get(cfg.category)!.push(cfg)
  }
  return Array.from(cats.entries()).map(([category, items]) => ({
    category,
    label: categoryLabels[category] || category,
    items
  }))
})

const currentCategoryItems = computed(() => {
  return categories.value.find(c => c.category === activeTab.value)?.items || []
})

const categoryLabels: Record<string, string> = {
  basic: '基本设置',
  seo: 'SEO 设置',
  email: '邮件配置',
  order: '订单配置',
  upload: '上传配置'
}

const categoryIcons: Record<string, string> = {
  basic: 'InfoFilled',
  seo: 'Search',
  email: 'Message',
  order: 'Tickets',
  upload: 'Upload'
}

async function fetchConfigs() {
  loading.value = true
  try {
    configs.value = await getConfigList()
  } catch { /* handled */ } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    await updateConfigs(currentCategoryItems.value)
    ElMessage.success('配置保存成功')
  } catch { /* handled */ } finally {
    saving.value = false
  }
}

function getConfigValue(key: string): string {
  const cfg = configs.value.find(c => c.configKey === key)
  return cfg?.configValue ?? ''
}

onMounted(() => {
  fetchConfigs()
})
</script>

<template>
  <div class="config-page">
    <div class="config-page__header">
      <h2 class="config-page__title">系统配置</h2>
    </div>

    <el-card v-loading="loading" shadow="never" class="config-card">
      <el-tabs v-model="activeTab" tab-position="left" class="config-tabs">
        <el-tab-pane
          v-for="cat in categories"
          :key="cat.category"
          :label="cat.label"
          :name="cat.category"
        >
          <div class="config-section">
            <div class="config-section__header">
              <h3>{{ cat.label }}</h3>
            </div>
            <el-form label-width="140px" class="config-form">
              <el-form-item
                v-for="item in cat.items"
                :key="item.id"
                :label="item.description"
              >
                <!-- 字符串输入 -->
                <el-input
                  v-if="item.dataType === 'string'"
                  v-model="item.configValue"
                  :placeholder="'请输入' + item.description"
                  clearable
                />
                <!-- 数字输入 -->
                <el-input-number
                  v-else-if="item.dataType === 'number'"
                  v-model="item.configValue"
                  :min="0"
                  :max="99999"
                  controls-position="right"
                  style="width: 200px"
                  @update:model-value="(val: any) => item.configValue = String(val)"
                />
                <!-- 开关 -->
                <el-switch
                  v-else-if="item.dataType === 'boolean'"
                  v-model="item.configValue"
                  :active-value="'true'"
                  :inactive-value="'false'"
                />
                <!-- 密码 -->
                <el-input
                  v-else-if="item.dataType === 'password'"
                  v-model="item.configValue"
                  type="password"
                  show-password
                  :placeholder="'请输入' + item.description"
                  clearable
                />
                <!-- 多行文本 -->
                <el-input
                  v-else-if="item.dataType === 'textarea'"
                  v-model="item.configValue"
                  type="textarea"
                  :rows="3"
                  :placeholder="'请输入' + item.description"
                />
                <!-- 图片上传（简化版：输入 URL） -->
                <div v-else-if="item.dataType === 'image'" class="config-image-wrap">
                  <el-input
                    v-model="item.configValue"
                    placeholder="请输入图片 URL"
                    clearable
                    style="flex:1"
                  />
                  <el-upload
                    class="config-image-upload"
                    action="/api/product/comment/upload-image"
                    :show-file-list="false"
                    :on-success="(res: any) => { item.configValue = res.data || res.url }"
                  >
                    <el-button type="primary" size="small">上传</el-button>
                  </el-upload>
                  <el-image
                    v-if="item.configValue"
                    :src="item.configValue"
                    class="config-image-preview"
                    fit="cover"
                  />
                </div>
                <!-- 兜底 -->
                <el-input v-else v-model="item.configValue" />
              </el-form-item>
            </el-form>
            <div class="config-actions">
              <el-button type="primary" :loading="saving" @click="handleSave">
                保存配置
              </el-button>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.config-page {
  padding: 24px;
}
.config-page__header {
  margin-bottom: 24px;
}
.config-page__title {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.config-card {
  border-radius: 8px;
}
.config-tabs {
  min-height: 400px;
}
.config-section__header {
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}
.config-section__header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.config-form {
  max-width: 700px;
}
.config-actions {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}
.config-image-wrap {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.config-image-upload {
  flex-shrink: 0;
}
.config-image-preview {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
}
</style>
```

---

### Task 9: 前端路由 + 菜单

**Files:**
- Modify: `admin-frontend/src/router/index.ts`
- Modify: `admin-frontend/src/layouts/AdminLayout.vue`

- [ ] **Step 1: 路由新增 /system/config**

在 `admin-frontend/src/router/index.ts` 中，在 `feedbacks` 路由之后增加：

```typescript
  {
    path: '/system/config',
    name: 'SystemConfig',
    component: () => import('@/views/SystemSettingsView.vue'),
    meta: { title: '系统配置' }
  },
```

插入后 feedbacks/consultations/profile 区域变成：
```typescript
  {
    path: '/feedbacks',
    name: 'Feedbacks',
    component: () => import('@/views/FeedbackView.vue'),
    meta: { title: '反馈管理' }
  },
  {
    path: '/system/config',
    name: 'SystemConfig',
    component: () => import('@/views/SystemSettingsView.vue'),
    meta: { title: '系统配置' }
  },
```

- [ ] **Step 2: 菜单新增"系统配置"项**

在 `admin-frontend/src/layouts/AdminLayout.vue` 的 `menuItems` 的"系统"分组下，在 `consultations` 之后新增：

```javascript
      { path: '/system/config', label: '系统配置' },
```

修改后"系统"分组：
```javascript
  {
    label: '系统', icon: 'Setting',
    children: [
      { path: '/feedbacks', label: '反馈管理' },
      { path: '/consultations', label: '咨询管理' },
      { path: '/system/config', label: '系统配置' },
      { path: '/profile', label: '个人信息' }
    ]
  }
```

---

## 自审检查

### 1. 规格覆盖率

| 规格需求 | 对应任务 |
|---------|---------|
| system_config 建表 + 预置数据 | Task 1 |
| SystemConfig 实体 | Task 2 |
| SystemConfigMapper | Task 3 |
| SystemConfigService（CRUD + 缓存） | Task 4 |
| ConfigHolder 工具类 | Task 5 |
| SystemConfigController 4 个端点 | Task 6 |
| 前端 API (system.ts) | Task 7 |
| 前端配置管理页面 | Task 8 |
| 前端路由 + 菜单 | Task 9 |

### 2. 占位符检查

所有步骤代码完整，无 "TBD"、"TODO" 或占位符。

### 3. 类型一致性检查

- `SystemConfig.configKey` ↔ `ConfigHolder.get(key)` — 一致
- 后端 `batchUpdate(List<SystemConfig>)` ↔ 前端 `updateConfigs(configs: SystemConfig[])` — 一致
- `getByCategory(category)` → `GET /api/auth/admin/config/category/{category}` — 一致
- `dataType` 值（string/number/boolean/image/password/textarea）在前端 template 中的 v-if 分支 — 全部覆盖
