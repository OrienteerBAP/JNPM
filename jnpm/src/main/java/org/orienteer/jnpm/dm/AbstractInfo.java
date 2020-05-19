package org.orienteer.jnpm.dm;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;

public abstract class AbstractInfo {
	
	private Map<String, Object> details = new LinkedHashMap<>();
	 
    @JsonAnySetter
    public void setDetail(String key, Object value) {
        details.put(key, value);
    }
    
    public Object getDetail(String key) {
    	return details.get(key);
    }
    
    public Map<String, Object> getDetails() {
		return details;
	}
    
}
