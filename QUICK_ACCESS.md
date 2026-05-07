# Quick Access Guide

## 🚀 System is Running!

### Frontend

👉 **Open in browser**: http://localhost:3003

### Backend API

👉 **Base URL**: http://localhost:8082/api

### Test Endpoints

#### 1. Health Check

```bash
curl http://localhost:8082/api/health
```

#### 2. Analyze Code

```bash
curl -X POST http://localhost:8082/api/review \
  -H "Content-Type: application/json" \
  -d '{"code":"public class Test { public void foo() { String s=\"\"; for(int i=0;i<100;i++){s=s+i;} } }"}'
```

#### 3. Get Metrics

```bash
curl http://localhost:8082/api/metrics
```

---

## 📊 What's Running

| Component | URL                    | Status     |
| --------- | ---------------------- | ---------- |
| Frontend  | http://localhost:3003  | ✅ Running |
| Backend   | http://localhost:8082  | ✅ Running |
| Ollama    | http://127.0.0.1:11434 | ✅ Running |

---

## 🛠️ Restart Commands

### Backend

```bash
cd ai-code-reviewer
mvn spring-boot:run
```

### Frontend

```bash
cd ai-code-reviewer/frontend-react
npm run dev
```

### Ollama

```bash
ollama serve
```

---

## 📝 Example Usage

### Via Frontend

1. Go to http://localhost:3003
2. Paste Java code
3. Click "Analyze"
4. View results

### Via API

```bash
curl -X POST http://localhost:8082/api/review \
  -H "Content-Type: application/json" \
  -d '{
    "code": "public class Example { public void process() { String result = \"\"; for(int i=0; i<1000; i++) { result += i; } } }"
  }'
```

---

## 🔍 Verify Everything Works

```bash
# 1. Check backend
curl http://localhost:8082/api/health

# 2. Check frontend
curl http://localhost:3003 | head -5

# 3. Check Ollama
curl http://127.0.0.1:11434/api/tags

# 4. Test full flow
curl -X POST http://localhost:8082/api/review \
  -H "Content-Type: application/json" \
  -d '{"code":"public class Test {}"}'
```

---

## 📋 Configuration

### Backend

- **Port**: 8082
- **Ollama URL**: http://127.0.0.1:11434
- **Model**: qwen2.5-coder:7b
- **Config file**: `ai-code-reviewer/src/main/resources/application.yml`

### Frontend

- **Port**: 3003
- **API URL**: http://localhost:8082/api
- **Config file**: `ai-code-reviewer/frontend-react/.env`

---

## ✅ System Ready!

Everything is configured and running. Start analyzing code! 🎉
