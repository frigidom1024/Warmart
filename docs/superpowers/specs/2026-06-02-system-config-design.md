# 系统配置管理功能设计

## 概述

为管理后台添加系统配置管理功能，让管理员通过可视化界面管理所有业务参数。采用通用 key-value 键值对存储，按分类分组管理，Redis 缓存提升读取性能。

## 目标

- 管理员可在后台管理基本设置、SEO、邮件、订单、上传等各类配置
- 新增配置类型无需改表结构
- 业务代码可通过工具类方便地读取配置
- Redis 缓存减少数据库读取压力

## 非目标

- 配置变更的审核和版本管理
- 配置的灰度发布
- 动态刷新已加载到内存中的配置（重启服务生效）

## 数据模型

### 新建表

```sql
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
```

### 预置数据

| category | config_key | config_value | data_type | description |
|----------|-----------|-------------|-----------|-------------|
| basic | site_name | Warmart | string | 网站名称 |
| basic | site_logo | /logo.png | image | 网站 Logo |
| basic | site_icp | 京ICP备xxx号 | string | ICP 备案号 |
| basic | site_copyright | ©2026 Warmart | string | 版权信息 |
| basic | customer_phone | 400-xxx-xxxx | string | 客服电话 |
| basic | customer_email | support@xxx.com | string | 客服邮箱 |
| basic | customer_address | 北京市朝阳区xxx | string | 公司地址 |
| seo | seo_title | Warmart - 温暖你的生活 | string | 首页标题 |
| seo | seo_keywords | 电商,购物,温暖 | string | 首页关键词 |
| seo | seo_description | 精选全球好物 | textarea | 首页描述 |
| seo | seo_analytics | | textarea | 统计代码（百度/GA） |
| email | dm_access_key_id | | password | 阿里云 AccessKey ID |
| email | dm_access_key_secret | | password | 阿里云 AccessKey Secret |
| email | dm_account_name | | string | 发信地址 |
| order | auto_confirm_days | 7 | number | 自动确认收货天数 |
| order | auto_review_days | 15 | number | 自动好评天数 |
| order | refund_timeout_hours | 72 | number | 退款超时时间（小时） |
| upload | max_file_size | 10 | number | 最大文件上传（MB） |
| upload | allowed_image_types | jpg,png,gif,webp | string | 允许图片格式 |

### Redis Key 设计

| Key | 结构 | TTL | 用途 |
|-----|------|-----|------|
| `auth:config:{config_key}` | String | 无（惰性删除） | 单个配置值缓存 |
| `auth:config:all` | Hash | 无（惰性删除） | 全量配置缓存 |

**缓存策略：**
- 读取：先查 Redis，miss 则查 DB 并回写 Redis
- 修改：更新 DB 后删除对应 Redis key
- 无需 TTL，配置只在修改时失效

## 后端设计

### 实体

`com.mall.auth.entity.SystemConfig`：
```java
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

### 新增文件

| 文件 | 说明 |
|------|------|
| `entity/SystemConfig.java` | 配置实体 |
| `mapper/SystemConfigMapper.java` | MyBatis-Plus BaseMapper |
| `service/SystemConfigService.java` | 配置业务逻辑（含缓存管理） |
| `service/ConfigHolder.java` | 配置读取工具类（供业务代码调用） |
| `controller/SystemConfigController.java` | 管理端 API 控制器 |

### 新增端点

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| `GET` | `/api/auth/admin/config/list` | Admin | 获取所有配置（按 category 排序） |
| `GET` | `/api/auth/admin/config/category/{category}` | Admin | 获取指定分类的配置列表 |
| `PUT` | `/api/auth/admin/config/update` | Admin | 批量更新配置（传入 List） |
| `GET` | `/api/auth/admin/config/categories` | Admin | 获取所有分类列表 |

### 核心逻辑

**查询配置列表（SystemConfigService.listAll()）：**
1. 查询全量：`mapper.selectList()`，按 category 和 sort_order 排序
2. 返回列表，前端按 category 分组展示

**按分类查询（getByCategory(category)）：**
1. 查 DB：`mapper.selectList(new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getCategory, category))`
2. 返回该分类下的配置列表

**批量更新（batchUpdate(configs)）：**
1. 遍历传入的配置列表
2. 对每条执行 `mapper.updateById(config)`
3. 删除对应的 Redis key：`redisTemplate.delete("auth:config:" + config.getConfigKey())`
4. 记录操作日志

### 配置读取工具

```java
@Component
public class ConfigHolder {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private SystemConfigMapper configMapper;

