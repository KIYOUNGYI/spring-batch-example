package app.config.part2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class SharedConfiguration {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;

  @Bean
  public Job shareJob() {
    return jobBuilderFactory.get("shareJob")
        .incrementer(new RunIdIncrementer())
        .start(this.shareStep())
        .next(this.shareStep2())
        .build();
  }

  /**
   * 이전 스텝에서 저장했던 내역 꺼내기
   * @return
   */
  @Bean
  public Step shareStep2() {
    return stepBuilderFactory.get("shareStep2")
        .tasklet(((contribution, chunkContext) -> {
          StepExecution stepExecution = contribution.getStepExecution();
          ExecutionContext stepExecutionContext = stepExecution.getExecutionContext();

          JobExecution jobExecution = stepExecution.getJobExecution();
          ExecutionContext jobExecutionContext = jobExecution.getExecutionContext();

          log.info("jobKey : {}, stepKey : {}", jobExecutionContext.getString("jobKey", "emptyJobKey"),
              stepExecutionContext.getString("stepKey", "emptyStepKey"));
          return RepeatStatus.FINISHED;
        })).build();
  }

  /**
   * job 에서 job 이름, step 에서 step 이름, job parameter 등을 꺼내서 로그를 찍어보기.
   */
  @Bean
  public Step shareStep() {

    return stepBuilderFactory.get("shareStep")
        .tasklet(((contribution, chunkContext) -> {
          StepExecution stepExecution = contribution.getStepExecution();
          ExecutionContext stepExecutionContext = stepExecution.getExecutionContext();
          stepExecutionContext.putString("stepKey", "step Execution context");

          JobExecution jobExecution = stepExecution.getJobExecution();
          JobInstance jobInstance = jobExecution.getJobInstance();
          ExecutionContext jobExecutionContext = jobExecution.getExecutionContext();
          jobExecutionContext.putString("jobKey", "job execution context");
          JobParameters jobParameters = jobExecution.getJobParameters();

          log.info("jobName : {}, stepName : {}, parameter : {}",
              jobInstance.getJobName(),
              stepExecution.getStepName(),
              jobParameters.getLong("run.id"));
          return RepeatStatus.FINISHED;
        })).build();

  }

}
