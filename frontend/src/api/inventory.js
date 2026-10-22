import request from '@/utils/request'

export function listInventory(params) {
  return request.get('/inventory', { params })
}

export function listInventoryTransactions(params) {
  return request.get('/inventory/transactions', { params })
}

export function materialInbound(data) {
  return request.post('/inventory/material-inbound', data)
}

export function finishedInbound(data) {
  return request.post('/inventory/finished-inbound', data)
}