package app.config.part3hw;


import app.config.part3hw.SavePersonListener.SavePersonAnnotationJobExecutionListener;
import app.config.part3hw.SavePersonListener.SavePersonJobExecutionListener;
import app.domain.Person;
import javax.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.cache.spi.support.AbstractReadWriteAccess.Item;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.support.builder.CompositeItemProcessorBuilder;
import org.springframework.batch.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

//gradle bootRun --args='--job.name=savePersonJob -allow_duplicate=false'
//gradle bootRun --args='--job.name=savePersonJob2 -allow_duplicate=true'
//gradle bootRun --args='--job.name=savePersonJob3 -allow_duplicate=true'  <- recovery call back 동작하는지 확인
@Configuration
@Slf4j
@RequiredArgsConstructor
public class SavePersonConfiguration {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final EntityManagerFactory entityManagerFactory;

  @Bean
  public Job savePersonJob3() throws Exception {
    return this.jobBuilderFactory.get("savePersonJob3")
        .incrementer(new RunIdIncrementer())
        .start(this.savePersonStepV3(null))
        .listener(new SavePersonJobExecutionListener())
        .listener(new SavePersonAnnotationJobExecutionListener())
        .build();
  }


  @Bean
  public Job savePersonJob2() throws Exception {
    return this.jobBuilderFactory.get("savePersonJob2")
        .incrementer(new RunIdIncrementer())
        .start(this.savePersonStepV2(null))
        .listener(new SavePersonJobExecutionListener())
        .listener(new SavePersonAnnotationJobExecutionListener())
        .build();
  }

  @Bean
  @JobScope
  public Step savePersonStepBackup(@Value("#{jobParameters[allow_duplicate]}") String allowDuplicate) throws Exception {
    return stepBuilderFactory.get("savePersonStep")
        .<Person, Person>chunk(10)
        .reader(this.itemReader())
        .processor(new DuplicateValidationProcessor<>(Person::getName, Boolean.parseBoolean(allowDuplicate)))
        .writer(itemWriter())
        .listener(new SavePersonListener.SavePersonAnnotationStepExecutionListener())
        .faultTolerant()
        .skip(NotFoundNameException.class)
        .skipLimit(2)
        .build();
  }

  @Bean
  @JobScope
  public Step savePersonStepV2(@Value("#{jobParameters[allow_duplicate]}") String allowDuplicate) throws Exception {
    return stepBuilderFactory.get("savePersonStep")
        .<Person, Person>chunk(10)
        .reader(this.itemReader())
        .processor(itemProcessor(allowDuplicate))
        .writer(jpaPersonWriter())
        .listener(new SavePersonListener.SavePersonAnnotationStepExecutionListener())
        .faultTolerant()
        .skip(NotFoundNameException.class)
        .skipLimit(2)
        .build();
  }

  @Bean
  @JobScope
  public Step savePersonStepV3(@Value("#{jobParameters[allow_duplicate]}") String allowDuplicate) throws Exception {
    return stepBuilderFactory.get("savePersonStep")
        .<Person, Person>chunk(10)
        .reader(this.itemReader())
        .processor(itemProcessorV3(allowDuplicate))
        .writer(jpaPersonWriter())
        .listener(new SavePersonListener.SavePersonAnnotationStepExecutionListener())
        .faultTolerant()
        .skip(NotFoundNameException.class)
        .skipLimit(2)
        .build();
  }


  private ItemProcessor<? super Person, ? extends Person> itemProcessorV3(String allowDuplicate) throws Exception {

    DuplicateValidationProcessor<Person> duplicateValidationProcessor
        = new DuplicateValidationProcessor<>(Person::getName, Boolean.parseBoolean(allowDuplicate));

    ItemProcessor<Person, Person> validationProcessor = item -> {
      if (item.isNotEmptyName()) {
        return item;
      }
      throw new NotFoundNameException();
    };

    CompositeItemProcessor<Person, Person> itemProcessor = new CompositeItemProcessorBuilder<Person, Person>()
        .delegates(new PersonValidateProcessor(),validationProcessor, duplicateValidationProcessor)
        .build();

    itemProcessor.afterPropertiesSet();

    return itemProcessor;
  }


  private ItemProcessor<? super Person, ? extends Person> itemProcessor(String allowDuplicate) throws Exception {

    DuplicateValidationProcessor<Person> duplicateValidationProcessor
        = new DuplicateValidationProcessor<>(Person::getName, Boolean.parseBoolean(allowDuplicate));

    ItemProcessor<Person, Person> validationProcessor = item -> {
      if (item.isNotEmptyName()) {
        return item;
      }
      throw new NotFoundNameException();
    };

    CompositeItemProcessor<Person, Person> itemProcessor = new CompositeItemProcessorBuilder<Person, Person>()
        .delegates(validationProcessor, duplicateValidationProcessor)
        .build();

    itemProcessor.afterPropertiesSet();

    return itemProcessor;
  }


  private ItemWriter<Person> jpaPersonWriter() throws Exception {
    JpaItemWriter<Person> jpaItemWriter = new JpaItemWriterBuilder<Person>()
        .entityManagerFactory(entityManagerFactory)
        .usePersist(true)//사실, 생성자에 아이디 값이 안들어가면 셀렉 절은 아나가고, 인서트만 나간다.
        .build();
    jpaItemWriter.afterPropertiesSet();

    ItemWriter<Person> logItemWriter = items -> log.info("person size : {} ", items.size());

    CompositeItemWriter<Person> compositeItemWriter = new CompositeItemWriterBuilder<Person>()
        .delegates(jpaItemWriter, logItemWriter)//delegate 순서 중
        .build();

    compositeItemWriter.afterPropertiesSet();

    return compositeItemWriter;

  }


  private FlatFileItemReader<Person> itemReader() throws Exception {

    DefaultLineMapper<Person> lineMapper = new DefaultLineMapper<>();

    //person field 설정 (매핑하기 위해)
    DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
    tokenizer.setNames("name", "age", "address");
    lineMapper.setLineTokenizer(tokenizer);

    lineMapper.setFieldSetMapper(fieldSet -> {
      String name = fieldSet.readString("name");
      String age = fieldSet.readString("age");
      String address = fieldSet.readString("address");

      return new Person(name, age, address);
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

  private ItemWriter<Person> itemWriter() {
    return items -> items.forEach(x -> log.info("저는 {} 입니다.", x.getName()));
  }

//  private ItemWriter<Person> itemWriter() {
//    return items -> log.info(items.stream().map(Person::getName).collect(Collectors.joining(", ")));
//  }

}
