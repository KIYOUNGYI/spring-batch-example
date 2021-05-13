package app.config.part3Jdbc;

import app.domain.Person;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * <p> 설명 : flatItemReader <- 파일 아이템 읽는 스프링 배치에서 제공하는 것
 * <p> 실행 명령어 $ gradle bootRun --args='--job.name=itemReaderJob3JDBC'
 * <p> 정산 할 때 (ex> b2b) 종종 활용
 *
 * <p> 여기서는 JdbcCursorItemReader 사용
 * <p> 다음은, JdbcPagingItemReader 사용해보기 (개인적으로)
 */

@Slf4j
@RequiredArgsConstructor
@Configuration
public class ItemReaderConfiguration3JDBC {

  private final StepBuilderFactory stepBuilderFactory;
  private final JobBuilderFactory jobBuilderFactory;
  private final DataSource dataSource;


  @Bean
  public Job itemReaderJob3JDBC() throws Exception {
    return this.jobBuilderFactory.get("itemReaderJob3JDBC")
        .incrementer(new RunIdIncrementer())
        .start(jdbcStep())
        .build();
  }

  @Bean
  public Step jdbcStep() throws Exception {
    return stepBuilderFactory.get("jdbcStep")
        .<Person, Person>chunk(10)
        .reader(this.jdbcCursorItemReader())
        .writer(itemWriter())
        .build();

  }

  private JdbcCursorItemReader<Person> jdbcCursorItemReader() {
    return new JdbcCursorItemReaderBuilder<Person>()
        .name("jdbcCursorItemReader")
        .dataSource(dataSource)
        .sql("select id,name,age,address from person")
        //column 인덱스는 0번이 아니라 1번부터 시작함
        .rowMapper((rs, rowNum) -> new Person(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)))
        .build();
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
