import request from '@/utils/request'

export function listMaterialBatches(params) {
  return request.get('/material-batches', { params })
}

export function getMaterialBatch(id) {
  return request.get(`/material-batches/${id}`)
}