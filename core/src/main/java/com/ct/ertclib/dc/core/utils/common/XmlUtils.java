/*
 * Copyright 2025-China Telecom Research Institute.
 * All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ct.ertclib.dc.core.utils.common;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight XML serialization utility replacing XStream.
 * Supports the specific structure used by DataChannelAppInfo / DataChannelApp / DataChannel:
 * <pre>
 * &lt;DataChannelAppInfo&gt;
 *   &lt;DataChannelApp appId="..."&gt;
 *     &lt;DataChannel dcId="..."&gt;
 *       &lt;DcLabel&gt;...&lt;/DcLabel&gt;
 *       &lt;QosHint&gt;...&lt;/QosHint&gt;
 *       ...
 *     &lt;/DataChannel&gt;
 *   &lt;/DataChannelApp&gt;
 * &lt;/DataChannelAppInfo&gt;
 * </pre>
 *
 * Field-to-XML mapping rules (matching the former XStream annotations):
 * - Root class name → root tag name
 * - Fields annotated as attributes → XML attributes
 * - Fields annotated as implicit collections → repeated child elements (no wrapper tag)
 * - Other fields → child elements with tag name = field name (or alias)
 */
public class XmlUtils {

    private static final String TAG = "XmlUtils";

    /**
     * Parse XML string into the specified class using XmlPullParser.
     * The classes array is kept for API compatibility but not needed by this implementation.
     */
    public static <T> T parseXml(String xml, Class[] classes, Class<T> cls) {
        if (xml == null || xml.isEmpty()) {
            return null;
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));
            return parseObject(parser, cls);
        } catch (XmlPullParserException | IOException e) {
            android.util.Log.e(TAG, "parseXml failed: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Serialize an object to XML string using XmlSerializer.
     * The classes array is kept for API compatibility but not needed by this implementation.
     */
    public static String toXml(Object obj, Class[] classes) {
        if (obj == null) {
            return "";
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlSerializer serializer = factory.newSerializer();
            StringWriter writer = new StringWriter();
            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", null);
            serializeObject(serializer, obj, obj.getClass().getSimpleName());
            serializer.endDocument();
            return writer.toString();
        } catch (IOException | XmlPullParserException e) {
            android.util.Log.e(TAG, "toXml failed: " + e.getMessage(), e);
            return "";
        }
    }

    // ==================== Parsing ====================

    @SuppressWarnings("unchecked")
    private static <T> T parseObject(XmlPullParser parser, Class<T> cls)
            throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();
        // Find the first START_TAG
        while (eventType != XmlPullParser.START_TAG && eventType != XmlPullParser.END_DOCUMENT) {
            eventType = parser.next();
        }
        if (eventType == XmlPullParser.END_DOCUMENT) {
            return null;
        }
        // parser is now at the root START_TAG
        return (T) parseElement(parser, cls);
    }

    private static Object parseElement(XmlPullParser parser, Class<?> cls)
            throws XmlPullParserException, IOException {
        try {
            Object instance = cls.getDeclaredConstructor().newInstance();
            String tagName = parser.getName();

            // Process attributes
            for (Field field : getAllFields(cls)) {
                if (isAttributeField(field)) {
                    String attrValue = parser.getAttributeValue(null, field.getName());
                    if (attrValue != null) {
                        setFieldValue(instance, field, attrValue);
                    }
                }
            }

            // Process child elements
            int eventType = parser.next();
            while (eventType != XmlPullParser.END_TAG) {
                if (eventType == XmlPullParser.START_TAG) {
                    String childTag = parser.getName();
                    Field matchedField = findFieldByTag(cls, childTag);
                    if (matchedField != null) {
                        if (isImplicitCollectionField(matchedField)) {
                            // Parse the child element as the collection element type
                            Class<?> elementType = getCollectionElementType(matchedField);
                            Object childObj = parseElement(parser, elementType);
                            addToCollection(instance, matchedField, childObj);
                        } else if (isComplexField(matchedField)) {
                            Object childObj = parseElement(parser, matchedField.getType());
                            setFieldValue(instance, matchedField, childObj);
                        } else {
                            String text = parser.nextText();
                            setFieldValue(instance, matchedField, text);
                        }
                    } else {
                        // Unknown element — skip (ignoreUnknownElements behavior)
                        skipSubTree(parser);
                    }
                }
                eventType = parser.next();
            }
            return instance;
        } catch (Exception e) {
            android.util.Log.e(TAG, "parseElement failed for " + cls.getName() + ": " + e.getMessage(), e);
            return null;
        }
    }

    private static void skipSubTree(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        int depth = 1;
        while (depth > 0) {
            int event = parser.next();
            if (event == XmlPullParser.START_TAG) depth++;
            else if (event == XmlPullParser.END_TAG) depth--;
        }
    }

    // ==================== Serialization ====================

    private static void serializeObject(XmlSerializer serializer, Object obj, String tagName)
            throws IOException {
        serializer.startTag(null, tagName);
        Class<?> cls = obj.getClass();

        // Write attributes first
        for (Field field : getAllFields(cls)) {
            if (isAttributeField(field)) {
                Object value = getFieldValue(obj, field);
                if (value != null) {
                    serializer.attribute(null, field.getName(), String.valueOf(value));
                }
            }
        }

        // Write child elements
        for (Field field : getAllFields(cls)) {
            if (isAttributeField(field)) continue;
            if (isImplicitCollectionField(field)) {
                List<?> list = (List<?>) getFieldValue(obj, field);
                if (list != null) {
                    Class<?> elementType = getCollectionElementType(field);
                    String elementTag = elementType.getSimpleName();
                    for (Object item : list) {
                        if (item != null) {
                            serializeObject(serializer, item, elementTag);
                        }
                    }
                }
            } else if (isComplexField(field)) {
                Object child = getFieldValue(obj, field);
                if (child != null) {
                    serializeObject(serializer, child, field.getName());
                }
            } else {
                Object value = getFieldValue(obj, field);
                if (value != null) {
                    String tag = getXmlElementName(field);
                    serializer.startTag(null, tag);
                    serializer.text(String.valueOf(value));
                    serializer.endTag(null, tag);
                }
            }
        }

        serializer.endTag(null, tagName);
    }

    // ==================== Reflection helpers ====================

    private static List<Field> getAllFields(Class<?> cls) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = cls;
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                f.setAccessible(true);
                fields.add(f);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    // ==================== Field-to-XML mapping ====================

    /**
     * Explicit field-name → XML tag-name overrides for cases where the
     * capitalize-first-letter rule does not match the original XStream alias.
     */
    private static final Map<String, String> FIELD_TAG_OVERRIDES = new HashMap<>();
    static {
        // subProtocol field was aliased to "Subprotocol" (lowercase 'p') by XStream
        FIELD_TAG_OVERRIDES.put("subProtocol", "Subprotocol");
    }

    /**
     * Fields that should be serialized as XML attributes (not child elements).
     * Matches the former @XStreamAsAttribute annotations.
     */
    private static final Map<String, Boolean> ATTRIBUTE_FIELDS = new HashMap<>();
    static {
        // DataChannelApp
        ATTRIBUTE_FIELDS.put("appId", true);
        // DataChannel
        ATTRIBUTE_FIELDS.put("dcId", true);
    }

    /**
     * Fields that are implicit collections (no wrapper tag, repeated child elements).
     * Matches the former @XStreamImplicit annotations.
     */
    private static final Map<String, Boolean> IMPLICIT_COLLECTION_FIELDS = new HashMap<>();
    static {
        // DataChannelApp
        IMPLICIT_COLLECTION_FIELDS.put("dataChannelList", true);
    }

    /**
     * Complex (non-primitive, non-String, non-List) fields that should be
     * recursively serialized as child objects.
     */
    private static boolean isComplexField(Field field) {
        Class<?> type = field.getType();
        return !type.isPrimitive()
                && !type.equals(String.class)
                && !type.equals(Integer.class)
                && !type.equals(Long.class)
                && !type.equals(Boolean.class)
                && !type.equals(Double.class)
                && !type.equals(Float.class)
                && !type.equals(int.class)
                && !type.equals(long.class)
                && !type.equals(boolean.class)
                && !type.equals(double.class)
                && !type.equals(float.class)
                && !type.equals(List.class);
    }

    private static boolean isAttributeField(Field field) {
        return ATTRIBUTE_FIELDS.getOrDefault(field.getName(), false);
    }

    private static boolean isImplicitCollectionField(Field field) {
        return IMPLICIT_COLLECTION_FIELDS.getOrDefault(field.getName(), false);
    }

    private static String getXmlElementName(Field field) {
        String override = FIELD_TAG_OVERRIDES.get(field.getName());
        if (override != null) {
            return override;
        }
        return capitalizeFirst(field.getName());
    }

    private static String capitalizeFirst(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Find a field matching the XML tag name.
     * Checks: explicit override → capitalized field name → raw field name.
     */
    private static Field findFieldByTag(Class<?> cls, String tag) {
        for (Field field : getAllFields(cls)) {
            String xmlName = getXmlElementName(field);
            if (xmlName.equals(tag)) {
                return field;
            }
            if (field.getName().equals(tag)) {
                return field;
            }
        }
        return null;
    }

    private static Class<?> getCollectionElementType(Field field) {
        // For our specific case, DataChannelApp.dataChannelList contains DataChannel
        // We infer from the field name pattern
        String fieldName = field.getName();
        if (fieldName.equals("dataChannelList")) {
            return com.ct.ertclib.dc.core.data.miniapp.DataChannel.class;
        }
        // Fallback: try generic type
        java.lang.reflect.ParameterizedType genericType =
                (java.lang.reflect.ParameterizedType) field.getGenericType();
        return (Class<?>) genericType.getActualTypeArguments()[0];
    }

    private static void addToCollection(Object instance, Field field, Object item)
            throws Exception {
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) field.get(instance);
        if (list == null) {
            list = new ArrayList<>();
            field.set(instance, list);
        }
        if (item != null) {
            list.add(item);
        }
    }

    private static void setFieldValue(Object instance, Field field, Object value)
            throws Exception {
        field.setAccessible(true);
        field.set(instance, value);
    }

    private static Object getFieldValue(Object instance, Field field) {
        try {
            field.setAccessible(true);
            return field.get(instance);
        } catch (Exception e) {
            return null;
        }
    }
}
