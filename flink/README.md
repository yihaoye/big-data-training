https://fuhuacn.github.io/2020/01/28/flink/  

在 Apache Flink 中，有一些概念和组件：

在 Flink 中，流处理程序被建模为一个有向无环图（DAG），称为 Flink 程序或 JobGraph。这可以看作是一个处理流水线 Streaming Dataflow，定义了数据流的转换和操作。  

### Source 和 Sink
在 Flink 中，Source (Operator) 用于定义数据输入，而 Sink (Operator) 用于定义数据输出（比如数据持久化）。  

### 编程模型和数据流  
用户实现的 Flink 程序是由 Stream 和 Transformation 这两个基本构建块组成：其中 Stream 是一个中间结果数据；将一个或多个 DataStream 生成新的 DataStream 的过程被称为 Transformation，转换过程中，每种操作类型被定义为不同的 Operator / Function。  
一个 Transformation 可以有多个 Operator，Operator 是一个数据处理的基本单元，每个 Operator 执行一些特定的操作，如映射、过滤、聚合等，每个 Operator 在运行时对应一个 Task。  
Flink 程序是并行和分布式的：一个 Stream 可以被分成多个 Stream 分区（Stream Partitions），一个 Operator 可以被分成多个 Operator Subtask，每一个 Operator Subtask 是在不同的线程中独立执行的。  
Operator 不在代码里显式地声明，它是由 Flink 内部自动创建和执行的，代码里主要还是写 Function，Operator 和 Function 之间的关系是，Function 定义了数据处理的逻辑规则，而 Operator 负责实际执行这些规则。Operator 是实际执行数据处理操作的组件，而 Function 则是定义数据处理逻辑的接口或抽象类。  

### State
Flink 提供了状态管理机制，允许在流处理任务中保存和访问状态。

### Function
Flink 框架提供了各种功能丰富的函数（Function）来支持流式数据处理。以下是 Flink 中一些主要的函数类型：
1. **MapFunction：** 将一个输入元素映射为一个输出元素的函数。MapFunction 接受一个输入元素并返回一个输出元素，可以用于对每个输入元素进行简单的转换或处理（数据清洗、数据转换、特征提取、数据分割、数据转发）。
2. **FlatMapFunction：** 将一个输入元素映射为零个、一个或多个输出元素的函数。FlatMapFunction 接受一个输入元素并返回一个或多个输出元素，可以用于对每个输入元素进行拆分、过滤或转换。
3. **FilterFunction：** 对输入流进行过滤的函数。FilterFunction 接受一个输入元素并返回一个布尔值，用于确定是否保留该输入元素，也用于分支逻辑。
4. **KeySelector：** 从输入元素中提取键（key）的函数。KeySelector 接受一个输入元素并返回一个键，用于对输入流进行分区或分组。
5. **ReduceFunction：** 将输入流中的相邻元素聚合为单个元素的函数。ReduceFunction 接受两个输入元素并返回一个聚合后的元素，用于对输入流进行聚合操作。
6. **AggregateFunction：** 将输入流中的元素聚合为一个累加器的函数。AggregateFunction 接受输入元素和一个累加器，并更新累加器的状态，通常用于计算统计信息或汇总数据。
7. **WindowFunction：** 将窗口中的元素聚合为一个或多个输出元素的函数。WindowFunction 接受一个窗口中的所有元素，并返回一个或多个输出元素，通常用于对窗口中的元素进行计算或处理。
8. **ProcessFunction：** 用于对流处理过程中的事件进行更复杂的处理和控制的函数。ProcessFunction 可以访问事件时间（event time）、处理时间（processing time）、定时器（timer）等信息，并能够生成新的事件或状态更新。
```java
public class Solution {
    public static void main(String[] args) throws Exception {
        // 创建流处理环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 创建输入数据流
        DataStream<String> inputStream = env.fromElements("data1", "data2", "data3");
        // 调用 ML 模型进行评分，并将评分结果发送到输出数据流中
        DataStream<Double> scoresStream = inputStream.process(new MLModelEvaluationFunction());
        // 打印评分结果
        scoresStream.print();
        // 执行任务
        env.execute("ML Model Evaluation Example");
    }

    // 定义 ProcessFunction 来调用 ML 模型进行评分
    public static class MLModelEvaluationFunction extends ProcessFunction<String, Double> {
        @Override
        public void processElement(String input, Context context, Collector<Double> collector) throws Exception {
            // 在这里调用 ML 模型进行评分
            double score = evaluateMLModel(input);
            // 将评分结果发送到输出数据流中
            collector.collect(score);
        }

        // 模拟 ML 模型的评分逻辑
        private double evaluateMLModel(String data) {
            // 这里可以是实际的 ML 模型调用逻辑，例如调用 TensorFlow、Scikit-learn 等库进行评分
            // 这里简单地返回一个随机评分作为示例
            return Math.random() * 100;
        }
    }
}
```

除此之外，如果需要并发执行 Function，有内置 API setParallelism() 方法，代码如下所示：
```java
final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

DataStream<String> text = [...];
DataStream<Tuple2<String, Integer>> wordCounts = text
    .flatMap(new LineSplitter())
    .keyBy(value -> value.f0)
    .window(TumblingEventTimeWindows.of(Time.seconds(5)))
    .sum(1).setParallelism(5);

wordCounts.print();

env.execute("Word Count Example");
```  

### 其他
* DataStream API（流数据 API）：
  * 是 Flink 中用于处理流式数据的核心 API。它提供了丰富的操作符和函数，用于对流数据进行转换、聚合、过滤、连接等操作。
  * 允许开发人员定义流式计算任务，包括实时数据处理、事件驱动的应用程序、复杂事件处理等。
  * 支持事件时间和处理时间两种时间模式，并提供了窗口操作、水位线等机制来处理乱序事件。
