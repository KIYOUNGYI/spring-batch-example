package app.config.part6;

import app.config.part4.LevelUpJobExecutionListener;
import app.config.part4.User;
import app.config.part4.UserRepository;
import app.config.part5.JobParameterDecide;
import app.config.part5.OrderStatistics;
import app.config.part5_1.SaveUserTasklet;
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
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.step.tasklet.TaskletStep;
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
import org.springframework.core.task.TaskExecutor;


//gradle bootRun --args='-date=2021-05 --job.name=parallelUserJobV2' <- user 저장하는 명령어
@Slf4j
@RequiredArgsConstructor
@Configuration
public class ParallelUserConfigurationV2 {

  private final String JOB_NAME = "parallelUserJobV2";
  private final int CHUNK = 1000;
  private final StepBuilderFactory stepBuilderFactory;
  private final JobBuilderFactory jobBuilderFactory;
  private final UserRepository userRepository;
  private final EntityManagerFactory entityManagerFactory;
  private final DataSource dataSource;
  private final TaskExecutor taskExecutor;

  @Bean(JOB_NAME + "_saveUserFlow")
  public Flow saveUserFlow() {
    TaskletStep saveUserStep = this.stepBuilderFactory.get(JOB_NAME + "_saveUserStep")
        .tasklet(new SaveUserTasklet(userRepository))
        .build();

    return new FlowBuilder<SimpleFlow>(JOB_NAME + "_saveUserFlow").start(saveUserStep).build();
  }


  @Bean(JOB_NAME)
  public Job userJob() throws Exception {
    return this.jobBuilderFactory.get(JOB_NAME)
        .listener(new LevelUpJobExecutionListener(userRepository))//로깅
        .incrementer(new RunIdIncrementer())
        .start(saveUserFlow())//저장하고
        .next(this.splitFlow(null))//등급 올리고
        .build()
        .build();
  }

  //핵심
  @Bean(JOB_NAME + "_splitFlow")
  @JobScope
  public Flow splitFlow(@Value("#{jobParameters[date]}") String date) throws Exception {

    Flow userLevelUpFlow = new FlowBuilder<SimpleFlow>(JOB_NAME + "_userLevelUpFlow")
        .start(userLevelUpManagerStep())
        .build();

    return new FlowBuilder<SimpleFlow>(JOB_NAME + "_splitFlow")
        .split(this.taskExecutor)
        .add(userLevelUpFlow, orderStatisticsFlow(date)).build();
  }

  private Flow orderStatisticsFlow(String date) throws Exception {
    return new FlowBuilder<SimpleFlow>(JOB_NAME + "_orderStatisticsFlow")
        .start(new JobParameterDecide("date"))//파라미터 검사
        .on(JobParameterDecide.CONTINUE.getName())//continue 이면
        .to(this.orderStatisticsStep(date)).build();//통계))
  }

  public Step orderStatisticsStep(@Value("#{jobParameters[date]}") String date) throws Exception {
    return this.stepBuilderFactory.get(JOB_NAME + "_orderStatisticsStep")
        .<OrderStatistics, OrderStatistics>chunk(CHUNK)
        .reader(orderStatisticsItemReader(date))
        .writer(orderStatisticsItemWriter(date))
        .build();
  }

  //reader 에서 읽은 데이터 기준으로 파일을 생성해야 한다.
  private ItemWriter<? super OrderStatistics> orderStatisticsItemWriter(String date) throws Exception {

    YearMonth yearMonth = YearMonth.parse(date);

    String fileName = yearMonth.getYear() + "년_" + yearMonth.getMonth() + "월_일별_주문_금액.csv";

    BeanWrapperFieldExtractor<OrderStatistics> fieldExtractor = new BeanWrapperFieldExtractor<>();
    fieldExtractor.setNames(new String[]{"amount", "date"});

    DelimitedLineAggregator<OrderStatistics> lineAggregator = new DelimitedLineAggregator<>();
    lineAggregator.setDelimiter(",");
    lineAggregator.setFieldExtractor(fieldExtractor);

    FlatFileItemWriter<OrderStatistics> itemWriter = new FlatFileItemWriterBuilder<OrderStatistics>()
        .resource(new FileSystemResource("output/" + fileName))
        .lineAggregator(lineAggregator)
        .name(JOB_NAME + "_orderStatisticsItemWriter")
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
        .name(JOB_NAME + "_orderStatisticsItemReader")
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


  @Bean(JOB_NAME + "_userLevelUpStep")
  public Step userLevelUpStep() throws Exception {
    return stepBuilderFactory.get(JOB_NAME + "_userLevelUpStep")
        .<User, User>chunk(CHUNK)
        .reader(itemReader(null, null))
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

  //itemReader -> 에러남
  //스텝스코프는 프록시로 설정되기 때문에 정확하게 어떤 걸 쓰는지 명시해야 한다.
  @Bean(JOB_NAME + "_itemReader")
  @StepScope
  public JpaPagingItemReader<? extends User> itemReader(
      @Value("#{stepExecutionContext[minId]}") Long minId,
      @Value("#{stepExecutionContext[maxId]}") Long maxId) throws Exception {

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("minId", minId);
    parameters.put("maxId", maxId);

    JpaPagingItemReader<User> itemReader = new JpaPagingItemReaderBuilder<User>()
        .queryString("select u from User u where u.id between :minId and :maxId")
        .parameterValues(parameters)
        .entityManagerFactory(entityManagerFactory)
        .pageSize(CHUNK)
        .name(JOB_NAME + "_userItemReader")
        .build();

    itemReader.afterPropertiesSet();
    return itemReader;
  }

  @Bean(JOB_NAME + "_userLevelUpStep.manager")
  public Step userLevelUpManagerStep() throws Exception {
    return this.stepBuilderFactory.get(JOB_NAME + "_userLevelup.manager")
        .partitioner(JOB_NAME + "_userLevelup", new UserLevelUpPartitioner(userRepository))
        .step(userLevelUpStep())//slave
        .partitionHandler(taskExecutorPartitionHandler())
        .build();
  }

  @Bean(JOB_NAME + "_taskExecutorPartitionHandler")
  public PartitionHandler taskExecutorPartitionHandler() throws Exception {
    TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();

    handler.setStep(userLevelUpStep());
    handler.setGridSize(8);
    return handler;
  }
}
