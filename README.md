# AI 智能助手系统

基于 Spring AI 1.0.0-M6 和 Spring Boot 3.4.1 构建的智能问答助手系统。
已配置通义千问 (阿里云DashScope) 作为默认模型。

## 功能特性

- **用户认证系统**: 基于JWT的安全登录认证，支持用户隔离会话
- **会话历史管理**: 实时捕获并保存用户与系统的所有交互内容，支持分页查看、搜索和筛选
- **SSE 流式输出**: 实时流式响应，打字机效果
- **多轮对话**: 内置会话记忆，支持上下文对话
- **格式化输出**: 支持 Markdown、JSON、HTML、Table、Bullet List 等格式
- **Tool 函数调用**: 内置计算、日期、搜索、汇率转换等工具
- **Skill 技能系统**: 预定义代码审查、SQL专家、翻译等技能
- **MCP 工具集成**: 支持 Model Context Protocol 工具调用
- **RAG 增强检索**: 支持文档上传和知识库检索（暂时禁用，正在配置中）
- **Vue 前端**: 现代化暗色主题界面，实时流式交互，预设文本填充功能

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
├── stop.bat                    # 一键停止脚本
├── stop-services.ps1           # PowerShell 停止脚本
├── stop-services.sh            # Linux/macOS 停止脚本
├── run-backend.bat             # 后端运行脚本
└── run-frontend.bat            # 前端运行脚本
```

## 快速开始

### Windows 用户（推荐）

**一键部署（启动前后端）：**
```bash
deploy.bat
```

**一键停止：**
```bash
stop.bat
```

### macOS / Linux 用户

**停止服务：**
```bash
chmod +x stop-services.sh
./stop-services.sh
```

**分别启动：**
```bash
# 终端1：启动后端
cd backend && mvn spring-boot:run

# 终端2：启动前端
cd frontend && npm install  # 首次需要安装依赖
npm run dev
```

### 测试账号

系统内置默认测试账号：
- 用户名: `test`
- 密码: `test123`

### 访问地址

- 前端界面: http://localhost:5173 (或 5174)
- 后端 API: http://localhost:8080/api
- 健康检查: http://localhost:8080/api/chat/health

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

### 用户认证

#### 登录

```
POST /api/auth/login
Content-Type: application/json

{
  "username": "test",
  "password": "test123"
}
```

返回：
```json
{
  "token": "JWT令牌",
  "user": {
    "id": 1,
    "username": "test",
    "email": "test@example.com"
  }
}
```

#### 注册

```
POST /api/auth/register
Content-Type: application/json

{
  "username": "新用户名",
  "password": "密码",
  "email": "邮箱"
}
```

### SSE 流式对话

```
POST /api/chat/stream
Content-Type: application/json
Authorization: Bearer {token}

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
Authorization: Bearer {token}

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
Authorization: Bearer {token}
```

### 获取技能列表

```
GET /api/chat/skills
Authorization: Bearer {token}
```

### RAG 文档管理（暂时禁用）

#### 批量上传文档

```
POST /api/rag/documents/batch
Content-Type: multipart/form-data
Authorization: Bearer {token}

# 表单数据
files: [文件1, 文件2, ...]
```

#### 获取已上传文档

```
GET /api/rag/documents
Authorization: Bearer {token}
```

#### 删除文档

```
DELETE /api/rag/documents/{documentId}
Authorization: Bearer {token}
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
