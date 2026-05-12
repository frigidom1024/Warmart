import request from '@/api/request'

export const getBanners = () => request.get('/product/banner/list')
export const getCategories = () => request.get('/product/category/list')
export const getProducts = (params) => request.get('/product/list', { params })
export const getProductDetail = (id) => request.get(`/product/detail/${id}`)
export const searchProducts = (data) => request.post('/product/search', data)
export const getComments = (productId, params) => request.get(`/product/comment/list/${productId}`, { params })
export const addComment = (data) => request.post('/product/comment/add', data)
export const getConsultations = (productId) => request.get(`/product/consultation/list/${productId}`)
export const isFavorited = (productId) => request.get(`/product/favorite/check/${productId}`)
export const addFavorite = (data) => request.post('/product/favorite/add', data)
export const cancelFavorite = (productId) => request.delete(`/product/favorite/cancel/${productId}`)
