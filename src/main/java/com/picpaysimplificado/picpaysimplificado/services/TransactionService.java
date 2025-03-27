package com.picpaysimplificado.picpaysimplificado.services;

import com.picpaysimplificado.picpaysimplificado.domain.transaction.Transaction;
import com.picpaysimplificado.picpaysimplificado.domain.user.User;
import com.picpaysimplificado.picpaysimplificado.dtos.TransactionDTO;
import com.picpaysimplificado.picpaysimplificado.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class TransactionService {
    private static final Logger logger = Logger.getLogger(TransactionService.class.getName());

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionRepository repository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private NotificationService notificationService;

    public Transaction createTransaction(TransactionDTO transaction) throws Exception {
        // Recupera o remetente e o destinatário
        User sender = this.userService.findUserById(transaction.senderId());
        User receiver = this.userService.findUserById(transaction.receiverId());

        // Valida a transação
        userService.validateTransaction(sender, transaction.value());

        // Autoriza a transação
        boolean isAuthorized = this.authorizeTransaction(sender, transaction.value());
        if (!isAuthorized) {
            throw new Exception("Transaction not authorized");
        }

        // Cria e salva a transação
        Transaction newTransaction = new Transaction();
        newTransaction.setAmount(transaction.value());
        newTransaction.setSender(sender);
        newTransaction.setReceiver(receiver);
        newTransaction.setTimestamp(LocalDateTime.now());
        repository.save(newTransaction);

        sender.setBalance(sender.getBalance().subtract(transaction.value()));
        receiver.setBalance(receiver.getBalance().add(transaction.value()));

        this.repository.save(newTransaction);
        this.userService.saveUser(sender);
        this.userService.saveUser(receiver);

        this.notificationService.sendNotification(sender, "Transaction completed");
        this.notificationService.sendNotification(receiver, "Transaction received");

        return newTransaction;
    }

    public boolean authorizeTransaction(User sender, BigDecimal value) {
        try {
            ResponseEntity<Map> authorizationResponse = restTemplate.getForEntity("https://util.devi.tools/api/v2/authorize", Map.class);

            if (authorizationResponse.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = authorizationResponse.getBody();
                logger.info("Authorization response: " + responseBody);
                if ("success".equals(responseBody.get("status"))) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                    return (Boolean) data.get("authorization");
                }
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                logger.warning("Authorization failed with status 403");
                return false;
            }
            throw e;
        }
        return false;
    }
}