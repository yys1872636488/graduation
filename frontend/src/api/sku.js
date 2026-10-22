import request from '@/utils/request'

export function listSkus(params) { return request.get('/skus', { params }) }
export function createSku(data) { return request.post('/skus', data) }
export function updateSku(id, data) { return request.put(`/skus/${id}`, data) }
export function toggleSkuStatus(id) { return request.put(`/skus/${id}/toggle-status`) }