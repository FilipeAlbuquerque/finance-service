package com.example.financeservice.client;

import com.example.financeservice.dto.ClientDTO;
import com.example.financeservice.exception.ResourceAlreadyExistsException;
import com.example.financeservice.exception.ResourceNotFoundException;
import com.example.financeservice.model.Client;
import com.example.financeservice.repository.ClientRepository;
import com.example.financeservice.service.client.ClientService;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

  @Mock
  private ClientRepository clientRepository;

  @InjectMocks
  private ClientService clientService;

  private Client client;
  private ClientDTO clientDTO;

  @BeforeEach
  void setUp() {
    // Setup test data
    client = new Client();
    client.setId(1L);
    client.setName("Test Client");
    client.setEmail("test@example.com");
    client.setDocumentNumber("12345678900");
    client.setPhone("1234567890");
    client.setAddress("Test Address");

    clientDTO = ClientDTO.builder()
        .id(1L)
        .name("Test Client")
        .email("test@example.com")
        .documentNumber("12345678900")
        .phone("1234567890")
        .address("Test Address")
        .build();
  }

  @Test
  void getAllClients_ShouldReturnListOfClients() {
    // Given
    when(clientRepository.findAll()).thenReturn(Collections.singletonList(client));

    // When
    List<ClientDTO> result = clientService.getAllClients();

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(client.getId(), result.getFirst().getId());
    assertEquals(client.getName(), result.getFirst().getName());
    verify(clientRepository, times(1)).findAll();
  }

  @Test
  void getClientById_WithValidId_ShouldReturnClient() {
    // Given
    when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

    // When
    ClientDTO result = clientService.getClientById(1L);

    // Then
    assertNotNull(result);
    assertEquals(client.getId(), result.getId());
    assertEquals(client.getName(), result.getName());
    verify(clientRepository, times(1)).findById(1L);
  }

  @Test
  void getClientById_WithInvalidId_ShouldThrowException() {
    // Given
    when(clientRepository.findById(999L)).thenReturn(Optional.empty());

    // When & Then
    assertThrows(ResourceNotFoundException.class, () -> clientService.getClientById(999L));
    verify(clientRepository, times(1)).findById(999L);
  }

  @Test
  void createClient_WithValidData_ShouldReturnCreatedClient() {
    // Given
    when(clientRepository.existsByEmail(clientDTO.getEmail())).thenReturn(false);
    when(clientRepository.existsByDocumentNumber(clientDTO.getDocumentNumber())).thenReturn(false);
    when(clientRepository.save(any(Client.class))).thenReturn(client);

    // When
    ClientDTO result = clientService.createClient(clientDTO);

    // Then
    assertNotNull(result);
    assertEquals(client.getId(), result.getId());
    assertEquals(client.getName(), result.getName());
    verify(clientRepository, times(1)).existsByEmail(clientDTO.getEmail());
    verify(clientRepository, times(1)).existsByDocumentNumber(clientDTO.getDocumentNumber());
    verify(clientRepository, times(1)).save(any(Client.class));
  }

  @Test
  void createClient_WithExistingEmail_ShouldThrowException() {
    // Given
    when(clientRepository.existsByEmail(clientDTO.getEmail())).thenReturn(true);

    // When & Then
    assertThrows(ResourceAlreadyExistsException.class, () -> clientService.createClient(clientDTO));
    verify(clientRepository, times(1)).existsByEmail(clientDTO.getEmail());
    verify(clientRepository, never()).save(any(Client.class));
  }

  @Test
  void updateClient_WithValidData_ShouldReturnUpdatedClient() {
    // Given
    when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
    when(clientRepository.save(any(Client.class))).thenReturn(client);

    // When
    ClientDTO updatedClientDTO = ClientDTO.builder()
        .id(1L)
        .name("Updated Client")
        .email("test@example.com")
        .documentNumber("12345678900")
        .phone("9876543210")
        .address("Updated Address")
        .build();

    ClientDTO result = clientService.updateClient(1L, updatedClientDTO);

    // Then
    assertNotNull(result);
    assertEquals(client.getId(), result.getId());
    assertEquals("Updated Client", client.getName());
    assertEquals("9876543210", client.getPhone());
    assertEquals("Updated Address", client.getAddress());
    verify(clientRepository, times(1)).findById(1L);
    verify(clientRepository, times(1)).save(any(Client.class));
  }

  @Test
  void deleteClient_WithValidId_ShouldDeleteClient() {
    // Given
    when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
    doNothing().when(clientRepository).delete(client);

    // When
    clientService.deleteClient(1L);

    // Then
    verify(clientRepository, times(1)).findById(1L);
    verify(clientRepository, times(1)).delete(client);
  }

  @Test
  void deleteClient_WithInvalidId_ShouldThrowException() {
    // Given
    when(clientRepository.findById(999L)).thenReturn(Optional.empty());

    // When & Then
    assertThrows(ResourceNotFoundException.class, () -> clientService.deleteClient(999L));
    verify(clientRepository, times(1)).findById(999L);
    verify(clientRepository, never()).delete(any(Client.class));
  }
}
