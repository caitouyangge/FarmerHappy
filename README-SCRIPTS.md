# 🚀 FarmerHappy 一键启动脚本使用说明

本文档说明如何使用一键启动脚本快速运行前后端服务。

## 📁 脚本文件

### Windows 系统
- `start-all.bat` - 一键启动前后端服务
- `stop-all.bat` - 一键停止所有服务

### Linux / Mac 系统
- `start-all.sh` - 一键启动前后端服务
- `stop-all.sh` - 一键停止所有服务

---

## 🪟 Windows 使用方法

### 启动服务

**方式一：双击运行**
1. 在项目根目录（`FarmerHappy`）下找到 `start-all.bat`
2. 双击运行即可

**方式二：命令行运行**
```cmd
cd D:\STUDY\FarmerHappy
start-all.bat
```

**启动流程：**
1. ✅ 检查前端依赖（首次运行会自动安装 npm 包）
2. ✅ 编译后端 Java 代码
3. ✅ 在新窗口启动后端服务（端口 8080）
4. ✅ 在新窗口启动前端服务（通常端口 8081）

**成功启动后：**
- 会打开两个新窗口（后端窗口和前端窗口）
- 后端窗口标题：`农乐平台-后端服务`
- 前端窗口标题：`农乐平台-前端服务`

### 停止服务

**方式一：双击运行**
1. 双击 `stop-all.bat`
2. 脚本会自动关闭所有服务窗口

**方式二：手动关闭**
- 直接关闭后端和前端的窗口
- 或在各窗口中按 `Ctrl + C`

**方式三：命令行运行**
```cmd
stop-all.bat
```

---

## 🐧 Linux / Mac 使用方法

### 首次使用：赋予执行权限

```bash
chmod +x start-all.sh
chmod +x stop-all.sh
```

### 启动服务

```bash
cd /path/to/FarmerHappy
./start-all.sh
```

**启动流程：**
1. ✅ 检查前端依赖（首次运行会自动安装 npm 包）
2. ✅ 编译后端 Java 代码
3. ✅ 后台启动后端服务（端口 8080）
4. ✅ 后台启动前端服务（通常端口 8081）

**成功启动后：**
- 服务在后台运行
- 日志保存在 `logs/` 目录：
  - `logs/backend.log` - 后端日志
  - `logs/frontend.log` - 前端日志
- 进程 ID 保存在：
  - `logs/backend.pid` - 后端进程 ID
  - `logs/frontend.pid` - 前端进程 ID

### 查看日志

```bash
# 查看后端日志（实时）
tail -f logs/backend.log

# 查看前端日志（实时）
tail -f logs/frontend.log

# 查看所有日志
tail -f logs/*.log
```

### 停止服务

```bash
./stop-all.sh
```

脚本会：
1. ✅ 优雅停止后端服务
2. ✅ 优雅停止前端服务
3. ✅ 清理残留进程
4. ✅ 删除 PID 文件

---

## 📋 详细说明

### 启动脚本功能

#### Windows (`start-all.bat`)
- ✅ 自动检测项目目录
- ✅ 首次运行自动安装前端依赖
- ✅ 自动编译后端 Java 代码
- ✅ 在独立窗口启动服务（方便查看日志）
- ✅ 彩色输出和进度提示
- ✅ 错误检测和提示

#### Linux/Mac (`start-all.sh`)
- ✅ 自动检测项目目录
- ✅ 首次运行自动安装前端依赖
- ✅ 自动编译后端 Java 代码
- ✅ 后台运行服务
- ✅ 自动创建日志文件
- ✅ 保存进程 ID 便于管理
- ✅ 彩色输出和进度提示
- ✅ 错误检测和提示

### 停止脚本功能

#### Windows (`stop-all.bat`)
- ✅ 通过窗口标题关闭服务窗口
- ✅ 清理残留的 Java 和 Node 进程
- ✅ 安全的批量停止

#### Linux/Mac (`stop-all.sh`)
- ✅ 通过 PID 文件优雅停止进程
- ✅ 强制停止无响应进程
- ✅ 清理残留进程
- ✅ 自动清理 PID 文件

---

## 🌐 访问应用

启动成功后，在浏览器中访问：

