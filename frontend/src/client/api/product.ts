import request from './request'

export interface Product {
  id: number
  categoryId: number
  name: string
  description: string
  price: number
  originalPrice: number | null
  tag: string | null
  stock: number
  sales: number
  mainImage: string
  status: number
  isRecommend: number
  hasSpec: number | null
  createdTime: string
  updatedTime: string
  specList: ProductSpec[] | null
  imageList: string[] | null
}

export interface ProductSpec {
  id: number
  productId: number
  specName: string
  specValue: string
  extraPrice: number
  stock: number
  image: string
  sort: number
}

export interface Category {
  id: number
  name: string
  parentId: number
  sort: number
  imageUrl: string | null
  status: number | null
  createdTime: string
  updatedTime: string
  children: Category[]
}

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

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface SearchParams {
  keyword?: string
  categoryId?: number
  minPrice?: number
  maxPrice?: number
  sortBy?: string
  page?: number
  size?: number
}

export function getProductList(params: {
  categoryId?: number
  sortBy?: string
  page?: number
  size?: number
}) {
  return request.get<PageResult<Product>>('/product/list', { params })
}

export function getProductDetail(id: number) {
  return request.get<Product>('/product/detail/' + id)
}

export function searchProducts(params: SearchParams) {
  return request.post<PageResult<Product>>('/product/search', params)
}

export function getCategoryList() {
  return request.get<Category[]>('/product/category/list')
}

export function getBannerList() {
  return request.get<Banner[]>('/product/banner/list')
}

export function getProductSpecs(productId: number) {
  return request.get<ProductSpec[]>('/product/spec/list/' + productId)
}
