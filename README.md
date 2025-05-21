# 说明: 

## 拉取代码

* 拉取代码以后，在 `IDEA` 中打开，右下角有提示 `load maven script`, 这个可以点击，让 `IDEA` 自动加载 `maven` 依赖。</br>
* 如果错过了，可以在项目的 `pom.xml` 上右键，选择 `Add as maven project`，也可以重新加载依赖。</br>

> **注意：**
> `consumer`、`provider`、`micro-common` 都需要加载依赖。

### 测试代码

* 启动 provider-controller 服务
* 启动 consumer-controller 服务

可以通过 postman 调用 `consumer-controller` 服务的 `hello` 接口，查看是否能正常返回。
```text
# consumer-controller 中的 hello 接口
http://localhost:8088/consumer/sayHello?name=aa
```

根据实现原理，调用 `consumer-controller` 的 `hello` 接口，会被转到调用 `provider-controller` 的 `hello` 接口，
最终返回结果。
```text
# provider-controller 中的 hello 接口
http://localhost:8089/provider/sayHello?name=aa
```

[结构说明](./micro-common/docs/RestClient说明.md)