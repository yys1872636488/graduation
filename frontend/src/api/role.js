import request from '@/utils/request'

export function listRoles() {
  return request.get('/roles')
}

export function getRolePermissions(id) {
  return request.get(`/roles/${id}/permissions`)
}

export function assignPermissions(id, permissionIds) {
  return request.put(`/roles/${id}/permissions`, { permissionIds })
}

export function listPermissions() {
  return request.get('/permissions')
}