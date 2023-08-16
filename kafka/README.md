分布式系统中，Kafka 的多个 Client（Consumer 或 Producer） 或 Broker 运行在不同的服务器、容器？上，本地测试时可以用线程代替。
  
## Introduction
Kafka 基于事件（Event）概念。  
![](./Kafka%20Architecture%201.png)  
![](./Kafka%20Architecture%202.webp)  
  
## Topic
每个 Event 都归属于某个 Topic（同类 Event 的容器，就如关系数据库的 Table 概念），单个 Event 也可以同时归属于多个 Topic。  
每个 Topic 都是 Event 的 durable Log（存放在硬盘上的日志文件）而不是队列（Queue，因此不同于一般消息队列软件），这也是为什么 Kafka Queue 这种称呼不准确。Log 是一种简单的数据结构，只能 append 不能中间插入，读取时只能通过 offset 查找而不是 index，而且 Event Log 是 immutable（因为生活中一个事件发生了就不可改变），Log 也使得更好的吞吐性能成为了可能也使得 Message 过期设置（比如 2 周或永不过期）成为可能。  
  
## Partitioning
由于经济、系统设计的原因（比如单个 Topic 数据量太大，但是企业想省钱继而希望使用多个小节点而不是一个大节点），Partitioning - 把单个 Topic 的 Log 分割成多个 Log（Partition），它们可以被分布分配在 Kafka 集群的一个个独立的节点上。当新消息 Message 来临时，应该写入哪个 Partition 有多种算法选择，比如 Round Robin（消息没 Key）、哈希后对 Partition 总数取余（消息有 Key）等等，这些方法总是能保证单个 Partition 上该 Topic 的 Message 总是按写入顺序排序的（单个 Partition 上不一定顺序紧密相连因为中间的可能分布到其他 Partition，但是单个 Partition 上的总是单调顺序的比如 1、3、5）。  
每个主题（Topic）的消息有不同的分区，这样一方面消息的存储就不会受到单一服务器存储空间大小的限制，另一方面消息的处理也可以在多个服务器上并行。  

ProducerRecord 对象包含了目标主题、键和值。Kafka 的消息是一个个键值对，ProducerRecord 对象可以只包含目标主题和值，键可以设置为默认的 null，不过大多数应用程序会用到键。键有两个用途：可以作为消息的附加信息，也可以用来决定消息该被写到主题的哪个分区。拥有相同键的消息将被写到同一个分区。也就是说，如果一个进程只从一个主题的分区读取数据，那么具有相同键的所有记录都会被该进程读取。  
如果键值为 null，并且使用了默认的分区器，那么记录将被随机地发送到主题内各个可用的分区上。分区器使用轮询（Round Robin）算法将消息均衡地分布到各个分区上。
如果键不为空，并且使用了默认的分区器，那么 Kafka 会对键进行散列，然后根据散列值把消息映射到特定的分区上。这里的关键之处在于，同一个键总是被映射到同一个分区上。  
只有在不改变主题分区数量的情况下，键与分区之间的映射才能保持不变。一旦主题增加了新的分区，这些就无法保证了 —— 旧数据仍然留在原分区，但新的记录可能被写到其他分区上。如果要使用键来映射分区，那么最好在创建主题的时候就把分区规划好，而且永远不要增加新分区。  
Kafka 除了提供了默认分区器，使用者也可以实现自定义分区策略（自己写类 implements Partitioner）。  
  
## Brokers
Broker 即一个运行 Kafka broker 进程的机器 - 可以是服务器、计算机、实例或虚拟化容器。  
每个 Broker 可能托管了一个或多个 Partition（以及 Partition replication 的管理），并处理写入/读取的请求。  
Broker 设计简单所以容易 scale。  

Kafka 权威指南  
> broker 和集群  
> 一个独立的 Kafka 服务器被称为 broker。broker 接收来自生产者的消息，为消息设置偏移量，并提交消息到磁盘保存。broker 为消费者提供服务，对读取分区的请求作出响应，返回已经提交到磁盘上的消息。根据特定的硬件及其性能特征，单个 broker 可以轻松处理数千个分区以及每秒百万级的消息量。  
> broker 是集群的组成部分。每个集群都有一个 broker 同时充当了集群控制器的角色（自动从集群的活跃成员中选举出来）。控制器负责管理工作，包括将分区分配给 broker 和监控 broker。在集群中，一个分区从属于一个 broker，该 broker 被称为分区的首领。一个分区可以分配给多个 broker，这个时候会发生分区复制。这种复制机制为分区提供了消息冗余，如果有一个 broker 失效，其他 broker 可以接管领导权。不过，相关的消费者和生产者都要重新连接到新的首领。
  
