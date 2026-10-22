import request from '@/utils/request'

export function listSalesOrders(params) {
  return request.get('/sales-orders', { params })
}

export function getSalesOrder(id) {
  return request.get(`/sales-orders/${id}`)
}

export function createSalesOrder(data) {
  return request.post('/sales-orders', data)
}

export function cancelSalesOrder(id) {
  return request.post(`/sales-orders/${id}/cancel`)
}

export function createShipment(orderId, data) {
  return request.post(`/sales-orders/${orderId}/shipments`, data)
}

export function listShipments(params) {
  return request.get('/shipments', { params })
}

export function listAfterSales(params) {
  return request.get('/after-sales', { params })
}

export function createAfterSales(data) {
  return request.post('/after-sales', data)
}

export function processAfterSales(id, data) {
  return request.post(`/after-sales/${id}/process`, data)
}