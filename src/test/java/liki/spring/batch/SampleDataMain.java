package liki.spring.batch;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SampleDataMain {

  public static void main(String[] args) {

    List<String> ageGivenList = Arrays.asList("10", "20", "30", "40","50");
    List<String> cityGivenList = Arrays.asList("서울","대전","대구","부산","경주");
    Random rand = new Random();
    for(int i=1;i<=100;i++){
      int x = rand.nextInt(ageGivenList.size());
      int y = rand.nextInt(cityGivenList.size());
      System.out.println(i+","+"name_"+i+","+ageGivenList.get(x)+","+cityGivenList.get(y));
    }
  }
}
