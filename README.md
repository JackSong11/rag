>##各层说明
* web: WEB层，处理http请求，响应数据或页面，可以依赖biz和integration
* common: 通用工具层，应用通用常量，通用方法
* dal：持久层，使用MyBatis，mapper接口，数据对象
* integration：外部第三方服务调用，如调用外部钉钉，邮件，SDK等
* processor：回调入口，MQ消费，DTS订阅，Job，工作流等
* client：服务接口，如dubbo对外提供的接口定义，参数定义，数据传输格式定义等
* facade：服务实现，如dubbo的服务实现
* biz-service：领域业务，提供核心领域服务，可允许打包jar后外部引用，与client的区别是，最终领域服务是本地内调用实现，非Remoting实现；
* core-service：领域内部核心服务，提供核心领域服务
* starter：启动层，包装springboot的starter和一些注册服务的配置
>##对象说明
* BO：业务对象，与领域设计的实体对应，在service层使用
* DTO：数据传输对象，dubbo等远程调用接口层（client，facade）入参出参使用
* PO：持久化对象，dal层使用
* VO：值对象，在web层使用，直接对接前端接口

>注：各种对象通常不允许直接在其他层次使用，一般是进行对象转换后使用。各对象严禁跨层使用！