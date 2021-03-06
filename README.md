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
*  导出CSV/Excel格式的访问详细列表，方便做离线分析。
*  CPC统计下架商品，跳转到错误URL。URL中支持变量,${user_id},${shop_id},${num_iid},${short_key}
*  路由规则，支持根据条件，设置不同的跳转方式，[详情见Wiki](https://github.com/emop/ClickGate/wiki/RouteRule)

数据导出功能
==========
*  http://c.zaol.cn/export/?f=shop_id&v=71596653
*  支持的查询字段：short_key, shop_id, num_iid, user_id(推广者)

服务稳定性
========
新版冒泡短网址服务，由多台聚石塔，阿里云服务，做负载均衡和故障转移。  
如果有特殊需求，可以建议部署到其他网络服务器，具体联系QQ: 81711368

已经部署节点:

```
c.zaol.cn -- 阿里云独立服务器  
c2.zaol.cn -- 聚石塔独立服务器  
click.zaol.cn -- 阿里云和聚石塔的DNS轮询。  
```

如果需要绑定自己的独立域名，在DNS里面设置一个CNAME到上面任意一个域名。让后联系管理员，在后台开通解析。


参数配置
=======

应用中的参数都是通过java的Property配置。 例如: java -Dtaodian.app_key=1, 支持的参数有：

*  http_port -- HTTP服务的端口号，默认：8082
*  taodian.api_id -- 淘点开放平台的，APP ID
*  taodian.api_secret -- 淘点开放平台的，APP SECRET
*  taodian.api_route -- API路由地址，默认：http://api.zaol.cn/api/route
*  log_level -- (debug,info,warn) 默认info
*  max_log_days -- 日志最多保留多少天，默认10.
*  write_log_queue_size -- 写点击统计的队列长度， 默认：1024
*  write_log_thread_count -- 写点击统计的线程数量，默认：10
*  write_access_log -- 是否写访问日志本地文件。 默认:y, 可选值:(y|n)
*  taoke_source_domain -- 淘客访问跳转域名。默认:www.emop.cn
*  old_emop_click -- 同步点击到老的冒泡统计流程。默认：0不开启。可选值(0|1)
*  default_http_index -- 首页跳转地址，直接访问短网址首页时跳转的地址。
*  cache_url_timeout -- 短网址缓存时间，单位:分钟。默认:60
*  in_sae -- 是否在SAE环境中运行。默认n, 可选值(y|n)
*  get_short_url_thread_count -- 查询短网址的线程数量。
*  default_cpc_click_price -- 默认点击价格。(单位：分)
*  export_no_auth -- 数据导出时，不做安全检查。
*  redis.host -- 配置本地redis 数据库地址。
*  host_DOMAIN -- 配置二次提交的域名。 例如:(host_rushui.com=c.rushui.com)

本地编译和运行
===========
* 本地编译前需要先部署SAE本地开发环境

```

mvn install:install-file -DgroupId=com.sina.sae -DartifactId=SAELocal -Dversion=1.0.0 -Dpackaging=jar -Dfile=lib/sae-local-1.0.0.jar -DgeneratePom=true

#mvn assembly:assembly  -- 编译一个完整的Jar包
#java -jar traget\ClickGate-1.0-SNAPSHOT.jar  --运行
```


编译为WAR包，放到SAE中运行
======================
*  在本地创建一个`local_short_url.conf` 配置文件，里面保存需要配置的参数。
*  修改POM.xml 里面的 `<scope>provided</scope>` 注释去掉
*  使用mvn打包一个war文件。

```

#mvn compile war:war  -- 编译一个完整的War包
```


