package com.example.financeservice.controller;

import com.example.financeservice.dto.ClientDTO;
import com.example.financeservice.service.client.ClientService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class ClientController {

  private static final Logger logger = LoggerFactory.getLogger(ClientController.class);

  private final ClientService clientService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<ClientDTO>> getAllClients() {
    logAuthentication("getAllClients");
    return ResponseEntity.ok(clientService.getAllClients());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ClientDTO> getClientById(@PathVariable Long id) {
    logAuthentication("getClientById");
    return ResponseEntity.ok(clientService.getClientById(id));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ClientDTO> createClient(@Valid @RequestBody ClientDTO clientDTO) {
    logAuthentication("createClient");
    ClientDTO createdClient = clientService.createClient(clientDTO);
    return new ResponseEntity<>(createdClient, HttpStatus.CREATED);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ClientDTO> updateClient(@PathVariable Long id,
      @Valid @RequestBody ClientDTO clientDTO) {
    logAuthentication("updateClient");
    return ResponseEntity.ok(clientService.updateClient(id, clientDTO));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
    logAuthentication("deleteClient");
    clientService.deleteClient(id);
    return ResponseEntity.noContent().build();
  }

  // Metodo auxiliar para logar informações da autenticação para debug
  private void logAuthentication(String method) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null) {
      logger.debug("{} - User: {}, Authenticated: {}, Authorities: {}",
          method, auth.getName(), auth.isAuthenticated(), auth.getAuthorities());
    } else {
      logger.debug("{} - No authentication found in SecurityContext", method);
    }
  }
}