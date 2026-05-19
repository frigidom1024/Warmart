<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const stats = [
  { value: '100W+', label: '忠实用户' },
  { value: '5000+', label: '入驻品牌' },
  { value: '99%', label: '好评率' }
]

const categories = [
  { name: '潮流服饰', icon: 'fa-shirt' },
  { name: '美妆护肤', icon: 'fa-spa' },
  { name: '数码家电', icon: 'fa-laptop' },
  { name: '家居软装', icon: 'fa-couch' },
  { name: '休闲零食', icon: 'fa-cookie-bite' },
  { name: '运动户外', icon: 'fa-dumbbell' }
]

const products = ref([
  { id: 1, title: '轻奢简约百搭手提包包女新款高级感单包', price: 199, old: 399, tag: '限时特惠', tagColor: 'orange' },
  { id: 2, title: '家用多功能全自动小型破壁机静音料理机', price: 269, old: 459, tag: '新品上新', tagColor: 'green' },
  { id: 3, title: '轻薄持妆粉底液控油遮瑕保湿自然不假白', price: 89, old: 159, tag: '美妆热卖', tagColor: 'purple' },
  { id: 4, title: '无线蓝牙耳机高音质长续航运动入耳式', price: 129, old: 229, tag: '数码爆款', tagColor: 'blue' },
  { id: 5, title: '日系简约纯棉卫衣男女通用宽松百搭外套', price: 159, old: 299, tag: '热卖爆款', tagColor: 'orange' },
  { id: 6, title: '家用智能扫地机器人全自动扫拖一体机', price: 1299, old: 1999, tag: '新品首发', tagColor: 'green' },
  { id: 7, title: '天然植物精萃精华液保湿补水修护肌肤屏障', price: 139, old: 269, tag: '口碑爆品', tagColor: 'purple' },
  { id: 8, title: '户外运动背包大容量防水轻便双肩包', price: 179, old: 349, tag: '限时特惠', tagColor: 'blue' }
])

const reviews = [
  { name: '小琳同学', role: '美妆爱好者', avatar: 'https://picsum.photos/id/65/60/60', text: '物流速度超快，第二天就收到货了，实物和图片一模一样，质量超出预期，已经推荐朋友来买了！', stars: 5 },
  { name: '王先生', role: '居家好物达人', avatar: 'https://picsum.photos/id/91/60/60', text: '平台售后特别贴心，尺码不合适直接一键退换，没有任何麻烦，购物体验满分！', stars: 5 },
  { name: '暖阳', role: '资深网购用户', avatar: 'https://picsum.photos/id/54/60/60', text: '正品无疑，价格比线下实体店划算很多，活动入手太划算，会长期回购！', stars: 4.5 }
]

function goProduct() {
  router.push('/product/list')
}
</script>

