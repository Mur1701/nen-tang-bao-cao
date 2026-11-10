-- CI ONLY: synthetic fixture. DO NOT APPLY TO pottel_bss.
-- Deliberately creates fake BSS-looking tables in a disposable PostgreSQL DB.
BEGIN;
CREATE SCHEMA crm;
CREATE SCHEMA esim_agency;
CREATE SCHEMA inventory;
CREATE SCHEMA cdr;
CREATE SCHEMA vnpost_bss;

CREATE TABLE esim_agency.agencies (
 id integer PRIMARY KEY, code text, name text, warehouse_id integer
);
CREATE TABLE inventory.products (code text PRIMARY KEY, base_price numeric);
CREATE TABLE crm.linhnn_kits_prod (
 id bigint PRIMARY KEY,
 msisdn_snapshot text,
 agency_id integer,
 main_package_code text,
 registed_date timestamptz
);
CREATE TABLE cdr.vnpt_package_new (msisdn text, insert_time text);
CREATE TABLE crm.pottel_kit_warehouse_transfer_requests (
 id bigint PRIMARY KEY, request_code text, created_at timestamp,
 completed_at timestamp, to_warehouse_id integer, status text
);
CREATE TABLE crm.pottel_kit_warehouse_transfer_request_items (
 request_id bigint, kit_id bigint
);
CREATE TABLE vnpost_bss.chi_tra_hieu_luc (
 kit_id bigint, loai_khoan text, dot smallint, den_luc timestamptz,
 ngay_chi_tra date, so_tien numeric
);

INSERT INTO esim_agency.agencies VALUES (1,'DL01','Đại lý thử',10);
INSERT INTO inventory.products VALUES ('DL7',108000.00);
INSERT INTO crm.linhnn_kits_prod VALUES
(1,'840000000001',1,'DL7','2026-10-01 09:00:00+07'),
(2,'840000000002',1,'DL7','2026-10-11 09:00:00+07');
INSERT INTO cdr.vnpt_package_new VALUES
('840000000001','31-OCT-26'),
('840000000001','02-NOV-26');
INSERT INTO crm.pottel_kit_warehouse_transfer_requests
 VALUES (11,'TEST-01','2026-10-01 08:00:00','2026-10-01 09:00:00',10,'COMPLETED');
INSERT INTO crm.pottel_kit_warehouse_transfer_request_items VALUES (11,1),(11,2);
INSERT INTO vnpost_bss.chi_tra_hieu_luc
 VALUES (1,'HHBG',1,NULL,'2026-10-15',24300.00);
COMMIT;
