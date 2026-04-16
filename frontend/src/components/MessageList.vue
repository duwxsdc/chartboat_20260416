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

    <div v-for="(msg, index) in messages" :key="index" class="message-wrapper">
      <div class="message" :class="msg.role">
        <div class="message-avatar">
          <span v-if="msg.role === 'user'">👤</span>
          <span v-else>🤖</span>
        </div>
        <div class="message-content">
          <div class="message-role">{{ msg.role === 'user' ? '你' : 'AI 助手' }}</div>
          <div class="message-text" v-if="msg.role === 'user'">{{ msg.content }}</div>
          <div class="message-text markdown-body" v-else v-html="renderMarkdown(msg.content)"></div>
          <div v-if="msg.isStreaming" class="streaming-indicator">
            <span class="dot"></span>
            <span class="dot"></span>
            <span class="dot"></span>
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

import hljs from 'highlight.js'

export default {
  name: 'MessageList',
  props: {
    messages: {
      type: Array,
      required: true
    },
    isStreaming: {
      type: Boolean,
      default: false
    }
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

    return {
      messageListRef,
      renderMarkdown
    }
  }
}
</script>

<style scoped>
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
}

.welcome-screen {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.welcome-content {
  text-align: center;
  max-width: 600px;
}

.welcome-icon {
  font-size: 64px;
  margin-bottom: 24px;
}

.welcome-content h1 {
  font-size: 32px;
  margin-bottom: 12px;
  background: linear-gradient(135deg, var(--primary-color), #34d399);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.welcome-content p {
  color: var(--text-secondary);
  margin-bottom: 32px;
}

.suggestions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.suggestion-btn {
  padding: 16px;
  background-color: var(--bg-chat);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  color: var(--text-primary);
  cursor: pointer;
  text-align: left;
  font-size: 14px;
  transition: all 0.2s;
}

.suggestion-btn:hover {
  border-color: var(--primary-color);
  background-color: var(--bg-message-assistant);
}

.message-wrapper {
  margin-bottom: 24px;
}

.message {
  display: flex;
  gap: 16px;
  max-width: 900px;
  margin: 0 auto;
  width: 100%;
}

.message.user {
  background-color: var(--bg-message-user);
  padding: 20px;
  border-radius: 12px;
}

.message.assistant {
  background-color: var(--bg-message-assistant);
  padding: 20px;
  border-radius: 12px;
}

.message-avatar {
  font-size: 24px;
  flex-shrink: 0;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-role {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 8px;
  font-weight: 500;
}

.message-text {
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-text :deep(p) {
  margin-bottom: 12px;
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
}

.message-text :deep(pre) {
  background-color: #1e1e1e;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 12px 0;
}

.message-text :deep(pre code) {
  background: none;
  padding: 0;
}

.message-text :deep(ul), .message-text :deep(ol) {
  padding-left: 24px;
  margin: 12px 0;
}

.message-text :deep(li) {
  margin-bottom: 6px;
}

.message-text :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 12px 0;
}

.message-text :deep(th), .message-text :deep(td) {
  border: 1px solid var(--border-color);
  padding: 10px 14px;
  text-align: left;
}

.message-text :deep(th) {
  background-color: var(--bg-chat);
  font-weight: 600;
}

.message-text :deep(blockquote) {
  border-left: 4px solid var(--primary-color);
  padding-left: 16px;
  margin: 12px 0;
  color: var(--text-secondary);
}

.message-text :deep(h1), .message-text :deep(h2), .message-text :deep(h3) {
  margin: 20px 0 12px;
}

.message-text :deep(h1) { font-size: 1.8em; }
.message-text :deep(h2) { font-size: 1.5em; }
.message-text :deep(h3) { font-size: 1.2em; }

.streaming-indicator {
  display: flex;
  gap: 6px;
  margin-top: 12px;
}

.dot {
  width: 8px;
  height: 8px;
  background-color: var(--primary-color);
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}
</style>