<template>
  <div class="home">
    <!-- ============ HERO ============ -->
    <section class="hero">
      <div class="hero__container">
        <div class="hero__left">
          <span class="hero__label">严选好物 · 品质生活</span>
          <h1 class="hero__title">精选全球好物<br>一站式轻松购齐</h1>
          <p class="hero__desc">正品直供极速发货，严选轻奢穿搭、家居日用、美妆护肤、数码潮品全品类覆盖</p>
          <div class="hero__btns">
            <button class="hero__btn-primary" @click="goProduct">即刻选购</button>
            <button class="hero__btn-ghost" @click="goProduct">查看爆款</button>
          </div>
          <div class="hero__stats">
            <div v-for="s in stats" :key="s.label" class="hero__stat">
              <p class="hero__stat-value">{{ s.value }}</p>
              <p class="hero__stat-label">{{ s.label }}</p>
            </div>
          </div>
        </div>
        <div class="hero__right">
          <img src="https://picsum.photos/id/26/700/600" class="hero__img" alt="hero">
        </div>
      </div>
    </section>

    <!-- ============ CATEGORIES ============ -->
    <section class="section">
      <div class="section__inner">
        <div class="section__header">
          <h2 class="section__title">全品类随心挑选</h2>
          <p class="section__sub">海量分类，精准找到你想要的好物</p>
        </div>
        <div class="cat-grid">
          <div v-for="cat in categories" :key="cat.name" class="cat-card">
            <div class="cat-card__icon">
              <i :class="['fa-solid', cat.icon]"></i>
            </div>
            <p class="cat-card__name">{{ cat.name }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ FEATURES ============ -->
    <section class="section section--alt">
      <div class="section__inner">
        <div class="features">
          <div class="features__left">
            <span class="features__label">平台核心优势</span>
            <h2 class="features__title">安心购物 多重保障</h2>
            <div class="features__list">
              <div class="feature">
                <div class="feature__icon"><i class="fa-solid fa-shield-halved"></i></div>
                <div class="feature__text">
                  <h4>正品保障</h4>
                  <p>官方直采授权货源，杜绝假货</p>
                </div>
              </div>
              <div class="feature">
                <div class="feature__icon"><i class="fa-solid fa-truck-fast"></i></div>
                <div class="feature__text">
                  <h4>极速物流</h4>
                  <p>多地仓储就近发货，次日速达</p>
                </div>
              </div>
              <div class="feature">
                <div class="feature__icon"><i class="fa-solid fa-arrow-rotate-left"></i></div>
                <div class="feature__text">
                  <h4>七天无忧退换</h4>
                  <p>售后极简流程，轻松退换货</p>
                </div>
              </div>
              <div class="feature">
                <div class="feature__icon"><i class="fa-solid fa-headset"></i></div>
                <div class="feature__text">
                  <h4>24小时在线客服</h4>
                  <p>随时解答购物疑问</p>
                </div>
              </div>
            </div>
          </div>
          <div class="features__right">
            <img src="https://picsum.photos/id/64/650/500" class="features__img" alt="features">
          </div>
        </div>
      </div>
    </section>

    <!-- ============ HOT PRODUCTS ============ -->
    <section class="section">
      <div class="section__inner">
        <div class="section__header section__header--row">
          <div>
            <span class="section__overline">HOT SALE</span>
            <h2 class="section__title">人气爆款好物</h2>
          </div>
          <a class="section__more" href="#" @click.prevent="goProduct">
            查看全部 <i class="fa-solid fa-arrow-right"></i>
          </a>
        </div>
        <div class="product-grid">
          <div v-for="p in products" :key="p.id" class="product-card">
            <div class="product-card__img">
              <img :src="`https://picsum.photos/id/${p.id}/400/350`" :alt="p.title">
            </div>
            <div class="product-card__body">
              <span :class="['product-card__tag', `product-card__tag--${p.tagColor}`]">{{ p.tag }}</span>
              <h3 class="product-card__title">{{ p.title }}</h3>
              <div class="product-card__prices">
                <span class="product-card__price">¥{{ p.price }}</span>
                <span class="product-card__old">¥{{ p.old }}</span>
              </div>
              <button class="product-card__btn" @click="goProduct">加入购物车</button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ REVIEWS ============ -->
    <section class="section section--alt">
      <div class="section__inner">
        <div class="section__header">
          <h2 class="section__title">真实用户口碑</h2>
          <p class="section__sub">千万用户真实评价，好品质看得见</p>
        </div>
        <div class="review-grid">
          <div v-for="r in reviews" :key="r.name" class="review-card">
            <div class="review-card__stars">
              <i v-for="i in 5" :key="i" class="fa-solid fa-star" :class="{ 'fa-star-half-stroke': i === 5 && r.stars === 4.5 }"></i>
            </div>
            <p class="review-card__text">{{ r.text }}</p>
            <div class="review-card__user">
              <img :src="r.avatar" class="review-card__avatar">
              <div>
                <p class="review-card__name">{{ r.name }}</p>
                <p class="review-card__role">{{ r.role }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ NEWSLETTER ============ -->
    <section class="section">
      <div class="section__inner section__inner--narrow">
        <div class="newsletter">
          <h2 class="newsletter__title">订阅优惠资讯</h2>
          <p class="newsletter__desc">第一时间领取大额优惠券，抢先知晓上新活动</p>
          <div class="newsletter__form">
            <input type="text" placeholder="请输入您的手机号" class="newsletter__input">
            <button class="newsletter__btn">立即订阅</button>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ FOOTER ============ -->
    <footer class="footer">
      <div class="section__inner">
        <div class="footer__grid">
          <div class="footer__brand">
            <h3><span class="footer__logo">暖</span> Warmart</h3>
            <p class="footer__desc">专注品质好物，打造轻松便捷高品质网购平台</p>
            <div class="footer__social">
              <a href="#"><i class="fa-brands fa-weixin"></i></a>
              <a href="#"><i class="fa-brands fa-weibo"></i></a>
              <a href="#"><i class="fa-brands fa-tiktok"></i></a>
            </div>
          </div>
          <div class="footer__col">
            <h4>购物指南</h4>
            <ul>
              <li><a href="#">注册登录</a></li>
              <li><a href="#">下单流程</a></li>
              <li><a href="#">支付方式</a></li>
              <li><a href="#">常见问题</a></li>
            </ul>
          </div>
          <div class="footer__col">
            <h4>售后服务</h4>
            <ul>
              <li><a href="#">退换货政策</a></li>
              <li><a href="#">物流查询</a></li>
              <li><a href="#">售后投诉</a></li>
              <li><a href="#">正品保障</a></li>
            </ul>
          </div>
          <div class="footer__col">
            <h4>联系我们</h4>
            <ul>
              <li><i class="fa-solid fa-phone mr-2"></i>400-888-9999</li>
              <li><i class="fa-solid fa-envelope mr-2"></i>service@warmart.com</li>
              <li><i class="fa-solid fa-location-dot mr-2"></i>杭州市电商产业园</li>
            </ul>
          </div>
        </div>
        <div class="footer__bottom">
          <p>© 2026 暖 Warmart 版权所有 浙ICP备12345678号</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
/* =============================================
   Home Page — Warmart Dark E-commerce
   ============================================= */

.home {
  padding-top: 60px;
  background: var(--wz-bg);
}

/* ---- Hero ---- */
.hero {
  background: var(--wz-bg);
  padding: 96px 24px 80px;
}

.hero__container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 48px;
}

@media (min-width: 768px) {
  .hero__container {
    flex-direction: row;
  }
}

.hero__left {
  flex: 1;
}

.hero__label {
  font-size: 13px;
  color: var(--wz-orange);
  letter-spacing: 0.15em;
  text-transform: uppercase;
}

.hero__title {
  font-family: var(--wz-font-display);
  font-size: 42px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--wz-text);
  margin: 16px 0;
}

