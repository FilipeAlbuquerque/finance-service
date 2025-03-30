package com.example.financeservice.controller;

import com.example.financeservice.dto.ClientDTO;
import com.example.financeservice.exception.ResourceNotFoundException;
import com.example.financeservice.service.ClientService;
import com.example.financeservice.security.JwtUtils;
import com.example.financeservice.security.JwtRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientController.class)
@Import({JwtRequestFilter.class})
class ClientControllerTest {

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

  private ClientDTO createdClientDTO;

  @BeforeEach
  void setUp() {
    // Setup user details
    UserDetails userDetails = new User("admin", "admin",
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));

    // Configure userDetailsService mock
    when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails);

    // Create a sample client for testing
    createdClientDTO = ClientDTO.builder()
        .id(1L)
        .name("Integration Test Client")
        .email("integration@test.com")
        .documentNumber("98765432100")
        .phone("5551234567")
        .address("Integration Test Address")
        .build();
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void createAndGetClient() throws Exception {
    // Mock clientService responses
    when(clientService.createClient(any(ClientDTO.class))).thenReturn(createdClientDTO);
    when(clientService.getAllClients()).thenReturn(Arrays.asList(createdClientDTO));

    // Important: This must match the actual implementation - direct ClientDTO return
    when(clientService.getClientById(1L)).thenReturn(createdClientDTO);

    // Updated client
    ClientDTO updatedClientDTO = ClientDTO.builder()
        .id(1L)
        .name("Updated Integration Test Client")
        .email(createdClientDTO.getEmail())
        .documentNumber(createdClientDTO.getDocumentNumber())
        .phone("5559876543")
        .address("Updated Integration Test Address")
        .build();
    when(clientService.updateClient(eq(1L), any(ClientDTO.class))).thenReturn(updatedClientDTO);

    // Mock delete
    doNothing().when(clientService).deleteClient(1L);

    // Test create client
    mockMvc.perform(MockMvcRequestBuilders.post("/clients")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createdClientDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value(createdClientDTO.getName()))
        .andExpect(jsonPath("$.email").value(createdClientDTO.getEmail()));

    // Test get client by id
    mockMvc.perform(MockMvcRequestBuilders.get("/clients/{id}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.name").value(createdClientDTO.getName()))
        .andExpect(jsonPath("$.email").value(createdClientDTO.getEmail()));

    // Test get all clients
    mockMvc.perform(MockMvcRequestBuilders.get("/clients"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$[*].id", hasItem(1)));

    // Test update client
    mockMvc.perform(MockMvcRequestBuilders.put("/clients/{id}", 1L)
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updatedClientDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(updatedClientDTO.getName()))
        .andExpect(jsonPath("$.phone").value(updatedClientDTO.getPhone()))
        .andExpect(jsonPath("$.address").value(updatedClientDTO.getAddress()));

    // For the delete test, setup ResourceNotFoundException when trying to access the deleted client
    doThrow(new ResourceNotFoundException("Client not found with id: 1"))
        .when(clientService).getClientById(1L);

    // Test delete client
    mockMvc.perform(MockMvcRequestBuilders.delete("/clients/{id}", 1L)
            .with(SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isNoContent());

    // Test verify client is deleted - should return 404 Not Found
    mockMvc.perform(MockMvcRequestBuilders.get("/clients/{id}", 1L))
        .andExpect(status().isNotFound());
  }
}