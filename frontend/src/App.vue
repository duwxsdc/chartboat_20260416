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
import { ref, computed, watch } from 'vue'
import Sidebar from './components/Sidebar.vue'
import ChatWindow from './components/ChatWindow.vue'

export default {
  name: 'App',
  components: {
    Sidebar,
    ChatWindow
  },
  setup() {
    const conversations = ref([
      { id: 'default', title: '新对话', messages: [] }
    ])
    const currentConversationId = ref('default')
    const isStreaming = ref(false)

    const currentMessages = computed(() => {
      const conv = conversations.value.find(c => c.id === currentConversationId.value)
      return conv ? conv.messages : []
    })

    const startNewChat = () => {
      const newId = 'conv_' + Date.now()
      conversations.value.unshift({
        id: newId,
        title: '新对话',
        messages: []
      })
      currentConversationId.value = newId
    }

    const selectConversation = (id) => {
      currentConversationId.value = id
    }

    const deleteConversation = (id) => {
      conversations.value = conversations.value.filter(c => c.id !== id)
      if (currentConversationId.value === id) {
        currentConversationId.value = conversations.value[0]?.id || 'default'
      }
    }

    const sendMessage = async (message, options = {}) => {
      const conv = conversations.value.find(c => c.id === currentConversationId.value)
      if (!conv) return

      conv.messages.push({
        role: 'user',
        content: message,
        timestamp: Date.now()
      })

      if (conv.messages.length === 2) {
        conv.title = message.substring(0, 30) + (message.length > 30 ? '...' : '')
      }

      isStreaming.value = true

      const assistantMessage = {
        role: 'assistant',
        content: '',
        timestamp: Date.now(),
        isStreaming: true
      }
      conv.messages.push(assistantMessage)

      try {
        const response = await fetch('/api/chat/stream', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            conversationId: currentConversationId.value,
            message: message,
            useTools: options.useTools || false,
            format: options.format || 'markdown'
          })
        })

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        while (true) {
          const { done, value } = await reader.read()
          if (done) break

          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop()

          for (const line of lines) {
            if (line.startsWith('data: ')) {
              try {
                const data = JSON.parse(line.slice(6))
                if (data.content) {
                  assistantMessage.content += data.content
                }
                if (data.isDone) {
                  assistantMessage.isStreaming = false
                }
              } catch (e) {
                console.log('Parse error:', e)
              }
            }
          }
        }

        assistantMessage.isStreaming = false
      } catch (error) {
        console.error('Stream error:', error)
        assistantMessage.content = '抱歉，连接出现错误：' + error.message
        assistantMessage.isStreaming = false
      } finally {
        isStreaming.value = false
      }
    }

    const clearConversation = async () => {
      const conv = conversations.value.find(c => c.id === currentConversationId.value)
      if (conv) {
        conv.messages = []
        conv.title = '新对话'
      }
    }

    return {
      conversations,
      currentConversationId,
      currentMessages,
      isStreaming,
      startNewChat,
      selectConversation,
      deleteConversation,
      sendMessage,
      clearConversation
    }
  }
}
</script>

<style scoped>
.app-container {
  display: flex;
  height: 100vh;
  width: 100vw;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
</style>
