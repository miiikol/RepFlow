<template>
  <div class="page">
    <div class="page-title">
      <div>
        <h2>交付验收</h2>
        <div class="sub-text">交付物清单与验收结论 · 验收通过后可归档</div>
      </div>
    </div>

    <div class="card filter-card">
      <div class="toolbar">
        <el-select v-model="query.status" placeholder="验收状态" clearable style="width: 160px" @change="search">
          <el-option v-for="item in statuses" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-checkbox v-model="query.mine" @change="search">待我验收</el-checkbox>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="acceptanceNo" label="验收单号" width="155">
          <template #default="{ row }"><span class="mono">{{ row.acceptanceNo }}</span></template>
        </el-table-column>
        <el-table-column label="关联需求" min-width="260">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="$router.push(`/requirements/${row.requirementId}`)">
              {{ row.requirementNo }} {{ row.requirementTitle }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="round" label="轮次" width="70" align="center" />
        <el-table-column prop="submitterName" label="提交人" width="90" />
        <el-table-column prop="acceptorName" label="验收人" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="acceptanceStatusType(row.status)">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="150">
          <template #default="{ row }">{{ fmtDateTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="query.page" :page-size="query.size" @change="load" />
      </div>
    </div>

    <el-drawer v-model="drawer.visible" title="验收详情" size="560px">
      <div v-if="current" v-loading="drawer.loading">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="验收单号">{{ current.acceptanceNo }}</el-descriptions-item>
          <el-descriptions-item label="轮次">第 {{ current.round }} 轮</el-descriptions-item>
          <el-descriptions-item label="关联需求" :span="2">
            <el-link type="primary" :underline="false" @click="$router.push(`/requirements/${current.requirementId}`)">
              {{ current.requirementNo }} {{ current.requirementTitle }}
            </el-link>
          </el-descriptions-item>
          <el-descriptions-item label="提交人">{{ current.submitterName }}</el-descriptions-item>
          <el-descriptions-item label="验收人">{{ current.acceptorName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="acceptanceStatusType(current.status)">{{ current.statusLabel }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ fmtDateTime(current.submittedAt) }}</el-descriptions-item>
          <el-descriptions-item label="验收意见" :span="2">{{ current.comment || '-' }}</el-descriptions-item>
          <el-descriptions-item label="返工要求" :span="2">{{ current.reworkRequirement || '-' }}</el-descriptions-item>
          <el-descriptions-item label="交付物快照" :span="2">{{ current.deliverableSnapshot || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="drawer-title">交付物清单</div>
        <el-table :data="deliverables" size="small">
          <el-table-column prop="typeLabel" label="类型" width="140" />
          <el-table-column prop="name" label="名称" min-width="220" show-overflow-tooltip />
          <el-table-column label="文件" width="80">
            <template #default="{ row }">
              <el-button v-if="row.fileName" link type="primary" @click="downloadDeliverable(row)">下载</el-button>
              <span v-else class="sub-text">仅登记</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="drawer-actions" v-if="canConclude">
          <el-button type="success" @click="openConclusion(true)">验收通过</el-button>
          <el-button type="danger" plain @click="openConclusion(false)">验收不通过</el-button>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="conclusion.visible" title="提交验收结论" width="500px">
      <el-form label-width="90px">
        <el-form-item label="结论">
          <el-tag :type="conclusion.pass ? 'success' : 'danger'">{{ conclusion.pass ? '通过' : '不通过' }}</el-tag>
        </el-form-item>
        <el-form-item label="验收意见">
          <el-input v-model="conclusion.comment" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="返工要求" v-if="!conclusion.pass">
          <el-input v-model="conclusion.reworkRequirement" type="textarea" :rows="3"
                    placeholder="系统将自动生成返工任务" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="conclusion.visible = false">取消</el-button>
        <el-button type="primary" :loading="conclusion.loading" @click="submitConclusion">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { downloadFile } from '../api/http'
import { useAuthStore } from '../store/auth'
import { acceptanceStatusType, fmtDateTime } from '../utils/dict'

const auth = useAuthStore()
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 20, status: '', mine: false })
const statuses = computed(() => auth.meta?.dicts?.acceptanceStatuses || [])

const load = async () => {
  loading.value = true
  try {
    const data = await api.acceptances({
      page: query.page,
      size: query.size,
      status: query.status || undefined,
      mine: query.mine || undefined
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

const drawer = reactive({ visible: false, loading: false })
const current = ref(null)
const deliverables = ref([])
const canConclude = computed(() => current.value?.status === 'PENDING'
  && auth.hasPerm('delivery:accept')
  && (current.value.acceptorId === auth.user.id || auth.isAdmin))

const openDetail = async (row) => {
  current.value = row
  drawer.visible = true
  drawer.loading = true
  try {
    deliverables.value = await api.deliverables(row.requirementId)
  } finally {
    drawer.loading = false
  }
}
const downloadDeliverable = (row) => downloadFile(`/deliverables/${row.id}/download`, row.fileName || row.name)

const conclusion = reactive({ visible: false, loading: false, pass: true, comment: '', reworkRequirement: '' })
const openConclusion = (pass) => {
  conclusion.pass = pass
  conclusion.comment = ''
  conclusion.reworkRequirement = ''
  conclusion.visible = true
}
const submitConclusion = async () => {
  if (!conclusion.pass && !conclusion.reworkRequirement) {
    ElMessage.warning('验收不通过时必须填写返工要求')
    return
  }
  conclusion.loading = true
  try {
    await api.concludeAcceptance(current.value.id, {
      pass: conclusion.pass,
      comment: conclusion.comment,
      reworkRequirement: conclusion.reworkRequirement
    })
    ElMessage.success('验收结论已提交')
    conclusion.visible = false
    drawer.visible = false
    await load()
  } finally {
    conclusion.loading = false
  }
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
.drawer-title {
  font-weight: 600;
  margin: 22px 0 10px;
  color: #23384f;
}
.drawer-actions {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}
</style>
