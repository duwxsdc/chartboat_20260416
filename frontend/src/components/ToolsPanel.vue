<template>
  <div v-if="showToolsPanel" class="modal-overlay" @click="closePanel">
    <div class="tools-panel" @click.stop>
      <div class="panel-header">
        <h3>工具与技能</h3>
        <button @click="closePanel" class="close-btn">×</button>
      </div>
      
      <div class="panel-content">
        <!-- Tools Section -->
        <div class="section">
          <h4>🔧 可用工具</h4>
          <div v-if="loading" class="loading">加载中...</div>
          <div v-else-if="tools.length === 0" class="empty">暂无工具</div>
          <div v-else class="tools-list">
            <div v-for="tool in tools" :key="tool.name" class="tool-item">
              <div class="tool-name">{{ tool.name }}</div>
              <div class="tool-desc">{{ tool.description }}</div>
            </div>
          </div>
        </div>

        <!-- Skills Section -->
        <div class="section">
          <h4>✨ 可用技能</h4>
          <div v-if="skills.length === 0" class="empty">暂无技能</div>
          <div v-else class="skills-grid">
            <div v-for="skill in skills" :key="skill.id" class="skill-card">
              <div class="skill-header">
                <span class="skill-name">{{ skill.name }}</span>
                <span class="skill-category">{{ skill.category }}</span>
              </div>
              <p class="skill-desc">{{ skill.description }}</p>
              <button class="use-skill-btn" @click="useSkill(skill.id)">使用此技能</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'

export default {
  name: 'ToolsPanel',
  props: {
    showToolsPanel: {
      type: Boolean,
      default: false
    }
  },
  emits: ['close', 'use-skill'],
  setup(props, { emit }) {
    const tools = ref([])
    const skills = ref([])
    const loading = ref(false)

    const loadToolsAndSkills = async () => {
      loading.value = true
      try {
        const token = localStorage.getItem('token')
        
        // Fetch tools
        const toolsResponse = await fetch('/api/chat/tools', {
          headers: { 'Authorization': `Bearer ${token}` }
        })
        if (toolsResponse.ok) {
          const toolsData = await toolsResponse.json()
          tools.value = toolsData.tools || []
        }

        // Fetch skills
        const skillsResponse = await fetch('/api/chat/skills', {
          headers: { 'Authorization': `Bearer ${token}` }
        })
        if (skillsResponse.ok) {
          const skillsData = await skillsResponse.json()
          skills.value = skillsData.skills || []
        }
      } catch (error) {
        console.error('Failed to load tools and skills:', error)
      } finally {
        loading.value = false
      }
    }

    const closePanel = () => {
      emit('close')
    }

    const useSkill = (skillId) => {
      emit('use-skill', skillId)
      closePanel()
    }

    onMounted(() => {
      if (props.showToolsPanel) {
        loadToolsAndSkills()
      }
    })

    return {
      tools,
      skills,
      loading,
      closePanel,
      useSkill
    }
  },
  watch: {
    showToolsPanel(newVal) {
      if (newVal) {
        loadToolsAndSkills()
      }
    }
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.tools-panel {
  background: var(--bg-card, #1e1e1e);
  border-radius: 12px;
  width: 90%;
  max-width: 800px;
  max-height: 80vh;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid var(--border-color, #333);
  background: linear-gradient(135deg, var(--bg-sidebar), var(--bg-chat));
}

.panel-header h3 {
  margin: 0;
  font-size: 18px;
  background: var(--primary-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.close-btn {
  background: none;
  border: none;
  color: var(--text-secondary, #999);
  font-size: 28px;
  cursor: pointer;
  padding: 0;
  line-height: 1;
  transition: color 0.2s;
}

.close-btn:hover {
  color: var(--text-primary, #fff);
}

.panel-content {
  padding: 20px;
  max-height: calc(80vh - 80px);
  overflow-y: auto;
}

.section {
  margin-bottom: 30px;
}

.section h4 {
  margin: 0 0 15px 0;
  font-size: 16px;
  color: var(--text-primary, #fff);
}

.loading, .empty {
  color: var(--text-muted, #888);
  font-style: italic;
  padding: 10px;
}

.tools-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tool-item {
  background: var(--bg-input, #2a2a2a);
  border: 1px solid var(--border-color, #333);
  border-radius: 8px;
  padding: 12px;
}

.tool-name {
  font-weight: 600;
  color: var(--primary-color, #10b981);
  margin-bottom: 4px;
  font-family: monospace;
}

.tool-desc {
  font-size: 13px;
  color: var(--text-secondary, #999);
}

.skills-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 15px;
}

.skill-card {
  background: var(--bg-input, #2a2a2a);
  border: 1px solid var(--border-color, #333);
  border-radius: 8px;
  padding: 15px;
  transition: all 0.3s;
}

.skill-card:hover {
  border-color: var(--primary-color, #10b981);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(16, 163, 127, 0.2);
}

.skill-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.skill-name {
  font-weight: 600;
  color: var(--text-primary, #fff);
}

.skill-category {
  font-size: 11px;
  color: var(--primary-color, #10b981);
  background: rgba(16, 163, 127, 0.1);
  padding: 2px 8px;
  border-radius: 10px;
}

.skill-desc {
  font-size: 12px;
  color: var(--text-secondary, #999);
  margin: 0 0 10px 0;
  line-height: 1.4;
}

.use-skill-btn {
  width: 100%;
  padding: 8px;
  background: var(--primary-gradient);
  border: none;
  border-radius: 6px;
  color: white;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.use-skill-btn:hover {
  transform: scale(1.02);
  box-shadow: 0 2px 8px rgba(16, 163, 127, 0.3);
}
</style>
