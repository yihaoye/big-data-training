分布式系统中，Kafka 的多个 Client（Consumer 或 Producer） 或 Broker 运行在不同的服务器、容器？上，本地测试时可以用线程代替。
  
## Introduction
Kafka 基于事件（Event）概念。  
  
## Topic
每个 Event 都归属于某个 Topic（同类 Event 的容器，就如关系数据库的 Table 概念），单个 Event 也可以同时归属于多个 Topic。  
每个 Topic 都是 Event 的 durable Log（存放在硬盘上的日志文件）而不是队列（Queue，因此不同于一般消息队列软件），这也是为什么 Kafka Queue 这种称呼不准确。Log 是一种简单的数据结构，只能 append 不能中间插入，读取时只能通过 offset 查找而不是 index，而且 Event Log 是 immutable（因为生活中一个事件发生了就不可改变），Log 也使得更好的吞吐性能成为了可能也使得 Message 过期设置（比如 2 周或永不过期）成为可能。  
  
### Partitioning
Partitioning 把单个 Topic 的 Log 分割成多个 Log（Partition），它们可以被分布分配在 Kafka 集群的一个个独立的节点上。当新消息 Message 来临时，应该写入哪个 Partition 有多种算法选择，比如 Round Robin（消息没 Key）、哈希后对 Partition 总数取余（消息有 Key）等等，这些方法总是能保证单个 Partition 上该 Topic 的 Message 总是按写入顺序排序的（单个 Partition 上不一定顺序紧密相连因为中间的可能分布到其他 Partition，但是单个 Partition 上的总是单调顺序的比如 1、3、5）。  
  
### Brokers
Broker 即一个运行 Kafka broker 进程的机器 - 可以是服务器、计算机、实例或虚拟化容器。  
每个 Broker 可能托管了一个或多个 Partition（以及 Partition replication 的管理），并处理写入/读取的请求。  
Broker 设计简单所以容易 scale。  
