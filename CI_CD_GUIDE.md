# CI/CD 自动化构建配置指南

## 概述

本项目配置了 GitHub Actions CI/CD 流水线，支持自动化构建 Android APK。

## 工作流程

### 触发条件

1. **推送代码到主分支**
   - `main`, `master`, `develop`, `feature/*`

2. **创建标签**
   - 格式：`v*.*.*` (如 `v1.0.0`, `v2.1.3`)

3. **创建 Pull Request**
   - 目标分支：`main`, `master`, `develop`

### 构建流程

#### 1. 代码检出
```yaml
- uses: actions/checkout@v4
```

#### 2. 配置 JDK 环境
```yaml
- uses: actions/setup-java@v4
  with:
    java-version: '11'
    distribution: 'temurin'
```

#### 3. 缓存 Gradle 依赖
```yaml
- uses: actions/cache@v3
  with:
    path: |
      ~/.gradle/caches
      ~/.gradle/wrapper
```

#### 4. 构建 APK
- Debug 版本：`./gradlew assembleDebug`
- Release 版本：`./gradlew assembleRelease`

#### 5. 运行单元测试
```yaml
- run: ./gradlew test --stacktrace
```

#### 6. 上传构建产物
- Debug APK
- Release APK

## 使用方法

### 方式一：自动构建（推荐）

1. **推送代码触发构建**
```bash
git add .
git commit -m "feat: 新功能"
git push origin feature/asr-with-service-protocol
```

2. **查看构建状态**
- 访问 GitHub 仓库的 "Actions" 标签
- 查看正在运行的构建任务

3. **下载构建产物**
- 构建完成后，在构建页面下载 APK 文件
- 或从 Actions Artifacts 中下载

### 方式二：标签发布

1. **创建版本标签**
```bash
# 创建标签
git tag v1.0.0

# 推送标签到远程仓库
git push origin v1.0.0
```

2. **自动触发 Release 构建**
- 推送标签后会自动触发 Release 构建
- APK 会自动发布到 GitHub Releases 页面

3. **下载 Release APK**
- 访问 GitHub 仓库的 "Releases" 页面
- 下载对应版本的 APK 文件

### 方式三：Pull Request 构建

1. **创建 Pull Request**
- 在 GitHub 上创建 PR
- 目标分支：`main`, `master`, `develop`

2. **自动构建**
- PR 创建后自动触发构建
- 构建结果会显示在 PR 页面

3. **合并检查**
- 确保构建通过后再合并

## 构建产物位置

### Debug APK
- **位置**：`app/build/outputs/apk/debug/app-debug.apk`
- **用途**：开发测试
- **签名**：使用 debug 签名

### Release APK
- **位置**：`app/build/outputs/apk/release/app-release-unsigned.apk`
- **用途**：正式发布
- **签名**：未签名（需要手动签名后使用）

## 配置文件说明

### GitHub Actions 配置
**文件**：`.github/workflows/android.yml`

**主要配置**：
- 构建环境：Ubuntu Latest
- JDK 版本：11
- Gradle 版本：7.4.2
- 构建类型：Debug & Release

## 本地构建

如果需要在本地构建 APK：

### 前提条件
- 安装 JDK 11 或更高版本
- 下载 Android SDK
- 配置环境变量

### 构建命令
```bash
# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease

# 运行测试
./gradlew test
```

## 签名 Release APK

Release APK 构建后需要签名才能安装：

### 1. 生成签名密钥
```bash
keytool -genkey -v -keystore release.keystore \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias release
```

### 2. 签名 APK
```bash
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 \
  -keystore release.keystore \
  app-release-unsigned.apk release
```

### 3. 对齐 APK（优化）
```bash
zipalign -v 4 app-release-unsigned.apk app-release.apk
```

## 环境变量配置

### GitHub Secrets（可选）

如果需要配置签名信息，可以在 GitHub 仓库设置中添加 Secrets：

- `KEYSTORE_FILE`: 签名密钥文件
- `KEYSTORE_PASSWORD`: 密钥库密码
- `KEY_ALIAS`: 密钥别名
- `KEY_PASSWORD`: 密钥密码

### 修改工作流使用签名

在工作流文件中添加签名步骤：

```yaml
- name: Sign APK
  run: |
    echo "${{ secrets.KEYSTORE_FILE }}" | base64 --decode > keystore.jks
    jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 \
      -keystore keystore.jks \
      -storepass "${{ secrets.KEYSTORE_PASSWORD }}" \
      -keypass "${{ secrets.KEY_PASSWORD }}" \
      app/build/outputs/apk/release/app-release-unsigned.apk \
      "${{ secrets.KEY_ALIAS }}"
```

## 故障排查

### 构建失败

1. **检查构建日志**
   - 在 GitHub Actions 页面查看详细日志

2. **常见问题**
   - 依赖下载失败：检查网络连接
   - Gradle 版本不兼容：检查 `gradle-wrapper.properties`
   - JDK 版本问题：确认 JDK 11 已安装

3. **重新构建**
   - 提交空触发器重新运行构建
   - 或在 Actions 页面手动触发重新运行

### 依赖缓存问题

如果遇到缓存问题，可以清除缓存：

1. 访问仓库的 Settings > Actions > Caches
2. 删除相关缓存
3. 重新触发构建

## 性能优化

### 缓存优化

当前配置已启用 Gradle 缓存：
- 缓存 `~/.gradle/caches`
- 缓存 `~/.gradle/wrapper`
- 使用构建文件内容作为缓存键

### 构建时间优化

- 首次构建较慢（下载依赖）
- 后续构建利用缓存会更快
- 使用增量编译加速构建

## 最佳实践

1. **代码提交规范**
   - 遵循语义化版本号
   - 使用清晰的提交信息
   - 及时处理构建失败

2. **版本管理**
   - 使用标签标记版本
   - 维护清晰的版本历史
   - 为每个版本编写 Release Notes

3. **持续集成**
   - 每次提交都运行测试
   - 确保 PR 构建通过
   - 定期更新依赖

## 相关文档

- [GitHub Actions 官方文档](https://docs.github.com/en/actions)
- [Gradle 构建指南](BUILD_GUIDE.md)
- [Android 官方文档](https://developer.android.com)

## 支持

如遇到问题，请：
1. 查看 GitHub Actions 构建日志
2. 检查本文档的故障排查部分
3. 提交 Issue 寻求帮助
