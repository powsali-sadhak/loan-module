package com.powsali.loan_module.entity;

import com.powsali.loan_module.entity.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(unique = true)
    private String username;

    // Service classification: HOO=A, DDO=B, employees=A/B/C/D.
    @Column(name = "service_group", length = 1)
    private String serviceGroup;

    @Column(name = "mobile_number", length = 20)
    private String mobileNumber;

    @Column(name = "whatsapp_notifications_enabled", nullable = false, columnDefinition = "boolean default false")
    private boolean whatsappNotificationsEnabled = false;
}
