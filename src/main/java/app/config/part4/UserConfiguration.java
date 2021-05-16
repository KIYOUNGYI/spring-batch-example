package app.config.part4;

import app.config.part5.JobParameterDecide;
import app.config.part5.OrderStatistics;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

//gradle bootRun --args='--job.name=userJob' <- user 저장하는 명령어

//gradle bootRun --args='-date=2021-05 --job.name=userJob' <- user 저장하는 명령어
@Slf4j
@RequiredArgsConstructor
@Configuration
public class UserConfiguration {

  private final int CHUNK = 100;
  private final StepBuilderFactory stepBuilderFactory;
  private final JobBuilderFactory jobBuilderFactory;
  private final UserRepository userRepository;
  private final EntityManagerFactory entityManagerFactory;
  private final DataSource dataSource;

  @Bean
  @JobScope
  public Step orderStatisticsStep(@Value("#{jobParameters[date]}") String date) throws Exception {
    return this.stepBuilderFactory.get("orderStatisticsStep")
        .<OrderStatistics, OrderStatistics>chunk(100)
        .reader(orderStatisticsItemReader(date))
        .writer(orderStatisticsItemWriter(date))
        .build();
  }

  //reader 에서 읽은 데이터 기준으로 파일을 생성해야 한다.
  private ItemWriter<? super OrderStatistics> orderStatisticsItemWriter(String date) throws Exception{

    YearMonth yearMonth = YearMonth.parse(date);

    String fileName = yearMonth.getYear() + "년_" + yearMonth.getMonth()+ "월_일별_주문_금액.csv";

    BeanWrapperFieldExtractor<OrderStatistics> fieldExtractor = new BeanWrapperFieldExtractor<>();
    fieldExtractor.setNames(new String[]{"amount", "date"});

    DelimitedLineAggregator<OrderStatistics> lineAggregator = new DelimitedLineAggregator<>();
    lineAggregator.setDelimiter(",");
    lineAggregator.setFieldExtractor(fieldExtractor);

    FlatFileItemWriter<OrderStatistics> itemWriter = new FlatFileItemWriterBuilder<OrderStatistics>()
        .resource(new FileSystemResource("output/" + fileName))
        .lineAggregator(lineAggregator)
        .name("orderStatisticsItemWriter")
        .encoding("UTF-8")
        .headerCallback(writer -> writer.write("total_amount,date"))
        .build();//header 설정

    itemWriter.afterPropertiesSet();

    return itemWriter;
  }

  //jdbc paging 을 이용해 합계를 구한다.
  private ItemReader<? extends OrderStatistics> orderStatisticsItemReader(String date) throws Exception {

    YearMonth yearMonth = YearMonth.parse(date);

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("startDate", yearMonth.atDay(1));
    parameters.put("endDate", yearMonth.atEndOfMonth());

    Map<String, Order> sortKey = new HashMap<>();
    sortKey.put("created_date", Order.ASCENDING);

    JdbcPagingItemReader<OrderStatistics> itemReader = new JdbcPagingItemReaderBuilder<OrderStatistics>()
        .dataSource(this.dataSource)
        .rowMapper((resultSet, i) -> OrderStatistics.builder()
            .amount(resultSet.getString(1))
            .date(LocalDate.parse(resultSet.getString(2), DateTimeFormatter.ISO_DATE))
            .build())
        .pageSize(CHUNK)
        .name("orderStatisticsItemReader")
        .selectClause("sum(amount),created_date")
        .fromClause("orders")
        .whereClause("created_date >= :startDate and created_date <= :endDate")
        .groupClause("created_date")
        .parameterValues(parameters)
        .sortKeys(sortKey)
        .build();

    itemReader.afterPropertiesSet();

    return itemReader;
  }

  @Bean
  public Job userJob() throws Exception {
    return this.jobBuilderFactory.get("userJob")
        .incrementer(new RunIdIncrementer())
        .start(this.saveUserStep())//저장하고
        .next(this.userLevelUpStep())//등급 올리고
        .listener(new LevelUpJobExecutionListener(userRepository))//로깅
        .next(new JobParameterDecide("date"))//파라미터 검사
        .on(JobParameterDecide.CONTINUE.getName())//continue 이면
        .to(this.orderStatisticsStep(null))//통계)
        .build()
        .build();
  }

  @Bean
  public Step saveUserStep() {
    return this.stepBuilderFactory.get("saveUserStep")
        .tasklet(new SaveUserTasklet(userRepository))
        .build();
  }

  @Bean
  public Step userLevelUpStep() throws Exception {
    return stepBuilderFactory.get("userLevelUpStep")
        .<User, User>chunk(CHUNK)
        .reader(itemReader())
        .processor(itemProcessor())
        .writer(itemWriter())
        .build();
  }

  private ItemWriter<? super User> itemWriter() {
    return users -> {
      users.forEach(x -> {
            x.levelUp();
            userRepository.save(x);
          }
      );
    };
  }

  private ItemProcessor<? super User, ? extends User> itemProcessor() {
    return user -> {
      if (user.availableLevelUp()) {
        return user;
      }
      return null;
    };
  }

  private ItemReader<? extends User> itemReader() throws Exception {
    JpaPagingItemReader<User> itemReader = new JpaPagingItemReaderBuilder<User>()
        .queryString("select u from User u")
        .entityManagerFactory(entityManagerFactory)
        .pageSize(CHUNK)
        .name("userItemReader")
        .build();

    itemReader.afterPropertiesSet();
    return itemReader;
  }
}
