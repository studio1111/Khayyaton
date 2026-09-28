package com.example.ui

import com.example.model.ModelPreset

internal object PresetWorkshopPolicy {
    fun forSave(preset: ModelPreset, activeWorkshopId: Long): ModelPreset {
        require(activeWorkshopId > 0L) { "کارگاه فعال برای ذخیره مدل مشخص نشده است." }
        return if (preset.id == 0L || preset.workshopId <= 0L) {
            preset.copy(workshopId = activeWorkshopId)
        } else {
            preset
        }
    }
}
