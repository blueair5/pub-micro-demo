我们引入了两个注解 `RestClientMapping` 和 `RestClientReference`, 这两个注解的周期如下：

```mermaid
sequenceDiagram
    participant Bean
    autonumber

    Bean ->> Bean: Bean 初始化，利用 BeanPostProcessor <br/> 在 Bean 初始化的时候，<br/> 判断 Bean 的 Field 被 RestClientReference 注解标记的

    Bean ->> Bean: 找到被 RestClientReference 标记的接口，然后<br/> 对这个接口的所有方法，判断是否被 RestClientMapping 标记<br/>，设置动态代理。

    Bean ->> Bean: 动态代理中，处理从两个注解中得到的 path 信息，拼成一个 URL<br/> 发起远程调用

```

从上面的情况，我们就可以看到，假设服务 server-provider 是接口提供方，那么服务 server-consumer 调用方需要引入 server-provider 的 `API`。然后 server-provider 需要在 `API`  中指定 `RestClientMapping` 。
`RestClientMapping` 中应该写上自己提供的服务的 URL。(也就是说需要提供一个 Controller 出来)。

服务 server-consumer 需要在 `API` 中引入 server-provider 的接口，并且在这个接口上标记 `RestClientReference` 注解。这样就可以完成远程调用了。

server-provider 和 server-consumer 的调用示例图如下:

```mermaid
sequenceDiagram
    autonumber
    participant server-consumer
    participant micro-common
    participant server-provider
    autonumber
    %% serverB 发起一个服务调用
    server-consumer ->> micro-common: bean.method 调用 server-provider 的方法, <br/> micro-common 代理 server-provider 的方法
    micro-common ->> server-provider: restTemplate 调用 server-provider 的 controller 服务
    server-provider -->> micro-common: 返回结果
    micro-common ->> server-consumer: 返回结果
```

服务 server-provider 和 服务 server-consumer 的文件结构:

- **server-provider**:
    - `server-provider-controller`
    - `server-provider-service`
    - `server-provider-api`

- **server-consumer**:
    - `server-consumer-controller`
    

