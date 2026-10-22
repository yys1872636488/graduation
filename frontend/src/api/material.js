import request from '@/utils/request'

export function listMaterials(params) { return request.get('/materials', { params }) }
export function createMaterial(data) { return request.post('/materials', data) }
export function updateMaterial(id, data) { return request.put(`/materials/${id}`, data) }
export function toggleMaterialStatus(id) { return request.put(`/materials/${id}/toggle-status`) }