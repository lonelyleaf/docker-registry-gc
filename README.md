# docker-registry-gc

[![Build Status](https://travis-ci.com/lonelyleaf/docker-registry-gc.svg?branch=master)](https://travis-ci.com/lonelyleaf/docker-registry-gc)

[中文文档](README-zh.md)

A simple application can **clean up old and unuesd docker images**
on registry.Developed by kotlin and gradle.

## Quick start

Write a `application.yml` config file first.
```yaml
docker.gc.registry:
  - baseUrl: https://your-private.registry.location
    #optinal,username and password for docker login    
    user: your-username
    pass: yourpass
    #when to run task    
    scheduler:
      #execute periodically,with ISO-8601 duration format.
      fix: PT1H #every 1 hour
      #fix: P3D #every 3 days
      #also support cron expression.but not use with fix.
      cron: 0 0 1 * * ? # 1AM everyday
    cleanup:
      #define which image to match      
      - image: "^.*gmt.*$" #regexp of image name 
        tag: ".*test.*"    #regexp of tag
        durationToKeep: P90D #image which is older than this will be deleted.also ISO-8601 duration format.
```

Then run `docker-registry-gc` by docker:
> docker run  -v {your_location}:/application.yaml:/app/config/application.yaml lonelyleaf/docker-registry-gc

## How to config
This is a stander spring boot application,so all config method in [Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html) 
are supported.But yaml is recommend,so we will only talk about how config by yaml.

A complete config file demo is given above.Under `docker.gc.registry`,you could write multi docker registry endpoint
to clean up.An every registry may have multi matcher under `cleanup`,to define which image to be deleted. 

Pay attention that The **duration** fields(fix and durationToKeep) in config use `ISO-8601` duration format.
There are some example:
- `PT30H` 30 hour
- `P90D` 90 days
- `P1DT4H` 1 day and 4 hour

## Run on kubernetes

In `k8s` there is a `docker-registry-gc.yaml` you may use in k8s, edit the ConfigMap before
you deploy.