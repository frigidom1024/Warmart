# 物流跟踪功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现物流轨迹跟踪，包含 logistics_track 表、后端 API、订单详情页精简时间线、独立物流详情页

**Architecture:** 新增 logistics_track 表记录物流节点，后端提供轨迹查询和录入接口，前端订单详情页嵌入精简时间线，独立物流详情页展示完整轨迹

**Tech Stack:** Spring Boot + MyBatis-Plus + MySQL + Vue 3 + Element Plus

---

### Task 1: 数据库 — logistics_track 建表

**Files:**
- Modify: `deploy/mysql/init.sql`

- [ ] **追加 logistics_track 建表语句**

在 `USE mall_order;` 区域内的 `payment` 表之后追加：

```sql
CREATE TABLE IF NOT EXISTS logistics_track (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    message VARCHAR(500),
    location VARCHAR(200),
    track_time DATETIME NOT NULL,
    created_time DATETIME NOT NULL,
    INDEX idx_order_id (order_id),
    INDEX idx_track_time (track_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **在运行中的 MySQL 执行建表**

```bash
docker exec -i mall-mysql mysql -uroot -proot123 mall_order < sql_file
```

- [ ] **提交**

```bash
git add deploy/mysql/init.sql
git commit -m "feat: 新增 logistics_track 物流轨迹表"
```

---

### Task 2: 后端 — LogisticsTrack 实体 + Mapper

**Files:**
- Create: `backend/order-service/src/main/java/com/mall/order/entity/LogisticsTrack.java`
- Create: `backend/order-service/src/main/java/com/mall/order/mapper/LogisticsTrackMapper.java`

- [ ] **创建实体类**

```java
package com.mall.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("logistics_track")
public class LogisticsTrack {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String status; // ORDERED, WAREHOUSE, IN_TRANSIT, PICKUP, DELIVERED
    private String message;
    private String location;
    private LocalDateTime trackTime;
    private LocalDateTime createdTime;
}
```

- [ ] **创建 Mapper**

```java
package com.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.order.entity.LogisticsTrack;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LogisticsTrackMapper extends BaseMapper<LogisticsTrack> {
}
```

- [ ] **提交**

```bash
git add backend/order-service/src/main/java/com/mall/order/entity/LogisticsTrack.java backend/order-service/src/main/java/com/mall/order/mapper/LogisticsTrackMapper.java
git commit -m "feat: 新增 LogisticsTrack 实体和 Mapper"
```

---

### Task 3: 后端 — LogisticsService 业务逻辑

**Files:**
- Create: `backend/order-service/src/main/java/com/mall/order/service/LogisticsService.java`

- [ ] **创建 LogisticsService**

```java
package com.mall.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.order.entity.LogisticsTrack;
import com.mall.order.mapper.LogisticsTrackMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LogisticsService {

    private final LogisticsTrackMapper logisticsTrackMapper;

    @Transactional
    public LogisticsTrack addTrack(Long orderId, String status, String message, String location, LocalDateTime trackTime) {
        LogisticsTrack track = new LogisticsTrack();
        track.setOrderId(orderId);
        track.setStatus(status);
        track.setMessage(message);
        track.setLocation(location);
        track.setTrackTime(trackTime != null ? trackTime : LocalDateTime.now());
        track.setCreatedTime(LocalDateTime.now());
        logisticsTrackMapper.insert(track);
        return track;
    }

    public List<LogisticsTrack> getTracks(Long orderId) {
        return logisticsTrackMapper.selectList(
                new LambdaQueryWrapper<LogisticsTrack>()
                        .eq(LogisticsTrack::getOrderId, orderId)
                        .orderByDesc(LogisticsTrack::getTrackTime));
    }
}
```

- [ ] **提交**

```bash
git add backend/order-service/src/main/java/com/mall/order/service/LogisticsService.java
git commit -m "feat: 新增 LogisticsService 物流轨迹业务"
```

---

### Task 4: 后端 — LogisticsController + 发货时自动生成轨迹

**Files:**
- Create: `backend/order-service/src/main/java/com/mall/order/controller/LogisticsController.java`
- Modify: `backend/order-service/src/main/java/com/mall/order/service/OrderService.java`

- [ ] **创建 LogisticsController**

```java
package com.mall.order.controller;

