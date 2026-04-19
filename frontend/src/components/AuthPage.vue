<template>
  <div class="auth-container">
    <div class="auth-card">
      <h2>{{ isLogin ? '登录' : '注册' }}</h2>
      <form @submit.prevent="handleSubmit">
        <div class="form-group">
          <label>用户名</label>
          <input v-model="form.username" type="text" required placeholder="输入用户名" />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input v-model="form.password" type="password" required placeholder="输入密码" />
        </div>
        <div v-if="!isLogin" class="form-group">
          <label>邮箱</label>
          <input v-model="form.email" type="email" required placeholder="输入邮箱" />
        </div>
        <div v-if="error" class="error-message">
          {{ error }}
        </div>
        <button type="submit" :disabled="loading">
          {{ loading ? '处理中...' : (isLogin ? '登录' : '注册') }}
        </button>
      </form>
      <p class="switch-mode">
        {{ isLogin ? '没有账号?' : '已有账号?' }}
        <a href="#" @click.prevent="isLogin = !isLogin">{{ isLogin ? '立即注册' : '立即登录' }}</a>
      </p>
      <div class="test-accounts">
        <p>测试账号：test / test123</p>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'

export default {
  name: 'AuthPage',
  props: {
    onAuthSuccess: {
      type: Function,
      required: true
    }
  },
  setup(props) {
    const isLogin = ref(true)
    const loading = ref(false)
    const error = ref('')
    const form = reactive({
      username: '',
      password: '',
      email: ''
    })

    const handleSubmit = async () => {
      error.value = ''
      loading.value = true
      
      try {
        const url = isLogin.value ? '/api/auth/login' : '/api/auth/register'
        
        const response = await fetch(url, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(isLogin.value ? {
            username: form.username,
            password: form.password
          } : {
            username: form.username,
            password: form.password,
            email: form.email
          })
        })

        // 检查响应状态
        if (!response.ok) {
          // 尝试解析错误信息
          const contentType = response.headers.get('content-type')
          if (contentType && contentType.includes('application/json')) {
            try {
              const errorData = await response.json()
              throw new Error(errorData.error || errorData.message || `请求失败 (${response.status})`)
            } catch (e) {
              if (e.message.includes('请求失败')) {
                throw e
              }
              throw new Error(`请求失败 (${response.status}): ${response.statusText}`)
            }
          } else {
            throw new Error(`请求失败 (${response.status}): ${response.statusText}`)
          }
        }

        // 尝试解析成功响应
        const contentType = response.headers.get('content-type')
        if (!contentType || !contentType.includes('application/json')) {
          const text = await response.text()
          console.error('Non-JSON response:', text)
          throw new Error('服务器返回格式错误，请稍后重试')
        }

        let data
        try {
          data = await response.json()
        } catch (e) {
          console.error('JSON parse error:', e)
          throw new Error('响应数据解析失败，请稍后重试')
        }

        if (!data.token || !data.user) {
          throw new Error('服务器返回数据格式错误')
        }

        localStorage.setItem('token', data.token)
        localStorage.setItem('user', JSON.stringify(data.user))
        
        props.onAuthSuccess(data)
      } catch (err) {
        console.error('Auth error:', err)
        error.value = err.message || '网络错误，请检查服务器是否运行'
      } finally {
        loading.value = false
      }
    }

    return {
      isLogin,
      loading,
      error,
      form,
      handleSubmit
    }
  }
}
</script>

<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  width: 100vw;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.auth-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml,%3Csvg width="60" height="60" viewBox="0 0 60 60" xmlns="http://www.w3.org/2000/svg"%3E%3Cg fill="none" fill-rule="evenodd"%3E%3Cg fill="%23ffffff" fill-opacity="0.05"%3E%3Cpath d="M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z"/%3E%3C/g%3E%3C/g%3E%3C/svg%3E');
  animation: animateBackground 20s linear infinite;
}

@keyframes animateBackground {
  0% { transform: translateY(0); }
  100% { transform: translateY(60px); }
}

.auth-card {
  background: rgba(255, 255, 255, 0.95);
  padding: 2.5rem;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  width: 100%;
  max-width: 420px;
  position: relative;
  z-index: 1;
}

h2 {
  text-align: center;
  color: #1a202c;
  margin-bottom: 2rem;
  font-size: 1.75rem;
  font-weight: 700;
}

.form-group {
  margin-bottom: 1.25rem;
}

label {
  display: block;
  margin-bottom: 0.5rem;
  color: #4a5568;
  font-weight: 500;
  font-size: 0.9rem;
}

input {
  width: 100%;
  padding: 0.875rem 1rem;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  font-size: 1rem;
  transition: all 0.3s;
  background: white;
  color: #2d3748;
  box-sizing: border-box;
}

input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

button {
  width: 100%;
  padding: 0.875rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  margin-top: 1rem;
}

button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error-message {
  background: #fed7d7;
  color: #c53030;
  padding: 0.75rem;
  border-radius: 6px;
  margin-bottom: 1rem;
  font-size: 0.875rem;
  border-left: 4px solid #c53030;
}

.switch-mode {
  text-align: center;
  margin-top: 1.5rem;
  color: #718096;
  font-size: 0.9rem;
}

.switch-mode a {
  color: #667eea;
  text-decoration: none;
  font-weight: 600;
  transition: color 0.3s;
}

.switch-mode a:hover {
  color: #764ba2;
  text-decoration: underline;
}

.test-accounts {
  margin-top: 1.5rem;
  padding: 1rem;
  background: #f7fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.test-accounts p {
  margin: 0;
  text-align: center;
  color: #718096;
  font-size: 0.875rem;
}
</style>
