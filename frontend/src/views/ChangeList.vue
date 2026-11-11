<template>
  <div class="page">
    <div class="page-title">
      <div>
        <h2>变更管理</h2>
        <div class="sub-text">变更申请、影响评估、审批与基线更新</div>
      </div>
      <el-button type="primary" v-if="auth.hasPerm('change:create')" @click="openCreate">发起变更</el-button>
    </div>

    <div class="card filter-card">
      <div class="toolbar">
        <el-select v-model="query.status" placeholder="变更状态" clearable style="width: 160px" @change="search">
          <el-option v-for="item in statuses" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="changeNo" label="变更单号" width="155">
          <template #default="{ row }"><span class="mono">{{ row.changeNo }}</span></template>
        </el-table-column>
        <el-table-column label="关联需求" min-width="220">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="$router.push(`/requirements/${row.requirementId}`)">
              {{ row.requirementNo }} {{ row.requirementTitle }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="变更原因" min-width="200" show-overflow-tooltip />
        <el-table-column prop="impactHours" label="工时影响" width="90" />
        <el-table-column prop="applicantName" label="申请人" width="90" />
        <el-table-column prop="approverName" label="审批人" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="changeStatusType(row.status)">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="150">
          <template #default="{ row }">{{ fmtDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING' && auth.hasPerm('change:approve')"
                       link type="primary" @click="openApprove(row)">审批</el-button>
            <el-button v-if="row.status === 'APPROVED' && auth.hasPerm('change:apply')"
                       link type="success" @click="apply(row)">实施生效</el-button>
            <el-button link type="info" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="query.page" :page-size="query.size" @change="load" />
      </div>
    </div>

    <el-dialog v-model="create.visible" title="发起变更申请" width="560px">
      <el-form label-width="90px">
        <el-form-item label="关联需求" required>
          <el-select v-model="create.requirementId" filterable style="width: 100%">
            <el-option v-for="item in candidates" :key="item.id"
                       :label="`${item.reqNo} ${item.title}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="变更原因">
          <el-input v-model="create.reason" />
        </el-form-item>
        <el-form-item label="变更内容">
          <el-input v-model="create.content" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="工时影响">
          <el-input-number v-model="create.impactHours" :min="0" :step="0.5" />
        </el-form-item>
        <el-form-item label="影响说明">
          <el-input v-model="create.impactDesc" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="create.visible = false">取消</el-button>
        <el-button type="primary" :loading="create.loading" @click="submitCreate">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="approve.visible" title="变更审批" width="480px">
      <el-form label-width="80px">
        <el-form-item label="结果">
          <el-radio-group v-model="approve.approved">
            <el-radio :value="true">批准</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="意见">
          <el-input v-model="approve.comment" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approve.visible = false">取消</el-button>
        <el-button type="primary" :loading="approve.loading" @click="submitApprove">提交</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="drawer.visible" title="变更详情" size="520px">
      <template v-if="current">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="变更单号">{{ current.changeNo }}</el-descriptions-item>
          <el-descriptions-item label="关联需求">{{ current.requirementNo }} {{ current.requirementTitle }}</el-descriptions-item>
          <el-descriptions-item label="变更原因">{{ current.reason }}</el-descriptions-item>
          <el-descriptions-item label="变更内容">{{ current.content }}</el-descriptions-item>
          <el-descriptions-item label="工时影响">{{ current.impactHours }} 小时</el-descriptions-item>
          <el-descriptions-item label="影响说明">{{ current.impactDesc || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审批意见">{{ current.approveComment || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ current.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="审批人">{{ current.approverName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ current.statusLabel }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'
import { useAuthStore } from '../store/auth'
import { changeStatusType, fmtDateTime } from '../utils/dict'

const auth = useAuthStore()
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const candidates = ref([])
const query = reactive({ page: 1, size: 20, status: '' })
const statuses = computed(() => auth.meta?.dicts?.changeStatuses || [])

const load = async () => {
  loading.value = true
  try {
    const data = await api.changes({
      page: query.page,
      size: query.size,
      status: query.status || undefined
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

const create = reactive({
  visible: false, loading: false, requirementId: null, reason: '', content: '', impactHours: 0, impactDesc: ''
})
const openCreate = async () => {
  Object.assign(create, { visible: true, loading: false, requirementId: null, reason: '', content: '', impactHours: 0, impactDesc: '' })
  const data = await api.requirements({
    page: 1, size: 100,
    status: 'REVIEW_PASSED,SCHEDULED,DEVELOPING,TESTING,PENDING_ACCEPTANCE'
  })
  candidates.value = data.records
}
const submitCreate = async () => {
  if (!create.requirementId || !create.reason || !create.content) {
    ElMessage.warning('请选择需求并填写变更原因与内容')
    return
  }
  create.loading = true
  try {
    await api.createChange({
      requirementId: create.requirementId,
      reason: create.reason,
      content: create.content,
      impactHours: create.impactHours,
      impactDesc: create.impactDesc
    })
    ElMessage.success('变更申请已提交')
    create.visible = false
    await load()
  } finally {
    create.loading = false
  }
}

const approve = reactive({ visible: false, loading: false, change: null, approved: true, comment: '' })
const openApprove = (row) => {
  approve.change = row
  approve.approved = true
  approve.comment = ''
  approve.visible = true
}
const submitApprove = async () => {
  approve.loading = true
  try {
    await api.approveChange(approve.change.id, { approved: approve.approved, comment: approve.comment })
    ElMessage.success('审批完成')
    approve.visible = false
    await load()
  } finally {
    approve.loading = false
  }
}
const apply = async (row) => {
  try {
    await ElMessageBox.confirm('实施后需求版本号将递增并生成版本快照，确认继续？', '变更实施', { type: 'warning' })
    await api.applyChange(row.id)
    ElMessage.success('变更已生效')
    await load()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const drawer = reactive({ visible: false })
const current = ref(null)
const showDetail = (row) => {
  current.value = row
  drawer.visible = true
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
