# nicelimit

## 1.介绍

nicelimit：限流工具，零代码（基于Redisson，稳定！）。

特性

1. 支持接口、前后端一体的静态路径。
2. 支持两种模式：单实例、所有实例（分布式）。
3. 支持URL白名单。
4. 支持动态更新配置（需搭配Nacos或Apollo）。
5. 无需写代码，完全由配置文件控制。
6. 支持自定义限流的返回：状态码、ContentType、提示语

## 2.快速使用

### 2.1 引入依赖
```xml
<dependency>
    <groupId>com.suchtool</groupId>
    <artifactId>nicelimit-spring-boot-starter</artifactId>
    <version>{newest-version}</version>
</dependency>
```

### 2.2 配置Redis

添加Redisson相关配置，比如：
```
spring:
  redis:
    host: 127.0.0.1
    port: 6379
    password: 222333
```
### 2.3 配置nicelimit

目标：
1. /aa/bb接口，10秒内只能有5个请求 
2. /aa/cc接口，5秒内只能有10个请求 
3. 禁止访问/aa/dd接口、/aa/ee接口

配置如下：

```
suchtool:
  nicelimit:
    inject: true
    enabled: true
    forbid-url:
      - /aa/dd
      - /aa/ee
    detail:
      -
        url: /aa/bb
        rate-type: OVERALL
        rate-interval: 10s
        rate: 5
      -
        url: /aa/cc
        rate-type: OVERALL
        rate-interval: 5s
        rate: 10
```

### 4.配置大全

支持yml等配置方式。

| 配置                  | 描述                                       | 默认值                           |
|-----------------------|------------------------------------------|----------------------------------|
| suchtool.nicelimit.inject                | 是否注入（是否注入容器）            | true      |
| suchtool.nicelimit.enabled               | 是否启用（inject为true时，才有效）  | true      |
| suchtool.nicelimit.debug               | 是否启用调试模式          | false               |
| suchtool.nicelimit.limited-status-code   | 被限流的状态码              | 429             |
| suchtool.nicelimit.limited-content-type  | 被限流的内容类型            | text/plain;charset=UTF-8         |
| suchtool.nicelimit.limited-message       | 被限流的提示信息            | 哎呀，访问量好大，请稍后再试试吧~  |
| suchtool.nicelimit.forbid-url       | 禁止访问的URL                    | null  |
| suchtool.nicelimit.config-key            | 配置的key                   | niceLimit:config                 |
| suchtool.nicelimit.update-lock-key       | 更新时用的锁的key（异步加锁，不影响业务性能）| niceLimit:update-lock   |
| suchtool.nicelimit.limiter-key-prefix    | 限流器的key前缀              | niceLimit:limiter               |
| suchtool.nicelimit.filter                | 过滤器配置       | null |
| suchtool.nicelimit.detail        | 详情（详细的限流配置）  | null  |

suchtool.nicelimit.filter的配置：

| filter-pattern        | 过滤器匹配模式（支持通配符） | ["/*"]                             |
| filter-name           | 过滤器名字                   | niceLimitFilter                  |
| filter-order          | 过滤器顺序                   | null                             |

suchtool.nicelimit.detail配置：

| 配置           | 描述                   | 举例       |
|----------------|------------------------|------------|
| url            | URL（不支持通配符，为了极致的效率） | /aa/bb |
| rate-type      | 速度类型：OVERALL（全实例），PER_CLIENT（单实例） | 略   |
| rate-interval  | 速度间隔（单位时间）   | 10s |
| rate           | 速度（数量）           | 5   |
| limited-status-code   | 被限流的状态码      | null  |
| limited-content-type  | 被限流的内容类型    | null  |
| limited-message       | 被限流的提示信息    | null  |

如果detail里的limited-status-code、limited-content-type、limited-message没配置，则取顶层（suchtool.nicelimit.xxx）的配置。