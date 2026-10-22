import request from '@/utils/request'

export function listPurchaseOrders(params) {
  return request.get('/purchase-orders', { params })
}

export function getPurchaseOrder(id) {
  return request.get(`/purchase-orders/${id}`)
}

export function createPurchaseOrder(data) {
  return request.post('/purchase-orders', data)
}

export function updatePurchaseOrder(id, data) {
  return request.put(`/purchase-orders/${id}`, data)
}

export function submitPurchaseOrder(id) {
  return request.post(`/purchase-orders/${id}/submit`)
}

export function cancelPurchaseOrder(id) {
  return request.post(`/purchase-orders/${id}/cancel`)
}