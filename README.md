# 스프링 배치 공부

## 101
- 스프링 배치란 : 배치를 처리하기 위한 스프링 프레임워크 기반 기술
- 기본 단위 : Job, Step
- 간단 : tasklet
- 대량 : chunk

## develop stack
- java 8+
- gradle
- spring boot 2.3.x
- mysql 5.7
- library 는 build.gradle 참고


## hello world
- project main 에 @EnableBatchProcessing 추가
- configuration 파일 생성 -> jobBuilderFactory, stepBuilderFactory 주입 
- job -> 배치의 실행 단위 / 이름 / incrementer / step 등을 설정  
- step -> job의 실행 단위
- 실행할 job을 program argument 로 정할 수 있다.(보통 이리 하겠쥬)

```program argument
    --spring.batch.job.names=helloJob
```
혹은 설정파일에 아래와 같이 설정한다. (아래와 같이 하면, 아규먼트로 받은 것만 실행 하게끔 되니 모든, 게 실행되는 것 방)
```
spring.batch.job.names: ${job.name:NONE}

```

## batch 기본 구조

- Job 은 JobLauncher 에 의해 실행
- JOb : 배치 실행 단위
- Job은 N개의 Step 을 실행할 수 있으며, 흐름(Flow) 를 관리할 수 있다. 
 - 예를 들어, a step 실행 후 조건에 따라 B스텝 또는 C스텝 을 실행 설정한다.
 - Step은 Job의 세부 실행 단위이며, N개가 등록돼 실행됨.
 - Step의 실행 단위는 크게 2가지
  - chunk 기반 : 하나의 큰 덩어리를 n개씩 나눠 실행
  - task 기반 : 하나의 작업 기반으로 실행
 - Chunk 기반 Step은 ItemReader, ItemProcessor, ItemWrite 가 있다.
  - 여기서 Item은 배치 처리 대상 객체를 의미한다.
 - ItemReader는 배치 처리 대상 객체를 읽어 ItemProcessor 또는 ItemWriter에게 전달한다.
  - 예를 들면, 파일 또는 DB에서 데이터를 읽는다.
 - ItemProcessor는 input 객체를 output 객체로 filtering 또는 processing 해 ItemWriter 에게 전달한다.
  - 예를 들면, ItemReader에서 읽은 데이터를 수정 또는 ItemWriter 대상인지 filtering 한다.
  - ItemProcessor는 옵셔널 하다.
  - 사실 ItemProcessor가 하는 일을 ItemReader 또는 ItemWriter 가 대신할 수 있다.
 - ItemWriter 는 배치 처리 대상 객체를 처리한다.
  - 예를 들면, DB update 를 하거나, 처리 대상 사용자에게 알림을 보낸다.

## 테이블 구조
- BATCH_JOB_INSTANCE : 최상위 테이블
- BATCH_JOB_EXECUTION : job 실행되는 동안 시작/종료 시간, job 상태 등을 관리
- BATCH_JOB_EXECUTION_PARAMS : job 실행하기 위해 주입된 파라미터 저장
- BATCH_JOB_EXECUTION_CONTEXT : job 실행되면서 공유해야할 데이터를 직렬화해 저장.
- BATCH_STEP_EXECUTION : step 이 실행되는 동안 필요한 데이터 또는 결과 저장
- BATCH_STEP_EXECUTION_CONTEXT : step 실행되면서 공유해야할 데이터를 직렬화해 저장

