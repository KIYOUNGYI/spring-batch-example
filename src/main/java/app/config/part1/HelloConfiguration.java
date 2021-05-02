package app.config.part1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * job, step 은 배치의 모든 것이라 보면 된다.
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class HelloConfiguration {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;

  // 배치 실행단위
  @Bean
  public Job helloJob() {
    return jobBuilderFactory.get("helloJob")//스프링 배치를 실행 시킬 수 있는 키
        .incrementer(new RunIdIncrementer())//이 클래스는 job 이 실행될 때마다 파라미터 아이디를 자동으로 생성해주는 클래스
        .start(this.helloStep())//최초 실행될 메서도
        .build();
  }
  // Job 의 실행단위
  @Bean
  public Step helloStep() {
    return stepBuilderFactory.get("helloStep")
        .tasklet(((contribution, chunkContext) -> {
          log.info("hello spring batch");
          return RepeatStatus.FINISHED;
        })).build();
  }
}