import com.mall.order.common.Result;
import com.mall.order.entity.LogisticsTrack;
import com.mall.order.service.LogisticsService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    private final LogisticsService logisticsService;

    @GetMapping("/tracks/{orderId}")
    public Result<List<LogisticsTrack>> getTracks(@PathVariable Long orderId) {
        return Result.success(logisticsService.getTracks(orderId));
    }
}
```

- [ ] **创建管理员轨迹追加接口（放到 RefundAdminController 中或单独）**

创建 `backend/order-service/src/main/java/com/mall/order/controller/LogisticsAdminController.java`：

```java
package com.mall.order.controller;

import com.mall.order.common.Result;
import com.mall.order.entity.LogisticsTrack;
import com.mall.order.service.LogisticsService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/order/admin/logistics")
@RequiredArgsConstructor
public class LogisticsAdminController {

    private final LogisticsService logisticsService;

    @PostMapping("/track")
    public Result<Void> addTrack(@RequestBody AddTrackRequest request) {
        logisticsService.addTrack(request.getOrderId(), request.getStatus(),
                request.getMessage(), request.getLocation(),
                request.getTrackTime() != null ? LocalDateTime.parse(request.getTrackTime()) : null);
        return Result.success(null);
    }

    @Data
    public static class AddTrackRequest {
        private Long orderId;
        private String status;
        private String message;
        private String location;
        private String trackTime; // ISO format string
    }
}
```

- [ ] **修改 OrderService.adminShip 自动生成初始轨迹**

在 `OrderService.java` 中注入 `LogisticsService`，在 `adminShip` 方法末尾追加：

```java
// Generate initial logistics tracks
logisticsService.addTrack(id, "ORDERED", "订单已确认", null, order.getCreatedTime());
logisticsService.addTrack(id, "WAREHOUSE", "商品已打包完成，等待快递揽收", null, LocalDateTime.now());
```

在类字段区域添加：
```java
private final LogisticsService logisticsService;
```

- [ ] **提交**

```bash
git add backend/order-service/src/main/java/com/mall/order/controller/LogisticsController.java backend/order-service/src/main/java/com/mall/order/controller/LogisticsAdminController.java backend/order-service/src/main/java/com/mall/order/service/OrderService.java
git commit -m "feat: 新增物流轨迹查询和管理接口,发货时自动生成轨迹"
```

---

### Task 5: 前端客户端 — API 层

**Files:**
- Create: `frontend/src/api/logistics.ts`

- [ ] **创建 logistics.ts**

```typescript
import request from './request'

export interface LogisticsTrack {
  id: number
  orderId: number
  status: 'ORDERED' | 'WAREHOUSE' | 'IN_TRANSIT' | 'PICKUP' | 'DELIVERED'
  message: string | null
  location: string | null
  trackTime: string
  createdTime: string
}

export function getLogisticsTracks(orderId: number) {
  return request.get<LogisticsTrack[]>('/order/logistics/tracks/' + orderId)
}
```

- [ ] **提交**

```bash
git add frontend/src/api/logistics.ts
git commit -m "feat: 客户端物流轨迹 API"
```

---

### Task 6: 前端客户端 — 订单详情页嵌入精简时间线

**Files:**
- Modify: `frontend/src/views/OrderDetailView.vue`

- [ ] **在物流信息卡片后追加精简物流时间线**

在 `OrderDetailView.vue` 的物流信息 section 之后，新增物流跟踪精简版：

找到现有的物流 section：
```html
<section v-if="order.logisticsCompany" class="order-detail__section">
  <h2 class="order-detail__section-title">物流信息</h2>
  ...
</section>
```

在该 section 之后追加物流跟踪时间线（当有物流轨迹数据时显示）。需要：

在 `<script>` 中：
1. 导入 `getLogisticsTracks` 和 `LogisticsTrack` 类型
2. 添加 `logisticsTracks` 响应式状态
3. 在 `onMounted` 中加载物流轨迹
4. 添加物流状态映射和工具函数
5. 添加 "查看完整物流" 处理函数

```typescript
// 在 import 区域追加
import { getLogisticsTracks } from '@/api/logistics'
import type { LogisticsTrack } from '@/api/logistics'

// 在状态变量区追加
const logisticsTracks = ref<LogisticsTrack[]>([])

// 物流状态映射
const logisticsStatusMap: Record<string, { label: string; color: string }> = {
  ORDERED: { label: '已下单', color: '#6b6c72' },
  WAREHOUSE: { label: '仓库处理中', color: '#6b6c72' },
  IN_TRANSIT: { label: '运输中', color: '#409eff' },
  PICKUP: { label: '待取件', color: '#ff9f0a' },
  DELIVERED: { label: '已签收', color: '#34c759' }
}

