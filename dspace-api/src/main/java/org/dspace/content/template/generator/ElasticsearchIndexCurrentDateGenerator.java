/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.template.generator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * Generates a value based on current date and format provided.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchIndexCurrentDateGenerator implements ElasticsearchIndexTemplateValueGenerator {

    private static final String REGEX = "(.*)\\-\\{DATE.([y-yY-Y]{2,4})}$";

    @Override
    public String generator(String index) {
        Pattern pattern = Pattern.compile(REGEX);
        Matcher matcher = pattern.matcher(index);
        if (matcher.find()) {
            String prefix = matcher.group(1);
            String suffix = matcher.group(2);
            SimpleDateFormat sdf = new SimpleDateFormat(suffix);
            String dataStr = sdf.format(new Date());
            return prefix + "-" + dataStr;
        }
        return StringUtils.EMPTY;
    }

    @Override
    public String getRegex() {
        return REGEX;
    }

}