@media (max-width: 768px) {
  .hero__title { font-size: 32px; }
}

.hero__desc {
  font-size: 16px;
  color: var(--wz-text-soft);
  line-height: 1.7;
  max-width: 460px;
  margin-bottom: 28px;
}

.hero__btns {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero__btn-primary {
  padding: 12px 32px;
  background: var(--wz-orange);
  border: none;
  border-radius: 10px;
  color: #fff;
  font-family: var(--wz-font-body);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: box-shadow var(--wz-duration-normal) var(--wz-ease-out),
              transform var(--wz-duration-normal) var(--wz-ease-out);
}

.hero__btn-primary:hover {
  box-shadow: 0 8px 28px rgba(255, 107, 53, 0.3);
  transform: translateY(-2px);
}

.hero__btn-ghost {
  padding: 12px 32px;
  background: transparent;
  border: 1px solid var(--wz-text);
  border-radius: 10px;
  color: var(--wz-text);
  font-family: var(--wz-font-body);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: border-color var(--wz-duration-fast) var(--wz-ease-out),
              color var(--wz-duration-fast) var(--wz-ease-out);
}

.hero__btn-ghost:hover {
  border-color: var(--wz-orange);
  color: var(--wz-orange);
}

.hero__stats {
  display: flex;
  gap: 40px;
  margin-top: 48px;
}

.hero__stat-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--wz-text);
  line-height: 1.2;
}

.hero__stat-label {
  font-size: 13px;
  color: var(--wz-text-muted);
  margin-top: 4px;
}

.hero__right {
  flex: 1;
}

.hero__img {
  width: 100%;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}

/* ---- Sections ---- */
.section {
  padding: 80px 24px;
}

.section--alt {
  background: var(--wz-bg);
  border-top: 1px solid var(--wz-border);
  border-bottom: 1px solid var(--wz-border);
}

.section__inner {
  max-width: 1200px;
  margin: 0 auto;
}

.section__inner--narrow {
  max-width: 640px;
}

.section__header {
  text-align: center;
  margin-bottom: 48px;
}

.section__header--row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  text-align: left;
}

