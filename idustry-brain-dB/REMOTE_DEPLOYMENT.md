# 其他主机运行后端 — 连接本机中间件指南

本文档说明如何在**其他主机**上运行后端 Spring Boot 服务，连接本机（中间件宿主机）Docker 容器里的 MySQL / Redis / Elasticsearch / Neo4j / MinIO。

## 1. 前置条件

- 本机已启动全部中间件容器（`idustry-brain-dB/industry-brain-manager.bat` 或 `docker compose up -d`）
- 本机防火墙放行以下端口：**3307, 6379, 9200, 7687, 7474, 9000, 9001**
- 其他主机与本机在同一局域网（可 ping 通本机 IP）

## 2. 获取本机局域网 IP

在本机执行 `ipconfig`，找到局域网 IPv4 地址（如 `172.21.160.1`），记为 `LAN_IP`。

## 3. 其他主机运行后端

在代码库根目录 `D:\work2` 执行（替换 `LAN_IP` 为本机局域网 IP）：

```bash
# Linux / macOS
export MYSQL_HOST=$LAN_IP
export MYSQL_PORT=3307
export MYSQL_USER=root
export MYSQL_PASSWORD=root123
export REDIS_HOST=$LAN_IP
export ELASTICSEARCH_HOST=$LAN_IP
export NEO4J_URI=bolt://$LAN_IP:7687
export MINIO_ENDPOINT=http://$LAN_IP:9000

mvn spring-boot:run -pl industry-brain-admin -Dspring-boot.run.profiles=dev
```

```bat
@REM Windows
set MYSQL_HOST=LAN_IP
set MYSQL_PORT=3307
set MYSQL_USER=root
set MYSQL_PASSWORD=root123
set REDIS_HOST=LAN_IP
set ELASTICSEARCH_HOST=LAN_IP
set NEO4J_URI=bolt://LAN_IP:7687
set MINIO_ENDPOINT=http://LAN_IP:9000

mvn spring-boot:run -pl industry-brain-admin -Dspring-boot.run.profiles=dev
```

## 4. 环境变量清单

| 环境变量 | 默认值 | 说明 |
|---------|--------|------|
| `MYSQL_HOST` | localhost | MySQL 主机 |
| `MYSQL_PORT` | 3307 | MySQL 端口（Docker 映射） |
| `MYSQL_USER` | root | MySQL 用户名 |
| `MYSQL_PASSWORD` | root | MySQL 密码（实际为 root123） |
| `REDIS_HOST` | localhost | Redis 主机 |
| `ELASTICSEARCH_HOST` | localhost | Elasticsearch 主机 |
| `NEO4J_URI` | bolt://localhost:7687 | Neo4j Bolt 地址 |
| `NEO4J_USERNAME` | neo4j | Neo4j 用户名 |
| `NEO4J_PASSWORD` | neo4j123 | Neo4j 密码 |
| `MINIO_ENDPOINT` | http://localhost:9000 | MinIO API 地址 |
| `MINIO_ACCESS_KEY` | minioadmin | MinIO Access Key |
| `MINIO_SECRET_KEY` | minioadmin123 | MinIO Secret Key |

> 本机默认使用 `MYSQL_PASSWORD=root123`（Docker MySQL 的 root 密码）。
> 若其他主机与后端不在同一主机，`server.port`（8080）保持默认即可。

## 5. 验证

后端启动后，在本机或其他主机访问：

- 健康检查：`curl http://BACKEND_IP:8080/api/admin/health`
- 产业链图谱：`curl http://BACKEND_IP:8080/api/v1/industry/chain/list`
- 首页 KPI：`curl http://BACKEND_IP:8080/api/v1/homedashboard/kpi`

## 6. 图谱重建

若需要重新生成产业链图谱（从企业标签聚类节点、A1 专利映射、诊断聚合）：

```bash
curl -X POST http://BACKEND_IP:8080/api/v1/industry/chain/rebuild-graph
```

后端启动时若检测到 `chain_node` 为空会自动构建一次。

## 7. 数据同步（ES / Neo4j）

ES 索引数据和 Neo4j 图谱数据由后端算法生成，具体入口：

- **MySQL → ES**：在本机执行 `python import_es_data.py`（一次性同步企业/专利到 ES）
- **MySQL → Neo4j**：后端 `GraphBuildService` 重建图谱时自动同步

## 8. 常见问题

- **MySQL 连接失败**：确认 `MYSQL_PASSWORD=root123`，且本机防火墙放行 3307
- **ES 无数据**：本机执行 `python import_es_data.py` 完成一次同步
- **Neo4j 无数据**：调用重建图谱接口 `POST /api/v1/industry/chain/rebuild-graph`
- **MinIO 连接失败**：确认 `MINIO_ENDPOINT` 指向本机 `http://LAN_IP:9000`
