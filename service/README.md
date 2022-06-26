# Fintan Service

An API designed to be used for running Fintan pipelines.
The recommended way to use it is via a Docker container. To build it, use
```cd api/
docker build -t fintan-api .
docker run -d -p 8080:8080 --name fintan-api fintan-api```

The Swagger UI should be available on http://localhost:8080
