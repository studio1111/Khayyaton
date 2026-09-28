package com.example.data.sync

class PendingAccountSwitchException : IllegalStateException(
    "حساب فعلی هنوز اطلاعات همگام‌نشده دارد و تعویض حساب تا تکمیل ذخیره‌سازی مجاز نیست."
)
