package com.example.financeservice.service;

import com.example.financeservice.dto.ClientDTO;
import com.example.financeservice.exception.ResourceAlreadyExistsException;
import com.example.financeservice.exception.ResourceNotFoundException;
import com.example.financeservice.model.Client;
import com.example.financeservice.repository.ClientRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClientService {

  private final ClientRepository clientRepository;

  @Transactional(readOnly = true)
  public List<ClientDTO> getAllClients() {
    return clientRepository.findAll().stream()
        .map(this::convertToDTO)
        .toList();
  }

  @Transactional(readOnly = true)
  public ClientDTO getClientById(Long id) {
    Client client = clientRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));
    return convertToDTO(client);
  }

  @Transactional(readOnly = true)
  public ClientDTO getClientByEmail(String email) {
    Client client = clientRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("Client not found with email: " + email));
    return convertToDTO(client);
  }

  @Transactional
  public ClientDTO createClient(ClientDTO clientDTO) {
    // Check if client with same email or document already exists
    if (clientRepository.existsByEmail(clientDTO.getEmail())) {
      throw new ResourceAlreadyExistsException("Client with email " + clientDTO.getEmail() + " already exists");
    }

    if (clientRepository.existsByDocumentNumber(clientDTO.getDocumentNumber())) {
      throw new ResourceAlreadyExistsException("Client with document number " + clientDTO.getDocumentNumber() + " already exists");
    }

    Client client = convertToEntity(clientDTO);
    Client savedClient = clientRepository.save(client);
    return convertToDTO(savedClient);
  }

  @Transactional
  public ClientDTO updateClient(Long id, ClientDTO clientDTO) {
    Client existingClient = clientRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));

    // Check if email is changed and if it's already in use
    if (!existingClient.getEmail().equals(clientDTO.getEmail()) && clientRepository.existsByEmail(clientDTO.getEmail())) {
      throw new ResourceAlreadyExistsException("Client with email " + clientDTO.getEmail() + " already exists");
    }

    // Check if document number is changed and if it's already in use
    if (!existingClient.getDocumentNumber().equals(clientDTO.getDocumentNumber()) &&
        clientRepository.existsByDocumentNumber(clientDTO.getDocumentNumber())) {
      throw new ResourceAlreadyExistsException("Client with document number " + clientDTO.getDocumentNumber() + " already exists");
    }

    // Update fields
    existingClient.setName(clientDTO.getName());
    existingClient.setEmail(clientDTO.getEmail());
    existingClient.setDocumentNumber(clientDTO.getDocumentNumber());
    existingClient.setPhone(clientDTO.getPhone());
    existingClient.setAddress(clientDTO.getAddress());

    Client updatedClient = clientRepository.save(existingClient);
    return convertToDTO(updatedClient);
  }

  @Transactional
  public void deleteClient(Long id) {
    Client client = clientRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));

    clientRepository.delete(client);
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
