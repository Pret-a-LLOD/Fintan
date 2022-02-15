# ------------------------ Step 1: Fintan Backend --------------------
#FROM openjdk:11-jdk-slim-buster AS fintan-builder
FROM maven:3.8.2-jdk-11-slim AS fintan-builder
WORKDIR /fintan

# required for some of the packages since man pages are cut from the slim debian image
RUN mkdir -p /usr/share/man/man1
RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
    # openjfx \
    # maven \
  && apt-get clean && rm -rf /var/lib/apt/lists/*

# Downloading Fintan release and removing all things Git
RUN git clone --depth 1 https://github.com/acoli-repo/fintan-backend.git \
  && cd fintan-backend \
  && chmod +x build.sh run.sh \
  && ./build.sh && rm -rf */.git \
  && cd ..

# since we have git on this step, let's download the service as well
RUN git clone --depth 1 https://github.com/acoli-repo/fintan-service.git \
  && rm -rf fintan-service/.git

# ------------------------ Step 2: Fintan Service --------------------

FROM python:3.7.10-slim-buster AS api-builder
WORKDIR /api-build

COPY --from=fintan-builder /fintan/fintan-service/api/requirements.txt /api-build

# set environment variables
ENV PYTHONDONTWRITEBYTECODE 1
ENV PYTHONUNBUFFERED 1

# install system dependencies
#RUN apt-get update && \
#    apt-get install -y --no-install-recommends gcc
RUN pip install --upgrade pip
RUN pip wheel --no-cache-dir --no-deps --wheel-dir ./wheels -r requirements.txt

# ------------------------ Step 3: Fintan API ------------------------

# We don't need maven or JDK anymore since we have a prebuild JAR
FROM openjdk:11-jre-slim-buster AS fintan-api
WORKDIR /fintan

# create the app user
RUN addgroup --system fintan && adduser --system --group fintan

RUN apt-get update && \
    apt-get install -y --no-install-recommends python3 python3-pip \
 && apt-get clean && rm -rf /var/lib/apt/lists/*

ENV FINTAN_PIPELINES=/fintan/pipelines/
ENV FINTAN_DATA=/fintan/data/
ENV FINTAN_PATH=.
# a hack to optionally copy the data directory
COPY ./*.json /fintan/pipelines/
COPY ./data /fintan/data

COPY --from=fintan-builder /fintan/fintan-backend/target/fintan-backend-*.jar /fintan/fintan-backend.jar
COPY --from=fintan-builder /root/.m2 /home/fintan/.m2/
COPY --from=fintan-builder /fintan/fintan-service/api/requirements.txt /fintan/fintan-service/api/api.py /fintan/fintan-service/api/openapi.yaml /fintan/
COPY --from=api-builder /api-build/wheels /wheels

RUN pip3 install --no-cache /wheels/*

# chown all the files to the app user
RUN chown -R fintan:fintan /fintan* /home/fintan/.m2

# change to the app user
USER fintan

EXPOSE 8080
ENTRYPOINT ["gunicorn", "--bind", "0.0.0.0:8080", "api:app", "--log-level=info", "--workers=4", "--log-file=/fintan/error.log", "--timeout=1200"]

# QUESTIONS:
# 1. do we need openjfx?
# 2. [SOLVED] how do I copy built packages to the next stage?