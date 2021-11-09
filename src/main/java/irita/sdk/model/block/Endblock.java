/**
 * Copyright 2021 jb51.net
 */
package irita.sdk.model.block;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

/**
 * Auto-generated: 2021-11-01 9:42:4
 *
 * @author jb51.net (i@jb51.net)
 * @website http://tools.jb51.net/code/json2javabean
 */
public class Endblock {

    private List<String> events;
    @JSONField(name = "validator_updates")
    private String validatorUpdates;

    public void setEvents(List<String> events) {
        this.events = events;
    }

    public List<String> getEvents() {
        return events;
    }

    public void setValidatorUpdates(String validatorUpdates) {
        this.validatorUpdates = validatorUpdates;
    }

    public String getValidatorUpdates() {
        return validatorUpdates;
    }

}