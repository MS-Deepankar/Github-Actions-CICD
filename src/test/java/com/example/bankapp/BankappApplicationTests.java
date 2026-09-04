package com.example.bankapp;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankappApplicationTests {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void depositRejectsZeroOrNegativeAmounts() {
        Account account = account("alice", "100.00");

        assertThrows(IllegalArgumentException.class,
                () -> accountService.deposit(account, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> accountService.deposit(account, new BigDecimal("-10.00")));

        assertEquals(new BigDecimal("100.00"), account.getBalance());
    }

    @Test
    void transferMovesFundsAndCreatesBothTransactionRecords() {
        Account sender = account("alice", "100.00");
        Account recipient = account("bob", "25.00");
        when(accountRepository.findByUsername("bob")).thenReturn(java.util.Optional.of(recipient));

        accountService.transferAmount(sender, "bob", new BigDecimal("40.00"));

        assertEquals(new BigDecimal("60.00"), sender.getBalance());
        assertEquals(new BigDecimal("65.00"), recipient.getBalance());
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository, times(2)).save(any());
    }

    @Test
    void transferRejectsTheSameAccount() {
        Account sender = account("alice", "100.00");

        assertThrows(IllegalArgumentException.class,
                () -> accountService.transferAmount(sender, "alice", new BigDecimal("10.00")));
    }

    private Account account(String username, String balance) {
        Account account = new Account();
        account.setUsername(username);
        account.setBalance(new BigDecimal(balance));
        return account;
    }
}
