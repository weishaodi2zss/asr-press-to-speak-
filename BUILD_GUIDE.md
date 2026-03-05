# APK构建指南

## 前提条件

- 已安装 Android Studio (推荐最新版本)
- 已安装 Android SDK
- 已配置 Java JDK (JDK 8 或更高版本)

## 构建步骤

### 方法一：使用Android Studio构建

1. **打开项目**
   ```
   打开Android Studio -> File -> Open -> 选择项目目录
   ```

2. **等待Gradle同步**
   - Android Studio会自动下载依赖和同步项目
   - 确保网络连接正常，首次同步可能需要较长时间

3. **选择Build Variant**
   ```
   View -> Tool Windows -> Build Variants
   选择 "debug" 变体
   ```

4. **构建APK**
   ```
   Build -> Build Bundle(s) / APK(s) -> Build APK(s)
   ```

5. **获取APK文件**
   ```
   构建完成后，会弹出一个通知，点击"locate"
   APK文件位置：app/build/outputs/apk/debug/app-debug.apk
   ```

### 方法二：使用命令行构建

1. **进入项目目录**
   ```bash
   cd /workspace/ie7nygw5ce717rji4cl8x
   ```

2. **构建Debug APK**
   ```bash
   # Linux/macOS
   ./gradlew assembleDebug

   # Windows
   gradlew.bat assembleDebug
   ```

3. **获取APK文件**
   ```
   APK文件位置：app/build/outputs/apk/debug/app-debug.apk
   ```

### 方法三：构建Release APK（带签名）

1. **生成签名密钥**
   ```bash
   keytool -genkey -v -keystore your-keystore.jks \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -alias your-alias
   ```

2. **配置签名**
   在 `app/build.gradle` 中添加：

   ```gradle
   android {
       signingConfigs {
           release {
               storeFile file("your-keystore.jks")
               storePassword "your-store-password"
               keyAlias "your-alias"
               keyPassword "your-key-password"
           }
       }

       buildTypes {
           release {
               signingConfig signingConfigs.release
               minifyEnabled true
               proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
           }
       }
   }
   ```

3. **构建Release APK**
   ```bash
   ./gradlew assembleRelease
   ```

4. **获取APK文件**
   ```
   APK文件位置：app/build/outputs/apk/release/app-release.apk
   ```

## 常见问题

### 1. Gradle同步失败
- 检查网络连接
- 尝试使用VPN或镜像源
- 清理Gradle缓存：`./gradlew clean`

### 2. 依赖下载失败
- 配置Maven镜像源（如阿里云镜像）
- 在 `build.gradle` 中添加：
  ```gradle
  repositories {
      maven { url 'https://maven.aliyun.com/repository/google' }
      maven { url 'https://maven.aliyun.com/repository/public' }
      google()
      mavenCentral()
  }
  ```

### 3. 构建失败
- 检查Java版本：`java -version`
- 检查Android SDK版本
- 查看详细的错误日志

## APK文件说明

- **Debug APK**：用于开发和测试
  - 未签名（或使用debug签名）
  - 包含调试信息
  - 文件较大

- **Release APK**：用于发布
  - 需要签名
  - 代码混淆优化
  - 文件较小

## 安装APK

### 方法一：USB连接安装
```bash
adb install app-debug.apk
```

### 方法二：通过文件管理器安装
1. 将APK文件传输到手机
2. 使用文件管理器打开APK文件
3. 点击"安装"

### 方法三：网页下载安装
1. 将APK文件上传到服务器
2. 在手机浏览器中访问下载链接
3. 点击安装

## 项目依赖

本项目依赖以下库：
- AndroidX Core KTX
- Material Components
- ConstraintLayout
- Kotlin Coroutines
- Java-WebSocket
- Gson
- OkHttp

所有依赖会在Gradle同步时自动下载。

## 注意事项

1. **首次构建**可能需要较长时间，因为需要下载依赖
2. **保持网络连接**，确保依赖能够正常下载
3. **磁盘空间**：确保有足够的磁盘空间（至少2GB）
4. **权限**：确保有读写权限

## 支持信息

如遇到构建问题，请提供：
- Android Studio版本
- 操作系统版本
- 完整的错误日志
- Gradle版本
