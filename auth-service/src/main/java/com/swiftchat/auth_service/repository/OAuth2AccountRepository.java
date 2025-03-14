package com.swiftchat.auth_service.repository;

import com.swiftchat.auth_service.model.OAuth2Account;
import com.swiftchat.auth_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OAuth2AccountRepository extends JpaRepository<OAuth2Account, UUID> {

    Optional<OAuth2Account> findByProviderAndProviderId(String provider, String providerId);

    List<OAuth2Account> findAllByUser(User user);

    void deleteAllByUser(User user);

    boolean existsByProviderAndProviderId(String provider, String providerId);
}
