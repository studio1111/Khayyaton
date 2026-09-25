# حساب مالک خیاطان

حساب مالک از **Firebase Authentication Custom Claims** کنترل می‌شود و از داخل برنامه قابل ارتقا نیست.

## افزودن چند مالک

هر کاربر Firebase یک UID مستقل دارد. برای هر UID که باید مالک باشد، همین دستور را جداگانه اجرا کنید.

### Termux

1. یک Service Account از Firebase/Google Cloud بگیرید و فایل JSON آن را فقط روی دستگاه خود نگه دارید.
2. در پوشه پروژه اجرا کنید:

`npm install firebase-admin`

3. مسیر فایل را فقط در متغیر محیطی قرار دهید:

`export GOOGLE_APPLICATION_CREDENTIALS="/path/to/service-account.json"`

4. مالک را فعال کنید:

`node scripts/set-owner-claim.mjs FIREBASE_UID`

5. برای حذف دسترسی مالک:

`node scripts/set-owner-claim.mjs FIREBASE_UID remove`

فایل Service Account، کلید خصوصی یا UIDهای واقعی را داخل GitHub قرار ندهید.

## منطق امنیتی

برنامه فقط claimهای Firebase Authentication را می‌پذیرد:

- `role=owner`
- یا `admin=true`

فیلدهایی مانند `owner_account` یا `role` داخل Firestore به‌تنهایی مجوز مالک ایجاد نمی‌کنند.

پس از تغییر claim، کاربر باید یک بار از حساب خارج و دوباره وارد شود، یا توکن Firebase تازه شود.
