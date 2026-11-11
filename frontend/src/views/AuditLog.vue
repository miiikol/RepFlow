<template>
  <div class="page">
    <div class="page-title">
      <div>
        <h2>审计日志</h2>
        <div class="sub-text">关键操作留痕 · 只读，不提供删除入口</div>
      </div>
    </div>

    <div class="card filter-card">
      <div class="toolbar">
        <el-input v-model="query.username" placeholder="操作人" clearable style="width: 140px" />
        <el-input v-model="query.module" placeholder="模块，如 需求管理" clearable style="width: 160px" />
        <el-input v-model="query.objectId" placeholder="单据 ID" clearable style="width: 120px" />
        <el-date-picker v-model="range" type="datetimerange" start-placeholder="开始时间"
                        end-placeholder="结束时间" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 360px" />
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" size="small">
        <el-table-column label="时间" width="155">
          <template #default="{ row }">{{ fmtDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="username" label="操作人" width="100" />
        <el-table-column prop="roleCodes" label="角色" width="120" show-overflow-tooltip />
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column prop="action" label="动作" min-width="180" show-overflow-tooltip />
        <el-table-column label="对象" width="140">
          <template #default="{ row }">
            <span class="mono">{{ row.objectType }}#{{ row.objectId || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.result === 'SUCCESS' ? 'success' : 'danger'">
              {{ row.result === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="durationMs" label="耗时(ms)" width="90" />
        <el-table-column prop="ip" label="IP" width="120" />
        <el-table-column prop="traceId" label="追踪号" width="100" />
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                       v-model:current-page="query.page" v-model:page-size="query.size"
                       :page-sizes="[20, 50, 100]" @change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api } from '../api'
import { fmtDateTime } from '../utils/dict'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const range = ref([])
const query = reactive({
  page: 1, size: 20, username: '', module: '', objectId: ''
})

const load = async () => {
  loading.value = true
  try {
    const data = await api.auditLogs({
      page: query.page,
      size: query.size,
      username: query.username || undefined,
      module: query.module || undefined,
      objectId: query.objectId || undefined,
      start: range.value?.[0] || undefined,
      end: range.value?.[1] || undefined
    })
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
  query.username = ''
  query.module = ''
  query.objectId = ''
  range.value = []
  search()
}

onMounted(load)
</script>

<style scoped>
.filter-card {
  padding: 14px 16px 4px;
  margin-bottom: 14px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  padding: 14px 16px;
}
</style>
