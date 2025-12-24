<template>
  <div class="app-container">
    <Navbar v-if="showNavbar" />
    <router-view />
  </div>
</template>

<script>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import Navbar from './components/Navbar.vue';

export default {
  name: 'App',
  components: {
    Navbar
  },
  setup() {
    const route = useRoute();
    
    const showNavbar = computed(() => {
      // 登录和注册页面不显示导航栏
      return route.path !== '/login' && route.path !== '/register';
    });

    return {
      showNavbar
    };
  }
}
</script>

<style>
@import './assets/styles/theme.css';

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  line-height: 1.5;
}

.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
</style>
