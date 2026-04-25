# 使用轻量级 JDK 17（Alpine，仅 ~150MB）
FROM eclipse-temurin:17-jre-alpine

# 设置时区（避免日志时间错乱）
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 创建工作目录
WORKDIR /app

# 复制 Maven 构建产物（JAR 包）
# 注意：Maven 默认打包名是 {artifactId}-{version}.jar
# 如果你改过 <finalName>，请对应修改下面的文件名
COPY target/CareerGraph-*.jar app.jar

# 暴露 Render 要求的端口（必须 10000）
EXPOSE 10000

# 启动命令：优先使用环境变量 PORT（Render 会注入）
CMD ["java", "-Dserver.port=${PORT:-8080}", "-jar", "app.jar"]