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
  width: 280px;
  background-color: var(--bg-sidebar);
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border-color);
  box-shadow: var(--shadow-md);
  transition: all var(--transition-slow);
}

.sidebar-header {
  padding: var(--spacing-lg);
  border-bottom: 1px solid var(--border-color);
  background: linear-gradient(135deg, var(--bg-sidebar), var(--bg-chat));
}

.new-chat-btn {
  width: 100%;
  padding: var(--spacing-md) var(--spacing-lg);
  background: transparent;
  border: 2px dashed var(--border-color);
  border-radius: var(--border-radius-lg);
  color: var(--text-primary);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition-normal);
  position: relative;
  overflow: hidden;
}

.new-chat-btn:hover {
  border-color: var(--primary-color);
  background-color: var(--primary-light);
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.new-chat-btn svg {
  flex-shrink: 0;
  transition: transform var(--transition-normal);
}

.new-chat-btn:hover svg {
  transform: rotate(90deg) scale(1.1);
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-sm);
}

.conversation-item {
  padding: var(--spacing-md);
  border-radius: var(--border-radius-md);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-xs);
  transition: all var(--transition-normal);
  position: relative;
  border: 1px solid transparent;
}

.conversation-item:hover {
  background-color: var(--bg-chat);
  border-color: var(--border-color);
  transform: translateX(4px);
}

.conversation-item.active {
  background-color: var(--primary-light);
  border-color: var(--primary-color);
  box-shadow: 0 2px 8px rgba(16, 163, 127, 0.2);
}

.conv-icon {
  flex-shrink: 0;
  color: var(--text-secondary);
  transition: color var(--transition-normal);
}

.conversation-item.active .conv-icon {
  color: var(--primary-color);
}

.conv-title {
  flex: 1;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color var(--transition-normal);
}

.conversation-item.active .conv-title {
  color: var(--text-primary);
  font-weight: 500;
}

.delete-btn {
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: var(--spacing-xs);
  border-radius: var(--border-radius-sm);
  opacity: 0;
  transition: all var(--transition-normal);
  flex-shrink: 0;
  position: relative;
}

.conversation-item:hover .delete-btn {
  opacity: 1;
  transform: scale(1.1);
}

.delete-btn:hover {
  background-color: rgba(255, 107, 107, 0.1);
  color: #ff6b6b;
  transform: scale(1.2);
}

.sidebar-footer {
  padding: var(--spacing-lg);
  border-top: 1px solid var(--border-color);
  background: linear-gradient(135deg, var(--bg-chat), var(--bg-sidebar));
}

.settings-info {
  padding: var(--spacing-md);
  font-size: 12px;
  color: var(--text-muted);
  text-align: center;
  border-radius: var(--border-radius-md);
  background-color: var(--bg-card);
  box-shadow: var(--shadow-sm);
}

/* Scrollbar styling */
.conversation-list::-webkit-scrollbar {
  width: 6px;
}

.conversation-list::-webkit-scrollbar-thumb {
  background: #444;
  border-radius: 3px;
}

.conversation-list::-webkit-scrollbar-thumb:hover {
  background: #555;
}

/* Responsive design */
@media (max-width: 768px) {
  .sidebar {
    width: 240px;
  }
  
  .new-chat-btn {
    padding: var(--spacing-sm) var(--spacing-md);
    font-size: 13px;
  }
  
  .conversation-item {
    padding: var(--spacing-sm);
  }
  
  .conv-title {
    font-size: 13px;
  }
}
</style>
