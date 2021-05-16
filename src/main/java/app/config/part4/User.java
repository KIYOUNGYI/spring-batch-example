package app.config.part4;

import static javax.persistence.GenerationType.IDENTITY;

import app.config.part5.Orders;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = IDENTITY)
  private Long id;

  private String userName;

  @Enumerated(EnumType.STRING)
  private Level level = Level.NORMAL;

  //  private int totalAmount;
  @OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)//user가 저장되면서 orders 도 저장될 수 있도록 설정
  @JoinColumn(name = "user_id")
  private List<Orders> orders = new ArrayList<>();

  private LocalDate updatedDate;

  @Builder
  private User(String userName, List<Orders> orders) {
    this.userName = userName;
    this.orders = orders;
  }

  public boolean availableLevelUp() {

    return Level.availableLevelUp(this.getLevel(), this.getTotalAmount());
  }

  private int getTotalAmount() {

    return this.orders.stream().mapToInt(Orders::getAmount).sum();
  }

  public Level levelUp() {
    Level nextLevel = Level.getNextLevel(this.getTotalAmount());

    this.level = nextLevel;
    this.updatedDate = LocalDate.now();

    return nextLevel;
  }

  private enum Level {
    VIP(500_000, null),
    GOLD(500_000, VIP),
    SILVER(300_000, GOLD),
    NORMAL(200_000, SILVER);

    private final int nextAmount;
    private final Level nextLevel;

    Level(int nextAmount, Level nextLevel) {
      this.nextLevel = nextLevel;
      this.nextAmount = nextAmount;
    }

    public static boolean availableLevelUp(Level level, int totalAmount) {

      if (Objects.isNull(level)) {
        return false;
      }

      if (Objects.isNull(level.nextLevel)) {
        return false;
      }

      return totalAmount >= level.nextAmount;
    }

    public static Level getNextLevel(int totalAmount) {
      if (totalAmount >= Level.VIP.nextAmount) {
        return VIP;
      }
      if (totalAmount >= Level.GOLD.nextAmount) {
        return GOLD.nextLevel;
      }
      if (totalAmount >= Level.SILVER.nextAmount) {
        return SILVER.nextLevel;
      }
      if (totalAmount >= Level.NORMAL.nextAmount) {
        return NORMAL.nextLevel;
      }
      return NORMAL;
    }
  }
}
