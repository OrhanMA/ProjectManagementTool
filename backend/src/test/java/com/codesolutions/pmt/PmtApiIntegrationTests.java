package com.codesolutions.pmt;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PmtApiIntegrationTests {
  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private JavaMailSender mailSender;

  @Test
  void completeProjectAndTaskWorkflow() throws Exception {
    mockMimeMessages();
    Session admin = register("admin.it", "admin.it@pmt.local");
    Session member = register("member.it", "member.it@pmt.local");
    Session observer = register("observer.it", "observer.it@pmt.local");

    MvcResult projectResult =
        mockMvc
            .perform(
                post("/api/v1/projects")
                    .header("Authorization", admin.bearer())
                    .contentType("application/json")
                    .content(
                        json(
                            Map.of(
                                "name",
                                "Projet integration",
                                "description",
                                "Projet couvert par un test d integration.",
                                "startDate",
                                LocalDate.now().toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Projet integration"))
            .andReturn();
    String projectId = json(projectResult).get("id").asText();

    mockMvc
        .perform(get("/api/v1/projects").header("Authorization", admin.bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(projectId));

    mockMvc
        .perform(get("/api/v1/projects/" + projectId).header("Authorization", admin.bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("Projet couvert par un test d integration."));

    addMember(projectId, admin, member.email(), "MEMBER");
    addMember(projectId, admin, observer.email(), "OBSERVER");

    mockMvc
        .perform(
            patch("/api/v1/projects/" + projectId + "/members/" + observer.userId() + "/role")
                .header("Authorization", admin.bearer())
                .contentType("application/json")
                .content(json(Map.of("role", "OBSERVER"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("OBSERVER"));

    mockMvc
        .perform(
            get("/api/v1/projects/" + projectId + "/members")
                .header("Authorization", admin.bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3));

    MvcResult taskResult =
        mockMvc
            .perform(
                post("/api/v1/projects/" + projectId + "/tasks")
                    .header("Authorization", admin.bearer())
                    .contentType("application/json")
                    .content(
                        json(
                            Map.of(
                                "name",
                                "Livrer le kanban",
                                "description",
                                "Construire le dashboard par statut.",
                                "dueDate",
                                LocalDate.now().plusDays(7).toString(),
                                "priority",
                                "HIGH",
                                "assigneeId",
                                member.userId()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("BACKLOG"))
            .andExpect(jsonPath("$.assignee.email").value(member.email()))
            .andReturn();
    String taskId = json(taskResult).get("id").asText();

    verify(mailSender).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));

    mockMvc
        .perform(
            get("/api/v1/projects/" + projectId + "/tasks")
                .header("Authorization", member.bearer())
                .queryParam("status", "BACKLOG"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(taskId));

    mockMvc
        .perform(
            get("/api/v1/projects/" + projectId + "/tasks/" + taskId)
                .header("Authorization", observer.bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Livrer le kanban"));

    mockMvc
        .perform(
            patch("/api/v1/projects/" + projectId + "/tasks/" + taskId)
                .header("Authorization", member.bearer())
                .contentType("application/json")
                .content(
                    json(
                        Map.of(
                            "name",
                            "Livrer le kanban PMT",
                            "description",
                            "Construire et valider le dashboard.",
                            "dueDate",
                            LocalDate.now().plusDays(8).toString(),
                            "endDate",
                            LocalDate.now().plusDays(9).toString(),
                            "priority",
                            "URGENT",
                            "status",
                            "DONE"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DONE"))
        .andExpect(jsonPath("$.priority").value("URGENT"));

    mockMvc
        .perform(
            patch("/api/v1/projects/" + projectId + "/tasks/" + taskId + "/assignee")
                .header("Authorization", admin.bearer())
                .contentType("application/json")
                .content(json(Map.of("assigneeId", observer.userId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.assignee.email").value(observer.email()));

    verify(mailSender, times(2)).createMimeMessage();
    verify(mailSender, times(2)).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));

    mockMvc
        .perform(
            get("/api/v1/projects/" + projectId + "/tasks/" + taskId + "/history")
                .header("Authorization", observer.bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").isNumber());

    mockMvc
        .perform(
            post("/api/v1/projects/" + projectId + "/tasks")
                .header("Authorization", observer.bearer())
                .contentType("application/json")
                .content(
                    json(
                        Map.of(
                            "name",
                            "Interdit",
                            "description",
                            "Observer ne peut pas creer.",
                            "dueDate",
                            LocalDate.now().plusDays(1).toString(),
                            "priority",
                            "LOW"))))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(post("/api/v1/auth/refresh").cookie(admin.refreshCookie()))
        .andExpect(status().isOk())
        .andExpect(cookie().exists("pmt_refresh_token"));

    mockMvc
        .perform(post("/api/v1/auth/logout").cookie(member.refreshCookie()))
        .andExpect(status().isOk());
    verifyNoMoreInteractions(mailSender);
  }

  @Test
  void validationAuthenticationAndPermissionErrorsRemainPredictable() throws Exception {
    mockMimeMessages();
    Session admin = register("admin.errors", "admin.errors@pmt.local");
    Session member = register("member.errors", "member.errors@pmt.local");

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType("application/json")
                .content(json(Map.of("email", admin.email(), "password", "Password123!"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.email").value(admin.email()));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType("application/json")
                .content(json(Map.of("email", admin.email(), "password", "Wrong123!"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Identifiants invalides."));

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType("application/json")
                .content(
                    json(
                        Map.of(
                            "username",
                            "weak.password",
                            "email",
                            "weak.password@pmt.local",
                            "password",
                            "short"))))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType("application/json")
                .content(
                    json(
                        Map.of(
                            "username",
                            "admin.errors.duplicate",
                            "email",
                            admin.email(),
                            "password",
                            "Password123!"))))
        .andExpect(status().isConflict());

    mockMvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isBadRequest());
    mockMvc
        .perform(post("/api/v1/auth/refresh").cookie(new Cookie("pmt_refresh_token", "invalid")))
        .andExpect(status().isBadRequest());
    mockMvc.perform(get("/api/v1/projects")).andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/v1/projects")
                .header("Authorization", admin.bearer())
                .contentType("application/json")
                .content(
                    json(
                        Map.of(
                            "name",
                            "",
                            "description",
                            "",
                            "startDate",
                            LocalDate.now().toString()))))
        .andExpect(status().isBadRequest());

    String projectId =
        json(mockMvc
                .perform(
                    post("/api/v1/projects")
                        .header("Authorization", admin.bearer())
                        .contentType("application/json")
                        .content(
                            json(
                                Map.of(
                                    "name",
                                    "Projet erreurs",
                                    "description",
                                    "Projet pour couvrir les erreurs.",
                                    "startDate",
                                    LocalDate.now().toString()))))
                .andExpect(status().isOk())
                .andReturn())
            .get("id")
            .asText();

    addMember(projectId, admin, member.email(), "MEMBER");

    mockMvc
        .perform(
            post("/api/v1/projects/" + projectId + "/members")
                .header("Authorization", admin.bearer())
                .contentType("application/json")
                .content(json(Map.of("email", member.email(), "role", "MEMBER"))))
        .andExpect(status().isConflict());

    mockMvc
        .perform(
            post("/api/v1/projects/" + projectId + "/members")
                .header("Authorization", member.bearer())
                .contentType("application/json")
                .content(json(Map.of("email", "unknown@pmt.local", "role", "OBSERVER"))))
        .andExpect(status().isForbidden());

    MvcResult taskWithoutAssignee =
        mockMvc
            .perform(
                post("/api/v1/projects/" + projectId + "/tasks")
                    .header("Authorization", member.bearer())
                    .contentType("application/json")
                    .content(
                        json(
                            Map.of(
                                "name",
                                "Tache sans assigne",
                                "description",
                                "Permet de couvrir le cas sans notification.",
                                "dueDate",
                                LocalDate.now().plusDays(5).toString(),
                                "priority",
                                "LOW"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assignee").doesNotExist())
            .andReturn();
    String taskId = json(taskWithoutAssignee).get("id").asText();

    mockMvc
        .perform(
            patch("/api/v1/projects/" + projectId + "/tasks/" + taskId)
                .header("Authorization", member.bearer())
                .contentType("application/json")
                .content(json(Map.of("name", "Tache sans assigne"))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            patch("/api/v1/projects/" + projectId + "/tasks/" + taskId + "/assignee")
                .header("Authorization", member.bearer())
                .contentType("application/json")
                .content(json(Map.of("assigneeId", member.userId()))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            patch("/api/v1/projects/" + projectId + "/tasks/" + taskId + "/assignee")
                .header("Authorization", member.bearer())
                .contentType("application/json")
                .content(json(Map.of("assigneeId", member.userId()))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/projects/" + projectId + "/tasks")
                .header("Authorization", member.bearer()))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/projects/" + projectId + "/tasks/00000000-0000-0000-0000-000000000000")
                .header("Authorization", member.bearer()))
        .andExpect(status().isNotFound());
  }

  private Session register(String username, String email) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType("application/json")
                    .content(
                        json(
                            Map.of(
                                "username", username, "email", email, "password", "Password123!"))))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("pmt_refresh_token"))
            .andReturn();
    JsonNode response = json(result);
    return new Session(
        response.get("accessToken").asText(),
        response.get("user").get("id").asText(),
        response.get("user").get("email").asText(),
        result.getResponse().getCookie("pmt_refresh_token"));
  }

  private void addMember(String projectId, Session admin, String email, String role)
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/projects/" + projectId + "/members")
                .header("Authorization", admin.bearer())
                .contentType("application/json")
                .content(json(Map.of("email", email, "role", role))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.role").value(role));
  }

  private JsonNode json(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private String json(Object value) throws Exception {
    return objectMapper.writeValueAsString(value);
  }

  private void mockMimeMessages() {
    when(mailSender.createMimeMessage())
        .thenAnswer(
            invocation -> new MimeMessage(jakarta.mail.Session.getInstance(new Properties())));
  }

  private record Session(String accessToken, String userId, String email, Cookie refreshCookie) {
    String bearer() {
      return "Bearer " + accessToken;
    }
  }
}
