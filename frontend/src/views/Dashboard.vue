<template>
  <div class="page">
    <div class="page-title">
      <div>
        <h2>工作台</h2>
        <div class="sub-text">需求全生命周期概览 · {{ today }}</div>
      </div>
      <el-button type="primary" :icon="Plus" @click="$router.push('/requirements/create')"
                 v-if="auth.hasPerm('requirement:create')">新建需求</el-button>
    </div>

    <div class="metric-grid">
      <div class="metric-card">
        <div class="label">需求总量</div>
        <div class="value">{{ summary.total || 0 }}</div>
      </div>
      <div class="metric-card warn">
        <div class="label">在途需求</div>
        <div class="value">{{ summary.inFlight || 0 }}</div>
      </div>
      <div class="metric-card">
        <div class="label">待验收</div>
        <div class="value">{{ summary.pendingAcceptance || 0 }}</div>
      </div>
      <div class="metric-card danger">
        <div class="label">逾期需求</div>
        <div class="value">{{ summary.overdue || 0 }}</div>
      </div>
      <div class="metric-card success">
        <div class="label">已归档</div>
        <div class="value">{{ summary.archived || 0 }}</div>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :md="10">
        <div class="card chart-card">
          <div class="chart-title">需求状态分布</div>
          <div ref="statusChart" class="chart"></div>
        </div>
      </el-col>
      <el-col :xs="24" :md="14">
        <div class="card chart-card">
          <div class="chart-title">业务部门需求分布</div>
          <div ref="deptChart" class="chart"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="second-row">
      <el-col :xs="24" :md="14">
        <div class="card chart-card">
          <div class="chart-title">近 6 个月需求新增趋势</div>
          <div ref="trendChart" class="chart trend"></div>
        </div>
      </el-col>
      <el-col :xs="24" :md="10">
        <div class="card todo-card">
          <div class="chart-title">
            我的待办
            <el-tag size="small" type="info" round>{{ todos.length }} 项</el-tag>
          </div>
          <div v-if="!todos.length" class="empty">暂无待办，保持这个节奏 ✨</div>
          <div v-for="item in todos" :key="item.refType + item.refId" class="todo-item"
               @click="go(item)">
            <el-tag size="small" :type="todoType(item.type)" effect="light">{{ item.type }}</el-tag>
            <div class="todo-body">
              <div class="todo-title">{{ item.title }}</div>
              <div class="todo-meta">{{ item.refNo }} · {{ fmtDateTime(item.time) }}</div>
            </div>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { Plus, ArrowRight } from '@element-plus/icons-vue'
import { api } from '../api'
import { useAuthStore } from '../store/auth'
import { fmtDateTime } from '../utils/dict'

const router = useRouter()
const auth = useAuthStore()
const summary = reactive({})
const todos = ref([])
const statusChart = ref(null)
const deptChart = ref(null)
const trendChart = ref(null)
let charts = []

const today = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric', month: 'long', day: 'numeric', weekday: 'long'
})

const todoType = (type) => {
  const map = { 待受理: 'warning', 待评审: 'warning', 待排期: 'primary', 待审批: 'primary', 待验收: 'success', 待修改: 'danger', 草稿: 'info', 任务: 'primary', 分析中: 'primary' }
  return map[type] || 'info'
}

const go = (item) => {
  if (item.refType === 'REQUIREMENT') router.push(`/requirements/${item.refId}`)
  else if (item.refType === 'REVIEW') router.push('/reviews')
  else if (item.refType === 'CHANGE') router.push('/changes')
  else if (item.refType === 'ACCEPTANCE') router.push('/delivery')
  else router.push('/projects')
}

const initChart = (element) => {
  if (!element) return null
  const chart = echarts.init(element)
  charts.push(chart)
  return chart
}

const load = async () => {
  const [s, statusData, deptData, trendData, todoData] = await Promise.all([
    api.dashboardSummary(),
    api.dashboardStatus(),
    api.dashboardDept(),
    api.dashboardTrend(),
    api.dashboardTodo()
  ])
  Object.assign(summary, s)
  todos.value = todoData

  initChart(statusChart.value)?.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, textStyle: { color: '#6b7f96' } },
    series: [{
      type: 'pie',
      radius: ['46%', '70%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      data: statusData
    }],
    color: ['#8fa4bd', '#e6a23c', '#2f6fed', '#f0b44c', '#67c23a', '#409eff', '#5470c6', '#e6a23c', '#f56c6c', '#67c23a', '#909399', '#f56c6c', '#909399', '#b1b3b8']
  })

  initChart(deptChart.value)?.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 16, right: 24, top: 20, bottom: 10, containLabel: true },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef2f8' } } },
    yAxis: { type: 'category', data: deptData.map((d) => d.name), axisLine: { show: false }, axisTick: { show: false } },
    series: [{
      type: 'bar',
      barWidth: 18,
      itemStyle: { borderRadius: [0, 6, 6, 0], color: '#2f6fed' },
      data: deptData.map((d) => d.value)
    }]
  })

  initChart(trendChart.value)?.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 16, right: 24, top: 24, bottom: 10, containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: trendData.map((d) => d.month), axisLine: { lineStyle: { color: '#dfe6ef' } } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef2f8' } } },
    series: [{
      type: 'line',
      smooth: true,
      symbolSize: 8,
      data: trendData.map((d) => d.value),
      lineStyle: { width: 3, color: '#2f6fed' },
      itemStyle: { color: '#2f6fed' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(47,111,237,0.28)' },
          { offset: 1, color: 'rgba(47,111,237,0.02)' }
        ])
      }
    }]
  })
}

const resize = () => charts.forEach((c) => c.resize())

onMounted(() => {
  load()
  window.addEventListener('resize', resize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  charts.forEach((c) => c.dispose())
  charts = []
})
</script>

<style scoped>
.chart-card {
  padding: 16px 18px 8px;
  margin-bottom: 16px;
}
.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: #23384f;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.chart {
  height: 280px;
  margin-top: 8px;
}
.chart.trend {
  height: 250px;
}
.second-row {
  margin-top: 0;
}
.todo-card {
  padding: 16px 18px;
  height: 315px;
  overflow-y: auto;
}
.todo-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 6px;
  border-bottom: 1px dashed #eef2f8;
  cursor: pointer;
}
.todo-item:hover {
  background: #f7faff;
}
.todo-body {
  flex: 1;
  min-width: 0;
}
.todo-title {
  font-size: 13px;
  color: #23384f;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.todo-meta {
  font-size: 11px;
  color: #93a4b8;
  margin-top: 3px;
}
</style>
