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

  private static final String REPOSITORY_NAME = "ClientRepository";
  private static final String CLIENT_NOT_FOUND_ID = "Client not found with id: ";
  private static final String CLIENT_NOT_FOUND_EMAIL = "Client not found with email: ";
  private static final String CLIENT_EMAIL_EXISTS = "Client with email %s already exists";
  private static final String CLIENT_DOCUMENT_EXISTS = "Client with document number %s already exists";
  private static final String CLIENT_NOT_FOUND_FOR_THIS_ID = "Service: Client not found with ID: {}";
  private static final String FIND_BY_ID = "findById";
  private static final String SAVE = "save";
  private static final String DELETE = "delete";
  private static final String RESOURCE_NOT_FOUND = "ResourceNotFoundException";
  private static final String RESOURCE_ALREADY_EXISTS = "ResourceAlreadyExistsException";
  private static final String CREATE_CLIENT = "createClient";
  private static final String UPDATE_CLIENT = "updateClient";
  private static final String DELETE_CLIENT = "deleteClient";

  private final ClientRepository clientRepository;
  private final MetricsService metricsService;

  @Transactional(readOnly = true)
  public List<ClientDTO> getAllClients() {
    log.debug("Service: Getting all clients");

    List<Client> clients = metricsService.recordRepositoryExecutionTime(
        REPOSITORY_NAME, "findAll",
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
        REPOSITORY_NAME, FIND_BY_ID,
        () -> clientRepository.findById(id)
            .orElseThrow(() -> {
              log.error(CLIENT_NOT_FOUND_FOR_THIS_ID, id);
              metricsService.recordExceptionOccurred(RESOURCE_NOT_FOUND, "getClientById");
              return new ResourceNotFoundException(CLIENT_NOT_FOUND_ID + id);
            }));

    log.debug("Service: Found client with ID: {}, name: {}", id, client.getName());
    return convertToDTO(client);
  }

  @Transactional(readOnly = true)
  public ClientDTO getClientByEmail(String email) {
    log.debug("Service: Getting client with email: {}", email);

    Client client = metricsService.recordRepositoryExecutionTime(
        REPOSITORY_NAME, "findByEmail",
        () -> clientRepository.findByEmail(email)
            .orElseThrow(() -> {
              log.error("Service: Client not found with email: {}", email);
              metricsService.recordExceptionOccurred(RESOURCE_NOT_FOUND, "getClientByEmail");
              return new ResourceNotFoundException(CLIENT_NOT_FOUND_EMAIL + email);
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
          REPOSITORY_NAME, "existsByEmail",
          () -> clientRepository.existsByEmail(clientDTO.getEmail()));

      if (emailExists) {
        log.warn("Service: Client with email {} already exists", clientDTO.getEmail());
        metricsService.recordExceptionOccurred(RESOURCE_ALREADY_EXISTS, CREATE_CLIENT);
        throw new ResourceAlreadyExistsException(String.format(CLIENT_EMAIL_EXISTS, clientDTO.getEmail()));
      }

      // Check if client with same document number already exists
      boolean documentExists = metricsService.recordRepositoryExecutionTime(
          REPOSITORY_NAME, "existsByDocumentNumber",
          () -> clientRepository.existsByDocumentNumber(clientDTO.getDocumentNumber()));

      if (documentExists) {
        log.warn("Service: Client with document number {} already exists", clientDTO.getDocumentNumber());
        metricsService.recordExceptionOccurred(RESOURCE_ALREADY_EXISTS, CREATE_CLIENT);
        throw new ResourceAlreadyExistsException(String.format(CLIENT_DOCUMENT_EXISTS, clientDTO.getDocumentNumber()));
      }

      Client client = convertToEntity(clientDTO);
      Client savedClient = metricsService.recordRepositoryExecutionTime(
          REPOSITORY_NAME, SAVE,
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
          REPOSITORY_NAME, FIND_BY_ID,
          () -> clientRepository.findById(id)
              .orElseThrow(() -> {
                log.error(CLIENT_NOT_FOUND_FOR_THIS_ID, id);
                metricsService.recordExceptionOccurred(RESOURCE_NOT_FOUND, UPDATE_CLIENT);
                return new ResourceNotFoundException(CLIENT_NOT_FOUND_ID + id);
              }));

      // Check if email is changed and if it's already in use
      boolean emailChanged = !existingClient.getEmail().equals(clientDTO.getEmail());

      if (emailChanged) {
        boolean emailExists = metricsService.recordRepositoryExecutionTime(
            REPOSITORY_NAME, "existsByEmail",
            () -> clientRepository.existsByEmail(clientDTO.getEmail()));

        if (emailExists) {
          log.warn("Service: Cannot update client - email {} is already in use", clientDTO.getEmail());
          metricsService.recordExceptionOccurred(RESOURCE_ALREADY_EXISTS, UPDATE_CLIENT);
          throw new ResourceAlreadyExistsException(String.format(CLIENT_EMAIL_EXISTS, clientDTO.getEmail()));
        }
      }

      // Check if document number is changed and if it's already in use
      boolean documentChanged = !existingClient.getDocumentNumber().equals(clientDTO.getDocumentNumber());

      if (documentChanged) {
        boolean documentExists = metricsService.recordRepositoryExecutionTime(
            REPOSITORY_NAME, "existsByDocumentNumber",
            () -> clientRepository.existsByDocumentNumber(clientDTO.getDocumentNumber()));

        if (documentExists) {
          log.warn("Service: Cannot update client - document number {} is already in use", clientDTO.getDocumentNumber());
          metricsService.recordExceptionOccurred(RESOURCE_ALREADY_EXISTS, UPDATE_CLIENT);
          throw new ResourceAlreadyExistsException(String.format(CLIENT_DOCUMENT_EXISTS, clientDTO.getDocumentNumber()));
        }
      }

      // Update fields
      existingClient.setName(clientDTO.getName());
      existingClient.setEmail(clientDTO.getEmail());
      existingClient.setDocumentNumber(clientDTO.getDocumentNumber());
      existingClient.setPhone(clientDTO.getPhone());
      existingClient.setAddress(clientDTO.getAddress());

      Client updatedClient = metricsService.recordRepositoryExecutionTime(
          REPOSITORY_NAME, SAVE,
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
          REPOSITORY_NAME, FIND_BY_ID,
          () -> clientRepository.findById(id)
              .orElseThrow(() -> {
                log.error(CLIENT_NOT_FOUND_FOR_THIS_ID, id);
                metricsService.recordExceptionOccurred(RESOURCE_NOT_FOUND, DELETE_CLIENT);
                return new ResourceNotFoundException(CLIENT_NOT_FOUND_ID + id);
              }));

      metricsService.recordRepositoryExecutionTime(
          REPOSITORY_NAME, DELETE,
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
