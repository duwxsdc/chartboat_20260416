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
.chat-window { display: flex; flex-direction: column; height: 100%; background-color: var(--bg-dark); }
.chat-header { padding: 16px 24px; border-bottom: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center; background-color: var(--bg-sidebar); }
.chat-header h2 { font-size: 18px; font-weight: 600; }
.header-actions { display: flex; align-items: center; gap: 16px; }
.format-select { padding: 8px 12px; background-color: var(--bg-chat); border: 1px solid var(--border-color); border-radius: 6px; color: var(--text-primary); font-size: 13px; cursor: pointer; }
.tool-toggle { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--text-secondary); cursor: pointer; }
.tool-toggle input[type="checkbox"] { accent-color: var(--primary-color); }
.clear-btn { padding: 8px 12px; background: transparent; border: 1px solid var(--border-color); border-radius: 6px; color: var(--text-secondary); cursor: pointer; display: flex; align-items: center; gap: 6px; font-size: 13px; transition: all 0.2s; }
.clear-btn:hover { border-color: #ff6b6b; color: #ff6b6b; }
</style>