// API调用工具 - 统一处理所有HTTP请求和错误

const API_BASE = '/api'

class ApiError extends Error {
  constructor(message, status, isNetworkError = false) {
    super(message)
    this.status = status
    this.isNetworkError = isNetworkError
    this.name = 'ApiError'
  }
}

async function request(url, options = {}) {
  const token = localStorage.getItem('token')
  
  const config = {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': `Bearer ${token}` }),
      ...options.headers
    }
  }

  try {
    const response = await fetch(`${API_BASE}${url}`, config)
    
    // 检查网络错误
    if (!response || response.status === 0) {
      throw new ApiError(
        '无法连接到服务器，请检查后端服务是否启动',
        0,
        true
      )
    }

    // 检查HTTP错误状态
    if (!response.ok) {
      const contentType = response.headers.get('content-type')
      
      if (contentType && contentType.includes('application/json')) {
        try {
          const errorData = await response.json()
          throw new ApiError(
            errorData.error || errorData.message || `请求失败 (${response.status})`,
            response.status
          )
        } catch (e) {
          if (e instanceof ApiError) throw e
          throw new ApiError(
            `请求失败 (${response.status}): ${response.statusText}`,
            response.status
          )
        }
      } else {
        throw new ApiError(
          `请求失败 (${response.status}): ${response.statusText}`,
          response.status
        )
      }
    }

    // 检查响应类型
    const contentType = response.headers.get('content-type')
    if (!contentType || !contentType.includes('application/json')) {
      const text = await response.text()
      console.error('Non-JSON response received:', text.substring(0, 200))
      throw new ApiError('服务器返回格式错误', response.status)
    }

    // 解析JSON
    try {
      return await response.json()
    } catch (e) {
      console.error('JSON parse error:', e)
      throw new ApiError('响应数据解析失败', response.status)
    }

  } catch (error) {
    if (error instanceof ApiError) {
      throw error
    }
    
    // 网络错误（fetch失败）
    console.error('Network error:', error)
    throw new ApiError(
      error.message || '网络连接失败，请检查服务器是否启动',
      0,
      true
    )
  }
}

// GET请求
export async function get(url) {
  return request(url, { method: 'GET' })
}

// POST请求
export async function post(url, data) {
  return request(url, {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

// PUT请求
export async function put(url, data) {
  return request(url, {
    method: 'PUT',
    body: JSON.stringify(data)
  })
}

// DELETE请求
export async function del(url) {
  return request(url, { method: 'DELETE' })
}

// 导出错误类
export { ApiError }
