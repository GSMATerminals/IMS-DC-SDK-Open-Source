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
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight XML serialization utility replacing XStream.
 * <p>
 * Uses {@link XmlAlias}, {@link XmlAttribute}, {@link XmlImplicit} annotations
 * on data classes to control the XML mapping. No external dependencies.
 * <p>
 * Behavior matches the former XStream usage:
 * <ul>
 *   <li>Class name (or {@code @XmlAlias} on class) → root tag name</li>
 *   <li>{@code @XmlAttribute} fields → XML attributes</li>
 *   <li>{@code @XmlImplicit} List fields → repeated child elements (no wrapper tag)</li>
 *   <li>{@code @XmlAlias} on fields → custom child tag name</li>
 *   <li>Other fields → child elements, tag = capitalized field name</li>
 *   <li>null fields are skipped (not emitted)</li>
 *   <li>Unknown elements during parsing are ignored</li>
 * </ul>
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
        } catch (Exception e) {
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
            serializeObject(serializer, obj, getClassName(obj.getClass()));
            serializer.endDocument();
            return writer.toString();
        } catch (Exception e) {
            android.util.Log.e(TAG, "toXml failed: " + e.getMessage(), e);
            return "";
        }
    }

    // ==================== Parsing ====================

    @SuppressWarnings("unchecked")
    private static <T> T parseObject(XmlPullParser parser, Class<T> cls)
            throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.START_TAG && eventType != XmlPullParser.END_DOCUMENT) {
            eventType = parser.next();
        }
        if (eventType == XmlPullParser.END_DOCUMENT) {
            return null;
        }
        return (T) parseElement(parser, cls);
    }

    private static Object parseElement(XmlPullParser parser, Class<?> cls)
            throws XmlPullParserException, IOException {
        try {
            Object instance = cls.getDeclaredConstructor().newInstance();

            // Process attributes
            for (Field field : getAllFields(cls)) {
                if (isAttributeField(field)) {
                    String attrValue = parser.getAttributeValue(null, getFieldName(field));
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
                    serializer.attribute(null, getFieldName(field), String.valueOf(value));
                }
            }
        }

        // Write child elements (in declaration order)
        for (Field field : getAllFields(cls)) {
            if (isAttributeField(field)) continue;
            if (isImplicitCollectionField(field)) {
                List<?> list = (List<?>) getFieldValue(obj, field);
                if (list != null) {
                    String itemTag = getItemFieldName(field);
                    for (Object item : list) {
                        if (item != null) {
                            serializeObject(serializer, item, itemTag);
                        }
                    }
                }
            } else if (isComplexField(field)) {
                Object child = getFieldValue(obj, field);
                if (child != null) {
                    serializeObject(serializer, child, getFieldXmlName(field));
                }
            } else {
                Object value = getFieldValue(obj, field);
                if (value != null) {
                    String tag = getFieldXmlName(field);
                    serializer.startTag(null, tag);
                    serializer.text(String.valueOf(value));
                    serializer.endTag(null, tag);
                }
                // null fields are skipped (matching XStream behavior)
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

    // ==================== Annotation-based mapping ====================

    /** Returns the XML name for a class: @XmlAlias value or simple name. */
    private static String getClassName(Class<?> cls) {
        XmlAlias alias = cls.getAnnotation(XmlAlias.class);
        return alias != null ? alias.value() : cls.getSimpleName();
    }

    /** Returns the XML attribute name for a field: @XmlAlias value or field name. */
    private static String getFieldName(Field field) {
        XmlAlias alias = field.getAnnotation(XmlAlias.class);
        return alias != null ? alias.value() : field.getName();
    }

    /** Returns the XML element tag name for a field: @XmlAlias value or capitalized field name. */
    private static String getFieldXmlName(Field field) {
        XmlAlias alias = field.getAnnotation(XmlAlias.class);
        if (alias != null) {
            return alias.value();
        }
        return capitalizeFirst(field.getName());
    }

    /** Returns the element tag name for an implicit collection field. */
    private static String getItemFieldName(Field field) {
        XmlImplicit implicit = field.getAnnotation(XmlImplicit.class);
        if (implicit != null && !implicit.itemFieldName().isEmpty()) {
            return implicit.itemFieldName();
        }
        // Default: element type's class name (via @XmlAlias or simple name)
        Class<?> elementType = getCollectionElementType(field);
        return getClassName(elementType);
    }

    private static boolean isAttributeField(Field field) {
        return field.isAnnotationPresent(XmlAttribute.class);
    }

    private static boolean isImplicitCollectionField(Field field) {
        return field.isAnnotationPresent(XmlImplicit.class);
    }

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

    /**
     * Find a field matching the XML tag name.
     * Checks: @XmlAlias value → capitalized field name → raw field name
     *         → implicit collection element type name.
     */
    private static Field findFieldByTag(Class<?> cls, String tag) {
        for (Field field : getAllFields(cls)) {
            if (getFieldXmlName(field).equals(tag)) {
                return field;
            }
            if (field.getName().equals(tag)) {
                return field;
            }
            if (isImplicitCollectionField(field)) {
                String itemTag = getItemFieldName(field);
                if (itemTag.equals(tag)) {
                    return field;
                }
            }
        }
        return null;
    }

    private static Class<?> getCollectionElementType(Field field) {
        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
        return (Class<?>) genericType.getActualTypeArguments()[0];
    }

    private static String capitalizeFirst(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    // ==================== Field value access ====================

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
