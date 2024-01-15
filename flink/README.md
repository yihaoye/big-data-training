https://fuhuacn.github.io/2020/01/28/flink/  

在 Apache Flink 中，有一些概念和组件：

在 Flink 中，流处理程序被建模为一个有向无环图（DAG），称为 Flink 程序或 JobGraph。这可以看作是一个处理流水线 Streaming Dataflow，定义了数据流的转换和操作。  

### Source 和 Sink
在 Flink 中，Source (Operator) 用于定义数据输入，而 Sink (Operator) 用于定义数据输出。  

### 编程模型和数据流  
用户实现的 Flink 程序是由 Stream 和 Transformation 这两个基本构建块组成，其中 Stream 是一个中间结果数据，而 Transformation (Operator) 是一个操作，它对一个或多个输入 Stream 进行计算处理，输出一个或多个结果 Stream。  
Operator 是一个数据处理的基本单元，每个 Operator 执行一些特定的操作，如映射、过滤、聚合等，每个 Operator 在运行时对应一个 Task。  
Flink 程序是并行和分布式的：一个 Stream 可以被分成多个 Stream 分区（Stream Partitions），一个 Operator 可以被分成多个 Operator Subtask，每一个 Operator Subtask 是在不同的线程中独立执行的。  

### State
Flink 提供了状态管理机制，允许在流处理任务中保存和访问状态。
