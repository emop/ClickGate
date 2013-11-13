冒泡短网址
========

冒泡网短网址跳转服务。支持反爬虫和实时统计功能。主要用于淘宝客推广连接跳转。 本服务运行需要冒泡的API
支持，暂时不对外提供转换长地址的API。但是本服务可以免费使用。具体详情访问 http://www.emop.cn

功能列表
=======
*  通过2次提交表单方式，过滤爬虫。
*  根据访问客户端，自动切换PC或移动版淘客链接。
*  支持多域名归集，在不同的渠道使用不同的域名，使用统一的域名跳转到淘宝。
*  支持API查询访问报表，点击详细。
*  支持根据商品ID实时转换淘客链接，有大量商品时，不必全部转换成功才推广。 *即将发布*
*  导出CSV/Excel格式的访问详细列表，方便做离线分析。 *稍后开发*
*  动态修改PID功能，相同短网址多PID轮询功能。 *稍后开发*
*  下架商品自动切换到其他商品功能。  *稍后开发*

服务稳定性
========
新版冒泡短网址服务，由多台聚石塔，阿里云服务，做负载均衡和故障转移。  
如果有特殊需求，可以建议部署到其他网络服务器，具体联系QQ: 81711368

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
