package app.config.part3jpa;

import app.config.part3.CustomItemReader;
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
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p> 설명 : flatItemReader <- 파일 아이템 읽는 스프링 배치에서 제공하는 것
 * <p> 실행 명령어 $ gradle bootRun --args='--job.name=itemWriterJob3JPA'
 *
 * <p> 여기서는 JdbcCursorItemReader 사용
 * <p> 다음은, JdbcPagingItemReader 사용해보기 (개인적으로)
 */

@Slf4j
@RequiredArgsConstructor
@Configuration
public class ItemWriterConfiguration3JPA {

  private final StepBuilderFactory stepBuilderFactory;
  private final JobBuilderFactory jobBuilderFactory;
  private final EntityManagerFactory entityManagerFactory;

  @Bean
  public Job itemWriterJob3JPA() throws Exception {
    return this.jobBuilderFactory.get("itemWriterJob3JPA")
        .incrementer(new RunIdIncrementer())
        .start(jpaItemWriterStep())
        .build();
  }

  @Bean
  public Step jpaItemWriterStep() throws Exception {
    return stepBuilderFactory.get("jpaStep")
        .<Person, Person>chunk(10)
        .reader(itemReader())
        .writer(jpaItemWriter())
        .build();

  }

  private ItemWriter<Person> jpaItemWriter() throws Exception {

    JpaItemWriter<Person> itemWriter = new JpaItemWriterBuilder<Person>()
        .entityManagerFactory(entityManagerFactory)
        //default -> merge
        .usePersist(true)//사실, 생성자에 아이디 값이 안들어가면 셀렉 절은 아나가고, 인서트만 나간다.
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
//      items.add(new Person( i+1,"test read name " + i, "test read age", "test read address"));
      items.add(new Person( "zz read name " + i, "zz read age", "zz read address"));
    }
    return items;
  }
}
