import request from '@/utils/request'

export function uploadInspectionImage(batchId, file) {
  const data = new FormData()
  data.append('file', file)
  return request.post(`/material-batches/${batchId}/inspection-images`, data)
}

export function deleteInspectionImage(batchId, imageId) {
  return request.delete(`/material-batches/${batchId}/inspection-images/${imageId}`)
}

export function detectImage(batchId, imageId) {
  return request.post(`/material-batches/${batchId}/detect`, { imageId })
}

export function getInspectionDetail(batchId) {
  return request.get(`/material-batches/${batchId}/inspection`)
}

export function reviewBatch(batchId, data) {
  return request.post(`/material-batches/${batchId}/review`, data)
}

export function listInspectionRecords(params) {
  return request.get('/inspection-records', { params })
}