package com.example.nidar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;

import java.net.URI;
@Configuration
public class VaultConfig {

    @Value("${vault.uri:http://localhost:8200}")
    private String vaultUri;

    @Value("${vault.token:nidar-vault-token}")
    private String vaultToken;

    @Bean
    public VaultTemplate vaultTemplate() {
        VaultEndpoint endpoint  = VaultEndpoint.from(URI.create(vaultUri));
        ClientAuthentication auth = new TokenAuthentication(vaultToken);
        return new VaultTemplate(endpoint, auth);
    }
}
