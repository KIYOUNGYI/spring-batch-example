package app.config.part4;

import java.time.LocalDate;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Collection<User> findAllByUpdatedDate(LocalDate updatedDate);

  @Query(value = "select min(u.id) from User u")
  long findMinId();

  @Query(value = "select max(u.id) from User u")
  long findMaxId();
}
