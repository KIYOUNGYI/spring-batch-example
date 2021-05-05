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
import org.springframework.batch.core.configuration.annotation.StepScope;
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

// gradle bootRun --args='--job.name=chunkProcessingJob5 -chunkSize=20'
@Slf4j
@RequiredArgsConstructor
@Configuration
public class ChunkProcessingConfiguration5 {

  private final StepBuilderFactory stepBuilderFactory;
  private final JobBuilderFactory jobBuilderFactory;

  @Bean
  public Job chunkProcessingJob5() {
    return jobBuilderFactory.get("chunkProcessingJob5")
        .incrementer(new RunIdIncrementer())
        .start(this.taskBaseStep5())
        .next(this.chunkBaseStep5(null))
        .build();
  }

  @Bean
  public Step taskBaseStep5() {
    return stepBuilderFactory.get("taskBaseStep5")
        .tasklet(this.tasklet(null))
        .build();
  }

  @Bean
  @JobScope//life cycle 때문에 아래 파라미터를 쓰려면, 해당 어노테이션 필요
  public Step chunkBaseStep5(@Value("#{jobParameters[chunkSize]}") String chunkSize) {
    return stepBuilderFactory.get("chunkBaseStep5")
        .<String, String>chunk(StringUtils.isNotEmpty(chunkSize) ? Integer.parseInt(chunkSize) : 10)
        .reader(itemReader())
        .processor(itemProcessor())
        .writer(itemWriter())
        .build();
  }


  @Bean
  @StepScope
  public Tasklet tasklet(@Value("#{jobParameters[chunkSize]}") String value) {
    List<String> items = getItems();

    return (contribution, chunkContext) -> {

      StepExecution stepExecution = contribution.getStepExecution();

      int chunkSize = StringUtils.isNotEmpty(value) ? Integer.parseInt(value) : 10;

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

  private ItemWriter<String> itemWriter() {
    return items -> System.out.println("chunk item size : {}," + items.size());
  }

  private ItemProcessor<String, String> itemProcessor() {

    return item -> item + ", Spring Batch.";
  }

  private ItemReader<String> itemReader() {
    return new ListItemReader<>(getItems());
  }

  private List<String> getItems() {

    List<String> items = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      items.add(i + " Hello");
    }
    return items;
  }
}
