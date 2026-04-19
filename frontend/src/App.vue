<template>
  <div v-if="!isAuthenticated" class="app-container">
    <AuthPage @auth-success="handleAuthSuccess" />
  </div>
  <div v-else class="app-container">
    <Sidebar
      :conversations="conversations"
      :current-conversation-id="currentConversationId"
      @new-chat="startNewChat"
      @select-conversation="selectConversation"
      @delete-conversation="deleteConversation"
      @logout="handleLogout"
      :user="user"
    />
    <main class="chat-main">
      <ChatWindow
        :conversation-id="currentConversationId"
        :messages="currentMessages"
        :is-streaming="isStreaming"
        @send-message="sendMessage"
        @clear-conversation="clearConversation"
        @open-tools-panel="showToolsPanel = true"
        ref="chatWindowRef"
      />
    </main>
    <ToolsPanel
      :show-tools-panel="showToolsPanel"
      @close="showToolsPanel = false"
      @use-skill="handleUseSkill"
    />
  </div>
</template>

<script>
import { ref, computed, reactive, onMounted } from 'vue'
import Sidebar from './components/Sidebar.vue'
import ChatWindow from './components/ChatWindow.vue'
import AuthPage from './components/AuthPage.vue'
import ToolsPanel from './components/ToolsPanel.vue'

export default {
  name: 'App',
  components: { Sidebar, ChatWindow, AuthPage, ToolsPanel },
  setup() {
    const isAuthenticated = ref(false)
    const user = ref(null)
    const conversations = reactive([])
    const currentConversationId = ref('')
    const isStreaming = ref(false)
    const loading = ref(false)
    const showToolsPanel = ref(false)
    const chatWindowRef = ref(null)
    const skillsCache = ref([])

    const currentMessages = computed(() => {
      const conv = conversations.find(c => c.id === currentConversationId.value)
      return conv ? conv.messages : []
    })

    const loadConversations = async () => {
      try {
        const response = await fetch('/api/chat/conversations', {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          }
        })
        
        if (response.ok) {
          const data = await response.json()
          conversations.splice(0, conversations.length, ...data.conversations.map(conv => ({
            id: conv.id,
            title: conv.title,
            messages: [],
            createdAt: conv.createdAt
          })))
          if (conversations.length > 0 && !currentConversationId.value) {
            currentConversationId.value = conversations[0].id
          }
        }
      } catch (error) {
        console.error('Failed to load conversations:', error)
      }
    }

    const handleAuthSuccess = (authData) => {
      isAuthenticated.value = true
      user.value = authData.user
      loadConversations()
    }

    const handleLogout = () => {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      isAuthenticated.value = false
      user.value = null
      conversations.length = 0
      currentConversationId.value = ''
    }

    const startNewChat = () => {
      const newId = 'conv_' + Date.now()
      conversations.unshift({ id: newId, title: '新对话', messages: [] })
      currentConversationId.value = newId
    }

    const selectConversation = async (id) => {
      currentConversationId.value = id
      try {
        const response = await fetch(`/api/chat/conversation/${id}`, {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          }
        })
        
        if (response.ok) {
          const data = await response.json()
          const conv = conversations.find(c => c.id === id)
          if (conv) {
            conv.messages = data.conversation.messages.map(msg => ({
              role: msg.role,
              content: msg.content,
              timestamp: new Date(msg.createdAt).getTime()
            }))
          }
        }
      } catch (error) {
        console.error('Failed to load conversation:', error)
      }
    }

    const deleteConversation = async (id) => {
      try {
        const response = await fetch(`/api/chat/conversation/${id}`, {
          method: 'DELETE',
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          }
        })
        
        if (response.ok) {
          const idx = conversations.findIndex(c => c.id === id)
          if (idx > -1) conversations.splice(idx, 1)
          if (currentConversationId.value === id) {
            currentConversationId.value = conversations[0]?.id || ''
          }
        }
      } catch (error) {
        console.error('Failed to delete conversation:', error)
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

      conv.messages.push({ role: 'assistant', content: '', timestamp: Date.now(), isStreaming: true })
      const assistantMsg = conv.messages[conv.messages.length - 1]

      try {
        const response = await fetch('/api/chat/stream', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          },
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
            if (!dataStr) continue
            if (dataStr === '[DONE]') {
              assistantMsg.isStreaming = false
              isStreaming.value = false
              continue
            }

            try {
              const data = JSON.parse(dataStr)
              if (data.content) {
                assistantMsg.content += data.content
              }
              if (data.done === true) {
                assistantMsg.isStreaming = false
                isStreaming.value = false
              }
            } catch (e) {
              console.warn('Parse error:', e)
            }
          }
        }

        assistantMsg.isStreaming = false
        isStreaming.value = false
      } catch (error) {
        console.error('Error in sendMessage:', error)
        assistantMsg.content = 'Error: ' + error.message
        assistantMsg.isStreaming = false
        isStreaming.value = false
      } finally {
        isStreaming.value = false
        assistantMsg.isStreaming = false
        console.log('sendMessage completed, isStreaming:', isStreaming.value)
      }
    }

    const clearConversation = async () => {
      try {
        const response = await fetch(`/api/chat/conversation/${currentConversationId.value}`, {
          method: 'DELETE',
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          }
        })
        
        if (response.ok) {
          const conv = conversations.find(c => c.id === currentConversationId.value)
          if (conv) {
            conv.messages = []
            conv.title = '新对话'
          }
        }
      } catch (error) {
        console.error('Failed to clear conversation:', error)
      }
    }

    const handleUseSkill = async (skillId) => {
      console.log('handleUseSkill called with skillId:', skillId)
      showToolsPanel.value = false
      
      try {
        const response = await fetch('/api/chat/skills', {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          }
        })
        
        if (response.ok) {
          const data = await response.json()
          const skill = data.skills.find(s => s.id === skillId)
          
          if (skill && skill.examplePrompt) {
            console.log('Skill found:', skill.name, 'Example prompt:', skill.examplePrompt)
            handleFillInput(skill.examplePrompt)
          } else {
            console.warn('Skill examplePrompt not found for skillId:', skillId)
          }
        }
      } catch (error) {
        console.error('Failed to use skill:', error)
      }
    }

    onMounted(() => {
      const token = localStorage.getItem('token')
      const userStr = localStorage.getItem('user')
      if (token && userStr) {
        isAuthenticated.value = true
        user.value = JSON.parse(userStr)
        loadConversations()
      }
    })

    return {
      isAuthenticated,
      user,
      conversations,
      currentConversationId,
      currentMessages,
      isStreaming,
      loading,
      showToolsPanel,
      chatWindowRef,
      handleAuthSuccess,
      handleLogout,
      startNewChat,
      selectConversation,
      deleteConversation,
      sendMessage,
      clearConversation,
      handleUseSkill
    }
  }
}
</script>

<style scoped>
.app-container { display: flex; height: 100vh; width: 100vw; }
.chat-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
</style>
