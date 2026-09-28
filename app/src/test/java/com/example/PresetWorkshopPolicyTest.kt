package com.example

import com.example.model.ModelPreset
import org.junit.Assert.assertEquals
import org.junit.Test

class PresetWorkshopPolicyTest {
    @Test
    fun new_preset_uses_active_workshop_id() {
        val preset = ModelPreset(id = 0L, workshopId = 1L, name = "مدل تست")
        val normalized = PresetWorkshopPolicy.forSave(preset, activeWorkshopId = 27L)
        assertEquals(27L, normalized.workshopId)
    }

    @Test
    fun no_workshop_rejects_new_preset() {
        val preset = ModelPreset(id = 0L, workshopId = 1L, name = "مدل تست")
        val result = runCatching {
            PresetWorkshopPolicy.forSave(preset, activeWorkshopId = 0L)
        }
        assertEquals(true, result.isFailure)
    }
}
