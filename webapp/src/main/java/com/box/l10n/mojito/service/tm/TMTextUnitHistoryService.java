/**
 * 
 */
package com.box.l10n.mojito.service.tm;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.box.l10n.mojito.entity.TMTextUnitVariant;

/**
 * @author ehoogerbeets
 *
 */
@Service
public class TMTextUnitHistoryService {
    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(TMTextUnitHistoryService.class);

    @Autowired
    TMTextUnitVariantRepository tmTextUnitVariantRepository;

    public List<TMTextUnitVariant> findHistory(Long localeId, Long tmTextUnitId) {
        return tmTextUnitVariantRepository.findAllByLocale_IdAndTmTextUnit_Id(localeId, tmTextUnitId);
    }
}
