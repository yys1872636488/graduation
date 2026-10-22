import request from '@/utils/request'

export function listProductionPlans(params) {
  return request.get('/production-plans', { params })
}

export function createProductionPlan(data) {
  return request.post('/production-plans', data)
}

export function updateProductionPlan(id, data) {
  return request.put(`/production-plans/${id}`, data)
}

export function releaseProductionPlan(id, quantity) {
  return request.post(`/production-plans/${id}/release`, { quantity })
}

export function getProductionPlanStatusLogs(id) {
  return request.get(`/production-plans/${id}/status-logs`)
}

export function cancelProductionPlan(id) {
  return request.post(`/production-plans/${id}/cancel`)
}

export function listWorkOrders(params) {
  return request.get('/work-orders', { params })
}

export function getWorkOrder(id) {
  return request.get(`/work-orders/${id}`)
}

export function createWorkOrder(data) {
  return request.post('/work-orders', data)
}

export function startWorkOrder(id) {
  return request.post(`/work-orders/${id}/start`)
}

export function cancelWorkOrder(id, data) {
  return request.post(`/work-orders/${id}/cancel`, data)
}

export function getWorkOrderStatusLogs(id) {
  return request.get(`/work-orders/${id}/status-logs`)
}

export function createMaterialIssue(workOrderId, data) {
  return request.post(`/work-orders/${workOrderId}/material-issues`, data)
}

export function listMaterialIssues(params) {
  return request.get('/material-issues', { params })
}

export function addProductionProgress(workOrderId, data) {
  return request.post(`/work-orders/${workOrderId}/progress`, data)
}

export function addFinishedInspection(workOrderId, data) {
  return request.post(`/work-orders/${workOrderId}/finished-inspections`, data)
}

export function listFinishedInspections(params) {
  return request.get('/finished-inspections', { params })
}

export function addQualityDisposition(data) {
  return request.post('/quality-dispositions', data)
}

export function createFinishedBatch(workOrderId, data) {
  return request.post(`/work-orders/${workOrderId}/finished-batches`, data)
}

export function listFinishedBatches(params) {
  return request.get('/finished-batches', { params })
}