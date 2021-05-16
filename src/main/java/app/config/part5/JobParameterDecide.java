package app.config.part5;

import io.micrometer.core.instrument.util.StringUtils;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;

public class JobParameterDecide implements JobExecutionDecider {

  public static final FlowExecutionStatus CONTINUE = new FlowExecutionStatus("CONTINUE");

  private final String key;

  public JobParameterDecide(String key) {
    this.key = key;
  }

  // 키가 있으면 continue 없으면, completed 리턴
  @Override
  public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
    String value = jobExecution.getJobParameters().getString(key);
    System.out.println("value = " + value);
    if(StringUtils.isEmpty(value)){
      return FlowExecutionStatus.COMPLETED;
    }
    System.out.println("continue condition!!");
    return CONTINUE;// to 메소드 실행
  }
}
