<script setup lang="ts">
import { useRouter } from 'vue-router'
import { showToast } from '@/utils/toast'

const router = useRouter()

function goHelp() { router.push('/help') }
function goOrderList() { router.push('/order/list') }
function goConsultation() { router.push('/service/consultation') }
function goAbout() { router.push('/about') }
function goPrivacy() { router.push('/privacy') }
function goFeedback() { router.push('/user/feedback') }

function onSocialClick(name: string) {
  showToast(`关注「暖·Warmart」${name}官方账号`, 'success')
}
</script>

<template>
  <footer class="app-footer">
    <div class="app-footer__inner">
      <div class="app-footer__grid">
        <!-- Brand -->
        <div class="app-footer__brand">
          <div class="app-footer__logo-wrap">
            <span class="app-footer__logo">暖</span>
            <span class="app-footer__brand-name">Warmart</span>
          </div>
          <p class="app-footer__desc">
            品质生活指南 · 严选全球好物<br />让每一次购物都成为温暖的体验
          </p>
          <div class="app-footer__social">
            <a href="javascript:;" class="app-footer__social-link" title="微信" @click="onSocialClick('微信')">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M17.5 10.5c.7 0 1.4.1 2 .3C19 7.5 16 5 12.5 5 8.4 5 5 8.1 5 12c0 1.7.6 3.3 1.7 4.5L6 19l2.5-1.3c.8.2 1.7.3 2.5.3"/><path d="M14 14.5c0 .3-.2.5-.5.5s-.5-.2-.5-.5.2-.5.5-.5.5.2.5.5zM18 14.5c0 .3-.2.5-.5.5s-.5-.2-.5-.5.2-.5.5-.5.5.2.5.5z"/></svg>
            </a>
            <a href="javascript:;" class="app-footer__social-link" title="微博" @click="onSocialClick('微博')">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 8a4 4 0 0 0-4 4"/><path d="M12 6a6 6 0 0 0-6 6"/></svg>
            </a>
            <a href="javascript:;" class="app-footer__social-link" title="小红书" @click="onSocialClick('小红书')">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a10 10 0 0 1 10 10c0 5.5-4.5 10-10 10S2 17.5 2 12 6.5 2 12 2z"/><path d="M8 12h8"/><path d="M12 8v8"/></svg>
            </a>
          </div>
        </div>

        <!-- 帮助中心 -->
        <div class="app-footer__col">
          <h4>帮助中心</h4>
          <ul>
            <li><a href="javascript:;" @click="goHelp">购物指南</a></li>
            <li><a href="javascript:;" @click="goOrderList">订单查询</a></li>
            <li><a href="javascript:;" @click="goHelp">退换货政策</a></li>
            <li><a href="javascript:;" @click="goConsultation">联系客服</a></li>
          </ul>
        </div>

        <!-- 关于我们 -->
        <div class="app-footer__col">
          <h4>关于我们</h4>
          <ul>
            <li><a href="javascript:;" @click="goAbout">公司介绍</a></li>
            <li><a href="javascript:;" @click="goAbout">加入我们</a></li>
            <li><a href="javascript:;" @click="goPrivacy">隐私政策</a></li>
            <li><a href="javascript:;" @click="goAbout">用户协议</a></li>
            <li><a href="javascript:;" @click="goFeedback">意见反馈</a></li>
          </ul>
        </div>

        <!-- 联系方式 -->
        <div class="app-footer__col">
          <h4>联系方式</h4>
          <ul>
            <li class="app-footer__contact-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.8 19.8 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.8 19.8 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.8 12.8 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.8 12.8 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
              <span>400-000-0000</span>
            </li>
            <li class="app-footer__contact-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polyline points="22 12 16 12 14 15 10 15 8 12 2 12"/><path d="M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"/></svg>
              <span>hello@warmart.cn</span>
            </li>
            <li class="app-footer__contact-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
              <span>09:00 - 21:00</span>
            </li>
          </ul>
        </div>
      </div>

      <div class="app-footer__bottom">
        <span>© 2024 暖·Warmart All Rights Reserved</span>
        <span class="app-footer__bottom-dot">·</span>
        <a href="javascript:;" class="app-footer__bottom-link">京ICP备XXXXXXXX号-1</a>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.app-footer {
  border-top: 1px solid var(--wz-border);
  background: var(--wz-bg);
}

