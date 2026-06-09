# 快绘AI (KuaiHuiAI)

📱 本地 Stable Diffusion AI 图像生成应用 - Android

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Android](https://img.shields.io/badge/Android-8.0%2B-green.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg)

## ✨ 功能特点

- 🎨 **文生图** - 输入提示词生成精美图像
- 🖼️ **图生图** - 基于参考图进行风格转换
- 🖌️ **局部重绘** - 智能修改图像局部区域
- ⚡ **NPU 加速** - 支持高通芯片 AI 加速
- 🔄 **CPU 回退** - 无 NPU 设备自动切换
- 📦 **模型市场** - 一键下载各种风格模型
- 🎭 **多种风格** - 动漫、写实、SDXL 等

## 📋 系统要求

- Android 8.0 (API 26) 或更高版本
- 推荐: 高通骁龙 865 / 870 / 888 / 8 Gen 系列
- 最低: 6GB RAM, 10GB 存储空间

## 🚀 快速开始

### 方式一: 安装 APK

1. 下载最新 APK
2. 允许安装未知来源应用
3. 安装并打开应用
4. 在模型市场下载喜欢的模型
5. 开始创作!

### 方式二: 从源码构建

```bash
# 克隆项目
git clone https://github.com/your-repo/kuaihuiai.git
cd kuaihuiai

# 构建 Debug APK
./gradlew assembleDebug

# APK 输出位置: app/build/outputs/apk/debug/app-debug.apk
```

### 方式三: Android Studio

1. File → Open → 选择项目文件夹
2. 等待 Gradle 同步完成
3. Run → Run 'app'

## 🎯 使用指南

### 文生图

1. 点击主界面「生成」按钮
2. 输入正向提示词 (英文效果更好)
3. 可选: 输入反向提示词排除不需要的元素
4. 选择模型和分辨率
5. 点击生成按钮

### 模型管理

- **动漫风格**: Anything V5, QTeaMix, CuteYukiMix
- **写实风格**: AbsoluteReality, ChilloutMix
- **高清模式**: SDXL 1.0 Base + Refiner

## 🛠️ 技术架构

```
┌─────────────────────────────────────────────────┐
│                  Android App                     │
├─────────────────────────────────────────────────┤
│  UI Layer (Jetpack Compose + Material3)         │
├─────────────────────────────────────────────────┤
│  Business Logic (Kotlin Coroutines + Flow)      │
├─────────────────────────────────────────────────┤
│  Data Layer (Repository Pattern)                │
├─────────────────────────────────────────────────┤
│  Service Layer (Foreground Service)             │
├─────────────────────────────────────────────────┤
│  Inference Engine (MNN / ONNX Runtime)          │
├─────────────────────────────────────────────────┤
│  Native Layer (C++ JNI)                         │
└─────────────────────────────────────────────────┘
```

## 📁 项目结构

```
app/
├── src/main/
│   ├── java/comkuaihuiai/
│   │   ├── data/           # 数据层
│   │   │   ├── model/      # 数据模型
│   │   │   └── repository/ # 仓库类
│   │   ├── service/        # 服务层
│   │   │   ├── inference/  # 推理引擎
│   │   │   └── download/   # 下载服务
│   │   ├── ui/             # UI层
│   │   │   ├── screens/    # 页面
│   │   │   ├── components/# 组件
│   │   │   └── theme/     # 主题
│   │   └── utils/         # 工具类
│   ├── cpp/                # C++ 原生代码
│   │   ├── localdream/     # Local Dream 核心
│   │   └── native/         # JNI 桥接
│   └── res/                # 资源文件
└── build.gradle
```

## 🔧 配置说明

### 模型下载源

默认使用 Hugging Face:
```
https://huggingface.co/xororz/sd-qnn/resolve/main
```

可在设置中配置自定义模型源。

### NPU 配置

应用会自动检测设备是否支持 NPU:
- **支持**: 使用 QNN (Qualcomm Neural Network)
- **不支持**: 自动切换到 ONNX Runtime (CPU)

## 📄 许可证

本项目基于 MIT 许可证开源。

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request!

## 🙏 致谢

- [Local Dream](https://github.com/xororz/LocalDream) - 核心架构参考
- [Stable Diffusion](https://github.com/CompVis/stable-diffusion) - 生成模型
- [MNN](https://github.com/alibaba/MNN) - 深度学习推理引擎

## 📧 联系

如有问题或建议，欢迎提交 Issue。
