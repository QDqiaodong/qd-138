# qd-138 剧本杀道具溯源系统

## 项目简介

剧本杀主题、人物角色与道具档案溯源管理系统。项目包含 Vue/Vite 前端、Spring Boot 后端、MySQL 与 Redis，已按依赖层缓存和固定端口交付链路规范整理。

## 访问地址

- 前端地址: [http://localhost:8138](http://localhost:8138)
- 127.0.0.1 地址: [http://127.0.0.1:8138](http://127.0.0.1:8138)
- 后端 API: http://localhost:8148/api

## 端口

- 前端: 8138
- 后端: 8148
- MySQL: 3364
- Redis: 6437

## 编译与启动

```bash
cd backend
mvn compile -q

cd ../frontend
npm ci
npm run build

cd ..
docker compose up -d --build
```

Docker Compose 端口均绑定到 `127.0.0.1`，镜像基础地址通过 `.env` 中的 `DOCKER_REGISTRY` 统一控制。
