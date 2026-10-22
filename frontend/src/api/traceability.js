import request from '@/utils/request'

export function getMaterialTrace(batchNo) {
  return request.get(`/traceability/material-batches/${encodeURIComponent(batchNo)}`)
}

export function getFinishedTrace(batchNo) {
  return request.get(`/traceability/finished-batches/${encodeURIComponent(batchNo)}`)
}