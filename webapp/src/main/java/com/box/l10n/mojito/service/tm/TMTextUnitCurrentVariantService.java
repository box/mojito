package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TMTextUnitCurrentVariantService {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(TMTextUnitCurrentVariantService.class);

    @Autowired
    TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

    /**
     * Removes a {@link TMTextUnitVariant} from being the current variant. In other words, removes the translation.
     *
     * We're updating the {@link TMTextUnitCurrentVariant#tmTextUnitVariant} with "null" value instead of removing
     * the full record to be able to very easily track translation deletion.
     *
     * If the record was removed we'd have to look into envers table, which would be more complicated. With this
     * it is also easy to fetch delta of changes and apply them to a previous state.
     *
     * @param textUnitId
     */
    public void removeCurrentVariant(Long tmTextUnitCurrentVariantId) {
        TMTextUnitCurrentVariant tmtucv = tmTextUnitCurrentVariantRepository.findById(tmTextUnitCurrentVariantId).orElse(null);

        if (tmtucv == null) {
            logger.debug("No current variant, do nothing");
        } else {
            logger.debug("Update tmTextUnitCurrentVariant with id: {} to remove the current variant (\"remove\" current translation)", tmtucv.getId());
            tmtucv.setTmTextUnitVariant(null);
            tmTextUnitCurrentVariantRepository.save(tmtucv);
        }
    }
}