.section__overline {
  font-size: 12px;
  color: var(--wz-orange);
  letter-spacing: 0.1em;
  font-weight: 500;
}

.section__title {
  font-family: var(--wz-font-display);
  font-size: 28px;
  font-weight: 700;
  color: var(--wz-text);
  margin-bottom: 8px;
}

.section__sub {
  font-size: 15px;
  color: var(--wz-text-muted);
}

.section__more {
  font-size: 14px;
  color: var(--wz-text-soft);
  display: flex;
  align-items: center;
  gap: 8px;
  transition: color var(--wz-duration-fast) var(--wz-ease-out);
  flex-shrink: 0;
}

.section__more:hover {
  color: var(--wz-orange);
}

/* ---- Categories ---- */
.cat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

@media (min-width: 640px) {
  .cat-grid { grid-template-columns: repeat(6, 1fr); }
}

.cat-card {
  text-align: center;
  cursor: pointer;
  padding: 24px 8px;
  border-radius: 12px;
  transition: background var(--wz-duration-fast) var(--wz-ease-out);
}

.cat-card:hover {
  background: var(--wz-bg-card);
}

.cat-card__icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 12px;
  border-radius: 50%;
  background: var(--wz-bg-card);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: var(--wz-orange);
  transition: background var(--wz-duration-fast) var(--wz-ease-out);
}

.cat-card:hover .cat-card__icon {
  background: var(--wz-orange-muted);
}

.cat-card__name {
  font-size: 13px;
  font-weight: 500;
  color: var(--wz-text-soft);
}

/* ---- Features ---- */
.features {
  display: flex;
  align-items: center;
  gap: 64px;
}

@media (max-width: 768px) {
  .features { flex-direction: column; gap: 40px; }
}

.features__left { flex: 1; }
.features__right { flex: 1; }

.features__label {
  font-size: 13px;
  color: var(--wz-orange);
  letter-spacing: 0.1em;
}

.features__title {
  font-family: var(--wz-font-display);
  font-size: 28px;
  font-weight: 700;
  color: var(--wz-text);
  margin: 12px 0 28px;
}

.features__list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.feature {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.feature__icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--wz-orange-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--wz-orange);
  font-size: 16px;
  flex-shrink: 0;
}

.feature__text h4 {
  font-size: 16px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 4px;
}

.feature__text p {
  font-size: 14px;
  color: var(--wz-text-muted);
}

.features__img {
  width: 100%;
  border-radius: 12px;
}

/* ---- Product Grid ---- */
.product-grid {
  display: grid;
  grid-template-columns: repeat(1, 1fr);
  gap: 24px;
}

@media (min-width: 640px) { .product-grid { grid-template-columns: repeat(2, 1fr); } }
@media (min-width: 1024px) { .product-grid { grid-template-columns: repeat(4, 1fr); } }

.product-card {
  background: var(--wz-bg-card);
  border-radius: 12px;
  overflow: hidden;
  transition: transform var(--wz-duration-normal) var(--wz-ease-out),
              box-shadow var(--wz-duration-normal) var(--wz-ease-out);
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.35);
}

.product-card__img {
  overflow: hidden;
}

.product-card__img img {
  width: 100%;
  height: 220px;
  object-fit: cover;
  transition: transform 0.5s var(--wz-ease-out);
}

.product-card:hover .product-card__img img {
  transform: scale(1.05);
}

.product-card__body {
  padding: 16px;
}

.product-card__tag {
  display: inline-block;
  font-size: 11px;
  font-weight: 500;
  padding: 2px 10px;
  border-radius: 4px;
  margin-bottom: 8px;
}

