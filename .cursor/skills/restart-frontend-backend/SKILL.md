---
name: restart-frontend-backend
description: >-
  Stop the running mhxy-price frontend and backend, then start both again.
  Use when the user asks to restart both servers, or mentions
  重启前后端, 一键重启, or /restart-frontend-backend.
---

# 一键重启前后端

先停掉本项目正在跑的前端和后端，确认端口已释放，再按启动流程重新拉起。不要停无关进程。

## 停掉旧进程

1. 用 `netstat -ano` 查看 `5173`、`8080`、`8081` 的监听 PID。
2. 用进程命令行确认归属，只结束本项目进程：
   - 前端：命令行包含本仓库且为 `vite` / `npm run dev` 的 `node`，以及它的父进程。
   - 后端：命令行包含本仓库 `server`、`price-server` 或 `spring-boot:run` 的 `java` / `mvn`，以及它的父进程。
3. 8080 上的 `steamwebhelper` 或其他非本项目进程一律不要结束。
4. 用 `Stop-Process -Id <pid> -Force` 结束确认过的 PID。先结束后端 Java，再结束对应的 Maven；先结束监听 5173 的 Node，再结束对应的 npm。
5. 再次检查端口。5173 必须已释放。本项目之前占用的后端端口必须已释放。8080 若仍被 Steam 等无关进程占用，保持不动。

## 再启动

旧进程停干净后，按 [start-frontend-backend](../start-frontend-backend/SKILL.md) 的「端口」「启动」「校验」执行。

重启即使原来是健康的，也必须先停再起，不能只报告“已在运行”。

用中文回复：已停掉的旧进程，以及新的前端和后端地址。
