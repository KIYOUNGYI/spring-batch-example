package app.config.part4;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//gradle bootRun --args='--job.name=userJob' <- user 저장하는 명령어
@Slf4j
@RequiredArgsConstructor
@Configuration
public class UserConfiguration {

  private final StepBuilderFactory stepBuilderFactory;
  private final JobBuilderFactory jobBuilderFactory;
  private final UserRepository userRepository;

  @Bean
  public Job UserJob() {
    return this.jobBuilderFactory.get("userJob")
        .incrementer(new RunIdIncrementer())
        .start(this.saveUserStep())
        .build();
  }

  @Bean
  public Step saveUserStep() {
    return this.stepBuilderFactory.get("saveUserStep")
        .tasklet(new SaveUserTasklet(userRepository))
        .build();
  }
}
