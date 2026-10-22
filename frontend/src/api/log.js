import request from '@/utils/request'

export function listOperationLogs(params) {
  return request.get('/operation-logs', { params })
}