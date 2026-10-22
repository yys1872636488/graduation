import request from '@/utils/request'

export function listArrivals(params) {
  return request.get('/arrivals', { params })
}

export function registerArrival(orderId, data) {
  return request.post(`/purchase-orders/${orderId}/arrivals`, data)
}