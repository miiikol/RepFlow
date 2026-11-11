<template>
  <div class="page">
    <div class="page-title">
      <div>
        <h2>{{ isEdit ? '编辑需求' : '新建需求' }}</h2>
        <div class="sub-text">需求编号由系统自动生成，保存后进入草稿状态</div>
      </div>
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <div class="card form-card" v-loading="loading">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
        <el-row :gutter="24">
          <el-col :span="16">
            <el-form-item label="需求标题" prop="title">
              <el-input v-model="form.title" maxlength="60" show-word-limit placeholder="一句话说明业务目标" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="优先级" prop="priority">
              <el-select v-model="form.priority" style="width: 100%">
                <el-option v-for="item in dicts.priorities" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="需求类型" prop="type">
              <el-select v-model="form.type" style="width: 100%">
                <el-option v-for="item in dicts.requirementTypes" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="需求来源" prop="source">
              <el-select v-model="form.source" style="width: 100%">
                <el-option v-for="item in dicts.requirementSources" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="业务部门" prop="deptId">
              <el-select v-model="form.deptId" style="width: 100%">
                <el-option v-for="dept in meta.depts" :key="dept.id" :label="dept.name" :value="dept.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="期望交付日期" prop="expectDate">
              <el-date-picker v-model="form.expectDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="分析人">
              <el-select v-model="form.analystId" clearable style="width: 100%">
                <el-option v-for="user in meta.users" :key="user.id" :label="user.name" :value="user.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="项目经理">
              <el-select v-model="form.pmId" clearable style="width: 100%">
                <el-option v-for="user in meta.users" :key="user.id" :label="user.name" :value="user.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="业务背景" prop="background">
          <el-input v-model="form.background" type="textarea" :rows="3"
                    placeholder="现状、问题，以及不解决的后果" />
        </el-form-item>
        <el-form-item label="需求描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4"
                    placeholder="范围、功能点与非目标" />
        </el-form-item>
        <el-form-item label="验收标准" prop="acceptanceCriteria">
          <el-input v-model="form.acceptanceCriteria" type="textarea" :rows="4"
                    placeholder="逐条列出可验证的验收标准，例如：1. 四类客户等级可分别配置..." />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="多个标签用逗号分隔，例如：手机银行,限额" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save(false)">保存</el-button>
          <el-button v-if="!isEdit" :loading="saving" @click="save(true)">保存并提交</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const isEdit = computed(() => Boolean(route.params.id))
const meta = computed(() => auth.meta || { depts: [], users: [], dicts: {} })
const dicts = computed(() => meta.value.dicts || {})

const form = reactive({
  title: '',
  type: 'NEW_FEATURE',
  source: 'BUSINESS_DEPT',
  deptId: auth.user?.deptId || null,
  priority: 'P2',
  expectDate: '',
  planDate: '',
  analystId: null,
  pmId: null,
  background: '',
  description: '',
  acceptanceCriteria: '',
  tags: ''
})

const rules = {
  title: [{ required: true, message: '请输入需求标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择需求类型', trigger: 'change' }],
  source: [{ required: true, message: '请选择需求来源', trigger: 'change' }],
  deptId: [{ required: true, message: '请选择业务部门', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
  expectDate: [{ required: true, message: '请选择期望交付日期', trigger: 'change' }],
  background: [{ required: true, message: '请填写业务背景', trigger: 'blur' }],
  description: [{ required: true, message: '请填写需求描述', trigger: 'blur' }],
  acceptanceCriteria: [{ required: true, message: '请填写验收标准', trigger: 'blur' }]
}

const load = async () => {
  if (!isEdit.value) return
  loading.value = true
  try {
    const data = await api.requirement(route.params.id)
    Object.assign(form, data.requirement)
  } finally {
    loading.value = false
  }
}

const save = async (submitAfter) => {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form }
    if (isEdit.value) {
      await api.updateRequirement(route.params.id, payload)
      ElMessage.success('保存成功')
      router.push(`/requirements/${route.params.id}`)
      return
    }
    const id = await api.createRequirement(payload)
    if (submitAfter) {
      await api.requirementAction(id, { action: 'SUBMIT' })
      ElMessage.success('需求已提交，等待需求分析受理')
    } else {
      ElMessage.success('需求已保存为草稿')
    }
    router.push(`/requirements/${id}`)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.form-card {
  padding: 24px 28px;
  max-width: 1100px;
}
</style>
