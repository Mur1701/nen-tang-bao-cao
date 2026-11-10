-- POTTEL SEC-ADMIN-01 — metadata only, never reads username or password hashes.
-- Run in local pottel_bss using psql -v ON_ERROR_STOP=1 -f <file>.
BEGIN TRANSACTION READ ONLY;
SELECT to_regclass('vnpost_bss.user_tai_khoan') IS NOT NULL AS accounts_table_exists,
       to_regclass('vnpost_bss.user_tai_khoan_vai_tro') IS NOT NULL AS account_roles_table_exists;
SELECT table_name, column_name, data_type
FROM information_schema.columns
WHERE table_schema = 'vnpost_bss'
  AND table_name IN ('user_tai_khoan','user_tai_khoan_vai_tro')
  AND column_name IN ('id','ten_dang_nhap','hash','khoa','loai','phien_ban_bao_mat','tai_khoan_id','vai_tro')
ORDER BY table_name, column_name;
COMMIT;