    public String get(String key) {
        String val = (String) redisTemplate.opsForValue().get("auth:config:" + key);
        if (val != null) return val;
        SystemConfig config = configMapper.selectOne(
            new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key));
        if (config == null) return null;
        redisTemplate.opsForValue().set("auth:config:" + key, config.getConfigValue());
        return config.getConfigValue();
    }

    public String get(String key, String defaultValue) {
        String val = get(key);
        return val != null ? val : defaultValue;
    }

    public Integer getInt(String key, Integer defaultValue) {
        String val = get(key);
        if (val == null) return defaultValue;
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return defaultValue; }
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        String val = get(key);
        return val != null ? Boolean.parseBoolean(val) : defaultValue;
    }
}
```

### 权限控制

所有配置管理端点需要 `ADMIN` 或 `SUPER_ADMIN` 角色认证（通过 Gateway 的 JWT 过滤 + 角色检查）。

## 前端设计

### 新增菜单

在 `AdminLayout.vue` 的"系统"分组下新增：
```javascript
{ label: '系统配置', route: '/system/config', icon: 'Setting' }
```

### 新增路由

```typescript
{
  path: '/system/config',
  name: 'SystemConfig',
  component: () => import('@/views/SystemSettingsView.vue'),
  meta: { title: '系统配置' }
}
```

### 页面布局

```
┌──────────────────────────────────────────────────────┐
│  ← 系统配置                                           │
│                                                       │
│  [基本设置] [SEO设置] [邮件配置] [订单配置] [上传配置]    │
│  ─────────────────────────────────────────────────    │
│                                                       │
│  当前: 基本设置                                        │
│                                                       │
│  ┌──────────────────────────────────────────────────┐ │
│  │ 网站名称    [ Warmart                    ]       │ │
│  │ 网站 Logo   [________________] [选择图片]         │ │
│  │ ICP备案号   [ 京ICP备xxx号              ]        │ │
│  │ 版权信息    [ ©2026 Warmart             ]        │ │
│  │ 客服电话    [ 400-xxx-xxxx              ]        │ │
│  │ 客服邮箱    [ support@xxx.com           ]        │ │
│  │                                                 │ │
│  │  [          保存配置          ]                  │ │
│  └──────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────┘
```

- 使用 `el-tabs` 按分类切换
- 根据 `data_type` 渲染不同控件：
  - `string` → el-input
  - `number` → el-input-number
  - `boolean` → el-switch
  - `image` → 文件上传组件 + 图片预览
  - `password` → el-input show-password
  - `textarea` → el-input type="textarea"
- 保存按钮提交整个分类的配置变更

## 涉及文件清单

### 后端（auth-service）

| 文件 | 操作 |
|------|------|
| `entity/SystemConfig.java` | **新建** |
| `mapper/SystemConfigMapper.java` | **新建** |
| `service/SystemConfigService.java` | **新建** |
| `service/ConfigHolder.java` | **新建** |
| `controller/SystemConfigController.java` | **新建** |

### 数据库

| 文件 | 操作 |
|------|------|
| `deploy/mysql/init.sql` | 新增 `system_config` 建表 + 预置数据 |

### 前端（admin-frontend）

| 文件 | 操作 |
|------|------|
| `src/api/system.ts` | **新建** — 配置相关 API 方法 |
| `src/views/SystemSettingsView.vue` | **新建** — 配置管理页面 |
| `src/router/index.ts` | 新增 `/system/config` 路由 |
| `src/layouts/AdminLayout.vue` | 新增"系统配置"菜单项 |
