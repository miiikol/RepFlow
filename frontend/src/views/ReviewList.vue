<template>
  <div class="page">
    <div class="page-title">
      <div>
        <h2>评审管理</h2>
        <div class="sub-text">评审单、评审意见与结论留痕</div>
      </div>
    </div>

    <div class="card filter-card">
      <div class="toolbar">
        <el-select v-model="query.status" placeholder="评审状态" clearable style="width: 160px" @change="search">
          <el-option v-for="item in statuses" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-checkbox v-model="query.mine" @change="search">我参与的评审</el-checkbox>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="reviewNo" label="评审单号" width="155">
          <template #default="{ row }"><span class="mono">{{ row.reviewNo }}</span></template>
        </el-table-column>
        <el-table-column label="关联需求" min-width="260">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="$router.push(`/requirements/${row.requirementId}`)">
              {{ row.requirementNo }} {{ row.requirementTitle }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="hostName" label="主持人" width="90" />
        <el-table-column prop="reviewerNames" label="评审人" width="150" show-overflow-tooltip />
        <el-table-column label="截止时间" width="150">
          <template #default="{ row }">{{ fmtDateTime(row.deadline) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="reviewStatusType(row.status)">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
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

    <el-drawer v-model="drawer.visible" title="评审详情" size="620px">
      <div v-if="detail" v-loading="drawer.loading">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="评审单号">{{ detail.reviewNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="reviewStatusType(detail.status)">{{ detail.statusLabel }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="需求" :span="2">
            <el-link type="primary" :underline="false" @click="$router.push(`/requirements/${detail.requirementId}`)">
              {{ detail.requirementNo }} {{ detail.requirementTitle }}
            </el-link>
          </el-descriptions-item>
          <el-descriptions-item label="主持人">{{ detail.hostName }}</el-descriptions-item>
          <el-descriptions-item label="评审人">{{ detail.reviewerNames }}</el-descriptions-item>
          <el-descriptions-item label="截止时间">{{ fmtDateTime(detail.deadline) }}</el-descriptions-item>
          <el-descriptions-item label="结论">{{ detail.conclusion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="结论说明" :span="2">{{ detail.comment || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="drawer-title">评审意见</div>
        <div v-if="!detail.opinions?.length" class="empty">暂无评审意见</div>
        <div v-for="opinion in detail.opinions" :key="opinion.id" class="opinion">
          <el-tag size="small" effect="plain"
                  :type="opinion.result === 'PASS' ? 'success' : opinion.result === 'OBJECT' ? 'danger' : 'warning'">
            {{ opinion.resultLabel }}
          </el-tag>
          <strong>{{ opinion.reviewerName }}</strong>
          <span class="opinion-content">{{ opinion.content }}</span>
          <span class="sub-text">{{ fmtDateTime(opinion.createdAt) }}</span>
        </div>

        <div class="drawer-actions" v-if="detail.status === 'PENDING'">
          <el-button v-if="canOpinion" @click="openOpinion">提交意见</el-button>
          <el-button v-if="canConclude" type="primary" @click="openConclusion">提交结论</el-button>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="opinion.visible" title="提交评审意见" width="460px">
      <el-form label-width="80px">
        <el-form-item label="意见类型">
          <el-radio-group v-model="opinion.result">
            <el-radio value="PASS">通过</el-radio>
            <el-radio value="NEED_CHANGE">需修改</el-radio>
            <el-radio value="OBJECT">反对</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="opinion.content" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="opinion.visible = false">取消</el-button>
        <el-button type="primary" :loading="opinion.loading" @click="submitOpinion">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="conclusion.visible" title="提交评审结论" width="460px">
      <el-form label-width="80px">
        <el-form-item label="结论">
          <el-radio-group v-model="conclusion.pass">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="conclusion.comment" type="textarea" :rows="4" />
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
import { useAuthStore } from '../store/auth'
import { fmtDateTime, reviewStatusType } from '../utils/dict'

const auth = useAuthStore()
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 20, status: '', mine: false })
const statuses = computed(() => auth.meta?.dicts?.reviewStatuses || [])

const drawer = reactive({ visible: false, loading: false })
const detail = ref(null)
const canOpinion = computed(() => {
  if (!detail.value || !auth.hasPerm('review:opinion')) return false
  const ids = (detail.value.reviewerIds || '').split(',').filter(Boolean)
  return ids.includes(String(auth.user.id))
})
const canConclude = computed(() => detail.value && auth.hasPerm('review:decide')
  && (detail.value.hostId === auth.user.id || auth.isAdmin))

const load = async () => {
  loading.value = true
  try {
    const data = await api.reviews({
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
const openDetail = async (row) => {
  drawer.visible = true
  drawer.loading = true
  try {
    detail.value = await api.review(row.id)
  } finally {
    drawer.loading = false
  }
}

const opinion = reactive({ visible: false, loading: false, result: 'PASS', content: '' })
const openOpinion = () => {
  opinion.result = 'PASS'
  opinion.content = ''
  opinion.visible = true
}
const submitOpinion = async () => {
  if (!opinion.content) return ElMessage.warning('请填写意见内容')
  opinion.loading = true
  try {
    await api.addOpinion(detail.value.id, { result: opinion.result, content: opinion.content })
    ElMessage.success('意见已提交')
    opinion.visible = false
    detail.value = await api.review(detail.value.id)
    await load()
  } finally {
    opinion.loading = false
  }
}

const conclusion = reactive({ visible: false, loading: false, pass: true, comment: '' })
const openConclusion = () => {
  conclusion.pass = true
  conclusion.comment = ''
  conclusion.visible = true
}
const submitConclusion = async () => {
  conclusion.loading = true
  try {
    await api.concludeReview(detail.value.id, { pass: conclusion.pass, comment: conclusion.comment })
    ElMessage.success('评审结论已提交')
    conclusion.visible = false
    detail.value = await api.review(detail.value.id)
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
.opinion {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px dashed #eef2f8;
  font-size: 13px;
  flex-wrap: wrap;
}
.opinion-content {
  flex: 1;
  color: #4a5f76;
}
.drawer-actions {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}
</style>
