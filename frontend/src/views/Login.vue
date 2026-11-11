<template>
  <div class="login-page">
    <div class="login-left">
      <div class="brand-line">
        <div class="logo">RF</div>
        <span>ReqFlow</span>
      </div>
      <h1>让每一条需求<br />都可追溯、可控制、可验收</h1>
      <p>
        面向业务部门与交付团队的轻量协同平台。需求提出、评审、排期、任务分解、
        开发测试、验收归档，全流程在线留痕。
      </p>
      <div class="feature-list">
        <div><el-icon><CircleCheck /></el-icon> 需求状态机与版本基线</div>
        <div><el-icon><CircleCheck /></el-icon> 评审、变更、验收单据闭环</div>
        <div><el-icon><CircleCheck /></el-icon> RBAC + 数据权限 + 审计日志</div>
      </div>
    </div>

    <div class="login-right">
      <el-card class="login-card" shadow="never">
        <div class="card-title">登录工作台</div>
        <div class="card-sub">使用演示账号体验完整交付流程</div>
        <el-form :model="form" @keyup.enter="submit">
          <el-form-item>
            <el-input v-model="form.username" size="large" placeholder="用户名" :prefix-icon="User" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" size="large" type="password" show-password
                      placeholder="密码" :prefix-icon="Lock" />
          </el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="submit">
            登 录
          </el-button>
        </el-form>
        <el-divider>快捷体验</el-divider>
        <div class="quick-grid">
          <div v-for="account in accounts" :key="account.username" class="quick-item"
               @click="fill(account)">
            <div class="quick-role">{{ account.role }}</div>
            <div class="quick-user">{{ account.username }}</div>
          </div>
        </div>
        <div class="tip">所有演示账号密码：Reqflow@123</div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'Reqflow@123' })

const accounts = [
  { role: '系统管理员', username: 'admin' },
  { role: '需求提出人', username: 'biz' },
  { role: '需求分析', username: 'ra' },
  { role: '项目经理', username: 'pm' },
  { role: '开发工程师', username: 'dev' },
  { role: '测试工程师', username: 'qa' },
  { role: '交付验收人', username: 'accept' }
]

const fill = (account) => {
  form.username = account.username
  form.password = 'Reqflow@123'
}

const submit = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await auth.login(form)
    ElMessage.success('欢迎回来，' + auth.user.realName)
    router.push(route.query.redirect || '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  background: #0d1f36;
}
.login-left {
  flex: 1.15;
  padding: 70px 76px;
  color: #fff;
  background:
    radial-gradient(circle at 20% 20%, rgba(47, 111, 237, 0.35), transparent 45%),
    radial-gradient(circle at 80% 80%, rgba(56, 189, 248, 0.18), transparent 40%),
    linear-gradient(145deg, #0d1f36, #163454);
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.brand-line {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 46px;
}
.logo {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #4b8bff, #2f6fed);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
}
h1 {
  font-size: 38px;
  line-height: 1.35;
  margin: 0 0 22px;
  letter-spacing: 1px;
}
p {
  color: #a9bdd4;
  line-height: 1.9;
  max-width: 520px;
  margin: 0 0 34px;
}
.feature-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  color: #d6e2f0;
}
.feature-list > div {
  display: flex;
  align-items: center;
  gap: 10px;
}
.feature-list .el-icon {
  color: #57c78b;
}
.login-right {
  width: 480px;
  background: #f4f6fa;
  display: flex;
  align-items: center;
  justify-content: center;
}
.login-card {
  width: 380px;
  border-radius: 16px;
  padding: 10px 8px 16px;
}
.card-title {
  font-size: 22px;
  font-weight: 700;
  color: #10243f;
}
.card-sub {
  color: #8092a8;
  font-size: 13px;
  margin: 6px 0 24px;
}
.login-btn {
  width: 100%;
  background: #2f6fed;
  border-color: #2f6fed;
}
.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}
.quick-item {
  border: 1px solid #e2e9f3;
  border-radius: 8px;
  padding: 8px 4px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  background: #fafcff;
}
.quick-item:hover {
  border-color: #2f6fed;
  background: #eef4ff;
}
.quick-role {
  font-size: 11px;
  color: #6b7f96;
}
.quick-user {
  font-size: 13px;
  color: #10243f;
  font-weight: 600;
  margin-top: 2px;
}
.tip {
  text-align: center;
  color: #93a4b8;
  font-size: 12px;
  margin-top: 14px;
}
</style>
