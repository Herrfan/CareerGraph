import { createRouter, createWebHistory } from 'vue-router';
import HomePage from '@/pages/HomePage.vue';
import AuthPage from '@/pages/AuthPage.vue';
import AdminJobsPage from '@/pages/AdminJobsPage.vue';
import HistoryPage from '@/pages/HistoryPage.vue';
import ProfileUploadPage from '@/pages/ProfileUploadPage.vue';
import JobBrowserPage from '@/pages/JobBrowserPage.vue';
import JobMatchingPage from '@/pages/JobMatchingPage.vue';
import CareerPathPage from '@/pages/CareerPathPage.vue';
import ReportPage from '@/pages/ReportPage.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomePage },
    { path: '/auth', name: 'auth', component: AuthPage },
    { path: '/admin/jobs', name: 'admin-jobs', component: AdminJobsPage },
    { path: '/history', name: 'history', component: HistoryPage },
    { path: '/profile', name: 'profile', component: ProfileUploadPage },
    { path: '/jobs', name: 'jobs', component: JobBrowserPage },
    { path: '/matching', name: 'matching', component: JobMatchingPage },
    { path: '/career-path', name: 'career-path', component: CareerPathPage },
    { path: '/report', name: 'report', component: ReportPage },
  ],
  scrollBehavior() {
    return { top: 0 };
  },
});

export default router;
