FROM openjdk:11-jdk-alpine
LABEL maintainer="National Institute of Standards and Technology"

COPY VERSION /

ENV DEBIAN_FRONTEND noninteractive
ARG EXEC_DIR="/opt/executables"
ARG DATA_DIR="/data"

#Create folders
RUN mkdir -p ${EXEC_DIR} \
    && mkdir -p ${DATA_DIR}/inputs \
    && mkdir ${DATA_DIR}/outputs

# Copy wipp-thresholding-plugin JAR
COPY target/wipp-3d-segmentation-plugin*.jar ${EXEC_DIR}/wipp-3d-segmentation-plugin.jar


# Default command. Additional arguments are provided through the command line
ENTRYPOINT ["java",  "-Xmx12G", "-jar", "/opt/executables/wipp-3d-segmentation-plugin.jar"]