DELIMITER //
CREATE PROCEDURE fetch_text_unit_count_for_appending(IN repositoryId BIGINT, IN mainBranch VARCHAR(255), IN daysInterval INT, OUT result_count INT)
BEGIN
-- Pull out text units that are in a fully translated branch which are not in the latest push run.
SELECT COUNT(DISTINCT tu.id) INTO result_count
FROM tm_text_unit tu
JOIN asset_text_unit_to_tm_text_unit atutttu ON tu.id = atutttu.tm_text_unit_id
JOIN asset_text_unit atu ON atutttu.asset_text_unit_id = atu.id
JOIN branch b ON atu.branch_id = b.id
WHERE atu.branch_id IN (
    -- All fully translated branches that are not master or null (created last 2 weeks)
    SELECT b.id
    FROM branch b
             INNER JOIN branch_statistic bs ON b.id = bs.branch_id
    WHERE b.repository_id = repositoryId
      AND b.deleted = false
      AND b.name IS NOT NULL
      AND b.name <> mainBranch
      AND bs.for_translation_count = 0
      AND b.created_date >= DATE_SUB(CURDATE(), INTERVAL daysInterval DAY)
)
AND tu.id NOT IN (
    -- All text units that were in the last push run
    SELECT push_run_asset_tm_text_unit.tm_text_unit_id FROM push_run_asset_tm_text_unit
    JOIN push_run_asset ON push_run_asset.id = push_run_asset_tm_text_unit.push_run_asset_id
    JOIN push_run ON push_run.id = push_run_asset.push_run_id
    WHERE push_run.id = (
        SELECT p.id FROM push_run p WHERE p.repository_id = repositoryId ORDER BY p.created_date DESC LIMIT 1
        )
    );
END //
DELIMITER ;