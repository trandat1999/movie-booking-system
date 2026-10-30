package com.moviebooking.modules.setting;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.moviebooking.support.AbstractAdminIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SettingIntegrationTest extends AbstractAdminIntegrationTest {

    @Test
    void listsSettingsWithMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings").header("Authorization", admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].key", hasItem("cleaning_buffer_min")))
                .andExpect(jsonPath("$.data[?(@.key == 'orphan_seat_rule')].type").value(hasItem("OPTION")));
    }

    @Test
    void validatesAndUpdatesValues() throws Exception {
        putJson("/api/v1/admin/settings/{key}", Map.of("value", "150"), "refund_percent")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_SETTING_VALUE"));
        putJson("/api/v1/admin/settings/{key}", Map.of("value", "maybe"), "orphan_seat_rule")
                .andExpect(status().isBadRequest());
        putJson("/api/v1/admin/settings/{key}", Map.of("value", "1"), "no_such_key")
                .andExpect(status().isNotFound());

        putJson("/api/v1/admin/settings/{key}", Map.of("value", " 90 "), "refund_percent")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value").value("90"));
        putJson("/api/v1/admin/settings/{key}", Map.of("value", "100"), "refund_percent").andExpect(status().isOk());
    }
}
