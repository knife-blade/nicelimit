# nicelimit

## 1.介绍

nicelimit：限流工具，零代码（基于Redisson，稳定！）。

特性

1. 支持接口、前后端一体的静态路径。
2. 支持两种模式：单实例、所有实例（分布式）。
3. 支持URL白名单。
4. 搭配Nacos或Apollo，可实现动态更新配置。
5. 无需写代码，完全由配置文件控制。

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

待补充

### 3.示例大全
使用场景示例

1. 10秒内只能有3个请求。
2. 禁止访问某个接口。

### 4.配置大全

支持yml等配置方式。

| 配置                             | 描述               | 默认值       |
|--------------------------------|------------------|-----------|
| suchtool.nicelimit.storage-type | 存储方式。可选值：local、redis | local（本地） |
| suchtool.nicelimit.key-prefix   | 缓存的key的前缀        | nicelimit  |

### 5.注意事项
待填充