package com.example.financeservice.service;

import com.example.financeservice.dto.ClientDTO;
import com.example.financeservice.exception.ResourceAlreadyExistsException;
import com.example.financeservice.exception.ResourceNotFoundException;
import com.example.financeservice.model.Client;
import com.example.financeservice.repository.ClientRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService {

  private final ClientRepository clientRepository;
  private final MetricsService metricsService;

  @Transactional(readOnly = true)
  public List<ClientDTO> getAllClients() {
    log.debug("Service: Getting all clients");

    List<Client> clients = metricsService.recordRepositoryExecutionTime(
        "ClientRepository", "findAll",
        clientRepository::findAll);

    log.debug("Service: Found {} clients in database", clients.size());

    return clients.stream()
        .map(this::convertToDTO)
        .toList();
  }

  @Transactional(readOnly = true)
  public ClientDTO getClientById(Long id) {
    log.debug("Service: Getting client with ID: {}", id);

    Client client = metricsService.recordRepositoryExecutionTime(
        "ClientRepository", "findById",
        () -> clientRepository.findById(id)
            .orElseThrow(() -> {
              log.error("Service: Client not found with ID: {}", id);
              metricsService.recordExceptionOccurred("ResourceNotFoundException", "getClientById");
              return new ResourceNotFoundException("Client not found with id: " + id);
            }));

    log.debug("Service: Found client with ID: {}, name: {}", id, client.getName());
    return convertToDTO(client);
  }

  @Transactional(readOnly = true)
  public ClientDTO getClientByEmail(String email) {
    log.debug("Service: Getting client with email: {}", email);

    Client client = metricsService.recordRepositoryExecutionTime(
        "ClientRepository", "findByEmail",
        () -> clientRepository.findByEmail(email)
            .orElseThrow(() -> {
              log.error("Service: Client not found with email: {}", email);
              metricsService.recordExceptionOccurred("ResourceNotFoundException", "getClientByEmail");
              return new ResourceNotFoundException("Client not found with email: " + email);
            }));

    log.debug("Service: Found client with email: {}, name: {}", email, client.getName());
    return convertToDTO(client);
  }

  @Transactional
  public ClientDTO createClient(ClientDTO clientDTO) {
    log.debug("Service: Creating new client with email: {}", clientDTO.getEmail());

    // Timer para medir o tempo de criação do cliente
    var timer = metricsService.startTimer();

    try {
      // Check if client with same email already exists
      boolean emailExists = metricsService.recordRepositoryExecutionTime(
          "ClientRepository", "existsByEmail",
          () -> clientRepository.existsByEmail(clientDTO.getEmail()));

      if (emailExists) {
        log.warn("Service: Client with email {} already exists", clientDTO.getEmail());
        metricsService.recordExceptionOccurred("ResourceAlreadyExistsException", "createClient");
        throw new ResourceAlreadyExistsException("Client with email " + clientDTO.getEmail() + " already exists");
      }

      // Check if client with same document number already exists
      boolean documentExists = metricsService.recordRepositoryExecutionTime(
          "ClientRepository", "existsByDocumentNumber",
          () -> clientRepository.existsByDocumentNumber(clientDTO.getDocumentNumber()));

      if (documentExists) {
        log.warn("Service: Client with document number {} already exists", clientDTO.getDocumentNumber());
        metricsService.recordExceptionOccurred("ResourceAlreadyExistsException", "createClient");
        throw new ResourceAlreadyExistsException("Client with document number " + clientDTO.getDocumentNumber() + " already exists");
      }

      Client client = convertToEntity(clientDTO);
      Client savedClient = metricsService.recordRepositoryExecutionTime(
          "ClientRepository", "save",
          () -> clientRepository.save(client));

      log.info("Service: Client created successfully with ID: {}, email: {}", savedClient.getId(), savedClient.getEmail());

      // Registrar métrica para criação de cliente
      metricsService.recordClientCreated();

      return convertToDTO(savedClient);
    } finally {
      // Parar o timer e registrar o tempo total da operação
      metricsService.stopTimer(timer, "finance.clients.creation.time");
    }
  }

  @Transactional
  public ClientDTO updateClient(Long id, ClientDTO clientDTO) {
    log.debug("Service: Updating client with ID: {}", id);

    var timer = metricsService.startTimer();

    try {
      Client existingClient = metricsService.recordRepositoryExecutionTime(
          "ClientRepository", "findById",
          () -> clientRepository.findById(id)
              .orElseThrow(() -> {
                log.error("Service: Client not found with ID: {}", id);
                metricsService.recordExceptionOccurred("ResourceNotFoundException", "updateClient");
                return new ResourceNotFoundException("Client not found with id: " + id);
              }));

      // Check if email is changed and if it's already in use
      if (!existingClient.getEmail().equals(clientDTO.getEmail()) &&
          metricsService.recordRepositoryExecutionTime(
              "ClientRepository", "existsByEmail",
              () -> clientRepository.existsByEmail(clientDTO.getEmail()))) {
        log.warn("Service: Cannot update client - email {} is already in use", clientDTO.getEmail());
        metricsService.recordExceptionOccurred("ResourceAlreadyExistsException", "updateClient");
        throw new ResourceAlreadyExistsException("Client with email " + clientDTO.getEmail() + " already exists");
      }

      // Check if document number is changed and if it's already in use
      if (!existingClient.getDocumentNumber().equals(clientDTO.getDocumentNumber()) &&
          metricsService.recordRepositoryExecutionTime(
              "ClientRepository", "existsByDocumentNumber",
              () -> clientRepository.existsByDocumentNumber(clientDTO.getDocumentNumber()))) {
        log.warn("Service: Cannot update client - document number {} is already in use", clientDTO.getDocumentNumber());
        metricsService.recordExceptionOccurred("ResourceAlreadyExistsException", "updateClient");
        throw new ResourceAlreadyExistsException("Client with document number " + clientDTO.getDocumentNumber() + " already exists");
      }

      // Update fields
      existingClient.setName(clientDTO.getName());
      existingClient.setEmail(clientDTO.getEmail());
      existingClient.setDocumentNumber(clientDTO.getDocumentNumber());
      existingClient.setPhone(clientDTO.getPhone());
      existingClient.setAddress(clientDTO.getAddress());

      Client updatedClient = metricsService.recordRepositoryExecutionTime(
          "ClientRepository", "save",
          () -> clientRepository.save(existingClient));

      log.info("Service: Client updated successfully with ID: {}", updatedClient.getId());

      // Registrar métrica para atualização de cliente
      metricsService.recordClientUpdated();

      return convertToDTO(updatedClient);
    } finally {
      // Parar o timer e registrar o tempo total da operação
      metricsService.stopTimer(timer, "finance.clients.update.time");
    }
  }

  @Transactional
  public void deleteClient(Long id) {
    log.debug("Service: Deleting client with ID: {}", id);

    var timer = metricsService.startTimer();

    try {
      Client client = metricsService.recordRepositoryExecutionTime(
          "ClientRepository", "findById",
          () -> clientRepository.findById(id)
              .orElseThrow(() -> {
                log.error("Service: Client not found with ID: {}", id);
                metricsService.recordExceptionOccurred("ResourceNotFoundException", "deleteClient");
                return new ResourceNotFoundException("Client not found with id: " + id);
              }));

      metricsService.recordRepositoryExecutionTime(
          "ClientRepository", "delete",
          () -> {
            clientRepository.delete(client);
            return null;
          });

      log.info("Service: Client deleted successfully with ID: {}", id);

      // Registrar métrica para exclusão de cliente
      metricsService.recordClientDeleted();
    } finally {
      // Parar o timer e registrar o tempo total da operação
      metricsService.stopTimer(timer, "finance.clients.deletion.time");
    }
  }

  // Helper methods for DTO conversion
  private ClientDTO convertToDTO(Client client) {
    return ClientDTO.builder()
        .id(client.getId())
        .name(client.getName())
        .email(client.getEmail())
        .documentNumber(client.getDocumentNumber())
        .phone(client.getPhone())
        .address(client.getAddress())
        .build();
  }

  private Client convertToEntity(ClientDTO clientDTO) {
    Client client = new Client();
    client.setName(clientDTO.getName());
    client.setEmail(clientDTO.getEmail());
    client.setDocumentNumber(clientDTO.getDocumentNumber());
    client.setPhone(clientDTO.getPhone());
    client.setAddress(clientDTO.getAddress());
    return client;
  }
}