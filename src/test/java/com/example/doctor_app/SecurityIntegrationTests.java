package com.example.doctor_app;

import com.example.doctor_app.service.ClinicAccountService;
import com.example.doctor_app.repository.ClinicUserRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:doctor-security-tests;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class SecurityIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClinicAccountService accounts;

    @Autowired
    private ClinicUserRepository users;

    @Test
    void publicPagesAreAvailableAndDashboardNeedsAnySignedInAccount() throws Exception {
        mockMvc.perform(get("/home.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thoughtful care starts here")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("signup.html")));
        mockMvc.perform(get("/signup.html"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/index.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login.html"));
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Clinic Member","email":"first@example.test","password":"StrongTestPassword123"}
                                """))
                .andExpect(status().isCreated());
        HttpSession session = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"first@example.test","password":"StrongTestPassword123"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);

        mockMvc.perform(get("/api/dashboard").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk());
        mockMvc.perform(get("/dashboard").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk());
    }

    @Test
    void signupCreatesRegularAccountAndThatAccountCanUseClinicWorkspace() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New User","email":"user@example.test","password":"StrongTestPassword123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New User"));
        org.junit.jupiter.api.Assertions.assertEquals(
                "USER", users.findByEmailIgnoreCase("user@example.test").orElseThrow().getRole());
        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New User","email":"user@example.test","password":"StrongTestPassword123"}
                                """))
                .andExpect(status().isConflict());

        HttpSession authenticatedSession = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.test","password":"StrongTestPassword123"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
        mockMvc.perform(get("/api/dashboard")
                        .session((org.springframework.mock.web.MockHttpSession) authenticatedSession))
                .andExpect(status().isOk());
    }

    @Test
    void loginRequiresCsrfAndLogoutInvalidatesUserSession() throws Exception {
        accounts.signUp(new com.example.doctor_app.dto.SignUpRequest(
                "Clinic Member", "logout@example.test", "StrongTestPassword123"));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"logout@example.test","password":"StrongTestPassword123"}
                                """))
                .andExpect(status().isForbidden());

        HttpSession session = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"logout@example.test","password":"StrongTestPassword123"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);

        mockMvc.perform(post("/api/auth/logout")
                        .session((org.springframework.mock.web.MockHttpSession) session).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/dashboard")
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isUnauthorized());
    }
}
