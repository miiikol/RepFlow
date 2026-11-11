<template>
  <div class="page">
    <div class="page-title">
      <div>
        <h2>需求管理</h2>
        <div class="sub-text">需求单全生命周期 · 共 {{ total }} 条</div>
      </div>
      <div class="actions">
        <el-button :icon="Download" @click="exportList">导出 Excel</el-button>
        <el-button type="primary" :icon="Plus" v-if="auth.hasPerm('requirement:create')"
                   @click="$router.push('/requirements/create')">新建需求</el-button>
      </div>
    </div>

    <div class="card filter-card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索需求编号 / 标题" clearable style="width: 240px"
                  :prefix-icon="Search" @keyup.enter="search" />
        <el-select v-model="query.statusList" multiple collapse-tags placeholder="状态" clearable style="width: 220px">
          <el-option v-for="item in dicts.requirementStatuses" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.deptId" placeholder="业务部门" clearable style="width: 150px">
          <el-option v-for="dept in meta.depts" :key="dept.id" :label="dept.name" :value="dept.id" />
        </el-select>
        <el-select v-model="query.priority" placeholder="优先级" clearable style="width: 130px">
          <el-option v-for="item in dicts.priorities" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.type" placeholder="需求类型" clearable style="width: 150px">
          <el-option v-for="item in dicts.requirementTypes" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-checkbox v-model="query.mine">只看与我相关</el-checkbox>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" style="width: 100%" @row-dblclick="openDetail">
        <el-table-column prop="reqNo" label="需求编号" width="150">
          <template #default="{ row }">
            <span class="mono">{{ row.reqNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="需求标题" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="openDetail(row)">{{ row.title }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="deptName" label="业务部门" width="110" />
        <el-table-column prop="priority" label="优先级" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="priorityType(row.priority)" effect="dark">{{ row.priority }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="任务进度" width="150">
          <template #default="{ row }">
            <el-progress :percentage="row.progress" :stroke-width="8"
                         :status="row.progress === 100 ? 'success' : null" />
            <span class="progress-text">{{ row.taskDone }}/{{ row.taskTotal }} 任务</span>
          </template>
        </el-table-column>
        <el-table-column prop="expectDate" label="期望交付" width="110">
          <template #default="{ row }">
            <span :class="{ overdue: row.overdue }">{{ fmtDate(row.expectDate) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="pmName" label="项目经理" width="90">
          <template #default="{ row }">{{ row.pmName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="150">
          <template #default="{ row }">{{ fmtDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" v-if="canEdit(row)" @click="$router.push(`/requirements/${row.id}/edit`)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next, jumper" :total="total"
                       v-model:current-page="query.page" v-model:page-size="query.size"
                       :page-sizes="[10, 20, 50]" @change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Download, Plus, Search } from '@element-plus/icons-vue'
import { api } from '../api'
import { downloadFile } from '../api/http'
import { useAuthStore } from '../store/auth'
import { fmtDate, fmtDateTime, priorityType, statusType } from '../utils/dict'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const meta = computed(() => auth.meta || { depts: [], dicts: {} })
const dicts = computed(() => meta.value.dicts || {})

const query = reactive({
  page: 1,
  size: 20,
  keyword: '',
  statusList: [],
  deptId: null,
  priority: '',
  type: '',
  mine: false
})

const params = () => ({
  page: query.page,
  size: query.size,
  keyword: query.keyword || undefined,
  status: query.statusList.length ? query.statusList.join(',') : undefined,
  deptId: query.deptId || undefined,
  priority: query.priority || undefined,
  type: query.type || undefined,
  mine: query.mine || undefined
})

const load = async () => {
  loading.value = true
  try {
    const data = await api.requirements(params())
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const search = () => {
  query.page = 1
  load()
}

const reset = () => {
  query.keyword = ''
  query.statusList = []
  query.deptId = null
  query.priority = ''
  query.type = ''
  query.mine = false
  search()
}

const openDetail = (row) => router.push(`/requirements/${row.id}`)

const canEdit = (row) => ['DRAFT', 'REJECTED'].includes(row.status) && auth.hasPerm('requirement:edit')

const exportList = async () => {
  const p = new URLSearchParams()
  for (const [key, value] of Object.entries(params())) {
    if (value !== undefined && key !== 'page' && key !== 'size') p.append(key, value)
  }
  await downloadFile('/requirements/export?' + p.toString(), '需求清单.xlsx')
  ElMessage.success('导出成功')
}

onMounted(load)
</script>

<style scoped>
.actions {
  display: flex;
  gap: 8px;
}
.filter-card {
  padding: 14px 16px 2px;
  margin-bottom: 14px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  padding: 14px 16px;
}
.overdue {
  color: #f56c6c;
  font-weight: 600;
}
.progress-text {
  font-size: 11px;
  color: #93a4b8;
}
</style>