// 在 onMounted 的 getOrderDetail 之后追加
if (order.value?.logisticsCompany) {
  try {
    const res: any = await getLogisticsTracks(id)
    logisticsTracks.value = res?.data || res || []
  } catch {}
}
```

在模板中，在物流信息 section 之后再物流跟踪 section：

```html
<!-- Logistics Tracking Timeline (compact) -->
<section v-if="logisticsTracks.length" class="order-detail__section">
  <h2 class="order-detail__section-title">物流跟踪</h2>
  <div class="logistics-compact">
    <div
      v-for="(track, i) in logisticsTracks.slice(0, 4)"
      :key="track.id"
      class="logistics-compact__item"
    >
      <div class="logistics-compact__dot"
        :style="{ background: i === 0 ? logisticsStatusMap[track.status]?.color : '#2a2b30' }">
      </div>
      <div v-if="i < Math.min(logisticsTracks.length, 4) - 1" class="logistics-compact__line"></div>
      <div class="logistics-compact__content">
        <p class="logistics-compact__status"
          :style="{ color: i === 0 ? logisticsStatusMap[track.status]?.color : 'var(--wz-text-soft)' }">
          {{ logisticsStatusMap[track.status]?.label || track.status }}
        </p>
        <p class="logistics-compact__msg">{{ track.message || '' }}</p>
        <p class="logistics-compact__time">{{ formatTime(track.trackTime) }}</p>
      </div>
    </div>
    <div v-if="logisticsTracks.length > 4" class="logistics-compact__more">
      <span @click="$router.push('/logistics/' + order?.id)">查看完整物流 →</span>
    </div>
  </div>
</section>
```

添加 formatTime 工具函数：
```typescript
function formatTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}
```

添加 CSS 样式到 scoped style 区域：
```css
/* ── Logistics Compact Timeline ── */
.logistics-compact {
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: var(--wz-radius-md);
  padding: 20px;
}
.logistics-compact__item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  position: relative;
  padding-bottom: 16px;
}
.logistics-compact__item:last-child {
  padding-bottom: 0;
}
.logistics-compact__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 4px;
  position: relative;
  z-index: 1;
}
.logistics-compact__line {
  position: absolute;
  left: 4px;
  top: 16px;
  width: 2px;
  bottom: 0;
  background: var(--wz-border);
}
.logistics-compact__content {
  flex: 1;
  min-width: 0;
}
.logistics-compact__status {
  font-size: 14px;
  font-weight: 500;
  margin: 0;
}
.logistics-compact__msg {
  font-size: 12px;
  color: var(--wz-text-muted);
  margin: 2px 0;
}
.logistics-compact__time {
  font-size: 11px;
  color: var(--wz-text-muted);
  margin: 0;
}
.logistics-compact__more {
  text-align: center;
  padding-top: 8px;
  border-top: 1px solid var(--wz-border-light);
  margin-top: 8px;
}
.logistics-compact__more span {
  font-size: 13px;
  color: var(--wz-orange);
  cursor: pointer;
}
.logistics-compact__more span:hover {
  color: var(--wz-orange-dark);
}
```

- [ ] **提交**

```bash
git add frontend/src/views/OrderDetailView.vue
git commit -m "feat: 订单详情页嵌入精简物流时间线"
```

---

### Task 7: 前端客户端 — 物流详情页

**Files:**
- Create: `frontend/src/views/LogisticsView.vue`

- [ ] **创建 LogisticsView.vue**

完整页面包含：
- 顶部：物流公司和运单号 + 复制按钮
- 面包屑导航返回订单详情
- 完整物流时间线，按状态分组（运输中节点缩进显示）

```vue
<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail } from '@/api/order'
import { getLogisticsTracks } from '@/api/logistics'
import type { LogisticsTrack } from '@/api/logistics'
import type { Order } from '@/api/order'
import { showToast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const order = ref<Order | null>(null)
const tracks = ref<LogisticsTrack[]>([])
const loading = ref(true)

const statusConfig: Record<string, { label: string; color: string }> = {
  ORDERED: { label: '已下单', color: '#6b6c72' },
  WAREHOUSE: { label: '仓库处理中', color: '#6b6c72' },
  IN_TRANSIT: { label: '运输中', color: '#409eff' },
  PICKUP: { label: '待取件', color: '#ff9f0a' },
  DELIVERED: { label: '已签收', color: '#34c759' }
}

// Group tracks by status for display
const displayGroups = computed(() => {
  const groups: { status: string; label: string; color: string; items: LogisticsTrack[] }[] = []
  const statusOrder = ['DELIVERED', 'PICKUP', 'IN_TRANSIT', 'WAREHOUSE', 'ORDERED']

  // Collect unique statuses in order
  const seenStatuses = new Set<string>()
  for (const track of tracks.value) {
    if (!seenStatuses.has(track.status)) {
      seenStatuses.add(track.status)
    }
  }

  for (const status of statusOrder) {
    if (seenStatuses.has(status)) {
      const items = tracks.value.filter(t => t.status === status)
      groups.push({
        status,
        label: statusConfig[status]?.label || status,
        color: statusConfig[status]?.color || '#6b6c72',
        items
      })
    }
  }
  return groups
})

function formatTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

function copyNo(text: string) {
  navigator.clipboard.writeText(text)
  showToast('运单号已复制', 'success')
}

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) { router.replace('/order/list'); return }
  try {
    const [o, res]: any = await Promise.all([
      getOrderDetail(id),
      getLogisticsTracks(id).catch(() => ({}))
    ])
    order.value = o
    tracks.value = res?.data || res || []
  } catch {} finally { loading.value = false }
})
</script>

