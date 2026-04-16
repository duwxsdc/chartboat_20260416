<template>
  <div class="message-input-container">
    <form class="message-input-form" @submit.prevent="handleSend">
      <div class="input-wrapper">
        <textarea
          ref="textareaRef"
          v-model="message"
          class="message-textarea"
          placeholder="输入消息... (Enter发送, Shift+Enter换行)"
          :disabled="isStreaming"
          @keydown="handleKeydown"
          rows="1"
        ></textarea>
      </div>
      <button 
        type="submit" 
        class="send-btn"
        :disabled="!message.trim() || isStreaming"
      >
        <svg v-if="!isStreaming" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="22" y1="2" x2="11" y2="13"></line>
          <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
        </svg>
        <svg v-else class="spinner" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 12a9 9 0 11-6.219-8.56"></path>
        </svg>
      </button>
    </form>
    <div class="input-footer">
      <span>AI 可能会产生错误信息，请核实重要信息。</span>
    </div>
  </div>
</template>

<script>
import { ref, nextTick } from 'vue'

export default {
  name: 'MessageInput',
  props: {
    isStreaming: {
      type: Boolean,
      default: false
    }
  },
  emits: ['send'],
  setup(props, { emit }) {
    const message = ref('')
    const textareaRef = ref(null)

    const handleSend = () => {
      if (message.value.trim() && !props.isStreaming) {
        emit('send', message.value.trim())
        message.value = ''
        nextTick(() => {
          if (textareaRef.value) {
            textareaRef.value.style.height = 'auto'
          }
        })
      }
    }

    const handleKeydown = (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault()
        handleSend()
      }
    }

    const autoResize = () => {
      if (textareaRef.value) {
        textareaRef.value.style.height = 'auto'
        textareaRef.value.style.height = Math.min(textareaRef.value.scrollHeight, 200) + 'px'
      }
    }

    return {
      message,
      textareaRef,
      handleSend,
      handleKeydown,
      autoResize
    }
  }
}
</script>

<style scoped>
.message-input-container {
  padding: 16px 24px 24px;
  background-color: var(--bg-dark);
  border-top: 1px solid var(--border-color);
}

.message-input-form {
  display: flex;
  gap: 12px;
  max-width: 900px;
  margin: 0 auto;
}

.input-wrapper {
  flex: 1;
  position: relative;
}

.message-textarea {
  width: 100%;
  padding: 14px 48px 14px 16px;
  background-color: var(--bg-input);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  color: var(--text-primary);
  font-size: 15px;
  font-family: inherit;
  resize: none;
  outline: none;
  transition: border-color 0.2s;
  min-height: 52px;
  max-height: 200px;
  overflow-y: auto;
}

.message-textarea:focus {
  border-color: var(--primary-color);
}

.message-textarea:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.message-textarea::placeholder {
  color: var(--text-secondary);
}

.send-btn {
  position: absolute;
  right: 8px;
  bottom: 8px;
  width: 36px;
  height: 36px;
  background-color: var(--primary-color);
  border: none;
  border-radius: 8px;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.send-btn:hover:not(:disabled) {
  background-color: var(--primary-hover);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.spinner {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.input-footer {
  text-align: center;
  margin-top: 12px;
  font-size: 12px;
  color: var(--text-secondary);
}
</style>
