<template>
  <aside class="sidebar">
    <div class="sidebar-header">
      <div class="user-info" v-if="user">
        <div class="user-details">
          <div class="user-name">{{ user.username }}</div>
          <div class="user-role">{{ user.role }}</div>
        </div>
        <button @click="$emit('logout')" class="logout-btn">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
            <polyline points="16 17 21 12 16 7"></polyline>
            <line x1="21" y1="12" x2="9" y2="12"></line>
          </svg>
          登出
        </button>
      </div>
      <button class="new-chat-btn" @click="$emit('new-chat')">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="12" y1="5" x2="12" y2="19"></line>
          <line x1="5" y1="12" x2="19" y2="12"></line>
        </svg>
        新对话
      </button>
    </div>
    
    <div class="conversation-list">
      <div
        v-for="conv in conversations"
        :key="conv.id"
        class="conversation-item"
        :class="{ active: conv.id === currentConversationId }"
        @click="$emit('select-conversation', conv.id)"
      >
        <svg class="conv-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
        </svg>
        <span class="conv-title">{{ conv.title }}</span>
        <button 
          class="delete-btn" 
          @click.stop="$emit('delete-conversation', conv.id)"
          v-if="conversations.length > 1"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="3 6 5 6 21 6"></polyline>
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
          </svg>
        </button>
      </div>
    </div>

    <div class="sidebar-footer">
      <div class="footer-links">
        <button class="footer-link" @click="showRagModal = true">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
            <line x1="16" y1="13" x2="8" y2="13"></line>
            <line x1="16" y1="17" x2="8" y2="17"></line>
            <polyline points="10 9 9 9 8 9"></polyline>
          </svg>
          文档管理
        </button>
      </div>
      <div class="settings-info">
        <span>AI 杜博容 v1.0</span>
      </div>
    </div>
    
    <!-- RAG 文档管理模态框 -->
    <div v-if="showRagModal" class="modal-overlay" @click="showRagModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>文档管理</h3>
          <button @click="showRagModal = false" class="modal-close">×</button>
        </div>
        <div class="modal-body">
          <div class="upload-section">
            <h4>上传文档</h4>
            <input type="file" ref="fileInput" multiple @change="handleFileUpload" class="file-input" />
            <button @click="$refs.fileInput.click()" class="upload-btn">选择文件</button>
          </div>
          <div v-if="uploading" class="upload-status uploading">上传中...</div>
          <div v-else-if="uploadMessage" class="upload-status" :class="uploadSuccess ? 'success' : 'error'">
            {{ uploadMessage }}
          </div>
        </div>
      </div>
    </div>
  </aside>
</template>

<script>
import { ref } from 'vue'

export default {
  name: 'Sidebar',
  props: {
    conversations: {
      type: Array,
      required: true
    },
    currentConversationId: {
      type: String,
      required: true
    },
    user: {
      type: Object,
      default: null
    }
  },
  emits: ['new-chat', 'select-conversation', 'delete-conversation', 'logout'],
  setup() {
    const showRagModal = ref(false)
    const uploading = ref(false)
    const uploadMessage = ref('')
    const uploadSuccess = ref(false)

    const handleFileUpload = async (event) => {
      const files = event.target.files
      if (files.length === 0) return

      uploading.value = false
      uploadMessage.value = 'RAG 功能正在配置中，暂不可用'
      uploadSuccess.value = false
      
      event.target.value = ''
      
      // 3秒后清除消息
      setTimeout(() => {
        uploadMessage.value = ''
      }, 3000)
    }

    return {
      showRagModal,
      uploading,
      uploadMessage,
      uploadSuccess,
      handleFileUpload
    }
  }
}
</script>

<style scoped>
.sidebar {
  width: 280px;
  background-color: var(--bg-sidebar);
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border-color);
  box-shadow: var(--shadow-md);
  transition: all var(--transition-slow);
}

.sidebar-header {
  padding: var(--spacing-lg);
  border-bottom: 1px solid var(--border-color);
  background: linear-gradient(135deg, var(--bg-sidebar), var(--bg-chat));
}

.user-info {
  margin-bottom: var(--spacing-lg);
  padding: var(--spacing-md);
  background-color: var(--bg-card);
  border-radius: var(--border-radius-md);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.user-details {
  margin-bottom: var(--spacing-md);
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--spacing-xs);
}

.user-role {
  font-size: 12px;
  color: var(--text-secondary);
}

.logout-btn {
  width: 100%;
  padding: var(--spacing-sm) var(--spacing-md);
  background-color: var(--danger-light);
  border: 1px solid var(--danger-color);
  border-radius: var(--border-radius-sm);
  color: var(--danger-color);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: 12px;
  font-weight: 500;
  transition: all var(--transition-normal);
}

.logout-btn:hover {
  background-color: var(--danger-color);
  color: white;
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.new-chat-btn {
  width: 100%;
  padding: var(--spacing-md) var(--spacing-lg);
  background: transparent;
  border: 2px dashed var(--border-color);
  border-radius: var(--border-radius-lg);
  color: var(--text-primary);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition-normal);
  position: relative;
  overflow: hidden;
}

