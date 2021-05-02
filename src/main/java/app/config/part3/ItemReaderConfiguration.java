package app.config.part3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class ItemReaderConfiguration {

  private final StepBuilderFactory stepBuilderFactory;
  private final JobBuilderFactory jobBuilderFactory;

  @Bean
  public Job itemReaderJob() throws Exception {
    return this.jobBuilderFactory.get("itemReaderJob")
        .incrementer(new RunIdIncrementer())
        .start(this.customItemReaderStep())
        .next(this.csvFileStep())
        .build();
  }

  @Bean
  public Step csvFileStep() throws Exception {
    return stepBuilderFactory.get("csvFileStep")
        .<Person, Person>chunk(10)
        .reader(this.csvFileItemReader())
        .writer(itemWriter()).build();

  }

  private FlatFileItemReader<Person> csvFileItemReader() throws Exception {
    DefaultLineMapper<Person> lineMapper = new DefaultLineMapper<>();
    //person field 설정 (매핑하기 위해)
    DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
    tokenizer.setNames("id", "name", "age", "address");
    lineMapper.setLineTokenizer(tokenizer);

    lineMapper.setFieldSetMapper(fieldSet -> {
      int id = fieldSet.readInt("id");
      String name = fieldSet.readString("name");
      String age = fieldSet.readString("age");
      String address = fieldSet.readString("address");

      return new Person(id, name, age, address);
    });

    FlatFileItemReader<Person> itemReader = new FlatFileItemReaderBuilder<Person>()
        .name("csvFileItemReader")
        .encoding("UTF-8")
        .resource(new ClassPathResource("test.csv"))
        .linesToSkip(1)//첫번째 row 는 스킵 (데이터가 아니니)
        .lineMapper(lineMapper)
        .build();

    itemReader.afterPropertiesSet();

    return itemReader;
  }


  private Step customItemReaderStep() {
    return this.stepBuilderFactory.get("customItemReaderStep")
        .<Person, Person>chunk(10)
        .reader(new CustomItemReader<>(getItems()))
//        .processor()
        .writer(itemWriter())
        .build();
  }

  private ItemWriter<Person> itemWriter() {
    return items -> log.info(items.stream().map(Person::getName).collect(Collectors.joining(", ")));
  }

  private List<Person> getItems() {
    List<Person> list = new ArrayList<>();

    for (int i = 0; i < 100; i++) {
      Person p = new Person(i + 1, "test name " + i, "test age", "test address");
      list.add(p);
    }
    return list;
  }
}
