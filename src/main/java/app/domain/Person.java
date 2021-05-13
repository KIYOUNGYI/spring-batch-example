package app.domain;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
//@Table(name = "person",schema = "spring_batch")
public class Person {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;
  private String name;
  private String age;
  private String address;

  public Person(String name, String age, String address) {
    this(0, name, age, address);

  }

  public Person(int id, String name, String age, String address) {
    this.id = id;
    this.name = name;
    this.age = age;
    this.address = address;
  }

  public boolean isNotEmptyName() {
    return Objects.nonNull(this.name) && !name.isEmpty();
  }

  public Person unKnownName() {
    this.name = "UNKNOWN";
    return this;
  }
}
