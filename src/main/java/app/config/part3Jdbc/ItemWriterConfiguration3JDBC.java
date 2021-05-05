package app.config.part3Jdbc;

import app.config.part3.CustomItemReader;
import app.domain.Person;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * $ gradle bootRun --args='--job.name=jdbcWriteJob'
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
public class ItemWriterConfiguration3JDBC {

  private final StepBuilderFactory stepBuilderFactory;
  private final JobBuilderFactory jobBuilderFactory;
  private final DataSource dataSource;

  @Bean
  public Job jdbcWriteJob() {
    return this.jobBuilderFactory.get("jdbcWriteJob")
        .incrementer(new RunIdIncrementer())
        .start(this.jdbcWriteStep())
        .build();
  }

  private Step jdbcWriteStep() {
    return this.stepBuilderFactory.get("JdbcWriteStep")
        .<Person, Person>chunk(10)
        .reader(itemReader())
        .writer(itemWriter())
        .build();

  }

  private ItemWriter<Person> itemWriter() {
    JdbcBatchItemWriter<Person> itemWriter = new JdbcBatchItemWriterBuilder<Person>().dataSource(dataSource)
        .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
        .sql("insert into person(name,age,address) values(:name, :age, :address)")
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