<template>
  <div class="page-container">
    <div class="logistics" v-if="order">
      <div class="logistics__breadcrumb" @click="router.push('/order/detail/' + order.id)">订单详情 / 物流跟踪</div>
      <h1 class="logistics__title">物流跟踪</h1>

      <!-- Header -->
      <div class="logistics__header">
        <div class="logistics__header-row">
          <span class="logistics__header-label">物流公司</span>
          <span class="logistics__header-value">{{ order.logisticsCompany }}</span>
        </div>
        <div class="logistics__header-row">
          <span class="logistics__header-label">运单编号</span>
          <span class="logistics__header-value">
            {{ order.logisticsNo }}
            <span class="logistics__copy-btn" @click="copyNo(order.logisticsNo || '')">复制</span>
          </span>
        </div>
      </div>

      <!-- Timeline -->
      <div class="logistics__timeline" v-if="displayGroups.length">
        <template v-for="(group, gi) in displayGroups" :key="group.status">
          <div class="logistics__group">
            <div class="logistics__group-header">
              <span class="logistics__group-dot" :style="{ background: gi === 0 ? group.color : '' }"></span>
              <span class="logistics__group-status" :style="{ color: gi === 0 ? group.color : '' }">{{ group.label }}</span>
            </div>
            <div class="logistics__group-items">
              <div v-for="(item, ii) in group.items" :key="item.id" class="logistics__track-item">
                <div class="logistics__track-line" v-if="ii < group.items.length - 1"></div>
                <div class="logistics__track-content">
                  <p class="logistics__track-msg">{{ item.message }}</p>
                  <p class="logistics__track-location" v-if="item.location">{{ item.location }}</p>
                  <p class="logistics__track-time">{{ formatTime(item.trackTime) }}</p>
                </div>
              </div>
            </div>
          </div>
          <div v-if="gi < displayGroups.length - 1" class="logistics__group-connector"></div>
        </template>
      </div>
      <div v-else class="logistics__empty">暂无物流跟踪信息</div>
    </div>

    <div v-else-if="loading" class="logistics__loading">加载中...</div>
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100vh;
  padding-top: 64px;
  background: var(--wz-bg);
}
.logistics {
  max-width: 640px;
  margin: 0 auto;
  padding: 28px 24px 80px;
}
.logistics__breadcrumb {
  font-size: 13px;
  color: var(--wz-text-muted);
  margin-bottom: 16px;
  cursor: pointer;
}
.logistics__breadcrumb:hover { color: var(--wz-text-soft); }
.logistics__title {
  font-family: var(--wz-font-display);
  font-size: 26px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 28px;
}
.logistics__header {
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: var(--wz-radius-md);
  padding: 20px;
  margin-bottom: 24px;
}
.logistics__header-row {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 14px;
}
.logistics__header-row:last-child { margin-bottom: 0; }
.logistics__header-label {
  color: var(--wz-text-muted);
  min-width: 68px;
  flex-shrink: 0;
  font-size: 13px;
}
.logistics__header-value {
  color: var(--wz-text-soft);
  word-break: break-all;
}
.logistics__copy-btn {
  display: inline-block;
  margin-left: 6px;
  padding: 0 7px;
  font-size: 11px;
  color: var(--wz-orange);
  border: 1px solid var(--wz-orange);
  border-radius: 4px;
  cursor: pointer;
  line-height: 18px;
  vertical-align: middle;
}
.logistics__copy-btn:hover {
  background: var(--wz-orange);
  color: #fff;
}
/* Timeline */
.logistics__timeline {
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: var(--wz-radius-md);
  padding: 24px 20px;
}
.logistics__group {
  position: relative;
}
.logistics__group-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.logistics__group-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--wz-text-muted);
  flex-shrink: 0;
}
.logistics__group-status {
  font-size: 15px;
  font-weight: 600;
  color: var(--wz-text);
}
.logistics__group-items {
  padding-left: 22px;
  position: relative;
}
.logistics__track-item {
  position: relative;
  padding: 6px 0 6px 16px;
  border-left: 2px solid var(--wz-border-light);
}
.logistics__track-item:first-child { padding-top: 0; }
.logistics__track-item:last-child { padding-bottom: 0; }
.logistics__track-msg {
  font-size: 13px;
  color: var(--wz-text-soft);
  margin: 0;
}
.logistics__track-location {
  font-size: 12px;
  color: var(--wz-text-muted);
  margin: 2px 0;
}
.logistics__track-time {
  font-size: 11px;
  color: var(--wz-text-muted);
  margin: 2px 0 0;
}
.logistics__group-connector {
  height: 16px;
  margin-left: 5px;
  border-left: 2px dashed var(--wz-border);
}
.logistics__empty {
  text-align: center;
  padding: 40px;
  color: var(--wz-text-muted);
  font-size: 14px;
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: var(--wz-radius-md);
}
.logistics__loading {
  text-align: center;
  padding: 80px;
  color: var(--wz-text-muted);
}
@media (max-width: 640px) {
  .logistics { padding: 20px 16px 80px; }
}
</style>
```

- [ ] **添加路由**

在 `frontend/src/router/index.ts` 中追加：

```typescript
{
  path: '/logistics/:id',
  name: 'Logistics',
  component: () => import('@/views/LogisticsView.vue'),
  meta: { title: '物流跟踪' }
}
```

- [ ] **提交**

```bash
git add frontend/src/views/LogisticsView.vue frontend/src/router/index.ts
git commit -m "feat: 新增物流详情页和路由"
```

---

### Task 8: 管理端 — 物流轨迹录入

**Files:**
- Create: `admin-frontend/src/api/logistics.ts`
- Modify: `admin-frontend/src/views/OrderListView.vue`

- [ ] **创建管理端 API**

```typescript
import request from './request'

