CREATE TABLE scxa_analytics_e_curd_4 (LIKE scxa_analytics) WITH (autovacuum_enabled = false, toast.autovacuum_enabled = false);
CREATE TABLE scxa_analytics_e_ehca_2 (LIKE scxa_analytics) WITH (autovacuum_enabled = false, toast.autovacuum_enabled = false);
CREATE TABLE scxa_analytics_e_geod_71585 (LIKE scxa_analytics) WITH (autovacuum_enabled = false, toast.autovacuum_enabled = false);
CREATE TABLE scxa_analytics_e_geod_81547 (LIKE scxa_analytics) WITH (autovacuum_enabled = false, toast.autovacuum_enabled = false);
CREATE TABLE scxa_analytics_e_geod_99058 (LIKE scxa_analytics) WITH (autovacuum_enabled = false, toast.autovacuum_enabled = false);
CREATE TABLE scxa_analytics_e_mtab_5061 (LIKE scxa_analytics) WITH (autovacuum_enabled = false, toast.autovacuum_enabled = false);

ALTER TABLE scxa_analytics ATTACH PARTITION scxa_analytics_e_curd_4 FOR VALUES IN ('E-CURD-4');
ALTER TABLE scxa_analytics ATTACH PARTITION scxa_analytics_e_ehca_2 FOR VALUES IN ('E-EHCA-2');
ALTER TABLE scxa_analytics ATTACH PARTITION scxa_analytics_e_geod_71585 FOR VALUES IN ('E-GEOD-71585');
ALTER TABLE scxa_analytics ATTACH PARTITION scxa_analytics_e_geod_81547 FOR VALUES IN ('E-GEOD-81547');
ALTER TABLE scxa_analytics ATTACH PARTITION scxa_analytics_e_geod_99058 FOR VALUES IN ('E-GEOD-99058');
ALTER TABLE scxa_analytics ATTACH PARTITION scxa_analytics_e_mtab_5061 FOR VALUES IN ('E-MTAB-5061');

INSERT INTO scxa_analytics(experiment_accession, gene_id, cell_id, expression_level) VALUES ('E-MTAB-5061', 'ENSG00000102755', 'ERR1630362', 0.65343505);
INSERT INTO scxa_analytics(experiment_accession, gene_id, cell_id, expression_level) VALUES ('E-MTAB-5061', 'ENSG00000127920', 'ERR1631456', 27.720057);
INSERT INTO scxa_analytics(experiment_accession, gene_id, cell_id, expression_level) VALUES ('E-MTAB-5061', 'ENSG00000275896', 'ERR1630319', 1.4108431);
INSERT INTO scxa_analytics(experiment_accession, gene_id, cell_id, expression_level) VALUES ('E-MTAB-5061', 'ENSG00000265060', 'ERR1630539', 175.53897);
INSERT INTO scxa_analytics(experiment_accession, gene_id, cell_id, expression_level) VALUES ('E-MTAB-5061', 'ENSG00000171532', 'ERR1630329', 5.460639);
INSERT INTO scxa_analytics(experiment_accession, gene_id, cell_id, expression_level) VALUES ('E-MTAB-5061', 'ENSG00000128340', 'ERR1631826', 37.089924);
