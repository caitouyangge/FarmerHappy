import { createRouter, createWebHistory } from 'vue-router';
import Login from '../components/Login.vue';
import Register from '../components/Register.vue';
import Home from '../home/Home.vue';
import ProductList from '../trading/ProductList.vue';
import Community from '../community/Community.vue';
import ContentDetail from '../community/ContentDetail.vue';
import ContentForm from '../community/ContentForm.vue';

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/register',
    name: 'Register',
    component: Register
  },
  {
    path: '/home',
    name: 'Home',
    component: Home,
    meta: { requiresAuth: true }
  },
  {
    path: '/trading',
    name: 'Trading',
    component: ProductList,
    meta: { requiresAuth: true }
  },
  {
    path: '/community',
    name: 'Community',
    component: Community,
    meta: { requiresAuth: true }
  },
  {
    path: '/community/publish',
    name: 'ContentForm',
    component: ContentForm,
    meta: { requiresAuth: true }
  },
  {
    path: '/community/:id',
    name: 'ContentDetail',
    component: ContentDetail,
    meta: { requiresAuth: true }
  }
];

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
});

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStr = localStorage.getItem('user');
  const isAuthenticated = !!userStr;

  if (to.meta.requiresAuth && !isAuthenticated) {
    next('/login');
  } else if (to.path === '/login' && isAuthenticated) {
    next('/home');
  } else {
    next();
  }
});

export default router;