.product-card__tag--orange { background: rgba(255, 107, 53, 0.15); color: var(--wz-orange); }
.product-card__tag--green  { background: rgba(52, 199, 89, 0.15);  color: var(--wz-success); }
.product-card__tag--purple { background: rgba(175, 82, 222, 0.15); color: #af52de; }
.product-card__tag--blue   { background: rgba(0, 122, 255, 0.15);  color: #007aff; }

.product-card__title {
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 12px;
}

.product-card__prices {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.product-card__price {
  font-size: 18px;
  font-weight: 700;
  color: var(--wz-orange);
}

.product-card__old {
  font-size: 13px;
  color: var(--wz-text-muted);
  text-decoration: line-through;
}

.product-card__btn {
  width: 100%;
  padding: 8px 0;
  background: transparent;
  border: 1px solid var(--wz-orange);
  border-radius: 8px;
  color: var(--wz-orange);
  font-family: var(--wz-font-body);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background var(--wz-duration-fast) var(--wz-ease-out),
              color var(--wz-duration-fast) var(--wz-ease-out);
}

.product-card__btn:hover {
  background: var(--wz-orange);
  color: #fff;
}

/* ---- Reviews ---- */
.review-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}

@media (min-width: 768px) {
  .review-grid { grid-template-columns: repeat(3, 1fr); }
}

.review-card {
  background: var(--wz-bg-card);
  border-radius: 12px;
  padding: 28px;
  text-align: left;
}

.review-card__stars {
  color: var(--wz-orange);
  font-size: 16px;
  margin-bottom: 16px;
  display: flex;
  gap: 2px;
}

.review-card__text {
  font-size: 14px;
  color: var(--wz-text-soft);
  line-height: 1.7;
}

.review-card__user {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
}

.review-card__avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.review-card__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--wz-text);
}

.review-card__role {
  font-size: 12px;
  color: var(--wz-text-muted);
}

/* ---- Newsletter ---- */
.newsletter {
  text-align: center;
  padding: 0;
}

.newsletter__title {
  font-family: var(--wz-font-display);
  font-size: 28px;
  font-weight: 700;
  color: var(--wz-text);
  margin-bottom: 8px;
}

.newsletter__desc {
  font-size: 15px;
  color: var(--wz-text-muted);
  margin-bottom: 24px;
}

.newsletter__form {
  display: flex;
  max-width: 480px;
  margin: 0 auto;
}

.newsletter__input {
  flex: 1;
  padding: 14px 20px;
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-right: none;
  border-radius: 10px 0 0 10px;
  color: var(--wz-text);
  font-family: var(--wz-font-body);
  font-size: 14px;
  outline: none;
  transition: border-color var(--wz-duration-fast) var(--wz-ease-out);
}

.newsletter__input:focus {
  border-color: var(--wz-orange);
}

.newsletter__input::placeholder {
  color: var(--wz-text-muted);
}

.newsletter__btn {
  padding: 14px 28px;
  background: var(--wz-orange);
  border: none;
  border-radius: 0 10px 10px 0;
  color: #fff;
  font-family: var(--wz-font-body);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background var(--wz-duration-fast) var(--wz-ease-out);
}

.newsletter__btn:hover {
  background: var(--wz-orange-dark);
}

/* ---- Footer ---- */
.footer {
  padding: 64px 24px 32px;
  border-top: 1px solid var(--wz-border);
}

.footer__grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 40px;
  padding-bottom: 40px;
  border-bottom: 1px solid var(--wz-border);
}

@media (min-width: 768px) {
  .footer__grid { grid-template-columns: 2fr 1fr 1fr 1fr; }
}

.footer__logo {
  font-family: 'Noto Serif SC', serif;
  font-size: 22px;
  font-weight: 700;
  color: var(--wz-orange);
}

.footer__brand h3 {
  font-size: 20px;
  font-weight: 700;
  color: var(--wz-text);
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
}

.footer__desc {
  font-size: 14px;
  color: var(--wz-text-muted);
  line-height: 1.6;
  margin-bottom: 16px;
  max-width: 280px;
}

.footer__social {
  display: flex;
  gap: 10px;
}

.footer__social a {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--wz-bg-card);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--wz-text-soft);
  transition: background var(--wz-duration-fast) var(--wz-ease-out),
              color var(--wz-duration-fast) var(--wz-ease-out);
}

.footer__social a:hover {
  background: var(--wz-orange);
  color: #fff;
}

.footer__col h4 {
  font-size: 16px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 16px;
}

.footer__col ul {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.footer__col a {
  font-size: 14px;
  color: var(--wz-text-muted);
  transition: color var(--wz-duration-fast) var(--wz-ease-out);
}

.footer__col a:hover {
  color: var(--wz-orange);
}

.footer__col li {
  font-size: 14px;
  color: var(--wz-text-muted);
  display: flex;
  align-items: center;
  gap: 8px;
}

.footer__bottom {
  text-align: center;
  padding-top: 24px;
  font-size: 13px;
  color: var(--wz-text-muted);
}
</style>
