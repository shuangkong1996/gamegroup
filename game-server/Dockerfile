# 使用eclipse-temurin:21-jdk作为基础镜像
FROM eclipse-temurin:21-jdk

# 设置工作目录
WORKDIR /app

# 创建非root用户运行应用
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 复制jar文件到容器中
COPY game-server-1.0.0-SNAPSHOT.jar app.jar

# 修改文件所有者
RUN chown appuser:appuser app.jar

# 切换到非root用户
USER appuser

# 暴露端口
EXPOSE 8080

# 设置JVM参数（可选，可根据需要调整）
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

