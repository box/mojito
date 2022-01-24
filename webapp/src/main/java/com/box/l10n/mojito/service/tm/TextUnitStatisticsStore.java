package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.TMTextUnitStatistic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.Map;
import java.util.stream.Collectors;

@Configurable
public class TextUnitStatisticsStore {
    static Logger logger = LoggerFactory.getLogger(TextUnitStatisticsStore.class);

    @Autowired
    TMTextUnitStatisticRepository tmTextUnitStatisticRepository;

    Map<String, TMTextUnitStatistic> statisticsByTextUnitMD5;

    Asset asset;

    public TextUnitStatisticsStore(Asset asset) {
        this.asset = asset;
    }

    public Double getLastPeriodUsage(String md5) {
        TMTextUnitStatistic tmTextUnitStatistic = getTextUnitStatisticByMD5FromCache(asset).get(md5);

        if (tmTextUnitStatistic != null) {
            return tmTextUnitStatistic.getLastPeriodUsageCount();
        }

        return 0D;
    }

    private Map<String, TMTextUnitStatistic> getTextUnitStatisticByMD5FromCache(Asset asset) {
        if (statisticsByTextUnitMD5 == null) {
            logger.debug("No map in cache, get the map and cache it");
            statisticsByTextUnitMD5 = tmTextUnitStatisticRepository.findByAsset(asset.getId())
                    .stream().collect(Collectors.toMap(item -> item.getTMTextUnit().getMd5(), item -> item));
        }

        return statisticsByTextUnitMD5;
    }
}
