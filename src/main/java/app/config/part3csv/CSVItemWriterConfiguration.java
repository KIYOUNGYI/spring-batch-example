package app.config.part3csv;

import app.config.part3.CustomItemReader;
import app.domain.Person;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

/**
 * <p> 설명 : csv 쓰기 연습
 * <p> 실행 명령어 $ gradle bootRun --args='--job.name=csvItemWriterJob'
 * <p> 정산 할 때 (ex> b2b) 종종 활용
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
public class CSVItemWriterConfiguration {

  private final StepBuilderFactory stepBuilderFactory;
  private final JobBuilderFactory jobBuilderFactory;

  @Bean
  public Job csvItemWriterJob() throws Exception {
    return this.jobBuilderFactory.get("csvItemWriterJob")
        .incrementer(new RunIdIncrementer())
        .start(this.csvItemWriterStep())
        .build();
  }

  @Bean
  public Step csvItemWriterStep() throws Exception {
    return this.stepBuilderFactory.get("csvItemWriterStep")
        .<Person, Person>chunk(10)
        .reader(itemReader())
        .writer(csvItemWriter())
        .build();

  }

  private ItemWriter<Person> csvItemWriter() throws Exception {
    BeanWrapperFieldExtractor<Person> fieldExtractor = new BeanWrapperFieldExtractor<>();
    fieldExtractor.setNames(new String[]{"id", "name", "age", "address"});
    DelimitedLineAggregator<Person> lineAggregator = new DelimitedLineAggregator<>();
    lineAggregator.setDelimiter(",");
    lineAggregator.setFieldExtractor(fieldExtractor);

    FlatFileItemWriter<Person> itemWriter = new FlatFileItemWriterBuilder<Person>()
        .name("csvFileItemWriter")
        .encoding("UTF-8")
        .resource(new FileSystemResource("output/test-output.csv"))
        .lineAggregator(lineAggregator)
        .headerCallback(writer -> writer.write("id,이름,나이,거주지"))
        .footerCallback(writer -> writer.write("--------------\n"))
        .append(true)//이어 쓰고싶으면 + 푸터에 개행문자를 추가해줘야 함(\n 추가)
        .build();

    itemWriter.afterPropertiesSet();

    return itemWriter;
  }

  private ItemReader<Person> itemReader() {
    return new CustomItemReader<Person>(getItems());
  }

  private List<Person> getItems() {
    List<Person> items = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      items.add(new Person(i + 1, "test name " + i, "test age", "test address"));
    }
    return items;
  }


}
