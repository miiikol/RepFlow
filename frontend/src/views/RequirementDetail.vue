<template>
  <div class="page" v-loading="loading">
    <template v-if="detail">
      <div class="page-title detail-head">
        <div>
          <div class="title-line">
            <span class="mono req-no">{{ req.reqNo }}</span>
            <el-tag :type="statusType(req.status)" effect="dark">{{ req.statusLabel }}</el-tag>
            <el-tag :type="priorityType(req.priority)" effect="plain">{{ req.priority }}</el-tag>
            <span class="version">基线 {{ req.version }}</span>
          </div>
          <h2>{{ req.title }}</h2>
          <div class="sub-text">
            {{ req.deptName }} · 提出人 {{ req.proposerName }} · {{ fmtDateTime(req.createdAt) }}
            <span v-if="req.overdue" class="overdue">· 已逾期</span>
          </div>
        </div>
        <div class="head-actions">
          <el-button v-for="action in detail.availableActions" :key="action"
                     :type="actionType(action)" @click="handleAction(action)">
            {{ actionLabel(action) }}
          </el-button>
          <el-button v-if="canStartReview" type="primary" @click="openReviewDialog">发起评审</el-button>
          <el-button v-if="canSubmitAcceptance" type="success" @click="openAcceptanceDialog">提交验收</el-button>
          <el-button v-if="canEdit" @click="$router.push(`/requirements/${req.id}/edit`)">编辑</el-button>
          <el-button v-if="canDelete" type="danger" plain @click="removeRequirement">删除</el-button>
          <el-button @click="$router.push('/requirements')">返回列表</el-button>
        </div>
      </div>

      <el-row :gutter="16">
        <el-col :xs="24" :md="16">
          <div class="card block">
            <div class="block-title">需求内容</div>
            <div class="section">
              <div class="section-label">业务背景</div>
              <div class="detail-block">{{ req.background || '-' }}</div>
            </div>
            <div class="section">
              <div class="section-label">需求描述</div>
              <div class="detail-block">{{ req.description || '-' }}</div>
            </div>
            <div class="section">
              <div class="section-label">验收标准</div>
              <div class="detail-block">{{ req.acceptanceCriteria || '-' }}</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :md="8">
          <div class="card block">
            <div class="block-title">基础信息</div>
            <el-descriptions :column="1" size="small" border>
              <el-descriptions-item label="需求类型">{{ req.typeLabel }}</el-descriptions-item>
              <el-descriptions-item label="需求来源">{{ req.sourceLabel }}</el-descriptions-item>
              <el-descriptions-item label="业务部门">{{ req.deptName }}</el-descriptions-item>
              <el-descriptions-item label="提出人">{{ req.proposerName }}</el-descriptions-item>
              <el-descriptions-item label="分析人">{{ req.analystName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="项目经理">{{ req.pmName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="期望交付">{{ fmtDate(req.expectDate) }}</el-descriptions-item>
              <el-descriptions-item label="计划上线">{{ fmtDate(req.planDate) }}</el-descriptions-item>
              <el-descriptions-item label="所属项目">{{ req.projectName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="变更次数">{{ req.changeCount || 0 }}</el-descriptions-item>
              <el-descriptions-item label="任务进度">
                <el-progress :percentage="req.progress" :stroke-width="8" />
              </el-descriptions-item>
              <el-descriptions-item label="标签">{{ req.tags || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
      </el-row>

      <div class="card tabs-card">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="评审记录" name="reviews">
            <div v-if="!detail.reviews.length" class="empty">暂无评审记录</div>
            <div v-for="review in detail.reviews" :key="review.id" class="review-card">
              <div class="review-head">
                <div>
                  <span class="mono">{{ review.reviewNo }}</span>
                  <el-tag size="small" :type="reviewStatusType(review.status)" style="margin-left: 8px">
                    {{ review.statusLabel }}
                  </el-tag>
                </div>
                <div class="review-actions">
                  <el-button v-if="canAddOpinion(review)" size="small" @click="openOpinion(review)">提交意见</el-button>
                  <el-button v-if="canConclude(review)" size="small" type="primary" @click="openConclusion(review)">
                    提交结论
                  </el-button>
                </div>
              </div>
              <div class="review-meta">
                主持人：{{ review.hostName || '-' }} · 评审人：{{ review.reviewerNames || '-' }} ·
                截止：{{ fmtDateTime(review.deadline) }}
              </div>
              <div v-if="review.comment" class="review-comment">结论说明：{{ review.comment }}</div>
              <div v-for="opinion in review.opinions" :key="opinion.id" class="opinion">
                <el-tag size="small" effect="plain"
                        :type="opinion.result === 'PASS' ? 'success' : opinion.result === 'OBJECT' ? 'danger' : 'warning'">
                  {{ opinion.resultLabel }}
                </el-tag>
                <span class="opinion-user">{{ opinion.reviewerName }}</span>
                <span class="opinion-content">{{ opinion.content }}</span>
                <span class="opinion-time">{{ fmtDateTime(opinion.createdAt) }}</span>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="变更记录" name="changes">
            <div class="tab-toolbar">
              <el-button type="primary" size="small" v-if="canCreateChange" @click="openChangeDialog">发起变更</el-button>
            </div>
            <el-table :data="detail.changes" size="small">
              <el-table-column prop="changeNo" label="变更单号" width="150">
                <template #default="{ row }"><span class="mono">{{ row.changeNo }}</span></template>
              </el-table-column>
              <el-table-column prop="reason" label="变更原因" min-width="180" show-overflow-tooltip />
              <el-table-column prop="impactHours" label="工时影响" width="90" />
              <el-table-column prop="statusLabel" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag size="small" :type="changeStatusType(row.status)">{{ row.statusLabel }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="applicantName" label="申请人" width="90" />
              <el-table-column label="操作" width="160">
                <template #default="{ row }">
                  <el-button v-if="row.status === 'PENDING' && auth.hasPerm('change:approve')"
                             link type="primary" @click="openApproveChange(row)">审批</el-button>
                  <el-button v-if="row.status === 'APPROVED' && auth.hasPerm('change:apply')"
                             link type="success" @click="applyChange(row)">实施生效</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="任务" name="tasks">
            <div class="tab-toolbar">
              <el-button type="primary" size="small" v-if="canManageTask" @click="openTaskDialog">拆分任务</el-button>
            </div>
            <el-table :data="detail.tasks" size="small">
              <el-table-column prop="taskNo" label="任务编号" width="150">
                <template #default="{ row }"><span class="mono">{{ row.taskNo }}</span></template>
              </el-table-column>
              <el-table-column prop="name" label="任务名称" min-width="200" show-overflow-tooltip />
              <el-table-column prop="typeLabel" label="类型" width="80" />
              <el-table-column prop="assigneeName" label="负责人" width="90" />
              <el-table-column label="工时" width="100">
                <template #default="{ row }">{{ row.actualHours || 0 }} / {{ row.estimateHours || 0 }}h</template>
              </el-table-column>
              <el-table-column prop="planDate" label="计划完成" width="110">
                <template #default="{ row }">
                  <span :class="{ overdue: row.overdue }">{{ fmtDate(row.planDate) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="statusLabel" label="状态" width="90">
                <template #default="{ row }">
                  <el-tag size="small" :type="taskStatusType(row.status)">{{ row.statusLabel }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="140">
                <template #default="{ row }">
                  <el-button v-if="canManageTask && row.status === 'TODO'" link type="primary"
                             @click="openTaskStatus(row, 'IN_PROGRESS')">开始</el-button>
                  <el-button v-if="canManageTask && row.status === 'IN_PROGRESS'" link type="success"
                             @click="openTaskStatus(row, 'DONE')">完成</el-button>
                  <el-button v-if="canManageTask && row.status === 'BLOCKED'" link type="warning"
                             @click="openTaskStatus(row, 'IN_PROGRESS')">恢复</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="交付与验收" name="delivery">
            <div class="block-title sub">交付物清单</div>
            <div class="tab-toolbar">
              <el-button type="primary" size="small" v-if="canManageDelivery" @click="openDeliverableDialog">
                登记交付物
              </el-button>
            </div>
            <el-table :data="detail.deliverables" size="small">
              <el-table-column prop="deliverableNo" label="编号" width="150">
                <template #default="{ row }"><span class="mono">{{ row.deliverableNo }}</span></template>
              </el-table-column>
              <el-table-column prop="typeLabel" label="类型" width="150" />
              <el-table-column prop="name" label="名称" min-width="220" show-overflow-tooltip />
              <el-table-column prop="uploaderName" label="登记人" width="90" />
              <el-table-column label="文件" width="100">
                <template #default="{ row }">
                  <el-button v-if="row.fileName" link type="primary" @click="downloadDeliverable(row)">下载</el-button>
                  <span v-else class="sub-text">仅登记</span>
                </template>
              </el-table-column>
            </el-table>

            <div class="block-title sub" style="margin-top: 22px">验收单</div>
            <el-table :data="detail.acceptances" size="small">
              <el-table-column prop="acceptanceNo" label="验收单号" width="150">
                <template #default="{ row }"><span class="mono">{{ row.acceptanceNo }}</span></template>
              </el-table-column>
              <el-table-column prop="round" label="轮次" width="70" />
              <el-table-column prop="acceptorName" label="验收人" width="90" />
              <el-table-column prop="statusLabel" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag size="small" :type="acceptanceStatusType(row.status)">{{ row.statusLabel }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="comment" label="验收意见" min-width="180" show-overflow-tooltip />
              <el-table-column label="操作" width="130">
                <template #default="{ row }">
                  <el-button v-if="canConcludeAcceptance(row)" link type="primary" @click="openAcceptanceConclusion(row)">
                    提交结论
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="版本与附件" name="versions">
            <div class="block-title sub">版本历史</div>
            <el-timeline style="padding-left: 6px">
              <el-timeline-item v-for="version in detail.versions" :key="version.id"
                                :timestamp="fmtDateTime(version.createdAt)" placement="top">
                <div class="version-line">
                  <el-tag size="small" effect="dark">{{ version.version }}</el-tag>
                  <span>{{ version.reason }}</span>
                  <span class="sub-text">操作人 {{ version.operatorName }}</span>
                </div>
              </el-timeline-item>
            </el-timeline>

            <div class="block-title sub" style="margin-top: 20px">附件</div>
            <div class="tab-toolbar">
              <el-upload :show-file-list="false" :http-request="uploadAttachment" v-if="!isArchived">
                <el-button size="small" type="primary">上传附件</el-button>
              </el-upload>
              <span class="sub-text">支持 doc/docx/xls/xlsx/pdf/png/jpg/zip，单文件不超过 20MB</span>
            </div>
            <div v-for="file in detail.attachments" :key="file.id" class="file-item">
              <el-icon><Paperclip /></el-icon>
              <span class="file-name">{{ file.fileName }}</span>
              <span class="sub-text">{{ (file.fileSize / 1024).toFixed(1) }} KB · {{ file.uploaderName }}</span>
              <el-button link type="primary" @click="downloadAttachment(file)">下载</el-button>
              <el-button link type="danger" @click="removeAttachment(file)">删除</el-button>
            </div>
            <div v-if="!detail.attachments.length" class="empty">暂无附件</div>
          </el-tab-pane>

          <el-tab-pane label="流转时间线" name="timeline">
            <el-timeline style="padding-left: 6px">
              <el-timeline-item v-for="(item, index) in detail.timeline" :key="index"
                                :timestamp="fmtDateTime(item.time)" placement="top"
                                :type="item.result === 'SUCCESS' ? 'primary' : 'danger'">
                <div class="timeline-line">
                  <strong>{{ item.action }}</strong>
                  <span class="sub-text">{{ item.module }} · {{ item.user }}</span>
                </div>
              </el-timeline-item>
            </el-timeline>
          </el-tab-pane>
        </el-tabs>
      </div>
    </template>

    <!-- 通用动作 -->
    <el-dialog v-model="actionDialog.visible" :title="actionLabel(actionDialog.action)" width="480px">
      <el-form label-width="90px" v-if="actionDialog.action === 'SCHEDULE'">
        <el-form-item label="所属项目">
          <el-select v-model="actionDialog.projectId" style="width: 100%" @change="onProjectChange">
            <el-option v-for="p in meta.projects" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="里程碑">
          <el-select v-model="actionDialog.milestoneId" style="width: 100%">
            <el-option v-for="m in scheduleMilestones" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划上线">
          <el-date-picker v-model="actionDialog.planDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <el-form label-width="90px">
        <el-form-item label="说明">
          <el-input v-model="actionDialog.comment" type="textarea" :rows="3"
                    :placeholder="actionDialog.action === 'HOLD' ? '请填写挂起原因' : '可选'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="actionDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="actionDialog.loading" @click="submitAction">确认</el-button>
      </template>
    </el-dialog>

    <!-- 发起评审 -->
    <el-dialog v-model="reviewDialog.visible" title="发起需求评审" width="520px">
      <el-form label-width="90px">
        <el-form-item label="评审人" required>
          <el-select v-model="reviewDialog.reviewerIds" multiple style="width: 100%">
            <el-option v-for="user in meta.users" :key="user.id" :label="user.name" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker v-model="reviewDialog.deadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
                          style="width: 100%" />
        </el-form-item>
        <el-form-item label="评审说明">
          <el-input v-model="reviewDialog.comment" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="reviewDialog.loading" @click="submitReview">发起评审</el-button>
      </template>
    </el-dialog>

    <!-- 评审意见 -->
    <el-dialog v-model="opinionDialog.visible" title="提交评审意见" width="480px">
      <el-form label-width="80px">
        <el-form-item label="意见类型">
          <el-radio-group v-model="opinionDialog.result">
            <el-radio value="PASS">通过</el-radio>
            <el-radio value="NEED_CHANGE">需修改</el-radio>
            <el-radio value="OBJECT">反对</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="意见内容">
          <el-input v-model="opinionDialog.content" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="opinionDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="opinionDialog.loading" @click="submitOpinion">提交</el-button>
      </template>
    </el-dialog>

    <!-- 评审结论 -->
    <el-dialog v-model="reviewConclusion.visible" title="提交评审结论" width="480px">
      <el-form label-width="80px">
        <el-form-item label="结论">
          <el-radio-group v-model="reviewConclusion.pass">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="reviewConclusion.comment" type="textarea" :rows="4"
                    placeholder="驳回时必须说明原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewConclusion.visible = false">取消</el-button>
        <el-button type="primary" :loading="reviewConclusion.loading" @click="submitReviewConclusion">提交</el-button>
      </template>
    </el-dialog>

    <!-- 变更申请 -->
    <el-dialog v-model="changeDialog.visible" title="发起变更申请" width="540px">
      <el-form label-width="90px">
        <el-form-item label="变更原因">
          <el-input v-model="changeDialog.reason" />
        </el-form-item>
        <el-form-item label="变更内容">
          <el-input v-model="changeDialog.content" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="工时影响">
          <el-input-number v-model="changeDialog.impactHours" :min="0" :step="0.5" />
        </el-form-item>
        <el-form-item label="影响说明">
          <el-input v-model="changeDialog.impactDesc" type="textarea" :rows="2"
                    placeholder="影响的模块、任务、表结构等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changeDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="changeDialog.loading" @click="submitChange">提交</el-button>
      </template>
    </el-dialog>

    <!-- 变更审批 -->
    <el-dialog v-model="approveDialog.visible" title="变更审批" width="480px">
      <el-form label-width="80px">
        <el-form-item label="结果">
          <el-radio-group v-model="approveDialog.approved">
            <el-radio :value="true">批准</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="意见">
          <el-input v-model="approveDialog.comment" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="approveDialog.loading" @click="submitApprove">提交</el-button>
      </template>
    </el-dialog>

    <!-- 新建任务 -->
    <el-dialog v-model="taskDialog.visible" title="拆分任务" width="520px">
      <el-form label-width="90px">
        <el-form-item label="任务名称">
          <el-input v-model="taskDialog.name" />
        </el-form-item>
        <el-form-item label="任务类型">
          <el-select v-model="taskDialog.type" style="width: 100%">
            <el-option v-for="item in taskTypes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="taskDialog.assigneeId" style="width: 100%">
            <el-option v-for="user in meta.users" :key="user.id" :label="user.name" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预估工时">
          <el-input-number v-model="taskDialog.estimateHours" :min="0" :step="0.5" />
        </el-form-item>
        <el-form-item label="计划完成">
          <el-date-picker v-model="taskDialog.planDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="taskDialog.loading" @click="submitTask">创建</el-button>
      </template>
    </el-dialog>

    <!-- 任务状态 -->
    <el-dialog v-model="taskStatusDialog.visible" :title="'任务状态：' + taskStatusDialog.statusLabel" width="480px">
      <el-form label-width="90px">
        <el-form-item label="实际工时">
          <el-input-number v-model="taskStatusDialog.actualHours" :min="0" :step="0.5" />
        </el-form-item>
        <el-form-item label="完成说明" v-if="taskStatusDialog.status === 'DONE'">
          <el-input v-model="taskStatusDialog.finishNote" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="阻塞原因" v-if="taskStatusDialog.status === 'BLOCKED'">
          <el-input v-model="taskStatusDialog.blockReason" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskStatusDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="taskStatusDialog.loading" @click="submitTaskStatus">确认</el-button>
      </template>
    </el-dialog>

    <!-- 登记交付物 -->
    <el-dialog v-model="deliverableDialog.visible" title="登记交付物" width="520px">
      <el-form label-width="90px">
        <el-form-item label="交付物类型">
          <el-select v-model="deliverableDialog.type" style="width: 100%">
            <el-option v-for="item in deliverableTypes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="deliverableDialog.name" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="deliverableDialog.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="文件">
          <el-upload :auto-upload="false" :limit="1" :on-change="onDeliverableFile" :file-list="deliverableFiles">
            <el-button size="small">选择文件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deliverableDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="deliverableDialog.loading" @click="submitDeliverable">登记</el-button>
      </template>
    </el-dialog>

    <!-- 提交验收 -->
    <el-dialog v-model="acceptanceDialog.visible" title="提交验收" width="480px">
      <el-form label-width="90px">
        <el-form-item label="验收人">
          <el-select v-model="acceptanceDialog.acceptorId" style="width: 100%">
            <el-option v-for="user in meta.users" :key="user.id" :label="user.name" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="acceptanceDialog.comment" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <div class="sub-text" style="margin-left: 90px">
        发起前会校验需求规格说明书、测试报告、部署说明三类必备交付物
      </div>
      <template #footer>
        <el-button @click="acceptanceDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="acceptanceDialog.loading" @click="submitAcceptance">提交验收</el-button>
      </template>
    </el-dialog>

    <!-- 验收结论 -->
    <el-dialog v-model="acceptanceConclusion.visible" title="提交验收结论" width="500px">
      <el-form label-width="90px">
        <el-form-item label="结论">
          <el-radio-group v-model="acceptanceConclusion.pass">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">不通过</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="验收意见">
          <el-input v-model="acceptanceConclusion.comment" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="返工要求" v-if="!acceptanceConclusion.pass">
          <el-input v-model="acceptanceConclusion.reworkRequirement" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="acceptanceConclusion.visible = false">取消</el-button>
        <el-button type="primary" :loading="acceptanceConclusion.loading" @click="submitAcceptanceConclusion">
          提交
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Paperclip } from '@element-plus/icons-vue'
import { api } from '../api'
import { downloadFile } from '../api/http'
import { useAuthStore } from '../store/auth'
import {
  acceptanceStatusType, actionLabel, changeStatusType, fmtDate, fmtDateTime,
  priorityType, reviewStatusType, statusType, taskStatusType
} from '../utils/dict'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const detail = ref(null)
const activeTab = ref('reviews')

const meta = computed(() => auth.meta || { users: [], projects: [], dicts: {} })
const req = computed(() => detail.value?.requirement || {})
const isArchived = computed(() => req.value.status === 'ARCHIVED')
const taskTypes = computed(() => meta.value.dicts?.taskTypes || [])
const deliverableTypes = computed(() => meta.value.dicts?.deliverableTypes || [])

const canEdit = computed(() => ['DRAFT', 'REJECTED'].includes(req.value.status)
  && auth.hasPerm('requirement:edit'))
const canDelete = computed(() => req.value.status === 'DRAFT' && auth.hasPerm('requirement:delete'))
const canStartReview = computed(() => req.value.status === 'ANALYZING' && auth.hasPerm('review:create'))
const canSubmitAcceptance = computed(() => req.value.status === 'TESTING' && auth.hasPerm('delivery:manage'))
const canCreateChange = computed(() => ['REVIEW_PASSED', 'SCHEDULED', 'DEVELOPING', 'TESTING', 'PENDING_ACCEPTANCE']
  .includes(req.value.status) && auth.hasPerm('change:create'))
const canManageTask = computed(() => auth.hasPerm('task:manage') && !isArchived.value)
const canManageDelivery = computed(() => auth.hasPerm('delivery:manage') && !isArchived.value)

const load = async () => {
  loading.value = true
  try {
    detail.value = await api.requirement(route.params.id)
  } finally {
    loading.value = false
  }
}

const actionType = (action) => {
  if (action === 'CANCEL') return 'danger'
  if (action === 'HOLD') return 'warning'
  if (action === 'RESUME') return 'success'
  if (['SUBMIT', 'ACCEPT', 'SCHEDULE', 'START_DEV', 'SUBMIT_TEST', 'ARCHIVE'].includes(action)) return 'primary'
  return ''
}

const actionDialog = reactive({
  visible: false, loading: false, action: '', comment: '', projectId: null, milestoneId: null, planDate: ''
})
const scheduleMilestones = computed(() => {
  const project = meta.value.projects.find((p) => p.id === actionDialog.projectId)
  return project?.milestones || []
})
const onProjectChange = () => {
  actionDialog.milestoneId = null
}

const handleAction = async (action) => {
  if (action === 'SCHEDULE') {
    actionDialog.action = action
    actionDialog.comment = ''
    actionDialog.projectId = req.value.projectId || null
    actionDialog.milestoneId = req.value.milestoneId || null
    actionDialog.planDate = req.value.planDate || ''
    actionDialog.visible = true
    return
  }
  if (action === 'HOLD') {
    actionDialog.action = action
    actionDialog.comment = ''
    actionDialog.visible = true
    return
  }
  try {
    await ElMessageBox.confirm(`确认执行【${actionLabel(action)}】？`, '操作确认', { type: 'warning' })
    await api.requirementAction(req.value.id, { action })
    ElMessage.success('操作成功')
    await load()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const submitAction = async () => {
  actionDialog.loading = true
  try {
    await api.requirementAction(req.value.id, {
      action: actionDialog.action,
      comment: actionDialog.comment,
      projectId: actionDialog.projectId,
      milestoneId: actionDialog.milestoneId,
      planDate: actionDialog.planDate || null
    })
    ElMessage.success('操作成功')
    actionDialog.visible = false
    await load()
  } finally {
    actionDialog.loading = false
  }
}

const removeRequirement = async () => {
  try {
    await ElMessageBox.confirm('删除后不可恢复，确认删除该草稿需求？', '删除确认', { type: 'warning' })
    await api.deleteRequirement(req.value.id)
    ElMessage.success('已删除')
    router.push('/requirements')
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

/* 评审 */
const reviewDialog = reactive({
  visible: false, loading: false, reviewerIds: [], deadline: '', comment: ''
})
const openReviewDialog = () => {
  reviewDialog.reviewerIds = []
  reviewDialog.deadline = ''
  reviewDialog.comment = ''
  reviewDialog.visible = true
}
const submitReview = async () => {
  if (!reviewDialog.reviewerIds.length) {
    ElMessage.warning('请选择评审人')
    return
  }
  reviewDialog.loading = true
  try {
    await api.createReview({
      requirementId: req.value.id,
      reviewerIds: reviewDialog.reviewerIds,
      deadline: reviewDialog.deadline || null,
      comment: reviewDialog.comment
    })
    ElMessage.success('评审已发起')
    reviewDialog.visible = false
    await load()
  } finally {
    reviewDialog.loading = false
  }
}

const canAddOpinion = (review) => {
  if (review.status !== 'PENDING' || !auth.hasPerm('review:opinion')) return false
  const ids = (review.reviewerIds || '').split(',').filter(Boolean)
  return ids.includes(String(auth.user.id)) || review.hostId === auth.user.id
}
const canConclude = (review) => review.status === 'PENDING'
  && auth.hasPerm('review:decide') && (review.hostId === auth.user.id || auth.isAdmin)

const opinionDialog = reactive({ visible: false, loading: false, review: null, result: 'PASS', content: '' })
const openOpinion = (review) => {
  opinionDialog.review = review
  opinionDialog.result = 'PASS'
  opinionDialog.content = ''
  opinionDialog.visible = true
}
const submitOpinion = async () => {
  if (!opinionDialog.content) {
    ElMessage.warning('请填写意见内容')
    return
  }
  opinionDialog.loading = true
  try {
    await api.addOpinion(opinionDialog.review.id, {
      result: opinionDialog.result,
      content: opinionDialog.content
    })
    ElMessage.success('意见已提交')
    opinionDialog.visible = false
    await load()
  } finally {
    opinionDialog.loading = false
  }
}

const reviewConclusion = reactive({ visible: false, loading: false, review: null, pass: true, comment: '' })
const openConclusion = (review) => {
  reviewConclusion.review = review
  reviewConclusion.pass = true
  reviewConclusion.comment = ''
  reviewConclusion.visible = true
}
const submitReviewConclusion = async () => {
  reviewConclusion.loading = true
  try {
    await api.concludeReview(reviewConclusion.review.id, {
      pass: reviewConclusion.pass,
      comment: reviewConclusion.comment
    })
    ElMessage.success('评审结论已提交')
    reviewConclusion.visible = false
    await load()
  } finally {
    reviewConclusion.loading = false
  }
}

/* 变更 */
const changeDialog = reactive({
  visible: false, loading: false, reason: '', content: '', impactHours: 0, impactDesc: ''
})
const openChangeDialog = () => {
  Object.assign(changeDialog, { visible: true, reason: '', content: '', impactHours: 0, impactDesc: '' })
}
const submitChange = async () => {
  if (!changeDialog.reason || !changeDialog.content) {
    ElMessage.warning('请填写变更原因与内容')
    return
  }
  changeDialog.loading = true
  try {
    await api.createChange({
      requirementId: req.value.id,
      reason: changeDialog.reason,
      content: changeDialog.content,
      impactHours: changeDialog.impactHours,
      impactDesc: changeDialog.impactDesc
    })
    ElMessage.success('变更申请已提交')
    changeDialog.visible = false
    await load()
  } finally {
    changeDialog.loading = false
  }
}

const approveDialog = reactive({ visible: false, loading: false, change: null, approved: true, comment: '' })
const openApproveChange = (change) => {
  approveDialog.change = change
  approveDialog.approved = true
  approveDialog.comment = ''
  approveDialog.visible = true
}
const submitApprove = async () => {
  approveDialog.loading = true
  try {
    await api.approveChange(approveDialog.change.id, {
      approved: approveDialog.approved,
      comment: approveDialog.comment
    })
    ElMessage.success('审批完成')
    approveDialog.visible = false
    await load()
  } finally {
    approveDialog.loading = false
  }
}
const applyChange = async (change) => {
  try {
    await ElMessageBox.confirm('实施后版本号将递增并生成版本快照，确认继续？', '变更实施', { type: 'warning' })
    await api.applyChange(change.id)
    ElMessage.success('变更已生效')
    await load()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

/* 任务 */
const taskDialog = reactive({
  visible: false, loading: false, name: '', type: 'DEV', assigneeId: null, estimateHours: 8, planDate: ''
})
const openTaskDialog = () => {
  Object.assign(taskDialog, {
    visible: true, name: '', type: 'DEV', assigneeId: auth.user.id, estimateHours: 8, planDate: ''
  })
}
const submitTask = async () => {
  if (!taskDialog.name) {
    ElMessage.warning('请填写任务名称')
    return
  }
  taskDialog.loading = true
  try {
    await api.createTask({
      requirementId: req.value.id,
      name: taskDialog.name,
      type: taskDialog.type,
      assigneeId: taskDialog.assigneeId,
      estimateHours: taskDialog.estimateHours,
      planDate: taskDialog.planDate || null
    })
    ElMessage.success('任务已创建')
    taskDialog.visible = false
    await load()
  } finally {
    taskDialog.loading = false
  }
}

const taskStatusDialog = reactive({
  visible: false, loading: false, task: null, status: '', statusLabel: '',
  actualHours: 0, finishNote: '', blockReason: ''
})
const openTaskStatus = (task, status) => {
  const labels = { IN_PROGRESS: '进行中', DONE: '已完成', BLOCKED: '已阻塞' }
  Object.assign(taskStatusDialog, {
    visible: true, task, status, statusLabel: labels[status] || status,
    actualHours: task.actualHours || 0, finishNote: '', blockReason: ''
  })
}
const submitTaskStatus = async () => {
  if (taskStatusDialog.status === 'DONE' && !taskStatusDialog.finishNote) {
    ElMessage.warning('完成任务必须填写完成说明')
    return
  }
  taskStatusDialog.loading = true
  try {
    await api.updateTaskStatus(taskStatusDialog.task.id, {
      status: taskStatusDialog.status,
      actualHours: taskStatusDialog.actualHours,
      finishNote: taskStatusDialog.finishNote,
      blockReason: taskStatusDialog.blockReason
    })
    ElMessage.success('任务状态已更新')
    taskStatusDialog.visible = false
    await load()
  } finally {
    taskStatusDialog.loading = false
  }
}

/* 交付物 */
const deliverableDialog = reactive({ visible: false, loading: false, type: 'SPEC', name: '', description: '' })
const deliverableFiles = ref([])
const selectedDeliverableFile = ref(null)
const openDeliverableDialog = () => {
  Object.assign(deliverableDialog, { visible: true, type: 'SPEC', name: '', description: '' })
  deliverableFiles.value = []
  selectedDeliverableFile.value = null
}
const onDeliverableFile = (file) => {
  selectedDeliverableFile.value = file.raw
}
const submitDeliverable = async () => {
  deliverableDialog.loading = true
  try {
    const form = new FormData()
    form.append('type', deliverableDialog.type)
    form.append('name', deliverableDialog.name || '')
    form.append('description', deliverableDialog.description || '')
    if (selectedDeliverableFile.value) {
      form.append('file', selectedDeliverableFile.value)
    }
    await api.createDeliverable(req.value.id, form)
    ElMessage.success('交付物已登记')
    deliverableDialog.visible = false
    await load()
  } finally {
    deliverableDialog.loading = false
  }
}
const downloadDeliverable = (row) => downloadFile(`/deliverables/${row.id}/download`, row.fileName || row.name)

/* 验收 */
const acceptanceDialog = reactive({ visible: false, loading: false, acceptorId: null, comment: '' })
const openAcceptanceDialog = () => {
  Object.assign(acceptanceDialog, { visible: true, acceptorId: null, comment: '' })
}
const submitAcceptance = async () => {
  if (!acceptanceDialog.acceptorId) {
    ElMessage.warning('请选择验收人')
    return
  }
  acceptanceDialog.loading = true
  try {
    await api.createAcceptance({
      requirementId: req.value.id,
      acceptorId: acceptanceDialog.acceptorId,
      comment: acceptanceDialog.comment
    })
    ElMessage.success('已提交验收')
    acceptanceDialog.visible = false
    await load()
  } finally {
    acceptanceDialog.loading = false
  }
}
const canConcludeAcceptance = (acceptance) => acceptance.status === 'PENDING'
  && (acceptance.acceptorId === auth.user.id || auth.isAdmin) && auth.hasPerm('delivery:accept')

const acceptanceConclusion = reactive({
  visible: false, loading: false, acceptance: null, pass: true, comment: '', reworkRequirement: ''
})
const openAcceptanceConclusion = (acceptance) => {
  Object.assign(acceptanceConclusion, {
    visible: true, acceptance, pass: true, comment: '', reworkRequirement: ''
  })
}
const submitAcceptanceConclusion = async () => {
  if (!acceptanceConclusion.pass && !acceptanceConclusion.reworkRequirement) {
    ElMessage.warning('验收不通过时必须填写返工要求')
    return
  }
  acceptanceConclusion.loading = true
  try {
    await api.concludeAcceptance(acceptanceConclusion.acceptance.id, {
      pass: acceptanceConclusion.pass,
      comment: acceptanceConclusion.comment,
      reworkRequirement: acceptanceConclusion.reworkRequirement
    })
    ElMessage.success('验收结论已提交')
    acceptanceConclusion.visible = false
    await load()
  } finally {
    acceptanceConclusion.loading = false
  }
}

/* 附件 */
const uploadAttachment = async ({ file }) => {
  const form = new FormData()
  form.append('file', file)
  await api.uploadAttachment(req.value.id, form)
  ElMessage.success('附件已上传')
  await load()
}
const downloadAttachment = (file) => downloadFile(`/requirements/attachments/${file.id}/download`, file.fileName)
const removeAttachment = async (file) => {
  try {
    await ElMessageBox.confirm('确认删除该附件？', '提示', { type: 'warning' })
    await api.deleteAttachment(file.id)
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

onMounted(load)
</script>

<style scoped>
.detail-head {
  align-items: flex-start;
}
.title-line {
  display: flex;
  align-items: center;
  gap: 8px;
}
.req-no {
  color: #6b7f96;
}
.version {
  font-size: 12px;
  color: #93a4b8;
}
.head-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
  max-width: 620px;
}
.block {
  padding: 18px 20px;
  margin-bottom: 16px;
}
.block-title {
  font-size: 15px;
  font-weight: 600;
  color: #23384f;
  margin-bottom: 14px;
}
.block-title.sub {
  font-size: 14px;
  margin-bottom: 10px;
}
.section {
  margin-bottom: 16px;
}
.section-label {
  font-size: 13px;
  color: #8092a8;
  margin-bottom: 6px;
}
.tabs-card {
  padding: 8px 20px 20px;
}
.review-card {
  border: 1px solid #e8edf4;
  border-radius: 8px;
  padding: 14px 16px;
  margin-bottom: 12px;
}
.review-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.review-meta {
  color: #8092a8;
  font-size: 12px;
  margin-top: 6px;
}
.review-comment {
  background: #f7faff;
  border-radius: 6px;
  padding: 8px 10px;
  margin-top: 8px;
  font-size: 13px;
  color: #3d526b;
}
.opinion {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-top: 1px dashed #eef2f8;
  margin-top: 8px;
  font-size: 13px;
}
.opinion-user {
  color: #23384f;
  font-weight: 600;
}
.opinion-content {
  flex: 1;
  color: #4a5f76;
}
.opinion-time {
  color: #9aa9bb;
  font-size: 11px;
}
.tab-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.version-line {
  display: flex;
  align-items: center;
  gap: 10px;
}
.timeline-line {
  display: flex;
  gap: 12px;
  align-items: center;
}
.file-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px dashed #eef2f8;
  font-size: 13px;
}
.file-name {
  flex: 1;
  color: #23384f;
}
.overdue {
  color: #f56c6c;
  font-weight: 600;
}
</style>