.new-chat-btn:hover {
  border-color: var(--primary-color);
  background-color: var(--primary-light);
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.new-chat-btn svg {
  flex-shrink: 0;
  transition: transform var(--transition-normal);
}

.new-chat-btn:hover svg {
  transform: rotate(90deg) scale(1.1);
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-sm);
}

.conversation-item {
  padding: var(--spacing-md);
  border-radius: var(--border-radius-md);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-xs);
  transition: all var(--transition-normal);
  position: relative;
  border: 1px solid transparent;
}

.conversation-item:hover {
  background-color: var(--bg-chat);
  border-color: var(--border-color);
  transform: translateX(4px);
}

.conversation-item.active {
  background-color: var(--primary-light);
  border-color: var(--primary-color);
  box-shadow: 0 2px 8px rgba(16, 163, 127, 0.2);
}

.conv-icon {
  flex-shrink: 0;
  color: var(--text-secondary);
  transition: color var(--transition-normal);
}

.conversation-item.active .conv-icon {
  color: var(--primary-color);
}

.conv-title {
  flex: 1;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color var(--transition-normal);
}

.conversation-item.active .conv-title {
  color: var(--text-primary);
  font-weight: 500;
}

.delete-btn {
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: var(--spacing-xs);
  border-radius: var(--border-radius-sm);
  opacity: 0;
  transition: all var(--transition-normal);
  flex-shrink: 0;
  position: relative;
}

.conversation-item:hover .delete-btn {
  opacity: 1;
  transform: scale(1.1);
}

.delete-btn:hover {
  background-color: rgba(255, 107, 107, 0.1);
  color: #ff6b6b;
  transform: scale(1.2);
}

.sidebar-footer {
  padding: var(--spacing-lg);
  border-top: 1px solid var(--border-color);
  background: linear-gradient(135deg, var(--bg-chat), var(--bg-sidebar));
}

.footer-links {
  margin-bottom: var(--spacing-lg);
}

.footer-link {
  width: 100%;
  padding: var(--spacing-md);
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-md);
  color: var(--text-primary);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  font-size: 12px;
  font-weight: 500;
  transition: all var(--transition-normal);
}

.footer-link:hover {
  border-color: var(--primary-color);
  background-color: var(--primary-light);
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.settings-info {
  padding: var(--spacing-md);
  font-size: 12px;
  color: var(--text-muted);
  text-align: center;
  border-radius: var(--border-radius-md);
  background-color: var(--bg-card);
  box-shadow: var(--shadow-sm);
}

/* Modal styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.modal-content {
  background-color: var(--bg-card);
  border-radius: var(--border-radius-lg);
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--border-color);
}

.modal-header {
  padding: var(--spacing-lg);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, var(--bg-card), var(--bg-sidebar));
}

.modal-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.modal-close {
  background: none;
  border: none;
  color: var(--text-secondary);
  font-size: 20px;
  cursor: pointer;
  padding: 0;
  line-height: 1;
  transition: color var(--transition-normal);
}

.modal-close:hover {
  color: var(--text-primary);
  transform: scale(1.1);
}

.modal-body {
  padding: var(--spacing-lg);
}

.upload-section {
  margin-bottom: var(--spacing-lg);
}

.upload-section h4 {
  margin: 0 0 var(--spacing-md) 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.file-input {
  display: none;
}

.upload-btn {
  padding: var(--spacing-md) var(--spacing-lg);
  background-color: var(--primary-color);
  border: 1px solid var(--primary-color);
  border-radius: var(--border-radius-md);
  color: white;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition-normal);
}

.upload-btn:hover {
  background-color: var(--primary-dark);
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.upload-status {
  margin-top: var(--spacing-md);
  padding: var(--spacing-md);
  border-radius: var(--border-radius-sm);
  font-size: 14px;
  text-align: center;
}

.upload-status.uploading {
  background-color: var(--warning-light);
  color: var(--warning-color);
  border: 1px solid var(--warning-color);
}

.upload-status.success {
  background-color: var(--success-light);
  color: var(--success-color);
  border: 1px solid var(--success-color);
}

.upload-status.error {
  background-color: var(--danger-light);
  color: var(--danger-color);
  border: 1px solid var(--danger-color);
}

/* Scrollbar styling */
.conversation-list::-webkit-scrollbar {
  width: 6px;
}

.conversation-list::-webkit-scrollbar-thumb {
  background: #444;
  border-radius: 3px;
}

.conversation-list::-webkit-scrollbar-thumb:hover {
  background: #555;
}

/* Responsive design */
@media (max-width: 768px) {
  .sidebar {
    width: 240px;
  }
  
  .user-info {
    padding: var(--spacing-sm);
  }
  
  .user-name {
    font-size: 13px;
  }
  
  .user-role {
    font-size: 11px;
  }
  
  .new-chat-btn {
    padding: var(--spacing-sm) var(--spacing-md);
    font-size: 13px;
  }
  
  .conversation-item {
    padding: var(--spacing-sm);
  }
  
  .conv-title {
    font-size: 13px;
  }
  
  .footer-link {
    font-size: 11px;
    padding: var(--spacing-sm);
  }
}
</style>