## Replication
原来是一个 Partition 总是从属于某个 Broker，为了容错该 Broker 的故障可能，就把该 Partition 复制、备份到另外几个 Broker 上，称之为 follower replicas（与数据库 replicas 类似），原 Partition 为 leader replica（负责写入与读取），通常备份会自动进行，如果 leader replica 故障了则会有一个原 follower replica 自动取而代之。  
  
## Producer
即写入操作，通过轻量级的 Kafka Producer 库就可以进行（使用前进行正确配置即可，包括链接 Kafka 集群上的某几个 broker、安全设置、网络行为等等）。  
比如在 Java 里，ProducerRecord 即是开发者、程序准备写入的 Key-Value pair（亦即 Event）（写入还包括放置在哪个 Topic）。另外 Partitioning 由 Producer 负责。  
  
## Consumer
应用程序使用 KafkaConsumer 向 Kafka 订阅主题，并从订阅的主题上接收消息。  

即读取操作，通过 Kafka Consumer 库就可以进行（如 Kafka Producer 一样使用前进行正确配置即可）。  
然后让该 Kafka Consumer 对象 subscribe（订阅）Topic 即可（然后每次调用对象的 poll 方法得到、返回一个 ConsumerRecord，从 Partition 中读取的 Message 总是顺序的）。  
Consumer 相对 Producer 比较复杂（Producer 们一股脑往 Kafka 上写入即可）。Consumer 读完一个消息后并不会像其他消息队列将其删除，相反，消息（Event Log）仍然存在，因为可能有多个 Consumer（Group）订阅同一个 Topic（同理也可能有多个 Producer 写入同一个 Topic）。  

### 消费者和消费者群组
假设有一个应用程序需要从一个 Kafka 主题读取消息并验证这些消息，然后再把它们保存起来。应用程序需要创建一个消费者对象，订阅主题并开始接收并处理消息。过了一阵子，生产者往主题写入消息的速度超过了应用程序消费者处理数据的速度，这个时候该怎么办？如果只使用单个消费者处理消息，应用程序会远跟不上消息生成的速度。此时需要对消费者进行横向伸缩。就像多个生产者可以向相同的主题写入消息一样，也可以使用多个消费者从同一个主题读取消息，对消息进行分流。  
Kafka 消费者实例从属于消费者群组（Consumer Group）。一个群组里的消费者订阅的是同一个主题，每个消费者接收、处理主题一部分分区的消息（即同一群组里消费者互不重复消费同一事件、消息）。  
Consumer Group 的概念，每个分区 Partition 只能被同一个 Group 的一个 Consumer 消费，但可以被多个 Group 消费。  
Kafka 会自动识别且均衡给每个消费者实例合理再分配 Partition（该 Consumer 若只有单个实例，则该实例总是被分配全部 Partition 读取所有这些 Partition 上的 Message），且这种情况下，读取 Message 仍是有序保证的。如果往群组里添加过多的消费者实例，超过主题的分区数量，那么有一部分消费者就会被闲置，不会接收到任何消息 - 也因此，如果写入的主题消息量很多希望有大量消费者实例并发处理时，就需要增加分区的数量且一开始就设定一个合理值。  

另外，可以新增一个消费者的群组 G2，该群组的消费者可以订阅相同主题上的消息，与群组 G1 之间互不影响、争抢（即尽管订阅相同，G1 会接收到全部的消息，G2 也会接收到全部的消息）。  

### 消费者群组和分区再均衡
群组里的消费者共同读取主题的分区。一个新的消费者加入群组时，它读取的是原本由其他消费者读取的消息。当一个消费者被关闭或发生崩溃时，它就离开群组，原本由它读取的分区将由群组里的其他消费者来读取。在主题发生变化时，比如管理员添加了新的分区，会发生分区重分配。  
**分区的所有权从一个消费者转移到另一个消费者，这样的行为被称为再均衡。**  
再均衡非常重要，它为消费者群组带来了高可用性和伸缩性（系统可以放心地添加或移除消费者），不过在正常情况下，并不希望发生这样的行为。在再均衡期间，消费者无法读取消息，造成整个群组一小段时间的不可用。另外，当分区被重新分配给另一个消费者时，消费者当前的读取状态会丢失，它有可能还需要去刷新缓存，在它重新恢复状态之前会拖慢应用程序。  
消费者通过向被指派为群组协调器的 broker（不同的群组可以有不同的协调器）发送心跳来维持它们和群组的从属关系以及它们对分区的所有权关系。只要消费者以正常的时间间隔发送心跳，就被认为是活跃的，说明它还在读取分区里的消息。消费者会在轮询消息（为了获取消息）或提交偏移量时发送心跳。如果消费者停止发送心跳的时间足够长，会话就会过期，群组协调器认为它已经死亡，就会触发一次再均衡。  
如果一个消费者发生崩溃，并停止读取消息，群组协调器会等待几秒钟，确认它死亡了才会触发再均衡。在这几秒钟时间里，死掉的消费者不会读取分区里的消息。在清理消费者时，消费者会通知协调器它将要离开群组，协调器会立即触发一次再均衡，尽量降低处理停顿。  
  
