import request from '@/api/request'

// User info
export const getUserInfo = () => request.get('/user/info')
export const updateUserInfo = (data) => request.put('/user/info', data)
export const updatePassword = (data) => request.put('/user/password', data)

// Addresses
export const getAddressList = () => request.get('/user/address/list')
export const addAddress = (data) => request.post('/user/address/add', data)
export const updateAddress = (data) => request.put('/user/address/update', data)
export const deleteAddress = (id) => request.delete(`/user/address/delete/${id}`)
export const setDefaultAddress = (id) => request.put(`/user/address/default/${id}`)

// Favorites
export const getFavorites = (params) => request.get('/product/favorite/list', { params })
export const unfavorite = (productId) => request.delete(`/product/favorite/cancel/${productId}`)

// Feedback
export const getFeedbackList = () => request.get('/user/feedback/list')
export const addFeedback = (data) => request.post('/user/feedback/add', data)

// Notices
export const getNoticeList = (params) => request.get('/user/notice/list', { params })

// Consultations
export const getMyConsultations = () => request.get('/user/consultation/list')
export const addConsultation = (data) => request.post('/user/consultation/add', data)
