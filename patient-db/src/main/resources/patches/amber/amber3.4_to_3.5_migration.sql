DROP TABLE IF EXISTS amberPatient;
CREATE TABLE amberPatient
(
    modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    sampleId varchar(255) NOT NULL,
    patientId int NOT NULL,
    PRIMARY KEY (sampleId)
);

DROP TABLE IF EXISTS amberMapping;
CREATE TABLE amberMapping
(
    modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    firstSampleId varchar(255) NOT NULL,
    secondSampleId varchar(255) NOT NULL,
    matches int NOT NULL,
    sites int NOT NULL,
    likelihood DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (firstSampleId, secondSampleId)
);

DROP TABLE IF EXISTS amber;

CREATE TABLE if not exists amberSample
(
    modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    sampleId varchar(255) NOT NULL,
    site1 TINYINT NOT NULL,
    site2 TINYINT NOT NULL,
    site3 TINYINT NOT NULL,
    site4 TINYINT NOT NULL,
    site5 TINYINT NOT NULL,
    site6 TINYINT NOT NULL,
    site7 TINYINT NOT NULL,
    site8 TINYINT NOT NULL,
    site9 TINYINT NOT NULL,
    site10 TINYINT NOT NULL,
    site11 TINYINT NOT NULL,
    site12 TINYINT NOT NULL,
    site13 TINYINT NOT NULL,
    site14 TINYINT NOT NULL,
    site15 TINYINT NOT NULL,
    site16 TINYINT NOT NULL,
    site17 TINYINT NOT NULL,
    site18 TINYINT NOT NULL,
    site19 TINYINT NOT NULL,
    site20 TINYINT NOT NULL,
    site21 TINYINT NOT NULL,
    site22 TINYINT NOT NULL,
    site23 TINYINT NOT NULL,
    site24 TINYINT NOT NULL,
    site25 TINYINT NOT NULL,
    site26 TINYINT NOT NULL,
    site27 TINYINT NOT NULL,
    site28 TINYINT NOT NULL,
    site29 TINYINT NOT NULL,
    site30 TINYINT NOT NULL,
    site31 TINYINT NOT NULL,
    site32 TINYINT NOT NULL,
    site33 TINYINT NOT NULL,
    site34 TINYINT NOT NULL,
    site35 TINYINT NOT NULL,
    site36 TINYINT NOT NULL,
    site37 TINYINT NOT NULL,
    site38 TINYINT NOT NULL,
    site39 TINYINT NOT NULL,
    site40 TINYINT NOT NULL,
    site41 TINYINT NOT NULL,
    site42 TINYINT NOT NULL,
    site43 TINYINT NOT NULL,
    site44 TINYINT NOT NULL,
    site45 TINYINT NOT NULL,
    site46 TINYINT NOT NULL,
    site47 TINYINT NOT NULL,
    site48 TINYINT NOT NULL,
    site49 TINYINT NOT NULL,
    site50 TINYINT NOT NULL,
    site51 TINYINT NOT NULL,
    site52 TINYINT NOT NULL,
    site53 TINYINT NOT NULL,
    site54 TINYINT NOT NULL,
    site55 TINYINT NOT NULL,
    site56 TINYINT NOT NULL,
    site57 TINYINT NOT NULL,
    site58 TINYINT NOT NULL,
    site59 TINYINT NOT NULL,
    site60 TINYINT NOT NULL,
    site61 TINYINT NOT NULL,
    site62 TINYINT NOT NULL,
    site63 TINYINT NOT NULL,
    site64 TINYINT NOT NULL,
    site65 TINYINT NOT NULL,
    site66 TINYINT NOT NULL,
    site67 TINYINT NOT NULL,
    site68 TINYINT NOT NULL,
    site69 TINYINT NOT NULL,
    site70 TINYINT NOT NULL,
    site71 TINYINT NOT NULL,
    site72 TINYINT NOT NULL,
    site73 TINYINT NOT NULL,
    site74 TINYINT NOT NULL,
    site75 TINYINT NOT NULL,
    site76 TINYINT NOT NULL,
    site77 TINYINT NOT NULL,
    site78 TINYINT NOT NULL,
    site79 TINYINT NOT NULL,
    site80 TINYINT NOT NULL,
    site81 TINYINT NOT NULL,
    site82 TINYINT NOT NULL,
    site83 TINYINT NOT NULL,
    site84 TINYINT NOT NULL,
    site85 TINYINT NOT NULL,
    site86 TINYINT NOT NULL,
    site87 TINYINT NOT NULL,
    site88 TINYINT NOT NULL,
    site89 TINYINT NOT NULL,
    site90 TINYINT NOT NULL,
    site91 TINYINT NOT NULL,
    site92 TINYINT NOT NULL,
    site93 TINYINT NOT NULL,
    site94 TINYINT NOT NULL,
    site95 TINYINT NOT NULL,
    site96 TINYINT NOT NULL,
    site97 TINYINT NOT NULL,
    site98 TINYINT NOT NULL,
    site99 TINYINT NOT NULL,
    site100 TINYINT NOT NULL,
    PRIMARY KEY (sampleId)
);