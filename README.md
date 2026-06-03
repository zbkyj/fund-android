# LanFund Android

基金净值估算Android客户端，基于 [lanZzV/fund](https://github.com/lanZzV/fund) 项目核心功能开发。

## 功能特性

- 基金实时估值查看
- 自选基金添加/删除
- 持仓份额管理
- 下拉刷新数据
- 估算涨幅排序

## 项目结构

```
fund-android/
├── app/
│   ├── src/main/
│   │   ├── java/com/lanfund/app/
│   │   │   ├── data/
│   │   │   │   ├── api/          # API服务
│   │   │   │   ├── model/         # 数据模型
│   │   │   │   └── repository/    # 数据仓库
│   │   │   ├── ui/
│   │   │   │   ├── fund/          # 基金相关UI
│   │   │   │   └── MainActivity.kt
│   │   │   └── LanFundApp.kt
│   │   ├── res/                   # 资源文件
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── .github/workflows/              # GitHub Actions
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## GitHub Actions 编译

项目配置了GitHub Actions自动编译：

- **Push/PR**: 触发 Debug APK 构建
- **Main分支**: 额外构建 Release APK

编译产物 (APK) 会上传到 Actions Artifacts。

### 设置签名密钥 (Release构建)

如需生成签名APK，需要在GitHub仓库设置以下Secrets：

1. `KEYSTORE_FILE` - Base64编码的keystore文件
2. `KEYSTORE_PASSWORD` - keystore密码
3. `KEY_ALIAS` - 密钥别名
4. `KEY_PASSWORD` - 密钥密码

## 本地开发

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 35

### 运行步骤

1. Clone本仓库
2. 用Android Studio打开项目
3. 等待Gradle同步完成
4. 运行/调试应用

## 数据来源

基金数据来自 `fund123.cn` API，遵循其服务条款。

## 免责声明

本应用仅供学习交流，不构成任何投资建议。投资有风险，入市需谨慎。
