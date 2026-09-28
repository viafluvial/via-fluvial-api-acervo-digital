package br.com.viafluvial.acervodigital.adapters.in.web.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.viafluvial.acervodigital.adapters.in.web.generated.model.PendingItemsReportResponse;
import br.com.viafluvial.acervodigital.application.usecase.AcervoApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = AcervoController.class,
    properties = {
        "security.mode=dev",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/jwks"
    }
)
@AutoConfigureMockMvc(addFilters = false)
class AcervoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AcervoApplicationService service;

    @Test
    void shouldReturnPendingItemsReport() throws Exception {
        when(service.pendingItemsReport(null, null))
            .thenReturn(new PendingItemsReportResponse(1, 2, 0, 0));

        mockMvc.perform(get("/acervo/reports/pending-items"))
            .andExpect(status().isOk());
    }
}
