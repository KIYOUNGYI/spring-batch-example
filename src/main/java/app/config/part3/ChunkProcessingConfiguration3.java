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
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * tasklet 도 chunk 처럼 페이징 흉내낼 수 있음. 다만 코드양이 늘어남.
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
public class ChunkProcessingConfiguration3 {

  private final StepBuilderFactory stepBuilderFactory;
  private final JobBuilderFactory jobBuilderFactory;

  @Bean
  public Job chunkProcessingJob3() {
    return jobBuilderFactory.get("chunkProcessingJob3")
        .incrementer(new RunIdIncrementer())
        .start(this.taskBaseStep3())
        .next(this.chunkBaseStep3())
        .build();
  }

  @Bean
  public Step taskBaseStep3() {
    return stepBuilderFactory.get("taskBaseStep3")
        .tasklet(this.taskletV3())
        .build();
  }

  @Bean
  public Step chunkBaseStep3() {
    return stepBuilderFactory.get("chunkBaseStep3")
        .<String, String>chunk(10)//100개의 데이터를 10개씩 나눈다.  <String, String> 에서 첫번째 제네릭 타입 스트링은 아이템리더에서 읽은 것. 프로세서에서 처리하고, 아웃풋 프로세서에서 처리하는게 두번째 파라미터
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
