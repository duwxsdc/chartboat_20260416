<template>
  <div class="message-list" ref="messageListRef">
    <div v-if="messages.length === 0" class="welcome-screen">
      <div class="welcome-content">
        <div class="welcome-icon">🤖</div>
        <h1>AI 智能助手</h1>
        <p>支持多轮对话、工具调用、格式化输出</p>
        <div class="suggestions">
          <button class="suggestion-btn" @click="$emit('send', '你好，请介绍一下你自己')">👋 你好，请介绍一下你自己</button>
          <button class="suggestion-btn" @click="$emit('send', '帮我写一段Python快速排序代码')">💻 帮我写一段Python快速排序代码</button>
          <button class="suggestion-btn" @click="$emit('send', '计算 256 * 128 + 1024')">🔢 计算 256 * 128 + 1024</button>
          <button class="suggestion-btn" @click="$emit('send', '用表格形式列出太阳系八大行星')">🪐 用表格形式列出太阳系八大行星</button>
        </div>
      </div>
    </div>

    <div v-for="(msg, index) in messages" :key="msg.timestamp + '-' + index" class="message-wrapper">
      <div class="message" :class="msg.role">
        <div class="message-avatar">
          <span v-if="msg.role === 'user'">👤</span>
          <span v-else>🤖</span>
        </div>
        <div class="message-content">
          <div class="message-role">{{ msg.role === 'user' ? '你' : 'AI 助手' }}</div>
          <div v-if="msg.role === 'user'" class="message-text">{{ msg.content }}</div>
          <div v-else class="message-text markdown-body">
            <div v-if="msg.content" v-html="renderMarkdown(msg.content)"></div>
            <span v-if="msg.isStreaming" class="cursor"></span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, watch, nextTick } from 'vue'
import MarkdownIt from 'markdown-it'
import 'highlight.js/styles/github-dark.css'
import hljs from 'highlight.js'

const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  breaks: true,
  highlight: function (str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs"><code>' +
               hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
               '</code></pre>'
      } catch (__) {}
    }
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>'
  }
})

export default {
  name: 'MessageList',
  props: {
    messages: { type: Array, required: true },
    isStreaming: { type: Boolean, default: false }
  },
  emits: ['send'],
  setup(props) {
    const messageListRef = ref(null)

    watch(() => props.messages, async () => {
      await nextTick()
      if (messageListRef.value) {
        messageListRef.value.scrollTop = messageListRef.value.scrollHeight
      }
    }, { deep: true })

    const renderMarkdown = (content) => {
      if (!content) return ''
      return md.render(content)
    }

    return { messageListRef, renderMarkdown }
  }
}
</script>

<style scoped>
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-xl);
  display: flex;
  flex-direction: column;
  background-color: var(--bg-dark);
  background-image: 
    radial-gradient(circle at 20% 80%, rgba(16, 163, 127, 0.05) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(16, 163, 127, 0.03) 0%, transparent 50%);
}

.welcome-screen {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-2xl);
}

.welcome-content {
  text-align: center;
  max-width: 600px;
  background: var(--bg-card);
  padding: var(--spacing-2xl);
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border-color);
  transition: all var(--transition-slow);
  position: relative;
  overflow: hidden;
}

.welcome-content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: var(--primary-gradient);
}

.welcome-content:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg), 0 0 30px rgba(16, 163, 127, 0.2);
}

.welcome-icon {
  font-size: 80px;
  margin-bottom: var(--spacing-xl);
  animation: bounce 2s infinite;
  filter: drop-shadow(0 0 20px rgba(16, 163, 127, 0.3));
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-10px);
  }
  60% {
    transform: translateY(-5px);
  }
}

.welcome-content h1 {
  font-size: 36px;
  margin-bottom: var(--spacing-md);
  background: var(--primary-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.welcome-content p {
  color: var(--text-secondary);
  margin-bottom: var(--spacing-2xl);
  font-size: 16px;
  line-height: 1.6;
}

.suggestions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--spacing-md);
  margin-top: var(--spacing-xl);
}

.suggestion-btn {
  padding: var(--spacing-lg);
  background-color: var(--bg-chat);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-lg);
  color: var(--text-primary);
  cursor: pointer;
  text-align: left;
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition-normal);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.suggestion-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(16, 163, 127, 0.1), transparent);
  transition: left var(--transition-slow);
}

.suggestion-btn:hover::before {
  left: 100%;
}

