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

// gradle bootRun --args='--job.name=chunkProcessingJob4 -chunkSize=25'
@Slf4j
@RequiredArgsConstructor
@Configuration
public class ChunkProcessingConfiguration4 {

  private final StepBuilderFactory stepBuilderFactory;
  private final JobBuilderFactory jobBuilderFactory;

  @Bean
  public Job chunkProcessingJob4() {
    return jobBuilderFactory.get("chunkProcessingJob4")
        .incrementer(new RunIdIncrementer())
        .start(this.taskBaseStep4())
        .next(this.chunkBaseStep4(null))
        .build();
  }

  @Bean
  public Step taskBaseStep4() {
    return stepBuilderFactory.get("taskBaseStep4")
        .tasklet(this.tasklet())
        .build();
  }

  @Bean
  @JobScope//life cycle 때문에 아래 파라미터를 쓰려면, 해당 어노테이션 필요
  public Step chunkBaseStep4(@Value("#{jobParameters[chunkSize]}") String chunkSize) {
    return stepBuilderFactory.get("chunkBaseStep4")
        .<String, String>chunk(StringUtils.isNotEmpty(chunkSize) ? Integer.parseInt(chunkSize) : 10)
        .reader(itemReader())
        .processor(itemProcessor())
        .writer(itemWriter())
        .build();
  }


  //job parameter 사용
  private Tasklet tasklet() {
    List<String> items = getItems();

    return (contribution, chunkContext) -> {

      StepExecution stepExecution = contribution.getStepExecution();
      JobParameters jobParameters = stepExecution.getJobParameters();

      String value = jobParameters.getString("chunkSize", "10");
      int chunkSize = StringUtils.isNotEmpty(value) ? Integer.parseInt(value) : 10;

      int fromIndex = stepExecution.getReadCount();//처음 0  // 10
      int toIndex = fromIndex + chunkSize;//처음 10          // 20

      if (fromIndex >= items.size()) {//0<100
        return RepeatStatus.FINISHED;
      }

      List<String> subList = items.subList(fromIndex, toIndex);//0~9 꺼내고  //10~19

      log.info("task item size : %s", subList.size());

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
