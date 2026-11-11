<template>
  <div class="page">
    <div class="page-title">
      <div>
        <h2>用户管理</h2>
        <div class="sub-text">账号、部门与角色分配 · 内置 7 类角色</div>
      </div>
      <el-button type="primary" @click="openCreate">新增用户</el-button>
    </div>

    <div class="card">
      <el-table :data="users" v-loading="loading">
        <el-table-column prop="username" label="用户名" width="130">
          <template #default="{ row }"><span class="mono">{{ row.username }}</span></template>
        </el-table-column>
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column prop="deptName" label="部门" width="130" />
        <el-table-column label="角色" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role" size="small" style="margin-right: 6px" effect="plain">
              {{ roleName(role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="170" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="resetPassword(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog.visible" :title="dialog.id ? '编辑用户' : '新增用户'" width="520px">
      <el-form label-width="90px">
        <el-form-item label="用户名">
          <el-input v-model="dialog.username" :disabled="Boolean(dialog.id)" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="dialog.realName" />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="dialog.deptId" style="width: 100%">
            <el-option v-for="dept in meta.depts" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="dialog.roleIds" multiple style="width: 100%">
            <el-option v-for="role in roles" :key="role.id" :label="`${role.name}（${role.code}）`" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="dialog.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="dialog.email" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="dialog.status" :active-value="1" :inactive-value="0"
                     active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="初始密码" v-if="!dialog.id">
          <el-input v-model="dialog.password" placeholder="留空则使用 Reqflow@123" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.loading" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const meta = computed(() => auth.meta || { depts: [] })
const loading = ref(false)
const users = ref([])
const roles = ref([])

const roleName = (code) => {
  const role = roles.value.find((r) => r.code === code)
  return role ? role.name : code
}

const load = async () => {
  loading.value = true
  try {
    const [userList, roleList] = await Promise.all([api.adminUsers(), api.roles()])
    users.value = userList
    roles.value = roleList
  } finally {
    loading.value = false
  }
}

const dialog = reactive({
  visible: false, loading: false, id: null, username: '', realName: '', deptId: null,
  roleIds: [], phone: '', email: '', status: 1, password: ''
})
const openCreate = () => {
  Object.assign(dialog, {
    visible: true, loading: false, id: null, username: '', realName: '', deptId: auth.user.deptId,
    roleIds: [], phone: '', email: '', status: 1, password: ''
  })
}
const openEdit = (row) => {
  Object.assign(dialog, {
    visible: true, loading: false, id: row.id, username: row.username, realName: row.realName,
    deptId: row.deptId, roleIds: [...(row.roleIds || [])], phone: row.phone || '',
    email: row.email || '', status: row.status, password: ''
  })
}
const save = async () => {
  if (!dialog.username || !dialog.realName || !dialog.deptId) {
    ElMessage.warning('请填写用户名、姓名与部门')
    return
  }
  dialog.loading = true
  try {
    const payload = {
      username: dialog.username,
      realName: dialog.realName,
      deptId: dialog.deptId,
      roleIds: dialog.roleIds,
      phone: dialog.phone,
      email: dialog.email,
      status: dialog.status,
      password: dialog.password || undefined
    }
    if (dialog.id) {
      await api.updateUser(dialog.id, payload)
      ElMessage.success('用户已更新')
    } else {
      await api.createUser(payload)
      ElMessage.success('用户已创建，首次登录需修改密码')
    }
    dialog.visible = false
    await load()
  } finally {
    dialog.loading = false
  }
}

const resetPassword = async (row) => {
  try {
    await ElMessageBox.confirm(`确认将 ${row.realName} 的密码重置为 Reset@12345？`, '重置密码', { type: 'warning' })
    await api.resetPassword(row.id)
    ElMessage.success('密码已重置，用户下次登录需修改')
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

onMounted(load)
</script>
