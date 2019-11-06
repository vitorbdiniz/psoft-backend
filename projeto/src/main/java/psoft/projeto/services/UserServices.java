package psoft.projeto.services;


import org.springframework.stereotype.Service;
import psoft.projeto.entities.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Service
public class UserServices {
  private Map<String, User> users;

  public UserServices() {
    this.users = new HashMap<>();
  }

  public Map<String, User> getUsers() {
    return users;
  }

  public void insertUser(String firstName, String lastName, String email, long cartao, String senha) {
    if (!this.users.containsKey(email)) {
      User user = new User(firstName, lastName, email, cartao, senha);
      users.put(email, user);
      String msg = "Seja bem vindo(a) ao AJuDE!!";
      this.sendEmail(user.getEmail(), msg);
    }
  }

  public void insertUser(User user) {
    if (!this.users.containsKey(user.getEmail())) {
      users.put(user.getEmail(), user);
      String msg = "Seja bem vindo(a) ao AJuDE!!";
      this.sendEmail(user.getEmail(), msg);
    }
  }



  private void sendEmail(String email, String message) {



  }


  public Optional<User> getUser(String email) {
    return Optional.ofNullable(this.users.get(email));
  }
}
