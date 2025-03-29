package com.example.financeservice.controller;

import com.example.financeservice.dto.ClientDTO;
import com.example.financeservice.service.ClientService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
@Slf4j
public class ClientController {

  private final ClientService clientService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_ADMIN')")
  public ResponseEntity<List<ClientDTO>> getAllClients() {
    log.info("API Request: Fetching all clients");

    try {
      List<ClientDTO> clients = clientService.getAllClients();
      log.info("API Response: Retrieved {} clients", clients.size());
      return ResponseEntity.ok(clients);
    } catch (Exception e) {
      log.error("API Error: Failed to retrieve all clients", e);
      throw e;
    }
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_ADMIN')")
  public ResponseEntity<ClientDTO> getClientById(@PathVariable Long id) {
    log.info("API Request: Fetching client with ID: {}", id);

    try {
      ClientDTO client = clientService.getClientById(id);
      log.info("API Response: Retrieved client with ID: {}, email: {}", id, client.getEmail());
      return ResponseEntity.ok(client);
    } catch (Exception e) {
      log.error("API Error: Failed to retrieve client with ID: {}", id, e);
      throw e;
    }
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_ADMIN')")
  public ResponseEntity<ClientDTO> createClient(@Valid @RequestBody ClientDTO clientDTO) {
    log.info("API Request: Creating new client with email: {}", clientDTO.getEmail());

    try {
      ClientDTO createdClient = clientService.createClient(clientDTO);
      log.info("API Response: Successfully created client with ID: {}, email: {}",
          createdClient.getId(), createdClient.getEmail());
      return new ResponseEntity<>(createdClient, HttpStatus.CREATED);
    } catch (Exception e) {
      log.error("API Error: Failed to create client with email: {}", clientDTO.getEmail(), e);
      throw e;
    }
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_ADMIN')")
  public ResponseEntity<ClientDTO> updateClient(@PathVariable Long id,
      @Valid @RequestBody ClientDTO clientDTO) {
    log.info("API Request: Updating client with ID: {}", id);

    try {
      ClientDTO updatedClient = clientService.updateClient(id, clientDTO);
      log.info("API Response: Successfully updated client with ID: {}, email: {}",
          updatedClient.getId(), updatedClient.getEmail());
      return ResponseEntity.ok(updatedClient);
    } catch (Exception e) {
      log.error("API Error: Failed to update client with ID: {}", id, e);
      throw e;
    }
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_ADMIN')")
  public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
    log.info("API Request: Deleting client with ID: {}", id);

    try {
      clientService.deleteClient(id);
      log.info("API Response: Successfully deleted client with ID: {}", id);
      return ResponseEntity.noContent().build();
    } catch (Exception e) {
      log.error("API Error: Failed to delete client with ID: {}", id, e);
      throw e;
    }
  }

  // Metodo auxiliar para logar informações de autenticação
  private void logAuthentication() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null) {
      log.debug("Authentication: User={}, Authenticated={}, Authorities={}",
          auth.getName(), auth.isAuthenticated(), auth.getAuthorities());
    } else {
      log.debug("Authentication: No security context available");
    }
  }
}