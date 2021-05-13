package app.config.part3jpa;

import app.domain.Person;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p> 설명 : flatItemReader <- 파일 아이템 읽는 스프링 배치에서 제공하는 것
 * <p> 실행 명령어 $ gradle bootRun --args='--job.name=itemReaderJob3JPA'
 * <p> 정산 할 때 (ex> b2b) 종종 활용
 *
 * <p> 여기서는 JdbcCursorItemReader 사용
 * <p> 다음은, JdbcPagingItemReader 사용해보기 (개인적으로)
 */

@Slf4j
@RequiredArgsConstructor
@Configuration
public class ItemReaderConfiguration3JPA {

  private final StepBuilderFactory stepBuilderFactory;
  private final JobBuilderFactory jobBuilderFactory;
  private final EntityManagerFactory entityManagerFactory;

  @Bean
  public Job itemReaderJob3JPA() throws Exception {
    return this.jobBuilderFactory.get("itemReaderJob3JPA")
        .incrementer(new RunIdIncrementer())
        .start(jpaStep())
        .build();
  }

  @Bean
  public Step jpaStep() throws Exception {
    return stepBuilderFactory.get("jpaStep")
        .<Person, Person>chunk(10)
        .reader(jpaCursorItemReader())
        .writer(this.itemWriter())
        .build();

  }

  private JpaCursorItemReader<Person> jpaCursorItemReader() throws Exception {

    JpaCursorItemReader<Person> itemReader = new JpaCursorItemReaderBuilder<Person>()
        .name("jpaCursorItemReader")
        .entityManagerFactory(entityManagerFactory)
        .queryString("select p from Person p")
        .build();

    itemReader.afterPropertiesSet();
    return itemReader;
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
