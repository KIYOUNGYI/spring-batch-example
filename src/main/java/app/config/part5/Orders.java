package app.config.part5;

import static javax.persistence.FetchType.LAZY;

import app.config.part4.User;
import java.time.LocalDate;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Orders {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String itemName;

  private int amount;

  private LocalDate createdDate;

  @ManyToOne(fetch = LAZY)
//  @JoinColumn(name = "id")
  private User user;


  @Builder
  private Orders(String itemName, int amount, LocalDate createdDate) {
    this.itemName = itemName;
    this.amount = amount;
    this.createdDate = createdDate;
  }
}
