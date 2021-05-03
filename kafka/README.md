分布式系统中，Kafka 的多个 Client（Consumer 或 Producer） 或 Broker 运行在不同的服务器、容器？上，本地测试时可以用线程代替。
  
## Introduction
Kafka 基于事件（Event）概念。  
  
## Topic
每个 Event 都归属于某个 Topic（同类 Event 的容器，就如关系数据库的 Table 概念），单个 Event 也可以同时归属于多个 Topic。  
每个 Topic 都是 Event 的 durable Log（存放在硬盘上的日志文件）而不是队列（Queue，因此不同于一般消息队列软件），这也是为什么 Kafka Queue 这种称呼不准确。Log 是一种简单的数据结构，只能 append 不能中间插入，读取时只能通过 offset 查找而不是 index，而且 Event Log 是 immutable（因为生活中一个事件发生了就不可改变），Log 也使得更好的吞吐性能成为了可能也使得 Message 过期设置（比如 2 周或永不过期）成为可能。  
  
### Partitioning
由于经济、系统设计的原因（比如单个 Topic 数据量太大，但是企业想省钱继而希望使用多个小节点而不是一个大节点），Partitioning - 把单个 Topic 的 Log 分割成多个 Log（Partition），它们可以被分布分配在 Kafka 集群的一个个独立的节点上。当新消息 Message 来临时，应该写入哪个 Partition 有多种算法选择，比如 Round Robin（消息没 Key）、哈希后对 Partition 总数取余（消息有 Key）等等，这些方法总是能保证单个 Partition 上该 Topic 的 Message 总是按写入顺序排序的（单个 Partition 上不一定顺序紧密相连因为中间的可能分布到其他 Partition，但是单个 Partition 上的总是单调顺序的比如 1、3、5）。  
  
### Brokers
Broker 即一个运行 Kafka broker 进程的机器 - 可以是服务器、计算机、实例或虚拟化容器。  
每个 Broker 可能托管了一个或多个 Partition（以及 Partition replication 的管理），并处理写入/读取的请求。  
Broker 设计简单所以容易 scale。  
  
### Replication
原来是一个 Partition 总是从属于某个 Broker，为了容错该 Broker 的故障可能，就把该 Partition 复制、备份到另外几个 Broker 上，称之为 follower replicas（与数据库 replicas 类似），原 Partition 为 leader replica（负责写入与读取），通常备份会自动进行，如果 leader replica 故障了则会有一个原 follower replica 自动取而代之。  
  
### Producer
即写入操作，通过轻量级的 Kafka Producer 库就可以进行（使用前进行正确配置即可，包括链接 Kafka 集群上的某几个 broker、安全设置、网络行为等等）。  
比如在 Java 里，ProducerRecord 即是开发者、程序准备写入的 Key-Value pair（亦即 Event）（写入还包括放置在哪个 Topic）。另外 Partitioning 由 Producer 负责。  
  
### Consumer
即读取操作，通过 Kafka Consumer 库就可以进行（如 Kafka Producer 一样使用前进行正确配置即可）。  
然后让该 Kafka Consumer 对象 subscribe（订阅）Topic 即可（然后每次调用对象的 poll 方法得到、返回一个 ConsumerRecord，从 Partition 中读取的 Message 总是顺序的）。  
Consumer 相对 Producer 比较复杂（Producer 们一股脑往 Kafka 上写入即可）。Consumer 读完一个消息后并不会像其他消息队列将其删除，相反，消息（Event Log）仍然存在，因为可能有多个 Consumer 订阅同一个 Topic（同理也可能有多个 Producer 写入同一个 Topic）。  
Kafka 允许 scale up 某个 Consumer（即该 Consumer 的多个复制实例，Kafka 会自动识别且负载均衡给每个实例合理分配 Partition（该 Consumer 若只有单个实例，则该实例总是被分配全部 Partition 读取所有这些 Partition 上的 Message），且这种情况下，读取 Message 仍是有序保证的），其意义是因为实际中，可能需要对某个应用的对某 Topic 的消费速率有要求、同时又要考虑处理单个消息的经济成本，这样该应用的单个实例就可能无法达到消费速率要求（可以 vertical scaling 即加硬件资源如内存、CPU，但是这就违反了处理单个消息的经济成本要求），因此需要前述的 horizontal scaling 解决问题。可以称为 Scaling Consumer Group，因为它们共享同一个 Group ID 且是必须设置的，如果该 Consumer 的实例数量高于 Topic 的 Partition 数量，则多出来的 Consumer 是冗余得不到分配的。  

