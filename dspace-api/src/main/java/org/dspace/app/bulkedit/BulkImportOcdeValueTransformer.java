/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class BulkImportOcdeValueTransformer implements BulkImportValueTransformer {

    private static final String PATTERN = "^https.*[#](\\d[.]\\d\\d[.]\\d\\d)$";

    private String authorityName;

    @Autowired
    private ChoiceAuthorityService cas;

    @Override
    public MetadataValueVO transform(Context context, MetadataValueVO metadataValue) {
        String key = getKey(metadataValue);
        if (StringUtils.isNotBlank(key)) {
            ChoiceAuthority authority = cas.getChoiceAuthorityByAuthorityName(this.authorityName);
            Choice choice = authority.getChoice(key, Item.ANY);
            if (Objects.nonNull(choice)) {
                return new MetadataValueVO(choice.value, key);
            }
        }
        return metadataValue;
    }

    private String getKey(MetadataValueVO metadataValue) {
        Pattern pattern = Pattern.compile(PATTERN);
        Matcher matcher = pattern.matcher(metadataValue.getValue());
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return StringUtils.EMPTY;
    }

    public String getAuthorityName() {
        return authorityName;
    }

    public void setAuthorityName(String authorityName) {
        this.authorityName = authorityName;
    }

}