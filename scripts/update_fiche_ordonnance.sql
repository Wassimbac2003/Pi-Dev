-- Exécuter sur la base `annonce_db` avant d utiliser medecin / ordonnance sans RDV.
-- Si DROP FOREIGN KEY échoue : SHOW CREATE TABLE ordonnance; puis remplacer le nom exact de la contrainte sur id_rdv_id.

ALTER TABLE `fiche`
    ADD COLUMN `medecin_user_id` INT NULL DEFAULT NULL AFTER `id_u_id`;

ALTER TABLE `fiche`
    ADD CONSTRAINT `FK_fiche_medecin_user` FOREIGN KEY (`medecin_user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `ordonnance` DROP FOREIGN KEY `FK_924B326C6AF98A6B`;

ALTER TABLE `ordonnance`
    MODIFY `id_rdv_id` INT NULL DEFAULT NULL;

ALTER TABLE `ordonnance`
    ADD CONSTRAINT `FK_ordonnance_rdv` FOREIGN KEY (`id_rdv_id`) REFERENCES `rdv` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `ordonnance`
    ADD COLUMN `medecin_user_id` INT NULL DEFAULT NULL AFTER `id_fiche_id`;

ALTER TABLE `ordonnance`
    ADD CONSTRAINT `FK_ordonnance_medecin_user` FOREIGN KEY (`medecin_user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;
