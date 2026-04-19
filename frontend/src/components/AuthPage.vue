<template>
  <div class="auth-container">
    <div class="auth-card">
      <h1>{{ isLogin ? '登录' : '注册' }}</h1>
      
      <div v-if="error" class="error-message">{{ error }}</div>
      
      <form @submit.prevent="handleSubmit">
        <div class="form-group">
          <label>用户名</label>
          <input v-model="form.username" type="text" required />
        </div>
        
        <div class="form-group">
          <label>密码</label>
          <input v-model="form.password" type="password" required />
        </div>
        
        <div v-if="!isLogin" class="form-group">
          <label>邮箱</label>
          <input v-model="form.email" type="email" required />
        </div>
        
        <button type="submit" class="btn-primary" :disabled="loading">
          {{ loading ? '处理中...' : (isLogin ? '登录' : '注册') }}
        </button>
      </form>
      
      <div class="auth-toggle">
        {{ isLogin ? '还没有账号？' : '已有账号？' }}
        <button @click="isLogin = !isLogin" class="btn-link">
          {{ isLogin ? '立即注册' : '立即登录' }}
        </button>
      </div>
      
      <div class="test-accounts">
        <h3>测试账号</h3>
        <div>用户名: test, 密码: test123</div>
        <div>用户名: admin, 密码: admin123</div>
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

        const contentType = response.headers.get('content-type')
        let data
        
        if (contentType && contentType.includes('application/json')) {
          data = await response.json()
        } else {
          const text = await response.text()
          throw new Error(`服务器返回非JSON响应: ${text || '空响应'}`)
        }

        if (!response.ok) {
          throw new Error(data.error || '认证失败')
        }

        localStorage.setItem('token', data.token)
        localStorage.setItem('user', JSON.stringify(data.user))
        
        props.onAuthSuccess(data)
      } catch (err) {
        error.value = err.message
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
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  width: 100%;
  max-width: 420px;
  position: relative;
  z-index: 1;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.auth-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.2);
}

.auth-card h1 {
  margin-bottom: 2rem;
  color: #333;
  text-align: center;
  font-size: 2rem;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  position: relative;
}

.auth-card h1::after {
  content: '';
  display: block;
  width: 60px;
  height: 4px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 2px;
  margin: 0.5rem auto 0;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scaleX(1); }
  50% { transform: scaleX(1.2); }
}

.error-message {
  background: rgba(255, 107, 107, 0.1);
  color: #c62828;
  padding: 1rem;
  border-radius: 8px;
  margin-bottom: 1.5rem;
  font-size: 0.95rem;
  border-left: 4px solid #c62828;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    transform: translateX(-20px);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.form-group {
  margin-bottom: 1.25rem;
  position: relative;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #555;
  font-size: 0.9rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.form-group input {
  width: 100%;
  padding: 1rem;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 1rem;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.8);
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  background: white;
  transform: translateY(-2px);
}

.btn-primary {
  width: 100%;
  padding: 1rem;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-top: 1.5rem;
  position: relative;
  overflow: hidden;
}

.btn-primary::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(102, 126, 234, 0.3);
}

.btn-primary:hover:not(:disabled)::before {
  left: 100%;
}

.btn-primary:disabled {
  background: #ccc;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.auth-toggle {
  margin-top: 1.5rem;
  text-align: center;
  font-size: 0.95rem;
  color: #666;
}

.btn-link {
  background: none;
  border: none;
  color: #667eea;
  cursor: pointer;
  font-size: 0.95rem;
  font-weight: 600;
  padding: 0 0.5rem;
  transition: all 0.3s ease;
  position: relative;
}

.btn-link::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 0;
  height: 2px;
  background: #667eea;
  transition: width 0.3s ease;
}

.btn-link:hover {
  color: #764ba2;
}

.btn-link:hover::after {
  width: 100%;
}

.test-accounts {
  margin-top: 2.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
  font-size: 0.9rem;
  color: #666;
  background: rgba(0, 0, 0, 0.02);
  padding: 1.5rem;
  border-radius: 8px;
  margin-top: 2rem;
}

.test-accounts h3 {
  margin-bottom: 1rem;
  font-size: 1rem;
  color: #333;
  font-weight: 600;
  text-align: center;
}

.test-accounts div {
  margin: 0.5rem 0;
  padding: 0.5rem;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 4px;
  text-align: center;
  transition: background 0.3s ease;
}

.test-accounts div:hover {
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* Loading animation */
.btn-primary:disabled::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 20px;
  height: 20px;
  margin: -10px 0 0 -10px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* Responsive design */
@media (max-width: 768px) {
  .auth-card {
    margin: 0 1rem;
    padding: 2rem;
  }
  
  .auth-card h1 {
    font-size: 1.75rem;
  }
  
  .form-group input {
    padding: 0.8rem;
  }
  
  .btn-primary {
    padding: 0.8rem;
  }
}
</style>