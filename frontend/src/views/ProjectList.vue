<template>
  <div class="page">
    <div class="page-title">
      <div>
        <h2>计划与任务</h2>
        <div class="sub-text">项目、里程碑与任务进度</div>
      </div>
      <el-button type="primary" v-if="auth.hasPerm('project:manage')" @click="openProjectDialog">
        新建项目
      </el-button>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :md="12" v-for="project in projects" :key="project.id">
        <div class="card project-card">
          <div class="project-head">
            <div>
              <div class="project-name">{{ project.name }}</div>
              <div class="mono sub-text">{{ project.projectNo }}</div>
            </div>
            <el-tag size="small" :type="project.status === 'IN_PROGRESS' ? 'primary' : project.status === 'CLOSED' ? 'info' : 'warning'">
              {{ projectStatus(project.status) }}
            </el-tag>
          </div>
          <div class="project-meta">
            <span>项目经理：{{ project.pmName }}</span>
            <span>周期：{{ fmtDate(project.startDate) }} ~ {{ fmtDate(project.endDate) }}</span>
            <span>部门：{{ project.deptName }}</span>
          </div>
          <div class="milestone-head">
            <span>里程碑</span>
            <el-button v-if="auth.hasPerm('project:manage')" link type="primary" size="small"
                       @click="openMilestoneDialog(project)">添加</el-button>
          </div>
          <div class="milestones">
            <div v-for="m in project.milestones" :key="m.id" class="milestone">
              <el-icon :color="m.status === 'DONE' ? '#67c23a' : m.status === 'IN_PROGRESS' ? '#2f6fed' : '#c0c8d5'">
                <CircleCheck v-if="m.status === 'DONE'" />
                <Clock v-else />
              </el-icon>
              <span class="milestone-name">{{ m.name }}</span>
              <span class="sub-text">{{ fmtDate(m.planDate) }}</span>
            </div>
            <div v-if="!project.milestones?.length" class="empty" style="padding: 12px">暂无里程碑</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="card task-card">
      <div class="task-head">
        <div class="block-title">任务清单</div>
        <div class="toolbar" style="margin: 0">
          <el-select v-model="taskQuery.projectId" placeholder="选择项目" clearable style="width: 220px" @change="loadTasks">
            <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
          <el-select v-model="taskQuery.status" placeholder="任务状态" clearable style="width: 140px" @change="loadTasks">
            <el-option v-for="item in taskStatuses" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-checkbox v-model="taskQuery.mine" @change="loadTasks">我的任务</el-checkbox>
        </div>
      </div>
      <el-table :data="tasks" v-loading="taskLoading" size="small">
        <el-table-column prop="taskNo" label="任务编号" width="150">
          <template #default="{ row }"><span class="mono">{{ row.taskNo }}</span></template>
        </el-table-column>
        <el-table-column label="所属需求" min-width="220">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="$router.push(`/requirements/${row.requirementId}`)">
              {{ row.requirementNo }} {{ shortText(row.requirementTitle, 24) }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="任务名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="typeLabel" label="类型" width="80" />
        <el-table-column prop="assigneeName" label="负责人" width="90" />
        <el-table-column label="计划完成" width="110">
          <template #default="{ row }">
            <span :class="{ overdue: row.overdue }">{{ fmtDate(row.planDate) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="taskStatusType(row.status)">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="taskTotal"
                       v-model:current-page="taskQuery.page" :page-size="taskQuery.size" @change="loadTasks" />
      </div>
    </div>

    <el-dialog v-model="projectDialog.visible" title="新建项目" width="520px">
      <el-form label-width="90px">
        <el-form-item label="项目名称">
          <el-input v-model="projectDialog.name" />
        </el-form-item>
        <el-form-item label="所属部门">
          <el-select v-model="projectDialog.deptId" style="width: 100%">
            <el-option v-for="dept in meta.depts" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目经理">
          <el-select v-model="projectDialog.pmId" style="width: 100%">
            <el-option v-for="user in meta.users" :key="user.id" :label="user.name" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="projectDialog.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="projectDialog.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="projectDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="projectDialog.loading" @click="submitProject">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="milestoneDialog.visible" title="添加里程碑" width="480px">
      <el-form label-width="90px">
        <el-form-item label="所属项目">
          <el-input :model-value="milestoneDialog.projectName" disabled />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="milestoneDialog.name" />
        </el-form-item>
        <el-form-item label="计划日期">
          <el-date-picker v-model="milestoneDialog.planDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="milestoneDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="milestoneDialog.loading" @click="submitMilestone">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, Clock } from '@element-plus/icons-vue'
import { api } from '../api'
import { useAuthStore } from '../store/auth'
import { fmtDate, shortText, taskStatusType } from '../utils/dict'

const auth = useAuthStore()
const meta = computed(() => auth.meta || { depts: [], users: [], dicts: {} })
const taskStatuses = computed(() => meta.value.dicts?.taskStatuses || [])

const projects = ref([])
const tasks = ref([])
const taskTotal = ref(0)
const taskLoading = ref(false)
const taskQuery = reactive({ page: 1, size: 10, projectId: null, status: '', mine: false })

const projectStatus = (status) => {
  const map = { PLANNING: '规划中', IN_PROGRESS: '进行中', CLOSED: '已关闭' }
  return map[status] || status
}

const loadProjects = async () => {
  projects.value = await api.projects()
  if (!taskQuery.projectId && projects.value.length) {
    taskQuery.projectId = projects.value[0].id
  }
  await loadTasks()
}

const loadTasks = async () => {
  taskLoading.value = true
  try {
    const data = await api.tasks({
      page: taskQuery.page,
      size: taskQuery.size,
      projectId: taskQuery.projectId || undefined,
      status: taskQuery.status || undefined,
      mine: taskQuery.mine || undefined
    })
    tasks.value = data.records
    taskTotal.value = data.total
  } finally {
    taskLoading.value = false
  }
}

const projectDialog = reactive({
  visible: false, loading: false, name: '', deptId: null, pmId: null, startDate: '', endDate: ''
})
const openProjectDialog = () => {
  Object.assign(projectDialog, {
    visible: true, name: '', deptId: auth.user.deptId, pmId: auth.user.id, startDate: '', endDate: ''
  })
}
const submitProject = async () => {
  if (!projectDialog.name || !projectDialog.deptId || !projectDialog.pmId) {
    ElMessage.warning('请填写项目名称、部门与项目经理')
    return
  }
  projectDialog.loading = true
  try {
    await api.createProject({
      name: projectDialog.name,
      deptId: projectDialog.deptId,
      pmId: projectDialog.pmId,
      startDate: projectDialog.startDate || null,
      endDate: projectDialog.endDate || null
    })
    ElMessage.success('项目已创建')
    projectDialog.visible = false
    await loadProjects()
  } finally {
    projectDialog.loading = false
  }
}

const milestoneDialog = reactive({
  visible: false, loading: false, projectId: null, projectName: '', name: '', planDate: ''
})
const openMilestoneDialog = (project) => {
  Object.assign(milestoneDialog, {
    visible: true, projectId: project.id, projectName: project.name, name: '', planDate: ''
  })
}
const submitMilestone = async () => {
  if (!milestoneDialog.name) return ElMessage.warning('请填写里程碑名称')
  milestoneDialog.loading = true
  try {
    await api.createMilestone({
      projectId: milestoneDialog.projectId,
      name: milestoneDialog.name,
      planDate: milestoneDialog.planDate || null
    })
    ElMessage.success('里程碑已添加')
    milestoneDialog.visible = false
    await loadProjects()
  } finally {
    milestoneDialog.loading = false
  }
}

onMounted(loadProjects)
</script>

<style scoped>
.project-card {
  padding: 18px 20px;
  margin-bottom: 16px;
}
.project-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.project-name {
  font-size: 16px;
  font-weight: 600;
  color: #10243f;
}
.project-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: #6b7f96;
  font-size: 12px;
  margin: 12px 0 16px;
}
.milestone-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  font-weight: 600;
  color: #3d526b;
  border-top: 1px solid #eef2f8;
  padding-top: 12px;
}
.milestones {
  margin-top: 6px;
}
.milestone {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 0;
  font-size: 13px;
}
.milestone-name {
  flex: 1;
  color: #35495e;
}
.task-card {
  padding: 18px 20px;
  margin-top: 4px;
}
.task-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.block-title {
  font-size: 15px;
  font-weight: 600;
  color: #23384f;
}
.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}
.overdue {
  color: #f56c6c;
  font-weight: 600;
}
</style>
