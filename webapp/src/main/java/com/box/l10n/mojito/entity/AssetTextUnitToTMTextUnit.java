package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.service.assetExtraction.AssetMappingDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;
import org.hibernate.annotations.NamedNativeQueries;
import org.hibernate.annotations.NamedNativeQuery;

/**
 * @author wyau
 */
@Entity
@Table(
        name = "asset_text_unit_to_tm_text_unit",
        indexes = {
            @Index(name = "UK__ASSET_TEXT_UNIT_TO_TM_TEXT_UNIT__ASSET_TEXT_UNIT_ID", columnList = "asset_text_unit_id", unique = true)
        }
)
@SqlResultSetMapping(
        name = "AssetTextUnitToTMTextUnit.getExactMatches",
        classes = {
            @ConstructorResult(
                    targetClass = AssetMappingDTO.class,
                    columns = {
                        @ColumnResult(name = "assetExtractionId", type = Long.class),
                        @ColumnResult(name = "assetTextUnitId", type = Long.class),
                        @ColumnResult(name = "tmTextUnitId", type = Long.class)
                    }
            )
        }
)
@NamedNativeQueries(
        @NamedNativeQuery(name = "AssetTextUnitToTMTextUnit.getExactMatches",
                query
                = "select atu.asset_extraction_id as assetExtractionId, atu.id as assetTextUnitId, tu.id as tmTextUnitId "
                + "from tm_text_unit tu inner join asset_text_unit atu on tu.md5 = atu.md5 "
                + "                     left outer join asset_text_unit_to_tm_text_unit map on map.asset_text_unit_id = atu.id "
                + "where atu.asset_extraction_id = ?1 and tu.tm_id = ?2 and tu.asset_id = ?3 and map.asset_text_unit_id is NULL",
                resultSetMapping = "AssetTextUnitToTMTextUnit.getExactMatches"
        )
)
public class AssetTextUnitToTMTextUnit extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "tm_text_unit_id", foreignKey = @ForeignKey(name = "FK__ASSET_TEXT_UNIT_TO_TM_TEXT_UNIT__TM_TEXT_UNIT__ID"))
    private TMTextUnit tmTextUnit;

    @ManyToOne
    @JoinColumn(name = "asset_text_unit_id", foreignKey = @ForeignKey(name = "FK__ASSET_TEXT_UNIT_TO_TM_TEXT_UNIT__ASSET_TEXT_UNIT__ID"))
    private AssetTextUnit assetTextUnit;

    /**
     * Denormalize to able to do search on used/unused text unit, see
     * {@link TextUnitSearcher}
     */
    @ManyToOne
    @JoinColumn(name = "asset_extraction_id", foreignKey = @ForeignKey(name = "FK__ASSET_TEXT_UNIT_TO_TM_TEXT_UNIT__ASSET_EXTRACTION__ID"))
    private AssetExtraction assetExtraction;

    public TMTextUnit getTmTextUnit() {
        return tmTextUnit;
    }

    public void setTmTextUnit(TMTextUnit tmTextUnit) {
        this.tmTextUnit = tmTextUnit;
    }

    public AssetTextUnit getAssetTextUnit() {
        return assetTextUnit;
    }

    public void setAssetTextUnit(AssetTextUnit assetTextUnit) {
        this.assetTextUnit = assetTextUnit;
    }

    public AssetExtraction getAssetExtraction() {
        return assetExtraction;
    }

    public void setAssetExtraction(AssetExtraction assetExtraction) {
        this.assetExtraction = assetExtraction;
    }
}
