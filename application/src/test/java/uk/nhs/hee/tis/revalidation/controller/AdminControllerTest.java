package uk.nhs.hee.tis.revalidation.controller;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.nhs.hee.tis.revalidation.service.GmcDoctorNightlySyncService;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(AdminController.class)
public class AdminControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private GmcDoctorNightlySyncService gmcDoctorNightlySyncService;

  @InjectMocks
  private AdminController adminController;


  @Test
  void shouldStartGmcDoctorSync() throws Exception {
    this.mockMvc.perform(post("/api/admin/trigger-doctor-sync"))
        .andExpect(status().isOk());
    verify(gmcDoctorNightlySyncService, times(1)).startNightlyGmcDoctorSync();

  }
}
