package com.powsali.loan_module.repository;

import com.powsali.loan_module.entity.User;
import com.powsali.loan_module.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    List<User> findByRole(Role role);
}
