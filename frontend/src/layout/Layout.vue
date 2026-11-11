<template>
  <el-container class="layout">
    <el-aside width="228px" class="aside">
      <div class="brand">
        <div class="logo">RF</div>
        <div>
          <div class="brand-name">ReqFlow</div>
          <div class="brand-sub">需求交付协同平台</div>
        </div>
      </div>
      <el-menu :default-active="activeMenu" router class="menu" background-color="#10243f"
               text-color="#b9c7d8" active-text-color="#ffffff">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
      <div class="aside-footer">MVP · 单体分层架构</div>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ route.meta.title || '工作台' }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-tag size="small" effect="plain" class="role-tag">{{ roleText }}</el-tag>
          <el-dropdown @command="onCommand">
            <span class="user">
              <el-avatar :size="30" class="avatar">{{ (auth.user?.realName || 'U').slice(0, 1) }}</el-avatar>
              <span>{{ auth.user?.realName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>

    <el-dialog v-model="pwdVisible" title="修改密码" width="440px">
      <el-form :model="pwdForm" label-width="90px">
        <el-form-item label="原密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少 8 位，含字母和数字" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="pwdForm.confirm" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="changePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../store/auth'
import { api } from '../api'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const allMenus = [
  { path: '/dashboard', title: '工作台', icon: 'Odometer', perm: 'requirement:view' },
  { path: '/requirements', title: '需求管理', icon: 'Document', perm: 'requirement:view' },
  { path: '/reviews', title: '评审管理', icon: 'Stamp', perm: 'review:view' },
  { path: '/changes', title: '变更管理', icon: 'Refresh', perm: 'change:view' },
  { path: '/projects', title: '计划与任务', icon: 'Calendar', perm: 'project:view' },
  { path: '/delivery', title: '交付验收', icon: 'Finished', perm: 'delivery:view' },
  { path: '/admin/users', title: '用户管理', icon: 'User', perm: 'admin:user' },
  { path: '/admin/audit', title: '审计日志', icon: 'Tickets', perm: 'admin:audit' }
]

const menus = computed(() => allMenus.filter((item) => auth.hasPerm(item.perm)))
const activeMenu = computed(() => {
  if (route.path.startsWith('/requirements')) return '/requirements'
  if (route.path.startsWith('/admin/users')) return '/admin/users'
  if (route.path.startsWith('/admin/audit')) return '/admin/audit'
  return route.path
})
const roleText = computed(() => {
  const map = {
    ADMIN: '系统管理员', BUSINESS: '需求提出人', RA: '需求分析', PM: '项目经理',
    DEV: '开发工程师', QA: '测试工程师', ACCEPT: '交付验收人'
  }
  return (auth.roles || []).map((r) => map[r] || r).join(' / ')
})

const pwdVisible = ref(false)
const pwdLoading = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })

const onCommand = (command) => {
  if (command === 'logout') {
    auth.logout()
    router.push('/login')
  } else if (command === 'password') {
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirm = ''
    pwdVisible.value = true
  }
}

const changePassword = async () => {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning('请填写原密码与新密码')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirm) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  pwdLoading.value = true
  try {
    await api.changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功')
    pwdVisible.value = false
  } finally {
    pwdLoading.value = false
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
}
.aside {
  background: var(--rf-navy);
  display: flex;
  flex-direction: column;
}
.brand {
  height: 72px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.logo {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: linear-gradient(135deg, #4b8bff, #2f6fed);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  letter-spacing: 1px;
}
.brand-name {
  font-size: 17px;
  font-weight: 700;
}
.brand-sub {
  font-size: 11px;
  color: #8fa6c1;
  margin-top: 2px;
}
.menu {
  border-right: none;
  flex: 1;
  padding-top: 8px;
}
.menu :deep(.el-menu-item) {
  height: 46px;
  margin: 4px 10px;
  border-radius: 8px;
}
.menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, #2f6fed, #1b3a5c);
}
.aside-footer {
  color: #637d9a;
  font-size: 11px;
  padding: 16px 20px;
}
.header {
  height: 62px;
  background: #fff;
  border-bottom: 1px solid #e8edf4;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}
.role-tag {
  color: #456;
}
.user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #2c3e50;
}
.avatar {
  background: #2f6fed;
  font-size: 13px;
}
.main {
  padding: 0;
  overflow-y: auto;
  background: var(--rf-bg);
}
</style>
