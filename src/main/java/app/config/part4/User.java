package app.config.part4;

import static javax.persistence.GenerationType.IDENTITY;

import java.time.LocalDate;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
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

  private int totalAmount;

  private LocalDate updatedDate;

  @Builder
  private User(String userName, int totalAmount) {
    this.userName = userName;
    this.totalAmount = totalAmount;
  }

  public boolean availableLevelUp() {

    return Level.availableLevelUp(this.getLevel(), this.getTotalAmount());
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