## Ecosystem
实际工作中，有许多重复的工作、功能或逻辑，相比自己写这些与业务逻辑不直接相关的基础设施、内部工具、库、框架、轮子、common layer of application functionality，Kafka 通过其社区提供了 Ecosystem 来更好地减轻这些负担。  
这些 Ecosystem（Kafka 架构组件，均可在 Confluent Hub 找到）包括但不限于：  
* Kafka Connect - Kafka Integration Connect API & Data Integration Connector (e.g. sync kafka message to elasticsearch or cloud blob storage automatically connector)
  * Source Connector act as Producer (that is how kafka cluster consider it)
  * [Sink](https://en.wikipedia.org/wiki/Sink_(computing)) Connector act as Consumer (that is how kafka cluster consider it)
* Schema Registry - format of message (schema) evolve with the business, e.g. new field of a domain object
  * It is a standalone server process external to broker, and it is mainly about maintain/store those schemas which allow Producer/Consumer to call its API to predict whether the about-to-be-produced-or-consumed message is compatible with previous versions (otherwise will fail it before produce/consume to prevent runtime failure).
  * Support format: JSON Schema, Avro, Protocol Buffers
* Kafka Streams - consumer always grow/evolve more complex e.g. aggregation or enrichment which is stateful
  * Stream API - i.e. The Funtional Java API Library (filtering, grouping, aggregating, joining etc) run in context of your application. It is shared stream processing workload, Kafka Streams prevents state from going down with your stream processing application (consumer group) when that occurs, because it persists the state.
* ksqlDB - database severs run in another cluster (adjacent to kafka cluster)
  * A similar tool/replacement for Kafka Streams if need it
  * Provide REST API / Lib / Command Line to call, able to be hosted by docker container etc
  * It is a new event streaming database optimized for building stream processing applications in which queries are defined in SQL. It performs continuous processing of event streams and exposes the results to applications like a database.
  * Can Integrate with Kafka Connect
  
## Kafka 设置
可以设置为严格保证写入顺序，但是会牺牲一点性能，所以要注意使用场景来选择适用的设置。  

针对消息有序的业务需求，还分为全局有序和局部有序。
* 全局有序：一个Topic下的所有消息都需要按照生产顺序消费。
* 局部有序：一个Topic下的消息，只需要满足同一业务字段的要按照生产顺序消费。例如：Topic消息是订单的流水表，包含订单orderId，业务要求同一个orderId的消息需要按照生产顺序进行消费。

全局有序  
由于 Kafka 的一个 Topic 可以分为了多个 Partition，Producer 发送消息的时候，是分散在不同 Partition 的。当 Producer 按顺序发消息给 Broker，但进入 Kafka 之后，这些消息就不一定进到哪个 Partition，会导致顺序是乱的。  
因此要满足全局有序，需要 1 个 Topic 只能对应 1 个 Partition。而且对应的 consumer 也要使用单线程或者保证消费顺序的线程模型，否则会出现消费端造成的消费乱序。  

局部有序  
要满足局部有序，只需要在发消息的时候指定 Partition Key，Kafka 对其进行 Hash计算，根据计算结果决定放入哪个 Partition。这样 Partition Key 相同的消息会放在同一个 Partition。此时，Partition 的数量仍然可以设置多个，提升 Topic 的整体吞吐量。  

以上参考：https://cloud.tencent.com/developer/article/1839597  

### 序列化器
创建一个生产者对象必须指定序列化器。除了使用默认的字符串序列化器，Kafka 还提供了整型和字节数组序列化器，不过它们还不足以满足大部分场景的需求。  
自定义序列化器 - 如果发送到 Kafka 的对象不是简单的字符串或整型，那么可以使用序列化框架来创建消息记录，如 Avro、Thrift 或 Protobuf，或者使用自定义序列化器（但更建议使用前面的框架，因为要保证不同版本的 schema 兼容）。  
![](./序列化.png)  

比如使用 Avro：
1. 使用 Avro 的 KafkaAvroSerializer 来序列化对象。注意，AvroSerializer 也可以处理原语，这就是以后可以使用字符串作为记录键、使用客户对象作为值的原因。
2. schema.registry.url 是一个参数，指向 schema 的存储位置。
3. Customer 是生成的对象。会告诉生产者 Customer 对象就是记录的值。
4. 实例化一个 ProducerRecord 对象，并指定 Customer 为值的类型，然后再传给它一个 Customer 对象。
5. 把 Customer 对象作为记录发送出去，KafkaAvroSerializer 会处理剩下的事情。


# Spring Boot Kafka 项目实例
[Spring Boot Kafka 项目实例](https://github.com/yihaoye/spring-framework-example/tree/master/spring-boot-kafka)  
