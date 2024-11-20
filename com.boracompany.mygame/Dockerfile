FROM debian:11.11
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libxrandr2 \
    libxinerama1 \
    libxcursor1 \
    libfreetype6 \
    fontconfig \
    fonts-dejavu \
    && rm -rf /var/lib/apt/lists/*

# Enable anti-aliasing and subpixel rendering
RUN mkdir -p /etc/fonts/conf.d/ && \
    ln -sf /etc/fonts/conf.avail/10-antialias.conf /etc/fonts/conf.d/ && \
    ln -sf /etc/fonts/conf.avail/10-sub-pixel-rgb.conf /etc/fonts/conf.d/

# Set DPI and scaling for high-resolution displays
ENV GDK_SCALE=1
ENV GDK_DPI_SCALE=1.0
ENV QT_SCALE_FACTOR=1
# Set the working directory
WORKDIR /app

# Copy the application JAR file into the container
COPY ./target/mygame-*-jar-with-dependencies.jar mygame.jar

# Run the application with improved visuals
CMD ["java", "-Dawt.useSystemAAFontSettings=on", "-Dswing.aatext=true", "-jar", "mygame.jar", "--dburl", "${DB_URL}", "--dbuser", "${DB_USER}", "--dbpassword", "${DB_PASSWORD}"]
