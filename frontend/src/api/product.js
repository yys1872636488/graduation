import request from '@/utils/request'

export function listProducts(params) { return request.get('/products', { params }) }
export function createProduct(data) { return request.post('/products', data) }
export function updateProduct(id, data) { return request.put(`/products/${id}`, data) }
export function toggleProductStatus(id) { return request.put(`/products/${id}/toggle-status`) }