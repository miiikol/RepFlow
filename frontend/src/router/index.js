import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('../layout/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '工作台', perm: 'requirement:view' }
      },
      {
        path: 'requirements',
        name: 'requirements',
        component: () => import('../views/RequirementList.vue'),
        meta: { title: '需求管理', perm: 'requirement:view' }
      },
      {
        path: 'requirements/create',
        name: 'requirementCreate',
        component: () => import('../views/RequirementForm.vue'),
        meta: { title: '新建需求', perm: 'requirement:create' }
      },
      {
        path: 'requirements/:id',
        name: 'requirementDetail',
        component: () => import('../views/RequirementDetail.vue'),
        meta: { title: '需求详情', perm: 'requirement:view' }
      },
      {
        path: 'requirements/:id/edit',
        name: 'requirementEdit',
        component: () => import('../views/RequirementForm.vue'),
        meta: { title: '编辑需求', perm: 'requirement:edit' }
      },
      {
        path: 'reviews',
        name: 'reviews',
        component: () => import('../views/ReviewList.vue'),
        meta: { title: '评审管理', perm: 'review:view' }
      },
      {
        path: 'changes',
        name: 'changes',
        component: () => import('../views/ChangeList.vue'),
        meta: { title: '变更管理', perm: 'change:view' }
      },
      {
        path: 'projects',
        name: 'projects',
        component: () => import('../views/ProjectList.vue'),
        meta: { title: '计划与任务', perm: 'project:view' }
      },
      {
        path: 'delivery',
        name: 'delivery',
        component: () => import('../views/DeliveryList.vue'),
        meta: { title: '交付验收', perm: 'delivery:view' }
      },
      {
        path: 'admin/users',
        name: 'users',
        component: () => import('../views/UserList.vue'),
        meta: { title: '用户管理', perm: 'admin:user' }
      },
      {
        path: 'admin/audit',
        name: 'audit',
        component: () => import('../views/AuditLog.vue'),
        meta: { title: '审计日志', perm: 'admin:audit' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach(async (to) => {
  if (to.meta.public) return true
  const auth = useAuthStore()
  if (!auth.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (!auth.user) {
    try {
      await auth.loadMe()
    } catch (e) {
      auth.logout()
      return '/login'
    }
  }
  if (!auth.meta) {
    try {
      await auth.loadMeta()
    } catch (e) {
      // 后端不可用时仍允许进入页面，页面会给出错误提示
    }
  }
  if (to.meta.perm && !auth.hasPerm(to.meta.perm)) {
    return '/dashboard'
  }
  return true
})

export default router