.app-footer__inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: var(--wz-space-2xl) var(--wz-space-xl) 0;
}

.app-footer__grid {
  display: grid;
  grid-template-columns: 1.8fr 1fr 1fr 1fr;
  gap: var(--wz-space-2xl);
  padding-bottom: var(--wz-space-xl);
  border-bottom: 1px solid var(--wz-border);
}

.app-footer__logo-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
}

.app-footer__logo {
  font-family: 'Noto Serif SC', serif;
  font-size: 22px;
  font-weight: 700;
  color: var(--wz-orange);
  transition: text-shadow var(--wz-duration-normal) var(--wz-ease-out);
}

.app-footer__brand:hover .app-footer__logo {
  text-shadow: 0 0 16px rgba(255, 107, 53, 0.30), 0 0 48px rgba(255, 107, 53, 0.10);
}

.app-footer__brand-name {
  font-family: 'Noto Serif SC', serif;
  font-size: 18px;
  font-weight: 700;
  color: var(--wz-text);
}

.app-footer__desc {
  font-size: 13px;
  color: var(--wz-text-muted);
  line-height: 1.7;
  margin-bottom: 16px;
  max-width: 260px;
}

.app-footer__social {
  display: flex;
  gap: 8px;
}

.app-footer__social-link {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--wz-bg);
  border: 1px solid var(--wz-border);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--wz-text-muted);
  transition: background var(--wz-duration-fast) var(--wz-ease-out),
              color var(--wz-duration-fast) var(--wz-ease-out),
              border-color var(--wz-duration-fast) var(--wz-ease-out);
}

.app-footer__social-link:hover {
  background: var(--wz-orange);
  color: #fff;
  border-color: var(--wz-orange);
}

.app-footer__col h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 14px;
  letter-spacing: 0.04em;
}

.app-footer__col ul {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.app-footer__col a {
  font-size: 13px;
  color: var(--wz-text-muted);
  text-decoration: none;
  transition: color var(--wz-duration-fast) var(--wz-ease-out);
  cursor: pointer;
}

.app-footer__col a:hover {
  color: var(--wz-orange);
}

.app-footer__col li {
  font-size: 13px;
  color: var(--wz-text-muted);
}

.app-footer__contact-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--wz-text-muted);
}

.app-footer__contact-item svg {
  flex-shrink: 0;
  opacity: 0.5;
}

.app-footer__bottom {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  padding: var(--wz-space-lg) 0;
  font-size: 12px;
  color: var(--wz-text-muted);
  flex-wrap: wrap;
}

.app-footer__bottom-dot {
  margin: 0 10px;
  color: var(--wz-border);
}

.app-footer__bottom-link {
  font-size: 12px;
  color: var(--wz-text-muted);
  text-decoration: none;
  transition: color var(--wz-duration-fast) var(--wz-ease-out);
}

.app-footer__bottom-link:hover {
  color: var(--wz-orange);
}

/* ─── Responsive ─── */
@media (max-width: 860px) {
  .app-footer__grid {
    grid-template-columns: 1fr 1fr;
    gap: var(--wz-space-xl);
  }
  .app-footer__brand {
    grid-column: 1 / -1;
  }
  .app-footer__col h4 {
    font-size: 13px;
    margin-bottom: 10px;
  }
  .app-footer__col li {
    font-size: 12px;
  }
}

@media (max-width: 480px) {
  .app-footer__grid {
    grid-template-columns: 1fr;
    gap: var(--wz-space-lg);
    text-align: center;
  }
  .app-footer__brand {
    text-align: center;
  }
  .app-footer__logo-wrap {
    justify-content: center;
  }
  .app-footer__desc {
    margin-left: auto;
    margin-right: auto;
  }
  .app-footer__social {
    justify-content: center;
  }
  .app-footer__contact-item {
    justify-content: center;
  }
}
</style>
