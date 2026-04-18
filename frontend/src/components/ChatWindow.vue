<template>
  <div class="chat-window">
    <header class="chat-header">
      <h2>AI 智能助手</h2>
      <div class="header-actions">
        <select v-model="selectedFormat" class="format-select">
          <option value="markdown">Markdown</option>
          <option value="json">JSON</option>
          <option value="html">HTML</option>
          <option value="table">Table</option>
          <option value="bullet">Bullet List</option>
        </select>
        <label class="tool-toggle">
          <input type="checkbox" v-model="useTools" />
          <span>启用工具</span>
        </label>
        <button class="clear-btn" @click="$emit('clear-conversation')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="3 6 5 6 21 6"></polyline>
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
          </svg>
          清空对话
        </button>
      </div>
    </header>

    <MessageList 
      :messages="messages" 
      :is-streaming="isStreaming"
      @send="handleSendFromList"
    />

    <MessageInput 
      :is-streaming="isStreaming"
      @send="handleSend"
    />
  </div>
</template>

<script>
import { ref } from 'vue'
import MessageList from './MessageList.vue'
import MessageInput from './MessageInput.vue'

export default {
  name: 'ChatWindow',
  components: { MessageList, MessageInput },
  props: {
    conversationId: { type: String, required: true },
    messages: { type: Array, required: true },
    isStreaming: { type: Boolean, default: false }
  },
  emits: ['send-message', 'clear-conversation'],
  setup(props, { emit }) {
    const selectedFormat = ref('markdown')
    const useTools = ref(false)

    const handleSend = (message) => {
      emit('send-message', message, {
        format: selectedFormat.value,
        useTools: useTools.value
      })
    }

    // 处理来自MessageList的建议按钮点击
    const handleSendFromList = (message) => {
      handleSend(message)
    }

    return { selectedFormat, useTools, handleSend, handleSendFromList }
  }
}
</script>

<style scoped>
.chat-window {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--bg-dark);
}

.chat-header {
  padding: var(--spacing-lg) var(--spacing-xl);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, var(--bg-sidebar), var(--bg-chat));
  box-shadow: var(--shadow-md);
  position: sticky;
  top: 0;
  z-index: 10;
}

.chat-header h2 {
  font-size: 20px;
  font-weight: 600;
  background: var(--primary-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.chat-header h2::before {
  content: '🤖';
  font-size: 24px;
  line-height: 1;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  flex-wrap: wrap;
}

.format-select {
  padding: var(--spacing-sm) var(--spacing-md);
  background-color: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-md);
  color: var(--text-primary);
  font-size: 13px;
  cursor: pointer;
  transition: all var(--transition-normal);
  min-width: 120px;
}

.format-select:hover {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px var(--primary-light);
}

.format-select:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.tool-toggle {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  padding: var(--spacing-sm);
  border-radius: var(--border-radius-sm);
  transition: all var(--transition-normal);
}

.tool-toggle:hover {
  background-color: var(--bg-chat);
  color: var(--text-primary);
}

.tool-toggle input[type="checkbox"] {
  accent-color: var(--primary-color);
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.clear-btn {
  padding: var(--spacing-sm) var(--spacing-md);
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-md);
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: 13px;
  font-weight: 500;
  transition: all var(--transition-normal);
  position: relative;
  overflow: hidden;
}

.clear-btn:hover {
  border-color: #ff6b6b;
  color: #ff6b6b;
  background-color: rgba(255, 107, 107, 0.1);
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.clear-btn svg {
  transition: transform var(--transition-normal);
}

.clear-btn:hover svg {
  transform: rotate(90deg);
}

/* Responsive design */
@media (max-width: 768px) {
  .chat-header {
    padding: var(--spacing-md);
    flex-direction: column;
    gap: var(--spacing-md);
    align-items: stretch;
  }
  
  .header-actions {
    justify-content: space-between;
  }
  
  .chat-header h2 {
    font-size: 18px;
    justify-content: center;
  }
  
  .format-select {
    min-width: 100px;
  }
}

@media (max-width: 480px) {
  .header-actions {
    flex-direction: column;
    align-items: stretch;
  }
  
  .format-select,
  .clear-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>