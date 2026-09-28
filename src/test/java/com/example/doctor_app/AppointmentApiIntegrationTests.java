package com.example.doctor_app;

import com.example.doctor_app.repository.AppointmentRepository;
import com.example.doctor_app.repository.DoctorRepository;
import com.example.doctor_app.repository.DoctorSlotRepository;
import com.example.doctor_app.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:doctor-api-tests;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AppointmentApiIntegrationTests {
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorSlotRepository slotRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @BeforeEach
    void clearDatabase() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .defaultRequest(get("/")
                        .with(SecurityMockMvcRequestPostProcessors.user("clinic-admin").roles("ADMIN"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .build();
        appointmentRepository.deleteAll();
        slotRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();
    }

    @Test
    void clinicCanPublishBookCancelAndRebookASlot() throws Exception {
        String doctorResponse = mockMvc.perform(post("/api/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Dr Test","email":"doctor@example.test","specializations":["Cardiology"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.specializations[0]").value("Cardiology"))
                .andReturn().getResponse().getContentAsString();
        long doctorId = idFrom(doctorResponse);

        String patientResponse = mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Pat Test","email":"patient@example.test","phone":"555-0101"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long patientId = idFrom(patientResponse);

        mockMvc.perform(post("/api/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Duplicate","email":"doctor@example.test","specializations":["Cardiology"]}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A doctor with this email already exists"));

        mockMvc.perform(post("/api/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"invalid","specializations":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());

        mockMvc.perform(get("/api/doctors/{doctorId}", doctorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(doctorId));
        mockMvc.perform(get("/api/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Dr Test"))
                .andExpect(jsonPath("$[0].email").value("doctor@example.test"))
                .andExpect(jsonPath("$[0].specializations[0]").value("Cardiology"));
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Pat Test"))
                .andExpect(jsonPath("$[0].email").value("patient@example.test"))
                .andExpect(jsonPath("$[0].phone").value("555-0101"));
        mockMvc.perform(put("/api/patients/{patientId}", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Pat Updated","email":"patient@example.test","phone":"555-0199"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pat Updated"))
                .andExpect(jsonPath("$.phone").value("555-0199"));

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        String slotsResponse = mockMvc.perform(post("/api/doctors/{doctorId}/slots", doctorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dateFrom":"%s","dateTo":"%s","timeFrom":"09:00:00","timeTo":"10:00:00","slotMinutes":60}
                                """.formatted(tomorrow, tomorrow)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        long slotId = idFrom(slotsResponse);

        mockMvc.perform(post("/api/doctors/{doctorId}/slots", doctorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dateFrom":"%s","dateTo":"%s","timeFrom":"09:00:00","timeTo":"10:00:00","slotMinutes":60}
                                """.formatted(tomorrow, tomorrow)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("overlaps")));

        mockMvc.perform(post("/api/doctors/{doctorId}/slots", doctorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dateFrom":"%s","dateTo":"%s","timeFrom":"09:00:00","timeTo":"10:00:00","slotMinutes":0}
                                """.formatted(tomorrow, tomorrow)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/slots")
                        .param("date", tomorrow.toString())
                        .param("specialization", "cardiology"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/doctors/{doctorId}/slots", doctorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/doctors/{doctorId}/appointments/today", doctorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        String bookingRequest = """
                {"patientId":%d,"slotId":%d}
                """.formatted(patientId, slotId);
        String appointmentResponse = mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON).content(bookingRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andReturn().getResponse().getContentAsString();
        long appointmentId = idFrom(appointmentResponse);
        mockMvc.perform(get("/api/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(appointmentId))
                .andExpect(jsonPath("$[0].patientName").value("Pat Updated"))
                .andExpect(jsonPath("$[0].startTime").exists());
        mockMvc.perform(get("/api/appointments").param("date", tomorrow.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(appointmentId));
        mockMvc.perform(get("/api/appointments").param("date", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/slots").param("date", tomorrow.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON).content(bookingRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This slot is already booked"));
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":-1,"slotId":%d}
                                """.formatted(slotId)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doctorCount").value(1))
                .andExpect(jsonPath("$.bookedSlotCount").value(1))
                .andExpect(jsonPath("$.availableSlotCount").value(0));

        mockMvc.perform(post("/api/appointments/{appointmentId}/cancel", appointmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(get("/api/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(post("/api/appointments/{appointmentId}/cancel", appointmentId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This appointment has already been cancelled"));

        String secondAppointment = mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON).content(bookingRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andReturn().getResponse().getContentAsString();
        long secondAppointmentId = idFrom(secondAppointment);

        mockMvc.perform(put("/api/doctors/{doctorId}", doctorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Dr Test Updated","email":"doctor@example.test","specializations":["Neurology"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specializations[0]").value("Neurology"));

        mockMvc.perform(delete("/api/doctors/{doctorId}", doctorId))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/appointments/{appointmentId}/cancel", secondAppointmentId))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON).content(bookingRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "This doctor's schedule is no longer accepting bookings"));
    }

    @Test
    void missingRecordsReturnClearNotFoundResponses() throws Exception {
        mockMvc.perform(get("/api/doctors/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Doctor 9999 was not found"));
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":9999,"slotId":9999}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Patient 9999 was not found"));
        mockMvc.perform(post("/api/appointments/9999/cancel"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/doctors/9999/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dateFrom":"%s","dateTo":"%s","timeFrom":"09:00:00","timeTo":"10:00:00","slotMinutes":60}
                                """.formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(1))))
                .andExpect(status().isNotFound());
    }

    @Test
    void clinicDashboardIsServedByTheApplication() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/home.html"));
        mockMvc.perform(get("/home.html"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Welcome to Carepoint")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("public-navigation")));
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Clinic dashboard")));
        mockMvc.perform(get("/styles.css"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith("text/css"));
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("loadDashboard")));
    }

    private long idFrom(String json) {
        Matcher matcher = Pattern.compile("\"id\":\\s*(\\d+)").matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("Expected an id in the API response: " + json);
        }
        return Long.parseLong(matcher.group(1));
    }
}
