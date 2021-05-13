package app.config.part4;

import static javax.persistence.GenerationType.IDENTITY;

import java.time.LocalDate;
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
  }
}
