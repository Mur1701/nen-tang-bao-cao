-- Optional HHBG settlement source preserved from Dev.rar:
-- module-bao-cao/migrations/V1000__chi-tra-dai-ly.sql
-- Only reads vnpost_bss.chi_tra_hieu_luc if it already exists; does NOT
-- install old BSS migrations, create payment records, or modify BSS.
-- Current effective records (den_luc IS NULL), not historical closing snapshot.
BEGIN;
CREATE FUNCTION reporting.fn_phase1_reference_payments(p_month date)
RETURNS TABLE(kit_id bigint, payment_at date, paid_1 numeric, paid_2 numeric)
LANGUAGE plpgsql STABLE SECURITY INVOKER SET search_path = pg_catalog
AS $fn$
BEGIN
 IF to_regclass('vnpost_bss.chi_tra_hieu_luc') IS NULL THEN
   RETURN;
 END IF;
 RETURN QUERY EXECUTE $stmt$
   SELECT h.kit_id,
          min(h.ngay_chi_tra) FILTER (WHERE h.dot = 1)::date,
          sum(h.so_tien) FILTER (WHERE h.dot = 1)::numeric,
          sum(h.so_tien) FILTER (WHERE h.dot = 2)::numeric
   FROM vnpost_bss.chi_tra_hieu_luc h
   WHERE h.den_luc IS NULL AND h.loai_khoan = 'HHBG'
   GROUP BY h.kit_id
 $stmt$;
END
$fn$;
REVOKE ALL ON FUNCTION reporting.fn_phase1_reference_payments(date) FROM PUBLIC;
COMMIT;
