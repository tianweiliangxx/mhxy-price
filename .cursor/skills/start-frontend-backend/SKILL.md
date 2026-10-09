---
name: start-frontend-backend
description: >-
  Start the mhxy-price Vite frontend and Spring Boot backend together.
  Use when the user asks to start or run both servers, or mentions
  启动前后端, 运行前后端, 一键启动, or /start-frontend-backend.
---

# 一键启动前后端

在仓库根目录启动前端，在 `server/` 启动 Spring Boot。已经在跑且健康时不要再起一份。

## 已经在跑

同时满足则直接告诉用户地址，结束：

- `http://127.0.0.1:5173/` 返回 200
- `GET http://127.0.0.1:<后端端口>/api/recognize` 返回 405

后端端口以 `vite.config.js` 里 `/api` 代理为准。

## 端口

1. 配置里的后端端口是 `server/src/main/resources/application.yml` 的 `server.port`（8080）。
2. 先看 8080 的监听进程。`steamwebhelper` 或其他非本项目进程占用时，不要结束它。改用 8081：启动前把 `vite.config.js` 的 `/api` 代理改成 `http://127.0.0.1:8081`，并用环境变量 `SERVER_PORT=8081` 启动后端。
3. 8080 空闲时，把 `/api` 代理改回 `http://127.0.0.1:8080`，不要设置 `SERVER_PORT`。
4. 前端固定 `http://127.0.0.1:5173/`。5173 已被本项目 Vite 占用时，按「已经在跑」处理。

## 启动

PowerShell 不支持 `&&`。两条都放后台，不要阻塞到进程退出。

前端在仓库根目录：

```powershell
npm run dev
```

没有 `node_modules` 时先在根目录执行 `npm install`。官方源失败就用 `npm install --registry https://registry.npmmirror.com`，不要改全局 npm 配置。

后端在 `server/`：

```powershell
mvn spring-boot:run
```

需要换端口时先设置 `$env:SERVER_PORT='8081'`。

Maven Central 超时（常见于 `198.18.*`）时，不要改全局 Maven 配置。把下面的配置写到临时 settings 文件，再用 `mvn -s <该文件> spring-boot:run`：

```xml
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>*</mirrorOf>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
```

等到前端日志出现 `Local:`，后端日志出现 `Started PriceServerApplication`。

## 校验

- 前端：`GET http://127.0.0.1:5173/` 为 200
- 后端：`GET http://127.0.0.1:<端口>/api/recognize` 为 405（该接口只接受 POST）

用中文回复两个地址。8080 被占用而改用 8081 时，说明原因。