**前端页面：**
- 默认地址：http://localhost:8081
- 具体地址请查看前端启动日志

**后端API：**
- 地址：http://localhost:8080
- 健康检查：http://localhost:8080/api/v1/auth/

**查看日志：**
1. 打开浏览器开发者工具（F12）
2. 切换到 Console（控制台）标签
3. 执行操作（登录、注册等）
4. 查看详细的日志输出

---

## ⚠️ 常见问题

### 1. Windows 提示"无法识别的命令"

**原因：** 可能没有在正确的目录运行脚本

**解决：** 确保在项目根目录（`FarmerHappy`）下运行

### 2. 端口已被占用

**现象：**
```
Error: listen EADDRINUSE: address already in use :::8080
```

**解决：**
```bash
# Windows - 查找占用端口的进程
netstat -ano | findstr :8080
taskkill /PID <进程ID> /F

# Linux/Mac - 查找并停止占用端口的进程
lsof -ti:8080 | xargs kill -9
```

### 3. 前端依赖安装失败

**原因：** 网络问题或 npm 配置问题

**解决：**
```bash
cd farmer_happy
npm install --registry=https://registry.npmmirror.com
```

### 4. 后端编译失败

**原因：** 可能是 JDK 版本或编码问题

**解决：**
- 确保安装了 JDK 8 或更高版本
- 确保 `lib/mysql-connector-j-8.3.0.jar` 存在
- 检查文件编码是否为 UTF-8

### 5. Linux/Mac 无法执行脚本

**原因：** 没有执行权限

**解决：**
```bash
chmod +x start-all.sh
chmod +x stop-all.sh
```

### 6. 数据库连接失败

**原因：** MySQL 服务未启动或配置错误

**解决：**
- 确保 MySQL 服务正在运行
- 检查后端代码中的数据库配置
- 查看后端日志了解具体错误

---

## 🎯 最佳实践

### 开发流程

1. **启动服务**
   ```bash
   # Windows
   start-all.bat
   
   # Linux/Mac
   ./start-all.sh
   ```

2. **开发和测试**
   - 修改代码
   - 浏览器自动刷新（前端热重载）
   - 后端需要重新启动才能看到更改

3. **停止服务**
   ```bash
   # Windows
   stop-all.bat
   
   # Linux/Mac
   ./stop-all.sh
   ```

### 日志管理

**Windows：**
- 后端日志：直接在后端窗口查看
- 前端日志：在前端窗口查看，或在浏览器控制台查看

**Linux/Mac：**
```bash
# 实时查看后端日志
tail -f logs/backend.log

# 实时查看前端日志
tail -f logs/frontend.log

# 同时查看两个日志
tail -f logs/*.log
```

### 进程管理

**Linux/Mac：**
```bash
# 查看运行状态
ps aux | grep application    # 后端
ps aux | grep vue-cli-service  # 前端

# 手动停止（如果脚本失败）
kill $(cat logs/backend.pid)
kill $(cat logs/frontend.pid)

# 强制停止
kill -9 $(cat logs/backend.pid)
kill -9 $(cat logs/frontend.pid)
```

---

## 📝 脚本位置

所有启动脚本都放在项目根目录（`FarmerHappy`）下：

```
FarmerHappy/
├── start-all.bat      # Windows 启动脚本
├── stop-all.bat       # Windows 停止脚本
├── start-all.sh       # Linux/Mac 启动脚本
├── stop-all.sh        # Linux/Mac 停止脚本
├── README-SCRIPTS.md  # 本文档
├── farmer_happy/      # 前端项目
└── farmer_happy_backend/  # 后端项目
```

---

## 🎉 快速开始

只需三步即可启动整个项目：

1. **打开终端**，进入项目目录
2. **运行启动脚本**（首次运行会自动安装依赖）
3. **打开浏览器**访问应用

就是这么简单！🚀

---

## 💡 提示

- 首次运行会自动安装前端依赖，可能需要几分钟
- 后端会自动初始化数据库和表
- 前端支持热重载，修改代码会自动刷新
- 浏览器按 F12 打开控制台查看详细日志
- 遇到问题可查看日志文件排查

---

## 📞 支持

如有问题，请查看：
- 控制台输出的错误信息
- 日志文件（Linux/Mac）
- 项目的 README.md 文档

