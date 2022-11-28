# spring-batch-technic
spring batch example (Parallel Processing, ItemStream, Fault Tolerance)

### Skip & Retry (fault tolerance) 
- skip 동작에 관한 이해가 필요(예외 발생시 그냥 skip하고 넘어가는 방식이 아님)
- read, process, write 예외 발생에 대한 각각의 skip 방식 (단순 skip, chunk size=1로 재처리 등등) 
- processorNonTransactional() 옵션을 활용한 process 중복 처리 방지
- retry 설정후 write exception시 chunk단위 write 재처리 고려

### ItemStream
- itemStream이 언제 적용되는지 이해 필요 (처리 순서 -> chunk처리 / item stream update / execution, executionContext table update)
- itemStreamReader / itemStreamWriter 둘 중 하나만 사용하는 것을 권장. stream update 실행 시점에 큰 영향도는 없으므로 값을 넣어주기 쉬운 쪽으로 
- 보통 update에서 executionContext에 배치 처리 상황(마지막으로 처리한 item offset등)을 넣어주는면 재시도 관점에서 유용함 
(chunk size > 1인 경우 부분 성공에 대한 업데이트는 발생하지 않아 재시도 되는 상황을 고래해 주면 좋음) 
- context에 너무 큰 데이터는 넣지말자 (db에 저장되는거라 길이 제한이 존재함)

### Parallel Processing
- reader, processor, writer 어떤 부분에서 병목이 발생하는지를 먼저 파악
- 단일 process에서 step을 병렬 처리하는 대표적인 방법 3가지 ( taskExecutor + throttleLimit , asyncItemProcessor, partitioning)
- taskExecutor + throttleLimit 방식은 chunk단위로 병렬 처리 (chunk당(read , process, write) 각각의 스레드가 할당되서 처리)
- asyncItemProcessor 방식은 process를 병렬 처리 asyncItemWriter를 구현해서 같이 사용해야하지만 병렬 처리는 process만
- partitioning step을 파티셔닝해서 각각의 stepExecution을 만들어 병렬처리 (파티셔닝의 기준을 설정필요)
- chunk처리가 너무 빠른 경우 thread 수를 너무 늘렸을때 batch table update를 위한 커넥션이 고갈될 수 있으니 주의
