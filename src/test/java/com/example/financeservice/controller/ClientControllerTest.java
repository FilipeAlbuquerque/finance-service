package com.example.financeservice.controller;

import com.example.financeservice.dto.ClientDTO;
import com.example.financeservice.dto.auth.AuthRequestDTO;
import com.example.financeservice.dto.auth.AuthResponseDTO;
import com.example.financeservice.repository.ClientRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // Ativa o perfil de teste que usa H2 em vez de PostgreSQL
class ClientControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ClientRepository clientRepository;

  private String jwtToken;

  @BeforeEach
  void setUp() throws Exception {
    // Limpar todos os clientes antes de cada teste
    clientRepository.deleteAll();

    // Login para obter JWT token
    AuthRequestDTO authRequest = new AuthRequestDTO("admin", "admin");

    MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(authRequest)))
        .andExpect(status().isOk())
        .andReturn();

    String responseContent = result.getResponse().getContentAsString();
    if (responseContent.contains("token")) {
      AuthResponseDTO authResponse = objectMapper.readValue(responseContent, AuthResponseDTO.class);
      jwtToken = authResponse.getToken();
    } else {
      // Fallback para quando a resposta não é o formato esperado
      jwtToken = extractTokenFromResponse(responseContent);
    }
  }

  private String extractTokenFromResponse(String response) {
    // Metodo de fallback para extrair token quando a resposta
    // não está no formato AuthResponseDTO
    if (response.contains("token")) {
      int tokenIndex = response.indexOf("token") + 8;
      int endIndex = response.indexOf("\"", tokenIndex);
      if (endIndex > tokenIndex) {
        return response.substring(tokenIndex, endIndex);
      }
    }
    return "dummy-token-for-test";
  }

  @Test
  void createAndGetClient() throws Exception {
    // Create a client
    ClientDTO clientDTO = ClientDTO.builder()
        .name("Integration Test Client")
        .email("integration@test.com")
        .documentNumber("98765432100")
        .phone("5551234567")
        .address("Integration Test Address")
        .build();

    // Create client
    MvcResult createResult = mockMvc.perform(MockMvcRequestBuilders.post("/clients")
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(clientDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value(clientDTO.getName()))
        .andExpect(jsonPath("$.email").value(clientDTO.getEmail()))
        .andReturn();

    ClientDTO createdClient = objectMapper.readValue(
        createResult.getResponse().getContentAsString(), ClientDTO.class);

    // Get client by id
    mockMvc.perform(MockMvcRequestBuilders.get("/clients/{id}", createdClient.getId())
            .header("Authorization", "Bearer " + jwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(createdClient.getId()))
        .andExpect(jsonPath("$.name").value(createdClient.getName()))
        .andExpect(jsonPath("$.email").value(createdClient.getEmail()));

    // Get all clients
    mockMvc.perform(MockMvcRequestBuilders.get("/clients")
            .header("Authorization", "Bearer " + jwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$[*].id", hasItem(createdClient.getId().intValue())));

    // Update client
    ClientDTO updatedClientDTO = ClientDTO.builder()
        .id(createdClient.getId())
        .name("Updated Integration Test Client")
        .email(createdClient.getEmail())
        .documentNumber(createdClient.getDocumentNumber())
        .phone("5559876543")
        .address("Updated Integration Test Address")
        .build();

    mockMvc.perform(MockMvcRequestBuilders.put("/clients/{id}", createdClient.getId())
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updatedClientDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(updatedClientDTO.getName()))
        .andExpect(jsonPath("$.phone").value(updatedClientDTO.getPhone()))
        .andExpect(jsonPath("$.address").value(updatedClientDTO.getAddress()));

    // Delete client
    mockMvc.perform(MockMvcRequestBuilders.delete("/clients/{id}", createdClient.getId())
            .header("Authorization", "Bearer " + jwtToken))
        .andExpect(status().isNoContent());

    // Verify client is deleted
    mockMvc.perform(MockMvcRequestBuilders.get("/clients/{id}", createdClient.getId())
            .header("Authorization", "Bearer " + jwtToken))
        .andExpect(status().isNotFound());
  }
}