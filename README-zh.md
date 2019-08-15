# docker-registry-gc

[![Build Status](https://travis-ci.com/lonelyleaf/docker-registry-gc.svg?branch=master)](https://travis-ci.com/lonelyleaf/docker-registry-gc)

一个简单的，用于删除docker厂库无用镜像的服务。


## 快速开始

先写一个`application.yml配置文件.
```yaml
docker.gc.registry:
  - baseUrl: https://your-private.registry.location
    #用于docker登陆的用户名密码，可选  
    user: your-username
    pass: yourpass
    #定义任务何时执行
    scheduler:
      #周期性执行,周期格式为ISO-8601 duration.
      fix: PT1H #每1小时
      #fix: P3D #每3天
      #使用cron表达式，与fix字段不兼容
      cron: 0 0 1 * * ? # 每天早上1点
    cleanup:
      #定义如何匹配镜像    
      - image: "^.*gmt.*$" #镜像名的正则表达式
        tag: ".*test.*"    #镜像tag的正则表达式
        durationToKeep: P90D #镜像该保留多少天，也是ISO-8601 duration格式.
```

然后使用dockers启动`docker-registry-gc`：
> docker run  -v {your_location}:/application.yaml:/app/config/application.yaml lonelyleaf/docker-registry-gc:0.1

## 如何配置
该项目是标准的spring boot 项目，可以使用所有[Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html) 
中的配置方式。但推荐使用yaml来配置，下面也只会讨论yaml的配置方式。

一份完整的参考配置可以参考上面给出的例子。在 `docker.gc.registry`下，可以配置多个需要清理的docker仓库的端点，
每个仓库端点在`cleanup`下可以有多个镜像匹配规则，来匹配需要清理的镜像。

注意配置中使用的**周期**字段（包含fix 和 durationToKeep）都是使用的`ISO-8601`中的周期格式，下面有几个
例子：
- `PT30H` 30小时
- `P90D` 90天
- `P1DT4H` 1天又4个小时

## 在kubernetes上部署

在`k8s`文件夹下有`docker-registry-gc.yaml`文件，可以用于在k8s上部署，部署前请先根据自己情况编辑ConfigMap中
的字段