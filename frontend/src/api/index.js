import http from './http'

export const api = {
  login: (data) => http.post('/auth/login', data),
  me: () => http.get('/auth/me'),
  changePassword: (data) => http.put('/auth/password', data),
  meta: () => http.get('/meta/options'),

  dashboardSummary: () => http.get('/dashboard/summary'),
  dashboardStatus: () => http.get('/dashboard/status-distribution'),
  dashboardDept: () => http.get('/dashboard/dept-distribution'),
  dashboardTrend: () => http.get('/dashboard/trend'),
  dashboardTodo: () => http.get('/dashboard/todo'),

  requirements: (params) => http.get('/requirements', { params }),
  requirement: (id) => http.get(`/requirements/${id}`),
  createRequirement: (data) => http.post('/requirements', data),
  updateRequirement: (id, data) => http.put(`/requirements/${id}`, data),
  deleteRequirement: (id) => http.delete(`/requirements/${id}`),
  requirementAction: (id, data) => http.post(`/requirements/${id}/action`, data),
  requirementVersions: (id) => http.get(`/requirements/${id}/versions`),
  uploadAttachment: (id, form) => http.post(`/requirements/${id}/attachments`, form),
  deleteAttachment: (id) => http.delete(`/requirements/attachments/${id}`),

  reviews: (params) => http.get('/reviews', { params }),
  review: (id) => http.get(`/reviews/${id}`),
  createReview: (data) => http.post('/reviews', data),
  addOpinion: (id, data) => http.post(`/reviews/${id}/opinions`, data),
  concludeReview: (id, data) => http.post(`/reviews/${id}/conclusion`, data),

  changes: (params) => http.get('/changes', { params }),
  createChange: (data) => http.post('/changes', data),
  approveChange: (id, data) => http.post(`/changes/${id}/approve`, data),
  applyChange: (id) => http.post(`/changes/${id}/apply`),

  projects: () => http.get('/projects'),
  createProject: (data) => http.post('/projects', data),
  createMilestone: (data) => http.post('/projects/milestones', data),

  tasks: (params) => http.get('/tasks', { params }),
  createTask: (data) => http.post('/tasks', data),
  updateTaskStatus: (id, data) => http.put(`/tasks/${id}/status`, data),

  deliverables: (requirementId) => http.get(`/requirements/${requirementId}/deliverables`),
  createDeliverable: (requirementId, form) => http.post(`/requirements/${requirementId}/deliverables`, form),
  deleteDeliverable: (id) => http.delete(`/deliverables/${id}`),
  acceptances: (params) => http.get('/acceptances', { params }),
  createAcceptance: (data) => http.post('/acceptances', data),
  concludeAcceptance: (id, data) => http.post(`/acceptances/${id}/conclusion`, data),

  adminUsers: () => http.get('/admin/users'),
  createUser: (data) => http.post('/admin/users', data),
  updateUser: (id, data) => http.put(`/admin/users/${id}`, data),
  resetPassword: (id) => http.post(`/admin/users/${id}/reset-password`),
  roles: () => http.get('/admin/roles'),
  permissions: () => http.get('/admin/permissions'),
  auditLogs: (params) => http.get('/admin/audit-logs', { params })
}
