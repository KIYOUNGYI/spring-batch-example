package app.config.part3;

import io.micrometer.core.instrument.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * tasklet 도 chunk 처럼 페이징 흉내낼 수 있음. 다만 코드양이 늘어남.
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
public class ChunkProcessingConfiguration2 {

  private final StepBuilderFactory stepBuilderFactory;
  private final JobBuilderFactory jobBuilderFactory;

  @Bean
  public Job pagingUsingTasklet() {
    return jobBuilderFactory.get("pagingUsingTasklet")
        .incrementer(new RunIdIncrementer())
        .start(this.taskBaseStepV2())
        .build();
  }

  @Bean
  public Step taskBaseStepV2() {
    return stepBuilderFactory.get("taskBaseStepV2")
        .tasklet(this.taskletV2())
        .build();
  }


  private Tasklet taskletV2() {
    List<String> items = getItems();

    return (contribution, chunkContext) -> {

      StepExecution stepExecution = contribution.getStepExecution();
      int chunkSize = 10;
      int fromIndex = stepExecution.getReadCount();//처음 0  // 10
      int toIndex = fromIndex + chunkSize;//처음 10          // 20

      if (fromIndex >= items.size()) {//0<100
        return RepeatStatus.FINISHED;
      }

      List<String> subList = items.subList(fromIndex, toIndex);//0~9 꺼내고  //10~19

      log.info("task item size : {}", subList.size());

      stepExecution.setReadCount(toIndex);//10     // 20

      return RepeatStatus.CONTINUABLE;
    };
  }


  private List<String> getItems() {

    List<String> items = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      items.add(i + " Hello");
    }
    return items;
  }
}
