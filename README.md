ClickGate
=========

冒泡网短网址跳转服务。支持反爬虫和实时统计功能。主要用于淘宝客推广连接跳转。

参数配置
=======

应用中的参数都是通过java的Property配置。 例如: java -Dtaodian.app_key=1, 支持的参数有：

*  http_port -- HTTP服务的端口号，默认：8082
*  taodian.api_id -- 淘点开放平台的，APP ID
*  taodian.api_secret -- 淘点开放平台的，APP SECRET
*  taodian.api_route -- API路由地址，默认：http://api.zaol.cn/api/route
*  log_level -- (debug,info,warn) 默认info
*  max_log_days -- 日志最多保留多少天，默认10.


本地编译和运行
===========

```

#mvn mvn assembly:assembly  -- 编译一个完整的Jar包
#java -jar traget\ClickGate-1.0-SNAPSHOT.jar  --运行
```
