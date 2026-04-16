# AI 智能助手系统

基于 Spring AI 1.0.0-M6 和 Spring Boot 3.4.1 构建的智能问答助手系统。
已配置通义千问 (阿里云DashScope) 作为默认模型。

## 功能特性

- **SSE 流式输出**: 实时流式响应，打字机效果
- **多轮对话**: 内置会话记忆，支持上下文对话
- **格式化输出**: 支持 Markdown、JSON、HTML、Table、Bullet List 等格式
- **Tool 函数调用**: 内置计算、日期、搜索、汇率转换等工具
- **Skill 技能系统**: 预定义代码审查、SQL专家、翻译等技能
- **MCP 工具集成**: 支持 Model Context Protocol 工具调用
- **Vue 前端**: 现代化暗色主题界面，实时流式交互

## 项目结构

```
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/ai/assistant/
│   │   ├── config/            # 配置类
│   │   ├── controller/        # REST 控制器
│   │   ├── service/           # 业务逻辑
│   │   ├── tool/              # 工具和技能
│   │   └── model/             # 数据模型
│   └── src/main/resources/
│       └── application.yml    # 应用配置
├── frontend/                   # Vue 前端
│   ├── src/
│   │   ├── components/        # Vue 组件
│   │   └── assets/            # 样式文件
│   └── package.json
├── deploy.bat                  # 一键部署脚本
├── run-backend.bat             # 后端运行脚本
└── run-frontend.bat            # 前端运行脚本
```

## 快速开始

### 方式一：一键部署

```bash
deploy.bat
```

### 方式二：分别启动

**后端 (已配置通义千问):**
```bash
cd backend
mvn spring-boot:run
```
后端服务: http://localhost:8080/api

**前端:**
```bash
cd frontend
npm install
npm run dev
```
前端界面: http://localhost:5173

### 验证服务

启动后访问: http://localhost:8080/api/chat/health

## 配置说明

### 通义千问配置 (默认)

项目已配置阿里云通义千问 (DashScope) 作为默认模型：

**配置文件:** `backend/src/main/resources/application.yml`

```yaml
spring:
  ai:
    openai:
      api-key: sk-8a195c3c108c4acdb8a2b97ca24f861b
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      chat:
        options:
          model: deepseek-v3.2  # 可选模型见下方
```

**可选模型：**
- `qwen-max` - 通义千问 Max (推荐，效果最好)
- `qwen-plus` - 通义千问 Plus
- `qwen-turbo` - 通义千问 Turbo
- `deepseek-chat` - DeepSeek 对话模型

### 环境变量 (可选)

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DASHSCOPE_API_KEY` | DashScope API 密钥 | 配置文件中已设置 |
| `DASHSCOPE_BASE_URL` | DashScope 基础 URL | https://dashscope.aliyuncs.com/compatible-mode/v1 |
| `CHAT_MODEL` | 聊天模型 | deepseek-v3.2 |

### 切换其他 API 提供商

如需切换其他服务商，修改以下配置：

**DeepSeek:**
```yaml
base-url: https://api.deepseek.com
model: deepseek-chat
```

**OpenAI:**
```yaml
base-url: https://api.openai.com/v1
model: gpt-4
```

**SiliconFlow (国内):**
```yaml
base-url: https://api.siliconflow.cn/v1
model: deepseek-ai/DeepSeek-V3
```

## API 接口

### SSE 流式对话

```
POST /api/chat/stream
Content-Type: application/json

{
  "conversationId": "可选，用于多轮对话",
  "message": "你的问题",
  "useTools": true,
  "format": "markdown"
}
```

### 阻塞式对话

```
POST /api/chat/block
Content-Type: application/json

{
  "conversationId": "可选",
  "message": "你的问题",
  "useTools": false,
  "format": "json"
}
```

### 清空对话

```
DELETE /api/chat/conversation/{conversationId}
```

### 获取技能列表

```
GET /api/chat/skills
```

## 内置工具

| 工具 | 描述 |
|------|------|
| `getCurrentDateTime` | 获取当前日期时间 |
| `calculate` | 数学表达式计算 |
| `searchInfo` | 模拟搜索信息 |
| `getRandomNumber` | 生成随机数 |
| `convertCurrency` | 货币汇率转换 |
| `getTodayDate` | 获取今天日期 |

## 内置技能

| 技能 | 描述 |
|------|------|
| `code-review` | 代码审查专家 |
| `sql-expert` | SQL 数据库专家 |
| `translator` | 专业翻译 |
| `summarizer` | 文本摘要 |
| `data-analyst` | 数据分析 |
| `creative-writer` | 创意写作 |

## 技术栈

**后端:**
- Spring Boot 3.4.6
- Spring AI 1.1.4
- Spring WebFlux (SSE)
- Lombok

**前端:**
- Vue 3.5
- Vite 6
- Markdown-it
- Highlight.js

## License

MIT
