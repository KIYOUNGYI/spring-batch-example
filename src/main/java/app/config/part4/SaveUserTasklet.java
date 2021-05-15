package app.config.part4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
@RequiredArgsConstructor
public class SaveUserTasklet implements Tasklet {

  private final UserRepository userRepository;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
    List<User> users = createUsers();

    Collections.shuffle(users);

    userRepository.saveAll(users);

    return RepeatStatus.FINISHED;
  }

  //300명이 승급 대상
  private List<User> createUsers() {
    List<User> users = new ArrayList<>();

    for (int i = 0; i < 100; i++) {
      users.add(User.builder()
          .userName("test username " + i)
          .totalAmount(1_000)
          .build()
      );
    }

    for (int i = 100; i < 200; i++) {
      users.add(User.builder()
          .userName("test username " + i)
          .totalAmount(200_000)//silver
          .build()
      );
    }

    for (int i = 200; i < 300; i++) {
      users.add(User.builder()
          .userName("test username " + i)
          .totalAmount(300_000)//gold
          .build()
      );
    }

    for (int i = 300; i < 400; i++) {
      users.add(User.builder()
          .userName("test username " + i)
          .totalAmount(500_000)//gold
          .build()
      );
    }

    return users;
  }
}
