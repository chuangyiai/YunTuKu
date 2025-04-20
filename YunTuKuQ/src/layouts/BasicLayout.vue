<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalSider from '@/components/GlobalSider.vue'
</script>

<template>
  <div id="basic-layout">
    <a-layout style="min-height: 100vh">
      <a-layout-header class="header"><GlobalHeader /> </a-layout-header>
      <a-layout>
        <GlobalSider class="sidebar" />
        <a-layout-content class="content">
          <router-view />
        </a-layout-content>
      </a-layout>
      <a-layout-footer class="footer"
        ><a href="https://slxy.cn/" target="_blank">云图库Demo By 魏琪魁 </a>
      </a-layout-footer>
    </a-layout>
  </div>
</template>

<style scoped>
#basic-layout .footer {
  background-color: azure;
  padding: 1px;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  text-align: center;
}
#basic-layout .content {
  padding: 20px;
  background: linear-gradient(to right, #fefefe, #fff);
  margin-bottom: 28px;
}
#basic-layout .header {
  padding-inline: 20px;
  background: white;
  color: unset;
  margin-bottom: 1px;
}
#basic-layout .sidebar {
  background-color: white;
  border-right: 1px solid #eee;
  padding-top: 20px;
}
#basic-layout :deep(ant-menu-root) {
  border-bottom: none !important;
  border-inline-end: none !important;
}
</style>
