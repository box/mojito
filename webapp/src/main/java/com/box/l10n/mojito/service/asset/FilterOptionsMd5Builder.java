package com.box.l10n.mojito.service.asset;

import com.google.common.base.Joiner;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FilterOptionsMd5Builder {

    public String md5(List<String> filterOptions) {
        String joined = filterOptions == null ? "" : Joiner.on("").join(filterOptions);
        return DigestUtils.md5Hex(joined);
    }
}
