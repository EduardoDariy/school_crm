package com.crm.core;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CrmApplicationTests extends ApiTestSupport {

	@Test
	void contextLoads() {
	}

	@Test
	void apiDocumentationIsAvailableWithoutLogin() throws Exception {
		mvc.perform(get("/v3/api-docs/1-school"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths", hasKey("/api/auth/login")))
				.andExpect(jsonPath("$.paths", hasKey("/api/leads")))
				.andExpect(jsonPath("$.components.securitySchemes.bearer.scheme").value("bearer"));
		mvc.perform(get("/v3/api-docs/2-platform"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths", hasKey("/api/platform/organizations")));
		mvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isOk());
	}

}
