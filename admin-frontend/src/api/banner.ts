import request from './request'

export interface Banner {
  id: number
  title: string
  subtitle: string | null
  description: string | null
  imageUrl: string
  linkUrl: string
  btnText: string | null
  align: string
  sort: number
  status: number
  createdTime: string
}

export function getBannerList() {
  return request.get<any, Banner[]>('/product/banner/admin/list')
}

export function addBanner(data: Partial<Banner>) {
  return request.post<void>('/product/banner/admin/add', data)
}

export function updateBanner(data: Partial<Banner>) {
  return request.put<void>('/product/banner/admin/update', data)
}

export function deleteBanner(id: number) {
  return request.delete<void>('/product/banner/admin/delete/' + id)
}
