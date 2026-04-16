<template>
  <aside class="sidebar">
    <div class="sidebar-header">
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
      <div class="settings-info">
        <span>AI 智能助手 v1.0</span>
      </div>
    </div>
  </aside>
</template>

<script>
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
    }
  },
  emits: ['new-chat', 'select-conversation', 'delete-conversation']
}
</script>

<style scoped>
.sidebar {
  width: 260px;
  background-color: var(--bg-sidebar);
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border-color);
}

.sidebar-header {
  padding: 12px;
  border-bottom: 1px solid var(--border-color);
}

.new-chat-btn {
  width: 100%;
  padding: 12px 16px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-primary);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  transition: all 0.2s;
}

.new-chat-btn:hover {
  background-color: var(--bg-chat);
  border-color: var(--primary-color);
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.conversation-item {
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
  transition: background-color 0.2s;
  position: relative;
}

.conversation-item:hover {
  background-color: var(--bg-chat);
}

.conversation-item.active {
  background-color: var(--bg-chat);
  border: 1px solid var(--border-color);
}

.conv-icon {
  flex-shrink: 0;
  color: var(--text-secondary);
}

.conv-title {
  flex: 1;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.delete-btn {
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  opacity: 0;
  transition: all 0.2s;
}

.conversation-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: #ff6b6b;
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid var(--border-color);
}

.settings-info {
  padding: 12px;
  font-size: 12px;
  color: var(--text-secondary);
  text-align: center;
}
</style>
