package com.picpaysimplificado.picpaysimplificado.services;

import com.picpaysimplificado.picpaysimplificado.domain.user.User;
import com.picpaysimplificado.picpaysimplificado.domain.user.UserType;
import com.picpaysimplificado.picpaysimplificado.dtos.UserDTO;
import com.picpaysimplificado.picpaysimplificado.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;

@Service
public class UserService {
    @Autowired
    private UserRepository repository;
    public void validateTransaction(User sender, BigDecimal amount) throws Exception{
        if(sender.getUserType() == UserType.MERCHANT){
            throw new Exception("Merchants cannot send money");
        }

        if(sender.getBalance().compareTo(amount) < 0){
            throw new Exception("Insufficient balance");
        }
    }

    public User findUserById(Long id) throws Exception{
        return this.repository.findById(id).orElseThrow(() ->   new Exception("User not found"));
    }
    public void saveUser(User user){
        this.repository.save(user);
    }

    public User createUser(UserDTO data){
        User newUser = new User(data);
        this.saveUser(newUser);
        return newUser;
    }
    public ResponseEntity<List<User>> getAllUsers(){
        List<User> users = this.repository.findAll();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }
}
