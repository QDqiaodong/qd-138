import axios, { AxiosResponse } from 'axios'

const baseURL = '/api'

const rawApi = axios.create({
  baseURL,
  timeout: 10000
})

const unwrapResponse = <T>(response: AxiosResponse<{ code: number; data: T } | T>): T => {
  const data = response.data
  if (data && typeof data === 'object' && 'data' in data) {
    return (data as { data: T }).data
  }
  return data as T
}

const api: {
  get: <T>(url: string) => Promise<T>
  post: <T>(url: string, data?: any) => Promise<T>
  put: <T>(url: string, data?: any) => Promise<T>
  delete: <T>(url: string) => Promise<T>
} = {
  get: <T>(url: string) => rawApi.get<{ code: number; data: T } | T>(url).then(unwrapResponse),
  post: <T>(url: string, data?: any) => rawApi.post<{ code: number; data: T } | T>(url, data).then(unwrapResponse),
  put: <T>(url: string, data?: any) => rawApi.put<{ code: number; data: T } | T>(url, data).then(unwrapResponse),
  delete: <T>(url: string) => rawApi.delete<{ code: number; data: T } | T>(url).then(unwrapResponse)
}

export interface ScriptTheme {
  id: number
  themeName: string
  description: string
  era: string
  difficulty: string
  createdAt: string
  updatedAt: string
}

export interface CharacterRole {
  id: number
  roleName: string
  scriptThemeId: number
  gender: string
  age: number
  description: string
  createdAt: string
  updatedAt: string
}

export interface Prop {
  id: number
  propCode: string
  propName: string
  era: string
  propType: string
  description: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface ScanParseResponse {
  rawCode: string
  propCode: string
  exists: boolean
  prop: Prop | null
}

export const getErrorMessage = (err: unknown, fallback: string): string => {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as { message?: string } | undefined
    if (data?.message) {
      return data.message
    }
  }
  return fallback
}

export interface RolePropResponse {
  id: number
  characterRoleId: number
  roleName: string
  scriptThemeId: number
  themeName: string
  propId: number
  propCode: string
  propName: string
  propType: string
  era: string
  bindTime: string
}

export interface ScriptThemeDetailResponse {
  id: number
  themeName: string
  description: string
  era: string
  difficulty: string
  roles: {
    id: number
    roleName: string
    gender: string
    age: number
    description: string
    props: {
      id: number
      propCode: string
      propName: string
      propType: string
      era: string
      status: string
    }[]
  }[]
}

export interface RolePropsResponse {
  roleId: number
  roleName: string
  scriptThemeId: number
  themeName: string
  props: {
    id: number
    propCode: string
    propName: string
    propType: string
    era: string
    status: string
    description: string
  }[]
}

export interface PropChangeRecord {
  id: number
  propId: number
  characterRoleId: number
  scriptThemeId: number
  changeType: string
  beforeValue: string
  afterValue: string
  changeReason: string
  operator: string
  createdAt: string
}

export const themeApi = {
  getAll: () => api.get<ScriptTheme[]>('/theme'),
  getById: (id: number) => api.get<ScriptTheme>(`/theme/${id}`),
  getDetail: (id: number) => api.get<ScriptThemeDetailResponse>(`/theme/${id}/detail`),
  create: (data: Omit<ScriptTheme, 'id' | 'createdAt' | 'updatedAt'>) => 
    api.post<ScriptTheme>('/theme', data),
  update: (id: number, data: Partial<ScriptTheme>) => 
    api.put<ScriptTheme>(`/theme/${id}`, data),
  delete: (id: number) => api.delete<{ code: number }>(`/theme/${id}`)
}

export const roleApi = {
  getAll: () => api.get<CharacterRole[]>('/role'),
  getById: (id: number) => api.get<CharacterRole>(`/role/${id}`),
  getByTheme: (themeId: number) => api.get<CharacterRole[]>(`/role/theme/${themeId}`),
  create: (data: Omit<CharacterRole, 'id' | 'createdAt' | 'updatedAt'>) => 
    api.post<CharacterRole>('/role', data),
  update: (id: number, data: Partial<CharacterRole>) => 
    api.put<CharacterRole>(`/role/${id}`, data),
  delete: (id: number) => api.delete<{ code: number }>(`/role/${id}`)
}

export const propApi = {
  getAll: () => api.get<Prop[]>('/prop'),
  getById: (id: number) => api.get<Prop>(`/prop/${id}`),
  getByCode: (code: string) => api.get<Prop>(`/prop/code/${encodeURIComponent(code)}`),
  getByEra: (era: string) => api.get<Prop[]>(`/prop/era/${era}`),
  getByType: (type: string) => api.get<Prop[]>(`/prop/type/${type}`),
  getEraStyleTemplate: () => api.get<Record<string, string[]>>('/prop/era-style-template'),
  scanParse: (rawCode: string) => api.post<ScanParseResponse>('/prop/scan-parse', { rawCode }),
  create: (data: Omit<Prop, 'id' | 'createdAt' | 'updatedAt'>) =>
    api.post<Prop>('/prop', data),
  update: (id: number, data: Partial<Prop>) => 
    api.put<Prop>(`/prop/${id}`, data),
  delete: (id: number) => api.delete<{ code: number }>(`/prop/${id}`)
}

export const rolePropApi = {
  getAll: () => api.get<RolePropResponse[]>('/role-prop'),
  getByRoleId: (roleId: number) => api.get<RolePropResponse[]>(`/role-prop/role/${roleId}`),
  getByPropId: (propId: number) => api.get<RolePropResponse[]>(`/role-prop/prop/${propId}`),
  getByPropCode: (propCode: string) => api.get<RolePropResponse[]>(`/role-prop/prop-code/${encodeURIComponent(propCode)}`),
  getByThemeId: (themeId: number) => api.get<RolePropResponse[]>(`/role-prop/theme/${themeId}`),
  searchByRoleName: (roleName: string) => api.get<RolePropsResponse>(`/role-prop/search-by-role-name?roleName=${roleName}`),
  bind: (data: { characterRoleId: number; scriptThemeId: number; propIds: number[]; operator?: string; reason?: string }) => 
    api.post<{ code: number }>('/role-prop/bind', data),
  unbind: (data: { roleId: number; propIds: number[] }) => 
    api.post<{ code: number }>('/role-prop/unbind', data),
  change: (data: { propId: number; fromRoleId: number; toRoleId: number; scriptThemeId: number; operator?: string; reason?: string }) => 
    api.post<{ code: number }>('/role-prop/change', data)
}

export const changeRecordApi = {
  getAll: () => api.get<PropChangeRecord[]>('/change-record'),
  getByPropId: (propId: number) => api.get<PropChangeRecord[]>(`/change-record/prop/${propId}`),
  getByRoleId: (roleId: number) => api.get<PropChangeRecord[]>(`/change-record/role/${roleId}`),
  getByThemeId: (themeId: number) => api.get<PropChangeRecord[]>(`/change-record/theme/${themeId}`)
}