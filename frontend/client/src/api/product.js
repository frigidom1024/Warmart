import request from '@/api/request'

export const getBanners = () => request.get('/product/banner/list')
export const getCategories = () => request.get('/product/category/list')
export const getProducts = (params) => request.get('/product/list', { params })
export const getProductDetail = (id) => request.get(`/product/detail/${id}`)
export const searchProducts = (data) => request.post('/product/search', data)
