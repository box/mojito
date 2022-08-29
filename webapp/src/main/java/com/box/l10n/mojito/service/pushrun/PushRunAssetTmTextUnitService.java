package com.box.l10n.mojito.service.pushrun;

import com.box.l10n.mojito.entity.PushRunAsset;
import com.box.l10n.mojito.entity.PushRunAssetTmTextUnit;
import com.box.l10n.mojito.entity.TMTextUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service to manage PushRunAssetTmTextUnit entities.
 *
 * @author garion
 */
@Service
public class PushRunAssetTmTextUnitService {
  @Autowired PushRunAssetTmTextUnitRepository pushRunAssetTmTextUnitRepository;

  public PushRunAssetTmTextUnit createPushRunAssetTmTextUnit(
      PushRunAsset pushRunAsset, TMTextUnit tmTextUnit) {
    PushRunAssetTmTextUnit pushRunAssetTmTextUnit = new PushRunAssetTmTextUnit();
    pushRunAssetTmTextUnit.setPushRunAsset(pushRunAsset);
    pushRunAssetTmTextUnit.setTmTextUnit(tmTextUnit);

    return pushRunAssetTmTextUnitRepository.save(pushRunAssetTmTextUnit);
  }
}
