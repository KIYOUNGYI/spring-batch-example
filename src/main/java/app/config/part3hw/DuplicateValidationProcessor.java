package app.config.part3hw;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

@RequiredArgsConstructor
public class DuplicateValidationProcessor<T> implements ItemProcessor<T, T> {

  private final Map<String, Object> keyPool = new ConcurrentHashMap<>();
  private final Function<T, String> keyExtractor;
  private final boolean allowDuplicate;


  @Override
  public T process(T item) throws Exception {

    if (allowDuplicate) {
      return item;
    }
    String key = keyExtractor.apply(item);
    if (keyPool.containsKey(key)) {
      return null;
    }
    keyPool.put(key, key);//다음 check 를 위해서 save

    return item;
  }
}
