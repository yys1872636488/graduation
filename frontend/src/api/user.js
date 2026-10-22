import request from '@/utils/request'

export function listUsers(params) {
  return request.get('/users', { params })
}

export function createUser(data) {
  return request.post('/users', data)
}

export function updateUser(id, data) {
  return request.put(`/users/${id}`, data)
}

export function disableUser(id) {
  return request.put(`/users/${id}/disable`)
}

export function enableUser(id) {
  return request.put(`/users/${id}/enable`)
}

export function resetPassword(id, password) {
  return request.put(`/users/${id}/reset-password`, { password })
}

export function getUserRoleIds(id) {
  return request.get(`/users/${id}/role-ids`)
}