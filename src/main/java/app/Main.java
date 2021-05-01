package app;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableBatchProcessing//batch processing 을 하겠다.
public class Main {

  public static void main(String[] args) {
    SpringApplication.run(Main.class, args);
  }

}
