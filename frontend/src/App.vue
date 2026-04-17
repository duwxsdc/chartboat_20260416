<template>
  <div class="app-container">
    <Sidebar
      :conversations="conversations"
      :current-conversation-id="currentConversationId"
      @new-chat="startNewChat"
      @select-conversation="selectConversation"
      @delete-conversation="deleteConversation"
    />
    <main class="chat-main">
      <ChatWindow
        :conversation-id="currentConversationId"
        :messages="currentMessages"
        :is-streaming="isStreaming"
        @send-message="sendMessage"
        @clear-conversation="clearConversation"
      />
    </main>
  </div>
</template>

<script>
import { ref, computed, reactive } from 'vue'
import Sidebar from './components/Sidebar.vue'
import ChatWindow from './components/ChatWindow.vue'

export default {
  name: 'App',
  components: { Sidebar, ChatWindow },
  setup() {
    const conversations = reactive([
      { id: 'default', title: '新对话', messages: [] }
    ])
    const currentConversationId = ref('default')
    const isStreaming = ref(false)

    const currentMessages = computed(() => {
      const conv = conversations.find(c => c.id === currentConversationId.value)
      return conv ? conv.messages : []
    })

    const startNewChat = () => {
      const newId = 'conv_' + Date.now()
      conversations.unshift({ id: newId, title: '新对话', messages: [] })
      currentConversationId.value = newId
    }

    const selectConversation = (id) => { currentConversationId.value = id }

    const deleteConversation = (id) => {
      const idx = conversations.findIndex(c => c.id === id)
      if (idx > -1) conversations.splice(idx, 1)
      if (currentConversationId.value === id) {
        currentConversationId.value = conversations[0]?.id || 'default'
      }
    }

    const sendMessage = async (message, options = {}) => {
      const conv = conversations.find(c => c.id === currentConversationId.value)
      if (!conv) return

      conv.messages.push({ role: 'user', content: message, timestamp: Date.now() })

      if (conv.messages.length === 2) {
        conv.title = message.substring(0, 30) + (message.length > 30 ? '...' : '')
      }

      isStreaming.value = true

      // push空消息，再通过数组索引获取reactive Proxy引用
      conv.messages.push({ role: 'assistant', content: '', timestamp: Date.now(), isStreaming: true })
      // 关键：必须从reactive数组中取回Proxy对象，否则后续修改不触发视图更新
      const assistantMsg = conv.messages[conv.messages.length - 1]

      try {
        const response = await fetch('/api/chat/stream', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            conversationId: currentConversationId.value,
            message: message,
            useTools: options.useTools || false,
            format: options.format || 'markdown'
          })
        })

        if (!response.ok) throw new Error(`HTTP ${response.status}`)

        const reader = response.body.getReader()
        const decoder = new TextDecoder('utf-8')
        let buffer = ''

        while (true) {
          const { done, value } = await reader.read()
          if (done) break

          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''

          for (const line of lines) {
            const trimmed = line.trim()
            if (!trimmed.startsWith('data:')) continue
            const colonIdx = trimmed.indexOf(':')
            const dataStr = trimmed.substring(colonIdx + 1).trim()
            if (!dataStr || dataStr === '[DONE]') continue

            try {
              const data = JSON.parse(dataStr)
              if (data.content) {
                assistantMsg.content += data.content
              }
              if (data.done === true) {
                assistantMsg.isStreaming = false
              }
            } catch (e) {
              console.warn('Parse error:', e)
            }
          }
        }

        assistantMsg.isStreaming = false
      } catch (error) {
        assistantMsg.content = 'Error: ' + error.message
        assistantMsg.isStreaming = false
      } finally {
        isStreaming.value = false
      }
    }

    const clearConversation = async () => {
      const conv = conversations.find(c => c.id === currentConversationId.value)
      if (conv) { conv.messages = []; conv.title = '新对话' }
    }

    return {
      conversations, currentConversationId, currentMessages, isStreaming,
      startNewChat, selectConversation, deleteConversation, sendMessage, clearConversation
    }
  }
}
</script>

<style scoped>
.app-container { display: flex; height: 100vh; width: 100vw; }
.chat-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
</style>