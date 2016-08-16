package com.box.l10n.mojito.service.leveraging;

import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author jaurambault
 */
public class AbstractLeveragerTest {

    private AbstractLeverager getLeveragingImpl() {

        return new AbstractLeverager() {

            @Override
            public String getType() {
                return "for test";
            }

            @Override
            public List<TextUnitDTO> getLeveragingMatches(TMTextUnit tmTextUnit, Long sourceTmId) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public boolean isTranslationNeededIfUniqueMatch() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };

    }

    @Test
    public void testFilterTextUnitDTOWithSameTMTextUnitIdEmpty() {
        List<TextUnitDTO> textUnitDTOs = new ArrayList<>();
        getLeveragingImpl().filterTextUnitDTOWithSameTMTextUnitId(textUnitDTOs);
        assertTrue(textUnitDTOs.isEmpty());
    }

    @Test
    public void testFilterTextUnitDTOWithSameTMTextUnitId() {
        List<TextUnitDTO> textUnitDTOs = new ArrayList<>();

        TextUnitDTO textUnitDTO = new TextUnitDTO();
        textUnitDTO.setTmTextUnitId(1L);
        textUnitDTOs.add(textUnitDTO);

        TextUnitDTO textUnitDTO2 = new TextUnitDTO();
        textUnitDTO2.setTmTextUnitId(2L);
        textUnitDTOs.add(textUnitDTO2);

        TextUnitDTO textUnitDTO3 = new TextUnitDTO();
        textUnitDTO3.setTmTextUnitId(1L);
        textUnitDTOs.add(textUnitDTO3);

        getLeveragingImpl().filterTextUnitDTOWithSameTMTextUnitId(textUnitDTOs);

        assertEquals(2, textUnitDTOs.size());
        assertEquals(textUnitDTO, textUnitDTOs.get(0));
        assertEquals(textUnitDTO3, textUnitDTOs.get(1));
    }
}
