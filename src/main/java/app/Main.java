package app;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootApplication
@EnableBatchProcessing//batch processing 을 하겠다.
public class Main {

  public static void main(String[] args) {
//    SpringApplication.run(Main.class, args);
    System.exit(SpringApplication.exit(SpringApplication.run(Main.class, args)));//async 용 (잘 종료가 안되는 경우가 있어서 추가)
  }

  // spring 에서 taskExecutor 를 자동으로 등록해서 사용해서 프라이머리 지정 필요
  @Bean
  @Primary
  TaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
    taskExecutor.setCorePoolSize(10);//코어 풀 사이즈
    taskExecutor.setMaxPoolSize(20);//최대 스레드의 사이즈
    taskExecutor.setThreadNamePrefix("batch-thread-");
    taskExecutor.initialize();
    return taskExecutor;
  }

}
