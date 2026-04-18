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
  padding: var(--spacing-lg) var(--spacing-xl) var(--spacing-xl);
  background-color: var(--bg-dark);
  border-top: 1px solid var(--border-color);
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.3);
  position: relative;
  background-image: linear-gradient(180deg, transparent, var(--bg-sidebar));
}

.message-input-form {
  display: flex;
  gap: var(--spacing-md);
  max-width: 900px;
  margin: 0 auto;
  position: relative;
}

.input-wrapper {
  flex: 1;
  position: relative;
}

.message-textarea {
  width: 100%;
  padding: var(--spacing-lg) var(--spacing-xl) var(--spacing-lg) var(--spacing-lg);
  background-color: var(--bg-input);
  border: 2px solid var(--border-color);
  border-radius: var(--border-radius-lg);
  color: var(--text-primary);
  font-size: 15px;
  font-family: inherit;
  resize: none;
  outline: none;
  transition: all var(--transition-normal);
  min-height: 64px;
  max-height: 240px;
  overflow-y: auto;
  line-height: 1.6;
  position: relative;
  z-index: 1;
}

.message-textarea:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px var(--primary-light);
  background-color: var(--bg-card);
}

.message-textarea:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  border-color: var(--border-light);
}

.message-textarea::placeholder {
  color: var(--text-muted);
  font-style: italic;
}

.send-btn {
  position: absolute;
  right: var(--spacing-sm);
  bottom: var(--spacing-sm);
  width: 44px;
  height: 44px;
  background: var(--primary-gradient);
  border: none;
  border-radius: var(--border-radius-md);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-normal);
  box-shadow: var(--shadow-md);
  z-index: 2;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(16, 163, 127, 0.4);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: var(--shadow-sm);
}

.spinner {
  animation: spin 1s linear infinite;
  color: white;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.input-footer {
  text-align: center;
  margin-top: var(--spacing-md);
  font-size: 12px;
  color: var(--text-muted);
  font-style: italic;
  animation: fadeIn 0.5s ease-in;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Scrollbar styling for textarea */
.message-textarea::-webkit-scrollbar {
  width: 6px;
}

.message-textarea::-webkit-scrollbar-thumb {
  background: #555;
  border-radius: 3px;
  transition: background var(--transition-normal);
}

.message-textarea::-webkit-scrollbar-thumb:hover {
  background: #777;
}

/* Responsive design */
@media (max-width: 768px) {
  .message-input-container {
    padding: var(--spacing-md);
  }
  
  .message-textarea {
    padding: var(--spacing-md);
    font-size: 14px;
    min-height: 56px;
  }
  
  .send-btn {
    width: 38px;
    height: 38px;
  }
  
  .input-footer {
    font-size: 11px;
  }
}

@media (max-width: 480px) {
  .message-input-container {
    padding: var(--spacing-sm) var(--spacing-md);
  }
  
  .message-textarea {
    font-size: 13px;
  }
}

/* Focus animation */
@keyframes focusPulse {
  0% {
    box-shadow: 0 0 0 0 rgba(16, 163, 127, 0.4);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(16, 163, 127, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(16, 163, 127, 0);
  }
}

.message-textarea:focus {
  animation: focusPulse 1.5s ease-out;
}
</style>
