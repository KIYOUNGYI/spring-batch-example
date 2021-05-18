package app.config.part6;

import app.config.part4.UserRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

@RequiredArgsConstructor
public class UserLevelUpPartitioner implements Partitioner {

  private final UserRepository userRepository;

  @Override
  public Map<String, ExecutionContext> partition(int gridSize) {
    long minId = userRepository.findMinId();//1
    long maxId = userRepository.findMaxId();//40_000

    long targetSize = (maxId - minId) / gridSize + 1;

    /**
     * partition0 : 1,5000
     * partition1 : 5001,10000
     * partition2 : 10001,15000
     * partition7 : 35001,40000
     */
    Map<String, ExecutionContext> result = new HashMap<>();

    long number = 0;
    long start = minId;
    long end = start + targetSize - 1;

    while (start <= maxId) {
      ExecutionContext value = new ExecutionContext();

      result.put("partition" + number, value);

      if (end >= maxId) {
        end = maxId;
      }

      value.putLong("minId", start);
      value.putLong("maxId", end);

      start += targetSize;
      end += targetSize;
      number++;
    }

    return result;
  }
}