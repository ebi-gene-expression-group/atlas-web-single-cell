SET search_path = atlas3dev;

create table if not exists scxa_analytics_e_curd_4
    partition of scxa_analytics
(
    constraint scxa_analytics_e_curd_4_pk
    primary key (gene_id, experiment_accession, cell_id),
    constraint check_e_ebi_e_curd_4
    check ((experiment_accession)::text = 'E-CURD-4'::text)
    )
    FOR VALUES IN ('E-CURD-4');
