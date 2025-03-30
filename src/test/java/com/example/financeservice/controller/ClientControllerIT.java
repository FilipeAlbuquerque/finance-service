package com.example.financeservice.controller;

import com.example.financeservice.dto.ClientDTO;
import com.example.financeservice.exception.ResourceAlreadyExistsException;
import com.example.financeservice.exception.ResourceNotFoundException;
import com.example.financeservice.repository.ClientRepository;
import com.example.financeservice.security.JwtRequestFilter;
import com.example.financeservice.security.JwtUtils;
import com.example.financeservice.service.ClientService;
import com.example.financeservice.service.MetricsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ClientController.class)
@Import(JwtRequestFilter.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class ClientControllerIT {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ClientService clientService;

  @MockBean
  private UserDetailsService userDetailsService;

  @MockBean
  private JwtUtils jwtUtils;

  @MockBean
  private AuthenticationManager authenticationManager;

  // Mocks adicionais necessários para dependências
  @MockBean
  private ClientRepository clientRepository;

  @MockBean
  private MetricsService metricsService;

  private ClientDTO sampleClient;
  private ClientDTO updatedClient;

  @BeforeEach
  void setUp() {
    // Create a sample client for testing
    sampleClient = ClientDTO.builder()
        .id(1L)
        .name("Test Client")
        .email("test@example.com")
        .documentNumber("12345678900")
        .phone("5551234567")
        .address("Test Address")
        .build();

    // Create an updated client
    updatedClient = ClientDTO.builder()
        .id(1L)
        .name("Updated Client")
        .email("test@example.com")
        .documentNumber("12345678900")
        .phone("5559876543")
        .address("Updated Address")
        .build();
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void getAllClients_ShouldReturnClients() throws Exception {
    // Arrange
    when(clientService.getAllClients()).thenReturn(Arrays.asList(sampleClient));

    // Act & Assert
    mockMvc.perform(get("/clients"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(1L))
        .andExpect(jsonPath("$[0].name").value("Test Client"))
        .andExpect(jsonPath("$[0].email").value("test@example.com"));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void getClientById_ShouldReturnClient() throws Exception {
    // Arrange
    when(clientService.getClientById(1L)).thenReturn(sampleClient);

    // Act & Assert
    mockMvc.perform(get("/clients/{id}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.name").value("Test Client"))
        .andExpect(jsonPath("$.email").value("test@example.com"))
        .andExpect(jsonPath("$.documentNumber").value("12345678900"));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void getClientById_NotFound_ShouldReturn404() throws Exception {
    // Arrange
    when(clientService.getClientById(999L))
        .thenThrow(new ResourceNotFoundException("Client not found with id: 999"));

    // Act & Assert
    mockMvc.perform(get("/clients/{id}", 999L))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void createClient_ShouldCreateAndReturnClient() throws Exception {
    // Arrange
    when(clientService.createClient(any(ClientDTO.class))).thenReturn(sampleClient);

    // Act & Assert
    mockMvc.perform(post("/clients")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(sampleClient)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.name").value("Test Client"))
        .andExpect(jsonPath("$.email").value("test@example.com"));

    verify(clientService, times(1)).createClient(any(ClientDTO.class));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void createClient_WithDuplicateEmail_ShouldReturn409() throws Exception {
    // Arrange
    when(clientService.createClient(any(ClientDTO.class)))
        .thenThrow(new ResourceAlreadyExistsException(
            "Client with email test@example.com already exists"));

    // Act & Assert
    mockMvc.perform(post("/clients")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(sampleClient)))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void updateClient_ShouldUpdateAndReturnClient() throws Exception {
    // Arrange
    when(clientService.updateClient(eq(1L), any(ClientDTO.class))).thenReturn(updatedClient);

    // Act & Assert
    mockMvc.perform(put("/clients/{id}", 1L)
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updatedClient)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.name").value("Updated Client"))
        .andExpect(jsonPath("$.phone").value("5559876543"))
        .andExpect(jsonPath("$.address").value("Updated Address"));

    verify(clientService, times(1)).updateClient(eq(1L), any(ClientDTO.class));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void deleteClient_ShouldDeleteClient() throws Exception {
    // Arrange
    doNothing().when(clientService).deleteClient(1L);

    // Act & Assert
    mockMvc.perform(delete("/clients/{id}", 1L)
            .with(SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isNoContent());

    verify(clientService, times(1)).deleteClient(1L);
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void deleteClient_NotFound_ShouldReturn404() throws Exception {
    // Arrange
    doThrow(new ResourceNotFoundException("Client not found with id: 999"))
        .when(clientService).deleteClient(999L);

    // Act & Assert
    mockMvc.perform(delete("/clients/{id}", 999L)
            .with(SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isNotFound());
  }
}
