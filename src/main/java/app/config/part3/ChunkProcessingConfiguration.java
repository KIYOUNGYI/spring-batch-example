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
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

//-chunkSize=25 --job.name=chunkProcessingJobV2
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ChunkProcessingConfiguration {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;

  @Bean
  public Job chunkProcessingJob() {
    return jobBuilderFactory.get("chunkProcessingJob")
        .incrementer(new RunIdIncrementer())
        .start(this.taskBaseStep())
        .next(this.chunkBaseStep())
        .build();
  }

  @Bean
  public Job chunkProcessingJobV2() {
    System.out.println("ChunkProcessingConfiguration.chunkProcessingJobV2");
    return jobBuilderFactory.get("chunkProcessingJobV2")
        .incrementer(new RunIdIncrementer())
        .start(this.taskBaseStep())
        .next(this.chunkBaseStepV2(null))
        .build();
  }

  @Bean
  public Step chunkBaseStep() {
    return stepBuilderFactory.get("chunkBaseStep")
        .<String, String>chunk(10)//100개의 데이터를 10개씩 나눈다.  <String, String> 에서 첫번째 제네릭 타입 스트링은 아이템리더에서 읽은 것. 프로세서에서 처리하고, 아웃풋 프로세서에서 처리하는게 두번째 파라미터
        .reader(itemReader())
        .processor(itemProcessor())//데이터를 가공하는 것.
        .writer(itemWriter())
        .build();

  }

  @Bean
  @JobScope
  public Step chunkBaseStepV2(@Value("#{jobParameters[chunkSize]}") String chunkSize) {

    System.out.println("chunkSize ========> " + chunkSize);

    return stepBuilderFactory.get("chunkBaseStep")
        .<String, String>chunk(StringUtils.isNotEmpty(chunkSize) ? Integer.parseInt(chunkSize)
            : 10)//100개의 데이터를 10개씩 나눈다.  <String, String> 에서 첫번째 제네릭 타입 스트링은 아이템리더에서 읽은 것. 프로세서에서 처리하고, 아웃풋 프로세서에서 처리하는게 두번째 파라미터
        .reader(itemReader())
        .processor(itemProcessor())//데이터를 가공하는 것.
        .writer(itemWriter())
        .build();

  }

  private ItemWriter<String> itemWriter() {
    return items -> System.out.println("chunk item size : {}," + items.size());
//    return items -> items.forEach(log::info);
  }

  private ItemProcessor<String, String> itemProcessor() {

    return item -> item + ", Spring Batch.";
  }

  private ItemReader<String> itemReader() {
    return new ListItemReader<>(getItems());
  }

  @Bean
  public Step taskBaseStep() {
    return stepBuilderFactory.get("taskBaseStep")
        .tasklet(this.taskletV3())
        .build();
  }

  private Tasklet tasklet() {
    return (contribution, chunkContext) -> {

      List<String> items = getItems();
//      log.info("items size :",items.size());
      System.out.println("items size: " + items.size());
      return RepeatStatus.FINISHED;
    };
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

  //chunkSize 조정
  private Tasklet taskletV3() {
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
