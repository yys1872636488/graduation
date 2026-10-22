import request from '@/utils/request'

export function listWarehouses(params) { return request.get('/warehouses', { params }) }
export function createWarehouse(data) { return request.post('/warehouses', data) }
export function updateWarehouse(id, data) { return request.put(`/warehouses/${id}`, data) }
export function toggleWarehouseStatus(id) { return request.put(`/warehouses/${id}/toggle-status`) }