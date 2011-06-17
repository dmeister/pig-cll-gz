/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.pig.piggybank.test.storage.avro;

import org.apache.avro.Schema;
import org.apache.pig.piggybank.storage.avro.AvroStorageUtils;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class TestAvroStorageUtils {

    // Common elements of test records
    private final String TYPE_RECORD = "{ \"type\" : \"record\", ";
    private final String NAME_NODE   =   "\"name\": \"Node\" , ";
    private final String FIELDS_VALUE = " \"fields\": [ { \"name\": \"value\", \"type\":\"int\"}, ";

    public final String RECORD_BEGINNING = TYPE_RECORD + NAME_NODE + FIELDS_VALUE;

    @Test
    public void canIdentifyRecursiveRecords() throws IOException {
        final String str1 = RECORD_BEGINNING +
        		                     "{ \"name\": \"next\", \"type\": [\"null\", \"Node\"] } ] }";
        Schema s = Schema.parse(str1);
        assertTrue(AvroStorageUtils.containsRecursiveRecord(s));

        final String str2 = "{\"type\": \"array\", \"items\": "  + str1 + "}";
        s = Schema.parse(str2);
        assertTrue(AvroStorageUtils.containsRecursiveRecord(s));

        final String str3 ="[\"null\", " + str2 + "]";
        s = Schema.parse(str3);
        assertTrue(AvroStorageUtils.containsRecursiveRecord(s));
        
    }

    @Test
    public void canIdentifyNonRecursiveRecords() throws IOException {
        final String non = RECORD_BEGINNING + "{ \"name\": \"next\", \"type\": [\"null\", \"string\"] } ] }";
        assertFalse(AvroStorageUtils.containsRecursiveRecord(Schema.parse(non)));
        
        final String s1 =
            "{ \"type\":\"record\", \"name\":\"Event\", " +
               "\"fields\":[ " +
                       "{\"name\":\"f1\", " +
                         "\"type\":{ \"type\":\"record\",\"name\":\"Entity\", " +
                                          "\"fields\":[{\"name\":\"type\", \"type\": \"string\"}," +
                                                             "{\"name\":\"value\",\"type\": \"int\"}] " +
                                         "} }, " +
                       " {\"name\":\"f2\",\"type\": \"Entity\"}, " +
                       " {\"name\":\"f3\",\"type\": \"Entity\"} " +
                       "] }";
            Schema schema1 = Schema.parse(s1);
            assertFalse(AvroStorageUtils.containsRecursiveRecord(schema1));

    }

     @Test
     public void testGenericUnion() throws IOException {

        final String str1 = "[ \"string\", \"int\", \"boolean\"  ]";
        Schema s = Schema.parse(str1);
        assertTrue(AvroStorageUtils.containsGenericUnion(s));

        final String str2 = "[ \"string\", \"int\", \"null\"  ]";
        s = Schema.parse(str2);
        assertTrue(AvroStorageUtils.containsGenericUnion(s));

        final String str3 = "[ \"string\", \"null\"  ]";
        s = Schema.parse(str3);
        assertFalse(AvroStorageUtils.containsGenericUnion(s));
        Schema realSchema = AvroStorageUtils.getAcceptedType(s);
        assertEquals(AvroStorageUtils.StringSchema, realSchema);

        final String str4 =  "{\"type\": \"array\", \"items\": "  + str2 + "}";
        s = Schema.parse(str4);
        assertTrue(AvroStorageUtils.containsGenericUnion(s));
        try {
            realSchema = AvroStorageUtils.getAcceptedType(s);
            fail("\"Should throw a runtime exception when trying to get accepted type from a unacceptable union");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Cannot call this function on a unacceptable union"));
        }

        final String str5 = RECORD_BEGINNING +
                                 "{ \"name\": \"next\", \"type\": [\"null\", \"int\"] } ] }";
        s = Schema.parse(str5);
        assertFalse(AvroStorageUtils.containsGenericUnion(s));

        final String str6 = RECORD_BEGINNING +
                                 "{ \"name\": \"next\", \"type\": [\"string\", \"int\"] } ] }";
        s = Schema.parse(str6);
        assertTrue(AvroStorageUtils.containsGenericUnion(s));

        final String str7 = "[ \"string\"  ]"; /*union with one type*/
        s = Schema.parse(str7);
        assertFalse(AvroStorageUtils.containsGenericUnion(s));
        realSchema = AvroStorageUtils.getAcceptedType(s);
        assertEquals(AvroStorageUtils.StringSchema, realSchema);

        final String str8 = "[  ]"; /*union with no type*/
        s = Schema.parse(str8);
        assertFalse(AvroStorageUtils.containsGenericUnion(s));
        realSchema = AvroStorageUtils.getAcceptedType(s);
        assertNull(realSchema);
    }

}