export function addLogisticsTrack(data: {
  orderId: number
  status: string
  message?: string
  location?: string
  trackTime?: string
}) {
  return request.post<void>('/order/admin/logistics/track', data)
}
```

- [ ] **在发货弹窗中增加轨迹录入**

在 `admin-frontend/src/views/OrderListView.vue` 的发货弹窗中，在确认发货按钮上方增加轨迹录入区域。

找到发货弹窗模板，在确认发货按钮之前添加：

```html
<el-form-item label="物流状态">
  <el-select v-model="shipTrack.status" placeholder="选择物流状态" style="width:100%">
    <el-option label="仓库处理中" value="WAREHOUSE" />
    <el-option label="运输中" value="IN_TRANSIT" />
    <el-option label="待取件" value="PICKUP" />
    <el-option label="已签收" value="DELIVERED" />
  </el-select>
</el-form-item>
<el-form-item label="备注信息">
  <el-input v-model="shipTrack.message" placeholder="如: 快件已到达北京分拨中心" />
</el-form-item>
<el-form-item label="当前位置">
  <el-input v-model="shipTrack.location" placeholder="如: 北京市通州区" />
</el-form-item>
```

在 script 中添加：
```typescript
import { addLogisticsTrack } from '@/api/logistics'

const shipTrack = ref({ status: 'WAREHOUSE', message: '', location: '' })
```

在 `openShipDialog` 中重置：
```typescript
shipTrack.value = { status: 'WAREHOUSE', message: '', location: '' }
```

在 `handleShip` 的 `shipOrder` 成功后追加：
```typescript
await addLogisticsTrack({
  orderId: shipTarget.value.id,
  status: shipTrack.value.status,
  message: shipTrack.value.message || undefined,
  location: shipTrack.value.location || undefined
})
```

- [ ] **提交**

```bash
git add admin-frontend/src/api/logistics.ts admin-frontend/src/views/OrderListView.vue
git commit -m "feat: 管理端发货时支持录入物流轨迹"
```
