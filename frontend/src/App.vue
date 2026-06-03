<script setup lang="ts">
import HeaderNav from './layouts/HeaderNav.vue'
import AppFooter from './layouts/AppFooter.vue'
</script>

<template>
  <HeaderNav />
  <main>
    <router-view />
  </main>
  <AppFooter />
</template>
