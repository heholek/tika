/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.metadata;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

/**
 * A multi-valued metadata container.
 */
public class Metadata implements CreativeCommons, DublinCore, Geographic, HttpHeaders,
        Message, MSOffice, ClimateForcast, TIFF, TikaMetadataKeys, TikaMimeKeys {

    /**
     * A map of all metadata attributes.
     */
    private Map<String, String[]> metadata = null;
    
    /**
     * The ISO-8601 format string we use for Dates.
     * All dates are represented as UTC
     */
    private SimpleDateFormat iso8601Format = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'", new DateFormatSymbols(Locale.US));
    private SimpleDateFormat iso8601SpaceFormat = new SimpleDateFormat(
	           "yyyy-MM-dd' 'HH:mm:ss'Z'", new DateFormatSymbols(Locale.US));
    {
	iso8601Format.setTimeZone(TimeZone.getTimeZone("UTC"));
	iso8601SpaceFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    /**
     * Some parsers will have the date as a ISO-8601 string
     *  already, and will set that into the Metadata object.
     * So we can return Date objects for these, this is the
     *  list (in preference order) of the various ISO-8601
     *  variants that we try when processing a date based
     *  property.
     */
    private SimpleDateFormat[] iso8601InputFormats = new SimpleDateFormat[] {
	// yyyy-mm-ddThh...
        iso8601Format, // UTC/Zulu
        new SimpleDateFormat(
           "yyyy-MM-dd'T'HH:mm:ssZ", new DateFormatSymbols(Locale.US)), // With timezone
        new SimpleDateFormat(
           "yyyy-MM-dd'T'HH:mm:ss", new DateFormatSymbols(Locale.US)), // Without timezone
   	// yyyy-mm-dd hh...
        iso8601SpaceFormat, // UTC/Zulu
        new SimpleDateFormat(
           "yyyy-MM-dd' 'HH:mm:ssZ", new DateFormatSymbols(Locale.US)), // With timezone
        new SimpleDateFormat(
           "yyyy-MM-dd' 'HH:mm:ss", new DateFormatSymbols(Locale.US)), // Without timezone
    };

    /**
     * Constructs a new, empty metadata.
     */
    public Metadata() {
        metadata = new HashMap<String, String[]>();
    }

    /**
     * Returns true if named value is multivalued.
     * 
     * @param name
     *          name of metadata
     * @return true is named value is multivalued, false if single value or null
     */
    public boolean isMultiValued(final String name) {
        return metadata.get(name) != null && metadata.get(name).length > 1;
    }

    /**
     * Returns an array of the names contained in the metadata.
     * 
     * @return Metadata names
     */
    public String[] names() {
        return metadata.keySet().toArray(new String[metadata.keySet().size()]);
    }

    /**
     * Get the value associated to a metadata name. If many values are assiociated
     * to the specified name, then the first one is returned.
     * 
     * @param name
     *          of the metadata.
     * @return the value associated to the specified metadata name.
     */
    public String get(final String name) {
        String[] values = metadata.get(name);
        if (values == null) {
            return null;
        } else {
            return values[0];
        }
    }

    /**
     * Returns the value (if any) of the identified metadata property.
     *
     * @since Apache Tika 0.7
     * @param property property definition
     * @return property value, or <code>null</code> if the property is not set
     */
    public String get(Property property) {
        return get(property.getName());
    }
    
    /**
     * Returns the value of the identified Integer based metadata property.
     * 
     * @since Apache Tika 0.8
     * @param property simple integer property definition
     * @return property value as a Integer, or <code>null</code> if the property is not set, or not a valid Integer
     */
    public Integer getInt(Property property) {
        if(property.getPropertyType() != Property.PropertyType.SIMPLE)
            return null;
        if(property.getValueType() != Property.ValueType.INTEGER)
            return null;
        
        String v = get(property);
        if(v == null) {
            return null;
        }
        try {
            return new Integer(v);
        } catch(NumberFormatException e) {
            return null;
        }
    }

    /**
     * Returns the value of the identified Date based metadata property.
     * 
     * @since Apache Tika 0.8
     * @param property simple date property definition
     * @return property value as a Date, or <code>null</code> if the property is not set, or not a valid Date
     */
    public Date getDate(Property property) {
        if(property.getPropertyType() != Property.PropertyType.SIMPLE)
            return null;
        if(property.getValueType() != Property.ValueType.DATE)
            return null;
        
        String v = get(property);
        if(v == null) {
            return null;
        }
        // Java doesn't like timezones in the form ss+hh:mm
        // It only likes the hhmm form, without the colon
        if(v.charAt(v.length()-3) == ':' && 
            (v.charAt(v.length()-6) == '+' ||
             v.charAt(v.length()-6) == '-')) {
            v = v.substring(0, v.length()-3) + v.substring(v.length()-2);
        }
        
        // Try several different ISO-8601 variants
        for(SimpleDateFormat format : iso8601InputFormats) {
            try {
                return format.parse(v);
            } catch(ParseException e) {}
        }
        // It isn't in a supported date format, sorry
        return null;
    }

    /**
     * Get the values associated to a metadata name.
     * 
     * @param name
     *          of the metadata.
     * @return the values associated to a metadata name.
     */
    public String[] getValues(final String name) {
        return _getValues(name);
    }

    private String[] _getValues(final String name) {
        String[] values = metadata.get(name);
        if (values == null) {
            values = new String[0];
        }
        return values;
    }

    /**
     * Add a metadata name/value mapping. Add the specified value to the list of
     * values associated to the specified metadata name.
     * 
     * @param name
     *          the metadata name.
     * @param value
     *          the metadata value.
     */
    public void add(final String name, final String value) {
        String[] values = metadata.get(name);
        if (values == null) {
            set(name, value);
        } else {
            String[] newValues = new String[values.length + 1];
            System.arraycopy(values, 0, newValues, 0, values.length);
            newValues[newValues.length - 1] = value;
            metadata.put(name, newValues);
        }
    }

    /**
     * Copy All key-value pairs from properties.
     * 
     * @param properties
     *          properties to copy from
     */
    @SuppressWarnings("unchecked")
    public void setAll(Properties properties) {
        Enumeration<String> names =
            (Enumeration<String>) properties.propertyNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            metadata.put(name, new String[] { properties.getProperty(name) });
        }
    }

    /**
     * Set metadata name/value. Associate the specified value to the specified
     * metadata name. If some previous values were associated to this name, they
     * are removed.
     * 
     * @param name
     *          the metadata name.
     * @param value
     *          the metadata value.
     */
    public void set(String name, String value) {
        metadata.put(name, new String[] { value });
    }

    /**
     * Sets the value of the identified metadata property.
     *
     * @since Apache Tika 0.7
     * @param property property definition
     * @param value    property value
     */
    public void set(Property property, String value) {
        set(property.getName(), value);
    }

    /**
     * Sets the integer value of the identified metadata property.
     *
     * @since Apache Tika 0.8
     * @param property simple integer property definition
     * @param value    property value
     */
    public void set(Property property, int value) {
        if(property.getPropertyType() != Property.PropertyType.SIMPLE)
            throw new PropertyTypeException(Property.PropertyType.SIMPLE, property.getPropertyType());
        if(property.getValueType() != Property.ValueType.INTEGER)
            throw new PropertyTypeException(Property.ValueType.INTEGER, property.getValueType());
        set(property.getName(), Integer.toString(value));
    }

    /**
     * Sets the date value of the identified metadata property.
     *
     * @since Apache Tika 0.8
     * @param property simple integer property definition
     * @param value    property value
     */
    public void set(Property property, Date date) {
        if(property.getPropertyType() != Property.PropertyType.SIMPLE)
            throw new PropertyTypeException(Property.PropertyType.SIMPLE, property.getPropertyType());
        if(property.getValueType() != Property.ValueType.DATE)
            throw new PropertyTypeException(Property.ValueType.DATE, property.getValueType());
        set(property.getName(), iso8601Format.format(date));
    }

    /**
     * Remove a metadata and all its associated values.
     * 
     * @param name
     *          metadata name to remove
     */
    public void remove(String name) {
        metadata.remove(name);
    }

    /**
     * Returns the number of metadata names in this metadata.
     * 
     * @return number of metadata names
     */
    public int size() {
        return metadata.size();
    }

    public boolean equals(Object o) {

        if (o == null) {
            return false;
        }

        Metadata other = null;
        try {
            other = (Metadata) o;
        } catch (ClassCastException cce) {
            return false;
        }

        if (other.size() != size()) {
            return false;
        }

        String[] names = names();
        for (int i = 0; i < names.length; i++) {
            String[] otherValues = other._getValues(names[i]);
            String[] thisValues = _getValues(names[i]);
            if (otherValues.length != thisValues.length) {
                return false;
            }
            for (int j = 0; j < otherValues.length; j++) {
                if (!otherValues[j].equals(thisValues[j])) {
                    return false;
                }
            }
        }
        return true;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        String[] names = names();
        for (int i = 0; i < names.length; i++) {
            String[] values = _getValues(names[i]);
            for (int j = 0; j < values.length; j++) {
                buf.append(names[i]).append("=").append(values[j]).append(" ");
            }
        }
        return buf.toString();
    }

}
