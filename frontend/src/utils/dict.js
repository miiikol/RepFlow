export const statusType = (status) => {
  const map = {
    DRAFT: 'info',
    PENDING_ACCEPT: 'warning',
    ANALYZING: 'primary',
    PENDING_REVIEW: 'warning',
    REVIEW_PASSED: 'success',
    SCHEDULED: 'primary',
    DEVELOPING: 'primary',
    TESTING: 'warning',
    PENDING_ACCEPTANCE: 'warning',
    ACCEPTED: 'success',
    ARCHIVED: 'info',
    REJECTED: 'danger',
    CANCELLED: 'info',
    ON_HOLD: 'info'
  }
  return map[status] || 'info'
}

export const priorityType = (priority) => {
  const map = { P0: 'danger', P1: 'warning', P2: 'primary', P3: 'info' }
  return map[priority] || 'info'
}

export const taskStatusType = (status) => {
  const map = {
    TODO: 'info',
    IN_PROGRESS: 'primary',
    DONE: 'success',
    BLOCKED: 'danger',
    CANCELLED: 'info'
  }
  return map[status] || 'info'
}

export const reviewStatusType = (status) => {
  const map = { PENDING: 'warning', PASSED: 'success', REJECTED: 'danger', CANCELLED: 'info' }
  return map[status] || 'info'
}

export const changeStatusType = (status) => {
  const map = { PENDING: 'warning', APPROVED: 'primary', REJECTED: 'danger', APPLIED: 'success' }
  return map[status] || 'info'
}

export const acceptanceStatusType = (status) => {
  const map = { PENDING: 'warning', PASSED: 'success', FAILED: 'danger', CANCELLED: 'info' }
  return map[status] || 'info'
}

export const actionLabel = (action) => {
  const map = {
    SUBMIT: '提交',
    ACCEPT: '受理',
    RESUBMIT: '重新提交',
    SCHEDULE: '排期',
    START_DEV: '开始开发',
    SUBMIT_TEST: '提交测试',
    ARCHIVE: '归档',
    HOLD: '挂起',
    RESUME: '恢复',
    CANCEL: '撤销'
  }
  return map[action] || action
}

export const fmtDate = (value) => {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

export const fmtDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

export const shortText = (text, length = 40) => {
  if (!text) return '-'
  return text.length > length ? `${text.slice(0, length)}...` : text
}