.suggestion-btn:hover {
  border-color: var(--primary-color);
  background-color: var(--bg-message-assistant);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.suggestion-btn:hover {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px var(--primary-light);
}

.message-wrapper {
  margin-bottom: var(--spacing-xl);
  animation: fadeInUp 0.3s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message {
  display: flex;
  gap: var(--spacing-lg);
  max-width: 900px;
  margin: 0 auto;
  width: 100%;
  position: relative;
}

.message.user {
  background-color: var(--bg-message-user);
  padding: var(--spacing-xl);
  border-radius: var(--border-radius-lg);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  align-self: flex-end;
  margin-left: auto;
  max-width: 80%;
  border-bottom-right-radius: var(--border-radius-sm);
}

.message.assistant {
  background-color: var(--bg-message-assistant);
  padding: var(--spacing-xl);
  border-radius: var(--border-radius-lg);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  align-self: flex-start;
  max-width: 80%;
  border-bottom-left-radius: var(--border-radius-sm);
}

.message-avatar {
  font-size: 32px;
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background-color: var(--bg-card);
  border: 2px solid var(--border-color);
  box-shadow: var(--shadow-sm);
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-role {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: var(--spacing-sm);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.message-role::after {
  content: '';
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, var(--border-color), transparent);
  margin-left: var(--spacing-sm);
}

.message-text {
  line-height: 1.7;
  word-break: break-word;
  font-size: 15px;
}

.message-text :deep(p) {
  margin-bottom: var(--spacing-md);
  line-height: 1.6;
}

.message-text :deep(p:last-child) {
  margin-bottom: 0;
}

.message-text :deep(code) {
  background-color: rgba(255, 255, 255, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 0.9em;
  color: #f8f8f2;
}

.message-text :deep(pre) {
  background-color: #1e1e1e;
  padding: var(--spacing-lg);
  border-radius: var(--border-radius-md);
  overflow-x: auto;
  margin: var(--spacing-md) 0;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
}

.message-text :deep(pre code) {
  background: none;
  padding: 0;
  color: #f8f8f2;
}

.message-text :deep(ul), .message-text :deep(ol) {
  padding-left: var(--spacing-xl);
  margin: var(--spacing-md) 0;
}

.message-text :deep(li) {
  margin-bottom: var(--spacing-xs);
  line-height: 1.5;
}

.message-text :deep(li::marker) {
  color: var(--primary-color);
}

.message-text :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: var(--spacing-md) 0;
  background-color: var(--bg-card);
  border-radius: var(--border-radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.message-text :deep(th), .message-text :deep(td) {
  border: 1px solid var(--border-color);
  padding: var(--spacing-sm) var(--spacing-md);
  text-align: left;
  font-size: 14px;
}

.message-text :deep(th) {
  background-color: var(--bg-chat);
  font-weight: 600;
  color: var(--text-primary);
  border-bottom: 2px solid var(--primary-color);
}

.message-text :deep(blockquote) {
  border-left: 4px solid var(--primary-color);
  padding-left: var(--spacing-lg);
  margin: var(--spacing-md) 0;
  color: var(--text-secondary);
  font-style: italic;
  background-color: var(--primary-light);
  padding: var(--spacing-md) var(--spacing-lg);
  border-radius: 0 var(--border-radius-md) var(--border-radius-md) 0;
}

.message-text :deep(h1), .message-text :deep(h2), .message-text :deep(h3) {
  margin: var(--spacing-xl) 0 var(--spacing-md);
  color: var(--text-primary);
  font-weight: 600;
}

.message-text :deep(h1) {
  font-size: 1.8em;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: var(--spacing-sm);
}

.message-text :deep(h2) {
  font-size: 1.5em;
  border-bottom: 1px solid var(--border-light);
  padding-bottom: var(--spacing-xs);
}

.message-text :deep(h3) {
  font-size: 1.2em;
}

.message-text :deep(a) {
  color: var(--primary-color);
  text-decoration: none;
  transition: all var(--transition-normal);
  border-bottom: 1px solid transparent;
}

.message-text :deep(a:hover) {
  color: #34d399;
  border-bottom-color: var(--primary-color);
}

.cursor {
  display: inline-block;
  width: 3px;
  height: 20px;
  background-color: var(--primary-color);
  margin-left: 4px;
  vertical-align: text-bottom;
  animation: blink 0.8s infinite;
  border-radius: 2px;
  box-shadow: 0 0 8px var(--primary-color);
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* Scrollbar styling */
.message-list::-webkit-scrollbar {
  width: 8px;
}

.message-list::-webkit-scrollbar-thumb {
  background: #555;
  border-radius: 4px;
  transition: background var(--transition-normal);
}

.message-list::-webkit-scrollbar-thumb:hover {
  background: #777;
}

/* Responsive design */
@media (max-width: 768px) {
  .message-list {
    padding: var(--spacing-lg);
  }
  
  .welcome-content {
    padding: var(--spacing-xl);
    margin: 0 var(--spacing-md);
  }
  
  .welcome-icon {
    font-size: 64px;
  }
  
  .welcome-content h1 {
    font-size: 28px;
  }
  
  .suggestions {
    grid-template-columns: 1fr;
  }
  
  .message.user,
  .message.assistant {
    max-width: 95%;
    padding: var(--spacing-lg);
  }
  
  .message-avatar {
    font-size: 24px;
    width: 40px;
    height: 40px;
  }
}

@media (max-width: 480px) {
  .message-list {
    padding: var(--spacing-md);
  }
  
  .welcome-content {
    padding: var(--spacing-lg);
  }
  
  .suggestion-btn {
    font-size: 13px;
    padding: var(--spacing-md);
  }
  
  .message-text {
    font-size: 14px;
  }
}
</style>