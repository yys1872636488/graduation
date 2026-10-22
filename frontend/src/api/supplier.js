import request from '@/utils/request'

export function listSuppliers(params) { return request.get('/suppliers', { params }) }
export function createSupplier(data) { return request.post('/suppliers', data) }
export function updateSupplier(id, data) { return request.put(`/suppliers/${id}`, data) }
export function toggleSupplierStatus(id) { return request.put(`/suppliers/${id}/toggle-status`) }